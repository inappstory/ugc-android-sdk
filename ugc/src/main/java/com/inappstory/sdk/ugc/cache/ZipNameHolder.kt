package com.inappstory.sdk.ugc.cache

open class ZipNameHolder {
    protected fun getZipName(url: String): String {
        val parts = url.split("/").toTypedArray()
        val fName = parts[parts.size - 1].split("\\.").toTypedArray()[0]
        val nameParts = fName.split("_").toTypedArray()
        return if (nameParts.isNotEmpty()) nameParts[0] else ""
    }
}