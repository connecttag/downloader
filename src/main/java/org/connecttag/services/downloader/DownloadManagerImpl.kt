package org.connecttag.services.downloader

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class DownloadManagerImpl @Inject constructor(
    private val okHttpClient: OkHttpClient
) : IDownloadManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _statuses = MutableStateFlow<Map<String, DownloadStatus>>(emptyMap())
    private val activeJobs = mutableMapOf<String, Job>()
    
    // Store requests for resume
    private val pendingRequests = mutableMapOf<String, DownloadRequest>()

    override fun enqueue(request: DownloadRequest) {
        pendingRequests[request.id] = request
        if (activeJobs.containsKey(request.id)) return

        val job = scope.launch {
            _statuses.update { it + (request.id to DownloadStatus.Queued) }
            performDownload(request)
        }
        activeJobs[request.id] = job
    }

    override fun pause(downloadId: String) {
        activeJobs[downloadId]?.cancel()
        activeJobs.remove(downloadId)
        val currentStatus = _statuses.value[downloadId]
        if (currentStatus is DownloadStatus.Downloading) {
            _statuses.update {
                it + (downloadId to DownloadStatus.Paused(currentStatus.progress, currentStatus.downloadedBytes, currentStatus.totalBytes))
            }
        }
    }

    override fun resume(downloadId: String) {
        val request = pendingRequests[downloadId] ?: return
        enqueue(request)
    }

    override fun cancel(downloadId: String) {
        activeJobs[downloadId]?.cancel()
        activeJobs.remove(downloadId)
        _statuses.update { it - downloadId }
        
        // Clean up partial file
        pendingRequests[downloadId]?.let {
            val file = File(it.targetPath)
            if (file.exists()) file.delete()
        }
        pendingRequests.remove(downloadId)
    }

    override fun getStatus(downloadId: String): StateFlow<DownloadStatus> {
        return _statuses.map { it[downloadId] ?: DownloadStatus.Idle }
            .stateIn(scope, SharingStarted.WhileSubscribed(5000), DownloadStatus.Idle)
    }

    override fun getAllStatuses(): StateFlow<Map<String, DownloadStatus>> = _statuses.asStateFlow()

    private suspend fun performDownload(request: DownloadRequest) {
        val file = File(request.targetPath)
        val existingSize = if (file.exists()) file.length() else 0L
        val startTime = System.currentTimeMillis()

        try {
            val okHttpRequest = Request.Builder()
                .url(request.url)
                .apply {
                    request.headers.forEach { (key, value) -> addHeader(key, value) }
                    if (existingSize > 0) {
                        addHeader("Range", "bytes=$existingSize-")
                    }
                }
                .build()

            okHttpClient.newCall(okHttpRequest).execute().use { response ->
                if (!response.isSuccessful && response.code != 206) {
                    _statuses.update { it + (request.id to DownloadStatus.Failed("Server error: ${response.code}")) }
                    return
                }

                val body = response.body
                if (body == null) {
                    _statuses.update { it + (request.id to DownloadStatus.Failed("Empty response body")) }
                    return
                }

                val contentLength = body.contentLength()
                val totalBytes = if (response.code == 206) contentLength + existingSize else contentLength
                val inputStream = body.byteStream()

                file.parentFile?.mkdirs()

                val append = response.code == 206
                FileOutputStream(file, append).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = existingSize
                    var lastUpdateTime = 0L

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        yield()
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastUpdateTime >= 500) {
                            val progress = if (totalBytes > 0) ((totalRead * 100) / totalBytes).toInt() else -1
                            val speed = calculateSpeed(totalRead - existingSize, startTime)
                            val eta = calculateEta(totalRead, totalBytes, speed)
                            
                            _statuses.update {
                                it + (request.id to DownloadStatus.Downloading(progress, totalRead, totalBytes, speed, eta))
                            }
                            lastUpdateTime = currentTime
                        }
                    }
                }

                _statuses.update { it + (request.id to DownloadStatus.Completed(file.absolutePath)) }
                pendingRequests.remove(request.id)
            }

        } catch (e: Exception) {
            if (e !is CancellationException) {
                _statuses.update { it + (request.id to DownloadStatus.Failed(e.message ?: "Unknown error")) }
            }
        } finally {
            activeJobs.remove(request.id)
        }
    }

    private fun calculateSpeed(downloadedSinceStart: Long, startTime: Long): Long {
        val elapsedTime = System.currentTimeMillis() - startTime
        return if (elapsedTime > 0) (downloadedSinceStart * 1000) / elapsedTime else 0
    }

    private fun calculateEta(downloaded: Long, total: Long, speed: Long): Long {
        return if (speed > 0 && total > downloaded) (total - downloaded) / speed else 0
    }
}
