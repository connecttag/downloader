package org.connecttag.services.downloader

public data class DownloadRequest(
    val id: String,
    val url: String,
    val targetPath: String,
    val headers: Map<String, String> = emptyMap(),
    val description: String = "",
    val mimeType: String? = null,
    val allowMetered: Boolean = true
)
