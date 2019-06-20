package com.apigj.android.httpframework.utils

import android.content.Context
import android.text.TextUtils
import com.apigj.android.httpframework.Keep
import java.io.*

@Keep
class CacheUtils {

    companion object {
        fun getCachePath(file: File): String {
            var cachePath = file.absolutePath
            if (!cachePath.endsWith("/")) {
                cachePath = "$cachePath/"
            }
            return cachePath + "cache/"
        }

        fun getPartialPath(file: File): String {
            var dataPath = file.absolutePath
            if (!dataPath.endsWith("/")) {
                dataPath = "$dataPath/"
            }
            return dataPath + "partial/"
        }

        @Keep
        fun getTempFileDir(context: Context): File {
            var f = context.externalCacheDir
            if (f == null) {
                f = context.cacheDir
            }
            return f
        }

        fun moveFile(srcPath: String, desPath: String): Boolean {
            if (TextUtils.isEmpty(srcPath)) {
                return false
            }
            val srcFile = File(srcPath)
            if (!srcFile.exists()) {
                return false
            }
            if (TextUtils.isEmpty(desPath)) {
                return false
            }
            if (srcPath == desPath) {
                return false
            }
            val desFile = File(desPath)
            if (!desFile.exists()) {
                desFile.parentFile.mkdirs()
            } else {
                desFile.delete()
            }
            srcFile.renameTo(desFile)
            return true
        }

        fun copyFile(srcPath: String, desPath: String): Boolean {
            if (TextUtils.isEmpty(srcPath)) {
                return false
            }
            val srcFile = File(srcPath)
            if (!srcFile.exists()) {
                return false
            }
            if (TextUtils.isEmpty(desPath)) {
                return false
            }
            if (srcPath == desPath) {
                return false
            }
            val desFile = File(desPath)
            var fileOutputStream: FileOutputStream? = null
            var fileInputStream: FileInputStream? = null
            try {
                if (!desFile.exists()) {
                    desFile.parentFile.mkdirs()
                } else {
                    desFile.delete()
                }
                desFile.createNewFile()
                fileOutputStream = FileOutputStream(desFile)
                fileInputStream = FileInputStream(srcPath)
                val buffer = ByteArray(1024)
                var readdata = 0
                while (true) {
                    readdata = fileInputStream.read(buffer)
                    if (readdata == -1) {
                        break
                    }
                    fileOutputStream.write(buffer, 0, readdata)
                }
                return true
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                }
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                }
            }
            return false
        }

        fun deleteFile(filePath: String): Boolean {
            val file = File(filePath)
            return file.delete()
        }

        fun fileExists(filePath: String): Boolean {
            val file = File(filePath)
            return file.exists()
        }

        fun isDownloadFinished(file: File, tempFilePath: String): Boolean {
            return !tempFilePath.startsWith(getPartialPath(file))
        }

        fun getTempName(url: String): String {
            val s = url.lastIndexOf('/') + 1
            val e = url.lastIndexOf('.')
            val q = url.lastIndexOf('?')
            var name: String? = null
            var surfix = ""
            if (s >= 0 && s < e) {
                name = url.substring(s, e)
                if (q == -1) {
                    surfix = url.substring(e + 1)
                } else {
                    surfix = url.substring(e + 1, q)
                }
            } else {
                name = url.substring(s)
            }
            return name + System.currentTimeMillis() + if (surfix.isEmpty()) {
                ""
            } else {
                ".$surfix"
            }
        }

        /*
 * Java文件操作 获取文件扩展名
 *
 *  Created on: 2011-8-2
 *      Author: blueeagle
 */
        
        fun getExtensionName(filename: String?): String {
            if (filename != null && filename.isNotEmpty()) {
                val dot = filename.lastIndexOf('.')
                if (dot > -1 && dot < filename.length - 1) {
                    return filename.substring(dot + 1)
                }
            }
            return ""
        }


        /*
     * Java文件操作 获取不带扩展名的文件名
     *
     *  Created on: 2011-8-2
     *      Author: blueeagle
     */
        fun getFileNameNoEx(filename: String?): String? {
            if (filename != null && filename.isNotEmpty()) {
                val dot = filename.lastIndexOf('.')
                if (dot > -1 && dot < filename.length) {
                    return filename.substring(0, dot)
                }
            }
            return filename
        }


    }
}