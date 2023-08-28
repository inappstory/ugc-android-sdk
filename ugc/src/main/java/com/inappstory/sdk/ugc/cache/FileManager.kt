package com.inappstory.sdk.ugc.cache

import java.io.*
import java.security.MessageDigest

class FileManager {
    fun deleteRecursive(fileOrDirectory: File): Boolean {
        var res = true
        if (fileOrDirectory.isDirectory) {
            for (child in fileOrDirectory.listFiles()) {
                res = res and deleteRecursive(child)
            }
        }
        res = res and fileOrDirectory.delete()
        return res
    }

    fun getFileSHA1(file: File?): String {
        return try {
            val md = MessageDigest.getInstance("SHA1")
            val fis = FileInputStream(file)
            val dataBytes = ByteArray(1024)
            var nread = 0
            while (fis.read(dataBytes).also { nread = it } != -1) {
                md.update(dataBytes, 0, nread)
            }
            val mdbytes = md.digest()
            val sb = StringBuffer("")
            for (i in mdbytes.indices) {
                sb.append(
                    ((mdbytes[i].toInt() and 0xff) + 0x100).toString(16)
                        .substring(1)
                )
            }
            sb.toString()
        } catch (ex: Exception) {
            ""
        }
    }



    fun getStringFromFile(fl: File): String {
        val fin = FileInputStream(fl)
        val ret = convertStreamToString(fin)
        fin.close()
        return ret
    }

    private fun convertStreamToString(inputStream: InputStream): String {
        val reader = BufferedReader(InputStreamReader(inputStream))
        val sb = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            sb.append(line).append("\n")
        }
        reader.close()
        return sb.toString()
    }
}