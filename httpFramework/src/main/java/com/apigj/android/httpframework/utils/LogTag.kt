package com.apigj.android.httpframework.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import android.widget.Toast
import java.io.*

class LogTag {

    companion object {
        //	public final static String t = "";
        private var isToast = false

        val TAG = "fuguLib"

//	public static void setEncryptKey(String encryptKey){
//		t = encryptKey;
//	}

        fun setToast(isToast: Boolean) {
            LogTag.isToast = isToast
        }

        private fun isToast(): Boolean {
            return isToast
        }

        private fun prettyArray(array: Array<*>): String {
            try {
                if (array.size == 0) {
                    return "[]"
                }
                val sb = StringBuilder("[")
                val len = array.size - 1
                for (i in 0 until len) {
                    sb.append(array[i])
                    sb.append(", ")
                }
                sb.append(array[len])
                sb.append("]")
                return sb.toString()
            } catch (e: Exception) {
                return "null"
            }

        }

        private fun logFormat(format: String, vararg args: Any): String {
            for (i in args.indices) {
                if (args[i] is Array<*>) {
//                    args[i] = prettyArray(args[i] as Array<*>)
                }
            }
            try {
                var s = String.format(format, *args)
                s = "[" + Thread.currentThread().id + "] " + s
                return s
            } catch (e: Exception) {
                return format
            }

        }

        /**
         * tag is TAG == "cheyi"
         *
         * @param format
         * @param args
         */
        fun debug(format: String, vararg args: Any) {
            if (FrameworkConfiguration.isPublish()) {
                return
            }
            Log.d(TAG, logFormat(format, *args))
        }

        fun debug(format: String) {
            if (FrameworkConfiguration.isPublish()) {
                return
            }
            Log.d(TAG, logFormat(format))
        }

        /**
         * tag is TAG == "cheyi"
         *
         * @param format
         * @param args
         */
        fun warn(format: String, vararg args: Any) {
            if (FrameworkConfiguration.isPublish()) {
                return
            }
            Log.w(TAG, logFormat(format, *args))
        }

        /**
         * tag is TAG == "cheyi"
         *
         * @param format
         * @param args
         */
        fun error(format: String, vararg args: Any) {
            if (FrameworkConfiguration.isPublish()) {
                return
            }
        }

        /**
         * tag is Custom
         *
         * @param TAG
         * @param format
         * @param args
         */
        fun d(TAG: String, format: String, vararg args: Any) {
            if (FrameworkConfiguration.isPublish()) {
                return
            }
            Log.d(TAG, logFormat(format, *args))
        }

        fun d(context: Context?, TAG: String, msg: String) {
            if (FrameworkConfiguration.isPublish()) {
                return
            }
            Log.d(TAG, msg)
            if (isToast()) {
                return
            }
            if (context != null) {
                Toast.makeText(context, "$TAG  $msg", Toast.LENGTH_LONG).show()
            }
        }

        /**
         * tag is Custom
         *
         * @param TAG
         * @param format
         * @param args
         */
        fun w(TAG: String, format: String, vararg args: Any) {
            if (FrameworkConfiguration.isPublish()) {
                return
            }
            Log.w(TAG, logFormat(format, *args))
        }

        /**
         * tag is Custom
         *
         * @param TAG
         * @param format
         * @param args
         */
        fun e(TAG: String, format: String, vararg args: Any) {
            if (FrameworkConfiguration.isPublish()) {
                return
            }
        }

        /**
         * 将字符串写入SDcard文件中
         *
         * @param filename
         * 文件名
         * @param str
         * 要写入的字符串
         */
        fun writeFile(filename: String, str: String) {
            if (FrameworkConfiguration.isPublish()) {
                return
            }
            if (Environment.MEDIA_MOUNTED != Environment
                    .getExternalStorageState() || !Environment.getExternalStorageDirectory().canWrite()
            ) {
                return
            }
            val path = (Environment.getExternalStorageDirectory().path + "/"
                    + filename)
            val cbs = str.toByteArray()
            val inSt = ByteArrayInputStream(cbs)
            try {
                val out = FileOutputStream(path, true)

                var read: Int
                val buffer = ByteArray(4096)
                do {
                    read = inSt.read(buffer)
                    if(read > 0){
                        out.write(buffer, 0, read)
                    }else {
                        break
                    }
                } while(true)
                out.close()
                inSt.close()
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }

        /**
         * 将字符串写入SDcard文件夹中
         *
         * @param folder
         * 文件夹名
         * @param filename
         * 文件名
         * @param str
         * 要写入的字符串
         */
        fun writeFile(folder: String, filename: String, str: String) {
            if (FrameworkConfiguration.isPublish()) {
                return
            }
            if (Environment.MEDIA_MOUNTED != Environment
                    .getExternalStorageState() || !Environment.getExternalStorageDirectory().canWrite()
            ) {
                return
            }
            val directory = (Environment.getExternalStorageDirectory().path
                    + "/" + folder)
            val directoryFile = File(directory)
            if (!directoryFile.exists()) {
                directoryFile.mkdir()
            }
            val path = "$directory/$filename"
            val cbs = str.toByteArray()
            val inSt = ByteArrayInputStream(cbs)
            try {
                val out = FileOutputStream(path, true)
                var read: Int
                val buffer = ByteArray(4096)
                do{
                    read = inSt.read(buffer)
                    if(read > 0){
                        out.write(buffer, 0, read)
                    }else{
                        break
                    }
                }while (true)
                out.close()
                inSt.close()
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }

        fun makeLogTag(cls: Class<*>): String {
            return TAG + "_" + cls.simpleName
        }
    }
}