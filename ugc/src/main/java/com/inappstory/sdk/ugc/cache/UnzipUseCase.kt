package com.inappstory.sdk.ugc.cache

import androidx.annotation.WorkerThread
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class UnzipUseCase(private val zipFilePath: String) {

    @WorkerThread
    fun unzip(targetDirectoryPath: String, callback: ProgressCallback?): Boolean {
        return try {
            val zipFile = File(zipFilePath)
            if (!zipFile.exists()) return false
            val zis = ZipInputStream(
                BufferedInputStream(FileInputStream(zipFile))
            )
            zis.use { zis ->
                val targetDirectory = File(targetDirectoryPath)
                if (!targetDirectory.isDirectory && !targetDirectory.mkdirs()) throw FileNotFoundException(
                    "Failed to ensure directory: " +
                            targetDirectory.absolutePath
                )
                var zeNullable: ZipEntry?
                var count: Int
                val buffer = ByteArray(8192)
                val totalLength = zipFile.length()
                var curLength: Long = 0
                while (zis.nextEntry.also { zeNullable = it } != null) {
                    val ze = zeNullable ?: continue
                    val file = File(targetDirectoryPath, ze.name)
                    val dir = if (ze.isDirectory) file else file.parentFile
                    if (!dir.isDirectory && !dir.mkdirs()) throw FileNotFoundException(
                        "Failed to ensure directory: " +
                                dir.absolutePath
                    )
                    if (ze.isDirectory) continue
                    val canonicalPath = file.canonicalPath
                    if (!canonicalPath.startsWith(targetDirectory.canonicalPath)) {
                        continue
                    }
                    val fout = FileOutputStream(file)
                    fout.use { fout ->
                        while (zis.read(buffer).also { count = it } != -1) fout.write(
                            buffer,
                            0,
                            count
                        )
                    }
                    curLength += ze.compressedSize
                    callback?.onProgress(curLength, totalLength)
                }
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}