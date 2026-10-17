package org.connecttag.services.downloader

import kotlinx.coroutines.flow.StateFlow

public interface IDownloadManager {
    /**
     * إرسال طلب تحميل جديد
     */
    public fun enqueue(request: DownloadRequest)

    /**
     * إيقاف تحميل مؤقتاً
     */
    public fun pause(downloadId: String)

    /**
     * استئناف تحميل متوقف
     */
    public fun resume(downloadId: String)

    /**
     * إلغاء التحميل وحذف الملف المؤقت
     */
    public fun cancel(downloadId: String)

    /**
     * مراقبة حالة تحميل معين
     */
    public fun getStatus(downloadId: String): StateFlow<DownloadStatus>

    /**
     * مراقبة كافة التحميلات الجارية
     */
    public fun getAllStatuses(): StateFlow<Map<String, DownloadStatus>>
}
