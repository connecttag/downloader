package org.connecttag.services.downloader

public object DownloadFileNameResolver {
    public fun getFileNameFromUri(url: String): String {
        val fullFileName = url.substring(url.lastIndexOf('/') + 1)
        val queryParamIndex = fullFileName.indexOf('?')
        return if (queryParamIndex > 0) fullFileName.substring(0, queryParamIndex) else fullFileName
    }

    public fun getFileName(url: String, fallbackName: String, fileTypeName: String): String {
        val fromUrl = getFileNameFromUri(url).trim()
        val baseName = fromUrl.ifBlank { fallbackName.trim() }
        if (baseName.isBlank()) return "download_${System.currentTimeMillis()}"

        val extension = fileTypeName.trim().lowercase().takeIf { it.isNotBlank() && it != "unknown" }
            ?: return baseName
        return if (baseName.endsWith(".$extension", ignoreCase = true)) baseName else "$baseName.$extension"
    }
}

