package org.connecttag.services.downloader

public sealed class DownloadStatus {
    public data object Idle : DownloadStatus()
    public data object Queued : DownloadStatus()
    public data class Downloading(
        val progress: Int,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val speed: Long = 0, // Bytes per second
        val eta: Long = 0 // Seconds remaining
    ) : DownloadStatus()
    public data class Paused(val progress: Int, val downloadedBytes: Long, val totalBytes: Long) : DownloadStatus()
    public data class Completed(val filePath: String) : DownloadStatus()
    public data class Failed(val error: String) : DownloadStatus()
}
