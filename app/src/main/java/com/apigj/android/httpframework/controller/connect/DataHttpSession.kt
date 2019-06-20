package com.apigj.android.httpframework.controller.connect

import android.text.TextUtils
import com.apigj.android.httpframework.controller.exceptions.*
import com.apigj.android.httpframework.controller.formatTool.MultipartFormData
import com.apigj.android.httpframework.utils.AESCipher
import com.apigj.android.httpframework.utils.FrameworkConfiguration
import java.io.*
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager

class DataHttpSession {
    companion object {
        fun httpBytesExchange(
            locale: Locale,
            urlString: String,
            sentData: ByteArray,
            eventHandler: IHttpEventHandler,
            netConfig: NetworkConfiguration
        ) {
            var sentData = sentData
            var error: Exception? = null
            var res: ByteArray? = null
            var resHead: Map<String, String>? = null

            var os: OutputStream? = null
            var gos: GZIPOutputStream? = null
            var inst: InputStream? = null
            var gis: GZIPInputStream? = null
            var daos: ByteArrayOutputStream? = null

            val url: URL
            var conn: HttpURLConnection? = null
            eventHandler.publishStatus(ServiceStatus.STARTING_CONNECT, 0)
            try {
                try {
                    if (!TextUtils.isEmpty(FrameworkConfiguration.t)) {
                        sentData = AESCipher.encrypt(FrameworkConfiguration.t.toByteArray(), sentData)
                    }
                } catch (e: Exception) {
                    throw CryptException("Cannot encrypt", sentData)
                }

                url = URL(urlString)
                if (urlString.toLowerCase().startsWith("https://")) {
                    val sc = SSLContext.getInstance("TLS")
                    sc.init(null, arrayOf(netConfig.getTrustManager()), SecureRandom())
                    HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
                    HttpsURLConnection.setDefaultHostnameVerifier(netConfig.getHostVerifier())
                    conn = url.openConnection() as HttpsURLConnection
                } else if (urlString.toLowerCase().startsWith("http://")) {
                    conn = url.openConnection() as HttpURLConnection
                }
                conn!!.requestMethod = "POST"
                conn.setRequestProperty("User-Agent", NetworkConfiguration.USER_AGENT)
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.setRequestProperty("Accept-Language", locale.language)
                if (netConfig.getGzip()) {
                    conn.setRequestProperty("Accept-Encoding", "gzip")
                }
                val enu = netConfig.getExtraHead().entries
                for (entry in enu) {
                    conn.setRequestProperty(entry.key, entry.value)
                }
                conn.readTimeout = netConfig.getTimeOut()
                conn.connectTimeout = netConfig.getTimeOut()
                conn.doOutput = true
                conn.doInput = true

                conn.connect()
                //send data
                eventHandler.publishStatus(ServiceStatus.STARTING_SEND_DATA, 0)
                os = conn.outputStream
                if (netConfig.getGzip()) {
                    gos = GZIPOutputStream(os!!)
                    gos.write(sentData)
                    gos.flush()
                } else {
                    os!!.write(sentData)
                    os.flush()
                }
                val rc = conn.responseCode
                if (rc != HttpURLConnection.HTTP_OK && rc != HttpURLConnection.HTTP_ACCEPTED && rc != HttpURLConnection.HTTP_CREATED) {
                    throw HttpCodeException(rc, conn.url.toString())
                }

                //get data
                eventHandler.publishStatus(ServiceStatus.STARTING_GET_DATA, 0)

                inst = conn.inputStream
                resHead = getHttpResponseHeader(conn)
                val gzip = resHead["Content-Encoding"]
                if (!TextUtils.isEmpty(gzip) && gzip!!.indexOf("gzip") != -1) {
                    gis = GZIPInputStream(inst!!)
                }
                var d: Int
                val buffer = ByteArray(netConfig.getTruckSize())
                daos = ByteArrayOutputStream()
                while (true) {
                    if (gis != null) {
                        d = gis.read(buffer)
                    } else {
                        d = inst!!.read(buffer)
                    }
                    if (d == -1) {
                        break
                    }
                    //                if(d != netConfig.getTruckSize()){
                    //                    byte[] tbuffer = new byte[d];
                    //                    System.arraycopy(buffer, 0, tbuffer, 0, tbuffer.length);
                    //                    buffer = tbuffer;
                    //                }
                    daos.write(buffer, 0, d)
                }
                res = daos.toByteArray()
                try {
                    if (!TextUtils.isEmpty(FrameworkConfiguration.t)) {
                        res = AESCipher.decrypt(FrameworkConfiguration.t.toByteArray(), res)
                    }
                } catch (e: Exception) {
                    throw CryptException("Cannot decrypt", res)
                }

            } catch (e: IOException) {
                error = NetBrokenException()
            } catch (e: NoSuchAlgorithmException) {
                error = e
            } catch (e: KeyManagementException) {
                error = e
            } catch (e: HttpCodeException) {
                error = e
            } catch (e: CryptException) {
                error = e
            } finally {
                try {
                    gis?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                try {
                    inst?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                try {
                    daos?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                try {
                    conn!!.disconnect()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                try {
                    gos?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                try {
                    os?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
            eventHandler.connectFinished(resHead, res, error)
        }


        fun httpGetBytes(
            locale: Locale,
            urlString: String,
            eventHandler: IHttpEventHandler,
            netConfig: NetworkConfiguration
        ) {
            var error: Exception? = null
            var res: ByteArray? = null
            var resHead: Map<String, String>? = null

            var inst: InputStream? = null
            var gis: GZIPInputStream? = null
            var daos: ByteArrayOutputStream? = null

            val url: URL
            var conn: HttpURLConnection? = null
            eventHandler.publishStatus(ServiceStatus.STARTING_CONNECT, 0)
            try {
                url = URL(urlString)
                if (urlString.toLowerCase().startsWith("https://")) {
                    val sc = SSLContext.getInstance("TLS")
                    sc.init(null, arrayOf(netConfig.getTrustManager()), SecureRandom())
                    HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
                    HttpsURLConnection.setDefaultHostnameVerifier(netConfig.getHostVerifier())
                    conn = url.openConnection() as HttpsURLConnection
                } else if (urlString.toLowerCase().startsWith("http://")) {
                    conn = url.openConnection() as HttpURLConnection
                }
                conn!!.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", NetworkConfiguration.USER_AGENT)
                conn.setRequestProperty("Accept-Language", locale.language)
                if (netConfig.getGzip()) {
                    conn.setRequestProperty("Accept-Encoding", "gzip")
                }
                val enu = netConfig.getExtraHead().entries
                for (entry in enu) {
                    conn.setRequestProperty(entry.key, entry.value)
                }
                conn.readTimeout = netConfig.getTimeOut()
                conn.connectTimeout = netConfig.getTimeOut()
                conn.doOutput = false
                conn.doInput = true

                conn.connect()
                val rc = conn.responseCode
                if (rc != HttpURLConnection.HTTP_OK && rc != HttpURLConnection.HTTP_ACCEPTED && rc != HttpURLConnection.HTTP_CREATED) {
                    throw HttpCodeException(rc, conn.url.toString())
                }
                //get data
                eventHandler.publishStatus(ServiceStatus.STARTING_GET_DATA, 0)
                inst = conn.inputStream
                resHead = getHttpResponseHeader(conn)
                val gzip = resHead["Content-Encoding"]
                if (!TextUtils.isEmpty(gzip) && gzip!!.indexOf("gzip") != -1) {
                    gis = GZIPInputStream(inst!!)
                }
                var d: Int
                val buffer = ByteArray(netConfig.getTruckSize())
                daos = ByteArrayOutputStream()
                while (true) {
                    if (gis != null) {
                        d = gis.read(buffer)
                    } else {
                        d = inst!!.read(buffer)
                    }
                    if (d == -1) {
                        break
                    }
                    //                if(d != netConfig.getTruckSize()){
                    //                    byte[] tbuffer = new byte[d];
                    //                    System.arraycopy(buffer, 0, tbuffer, 0, tbuffer.length);
                    //                    buffer = tbuffer;
                    //                }
                    daos.write(buffer, 0, d)
                }
                res = daos.toByteArray()
            } catch (e: IOException) {
                error = NetBrokenException()
            } catch (e: HttpCodeException) {
                error = e
            } catch (e: NoSuchAlgorithmException) {
                error = e
            } catch (e: KeyManagementException) {
                error = e
            } finally {
                try {
                    gis?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                try {
                    inst?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                try {
                    daos?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                try {
                    conn!!.disconnect()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
            eventHandler.connectFinished(resHead, res, error)
        }

        fun httpFileDownload(
            locale: Locale,
            urlString: String,
            file: File,
            eventHandler: IHttpDownloadEventHandler,
            netConfig: NetworkConfiguration
        ) {
            var error: Exception? = null
            var resHead: Map<String, String>? = null

            var inst: InputStream? = null
            var gis: GZIPInputStream? = null
            var fos: FileOutputStream? = null

            val url: URL
            var conn: HttpURLConnection? = null
            try {
                if (!file.exists()) {
                    file.parentFile.mkdirs()
                    file.createNewFile()
                }
                url = URL(urlString)
                if (urlString.toLowerCase().startsWith("https://")) {
                    val sc = SSLContext.getInstance("TLS")
                    sc.init(null, arrayOf(netConfig.getTrustManager()), SecureRandom())
                    HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
                    HttpsURLConnection.setDefaultHostnameVerifier(netConfig.getHostVerifier())
                    conn = url.openConnection() as HttpsURLConnection
                } else if (urlString.toLowerCase().startsWith("http://")) {
                    conn = url.openConnection() as HttpURLConnection
                }
                conn!!.requestMethod = "GET"
                conn.readTimeout = netConfig.getTimeOut()
                conn.connectTimeout = netConfig.getTimeOut()
                conn.setRequestProperty("Accept-Language", locale.language)
                conn.addRequestProperty("content-Length", "0")
                if (file.length() != 0L) {
                    conn.addRequestProperty("Range", "bytes=" + file.length() + "-")
                }
                val enu = netConfig.getExtraHead().entries
                for (entry in enu) {
                    conn.setRequestProperty(entry.key, entry.value)
                }

                conn.doInput = true
                conn.connect()
                val rc = conn.responseCode
                if (rc != HttpURLConnection.HTTP_OK && rc != HttpURLConnection.HTTP_PARTIAL) {
                    throw HttpCodeException(rc, conn.url.toString())
                }
                eventHandler.publishStatus(ServiceStatus.STARTING_GET_DATA, 0)
                val fl = conn.getHeaderField("content-Length")
                var lfl: Long = 0
                var bdl = file.length()
                try {
                    lfl = java.lang.Long.parseLong(fl) + file.length()
                } catch (e: NumberFormatException) {
                }

                inst = conn.inputStream
                resHead = getHttpResponseHeader(conn)
                val gzip = resHead["content-Encoding"]
                if (!TextUtils.isEmpty(gzip) && gzip!!.indexOf("gzip") != -1) {
                    gis = GZIPInputStream(inst!!)
                }
                if (lfl > netConfig.getCachePartialFileSize()) {
                    eventHandler.saveResumeData(resHead, file)
                }
                if (bdl == 0L) {
                    fos = FileOutputStream(file)
                } else {
                    fos = FileOutputStream(file, true)
                }
                var d: Int
                val buffer = ByteArray(netConfig.getTruckSize())
                var lastpg = 0
                var pg = 0
                while (true) {
                    if (gis != null) {
                        d = gis.read(buffer)
                    } else {
                        d = inst!!.read(buffer)
                    }
                    if (d > 0) {
                        bdl += d.toLong()
                    }
                    if (lfl != 0L) {
                        pg = (bdl * 100 / lfl).toInt()
                        if (pg != lastpg) {
                            lastpg = pg
                            eventHandler.publishStatus(ServiceStatus.STARTING_PROGRESS, lastpg)
                        }
                    }
                    eventHandler.cancelDownloadAction(resHead, file)
                    eventHandler.pauseDownloadAction(resHead, file)
                    if (d == -1) {
                        if (lfl == 0L) {
                            //没有说明文件大小的得到 -1
                            eventHandler.downLoadFinished(resHead, file)
                        } else if (bdl == lfl) {
                            //下载完成
                            eventHandler.downLoadFinished(resHead, file)
                        } else {
                            //下载失败
                            eventHandler.downLoadError(
                                resHead,
                                file,
                                DescriptionException("$urlString download incomplete.")
                            )
                        }
                        break
                    }
                    //                if(d != netConfig.getTruckSize()){
                    //                    byte[] tbuffer = new byte[d];
                    //                    System.arraycopy(buffer, 0, tbuffer, 0, tbuffer.length);
                    //                    buffer = tbuffer;
                    //                }
                    fos.write(buffer, 0, d)
                }
                return
            } catch (e: NoSuchAlgorithmException) {
                error = e
            } catch (e: KeyManagementException) {
                error = e
            } catch (e: HttpCodeException) {
                error = e
            } catch (e: FileNotFoundException) {
                error = e
            } catch (e: MalformedURLException) {
                error = e
            } catch (e: IOException) {
                error = NetBrokenException()
            } catch (e: UserCancel) {
                error = e
            } finally {
                try {
                    gis?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                try {
                    inst?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                try {
                    fos?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                try {
                    conn!!.disconnect()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
            eventHandler.downLoadError(resHead, file, error)
        }

        @Throws(UnsupportedEncodingException::class)
        private fun getHttpResponseHeader(http: HttpURLConnection): Map<String, String> {
            val header = LinkedHashMap<String, String>()
            var i = 0
            while (true) {
                val mine = http.getHeaderField(i) ?: break
                header[http.getHeaderFieldKey(i)] = mine
                i++
            }
            return header
        }

        fun httpFileUpload(
            locale: Locale,
            urlString: String,
            file: File,
            eventHandler: IHttpUploadEventHandler,
            netConfig: NetworkConfiguration
        ) {
            var error: Exception? = null
            var res: ByteArray? = null
            var resHead: Map<String, String>? = null

            var os: OutputStream? = null
            var `is`: InputStream? = null
            var gis: GZIPInputStream? = null
            var daos: ByteArrayOutputStream? = null

            val url: URL
            var conn: HttpURLConnection? = null
            eventHandler.publishStatus(ServiceStatus.STARTING_CONNECT, 0)
            try {
                url = URL(urlString)
                if (urlString.toLowerCase().startsWith("https://")) {
                    val sc = SSLContext.getInstance("TLS")
                    sc.init(null, arrayOf(netConfig.getTrustManager()), SecureRandom())
                    HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
                    HttpsURLConnection.setDefaultHostnameVerifier(netConfig.getHostVerifier())
                    conn = url.openConnection() as HttpsURLConnection
                } else if (urlString.toLowerCase().startsWith("http://")) {
                    conn = url.openConnection() as HttpURLConnection
                }
                conn!!.requestMethod = "POST"
                conn.setRequestProperty("Accept-Language", locale.language)
                if (netConfig.getGzip()) {
                    conn.setRequestProperty("Accept-Encoding", "gzip")
                }
                val formData = MultipartFormData()
                formData.addBodyParts(file.absolutePath, "file")
                conn.setRequestProperty("User-Agent", NetworkConfiguration.USER_AGENT)
                conn.setRequestProperty("Content-Type", formData.getContentType())
                val enu = netConfig.getExtraHead().entries
                for (entry in enu) {
                    conn.setRequestProperty(entry.key, entry.value)
                }
                conn.readTimeout = netConfig.getTimeOut()
                conn.connectTimeout = netConfig.getTimeOut()
                conn.doOutput = true
                conn.doInput = true

                conn.connect()
                //send data
                eventHandler.publishStatus(ServiceStatus.STARTING_SEND_DATA, 0)
                os = conn.outputStream
                formData.writeBodyParts(os, eventHandler)
                os!!.flush()

                val rc = conn.responseCode
                if (rc != HttpURLConnection.HTTP_OK && rc != HttpURLConnection.HTTP_ACCEPTED && rc != HttpURLConnection.HTTP_CREATED) {
                    throw HttpCodeException(rc, conn.url.toString())
                }

                //get data
                eventHandler.publishStatus(ServiceStatus.STARTING_GET_DATA, 0)

                `is` = conn.inputStream
                resHead = getHttpResponseHeader(conn)
                val gzip = resHead["Content-Encoding"]
                if (!TextUtils.isEmpty(gzip) && gzip!!.indexOf("gzip") != -1) {
                    gis = GZIPInputStream(`is`!!)
                }
                var d: Int
                val buffer = ByteArray(netConfig.getTruckSize())
                daos = ByteArrayOutputStream()
                while (true) {
                    if (gis != null) {
                        d = gis.read(buffer)
                    } else {
                        d = `is`!!.read(buffer)
                    }
                    if (d == -1) {
                        break
                    }
                    daos.write(buffer, 0, d)
                }
                res = daos.toByteArray()
                eventHandler.upLoadFinished(resHead, res)
                return
            } catch (e: IOException) {
                error = NetBrokenException()
            } catch (e: NoSuchAlgorithmException) {
                error = e
            } catch (e: KeyManagementException) {
                error = e
            } catch (e: HttpCodeException) {
                error = e
            } catch (e: MultiFormException) {
                error = e
            } catch (e: UserCancel) {
                error = e
            } finally {
                try {
                    gis?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                try {
                    `is`?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                try {
                    daos?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                try {
                    conn!!.disconnect()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                try {
                    os?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
            eventHandler.upLoadError(resHead, error)
        }

        fun httpFilePut(
            locale: Locale,
            urlString: String,
            file: File,
            eventHandler: IHttpUploadEventHandler,
            netConfig: NetworkConfiguration
        ) {
            var error: Exception? = null
            val res: ByteArray? = null
            val resHead: Map<String, String>? = null

            var os: OutputStream? = null
            var fis: FileInputStream? = null
            val url: URL
            var conn: HttpURLConnection? = null
            eventHandler.publishStatus(ServiceStatus.STARTING_CONNECT, 0)
            try {
                if (!file.exists()) {
                    throw MultiFormException("bodyPartFilePathInvalid " + file.absolutePath)
                }
                if (!file.isFile) {
                    throw MultiFormException("bodyPartFileIsDirectory " + file.absolutePath)
                }
                fis = FileInputStream(file)
                url = URL(urlString)
                if (urlString.toLowerCase().startsWith("https://")) {
                    val sc = SSLContext.getInstance("TLS")
                    sc.init(null, arrayOf(netConfig.getTrustManager()), SecureRandom())
                    HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
                    HttpsURLConnection.setDefaultHostnameVerifier(netConfig.getHostVerifier())
                    conn = url.openConnection() as HttpsURLConnection
                } else if (urlString.toLowerCase().startsWith("http://")) {
                    conn = url.openConnection() as HttpURLConnection
                }
                conn!!.requestMethod = "PUT"
                conn.setRequestProperty("Accept-Language", locale.language)
                if (netConfig.getGzip()) {
                    conn.setRequestProperty("Accept-Encoding", "gzip")
                }
                conn.setRequestProperty("User-Agent", NetworkConfiguration.USER_AGENT)
                val enu = netConfig.getExtraHead().entries
                for (entry in enu) {
                    conn.setRequestProperty(entry.key, entry.value)
                }
                conn.readTimeout = netConfig.getTimeOut()
                conn.connectTimeout = netConfig.getTimeOut()
                conn.doOutput = true
                conn.doInput = false

                conn.connect()
                //send data
                eventHandler.publishStatus(ServiceStatus.STARTING_SEND_DATA, 0)
                os = conn.outputStream
                val totalsize = fis.available()
                var bytessent = 0
                while (true) {
                    val buffer = ByteArray(netConfig.getTruckSize())
                    val bytesRead = fis.read(buffer, 0, netConfig.getTruckSize())

                    if (bytesRead > 0) {
                        //                    if(bytesRead != netConfig.getTruckSize()){
                        //                        byte[] tbuffer = new byte[bytesRead];
                        //                        System.arraycopy(buffer, 0, tbuffer, 0, tbuffer.length);
                        //                        buffer = tbuffer;
                        //                    }
                        os!!.write(buffer, 0, bytesRead)
                        bytessent += bytesRead
                        eventHandler.publishStatus(ServiceStatus.STARTING_PROGRESS, 100 * bytessent / totalsize)
                    } else {
                        break
                    }
                    eventHandler.canceluploadAction()
                }
                os!!.flush()

                val rc = conn.responseCode
                if (rc != HttpURLConnection.HTTP_OK && rc != HttpURLConnection.HTTP_ACCEPTED && rc != HttpURLConnection.HTTP_CREATED) {
                    throw HttpCodeException(rc, conn.url.toString())
                }
                eventHandler.upLoadFinished(resHead, null)
                return
            } catch (e: IOException) {
                error = NetBrokenException()
            } catch (e: NoSuchAlgorithmException) {
                error = e
            } catch (e: KeyManagementException) {
                error = e
            } catch (e: HttpCodeException) {
                error = e
            } catch (e: MultiFormException) {
                error = e
            } catch (e: UserCancel) {
                error = e
            } finally {
                try {
                    conn!!.disconnect()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                try {
                    os?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
            eventHandler.upLoadError(resHead, error)
        }

        fun httpFileDelete(
            locale: Locale,
            urlString: String,
            eventHandler: IHttpEventHandler,
            netConfig: NetworkConfiguration
        ) {
            var error: Exception? = null
            val res: ByteArray? = null
            val resHead: Map<String, String>? = null

            val url: URL
            var conn: HttpURLConnection? = null
            eventHandler.publishStatus(ServiceStatus.STARTING_CONNECT, 0)
            try {
                url = URL(urlString)
                if (urlString.toLowerCase().startsWith("https://")) {
                    val sc = SSLContext.getInstance("TLS")
                    sc.init(null, arrayOf(netConfig.getTrustManager()), SecureRandom())
                    HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
                    HttpsURLConnection.setDefaultHostnameVerifier(netConfig.getHostVerifier())
                    conn = url.openConnection() as HttpsURLConnection
                } else if (urlString.toLowerCase().startsWith("http://")) {
                    conn = url.openConnection() as HttpURLConnection
                }
                conn!!.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", NetworkConfiguration.USER_AGENT)
                conn.setRequestProperty("Accept-Language", locale.language)
                if (netConfig.getGzip()) {
                    conn.setRequestProperty("Accept-Encoding", "gzip")
                }
                val enu = netConfig.getExtraHead().entries
                for (entry in enu) {
                    conn.setRequestProperty(entry.key, entry.value)
                }
                conn.readTimeout = netConfig.getTimeOut()
                conn.connectTimeout = netConfig.getTimeOut()
                conn.doOutput = false
                conn.doInput = false

                conn.connect()
                val rc = conn.responseCode
                if (rc != HttpURLConnection.HTTP_OK && rc != HttpURLConnection.HTTP_ACCEPTED && rc != HttpURLConnection.HTTP_CREATED) {
                    throw HttpCodeException(rc, conn.url.toString())
                }
                //get data
            } catch (e: IOException) {
                error = NetBrokenException()
            } catch (e: HttpCodeException) {
                error = e
            } catch (e: NoSuchAlgorithmException) {
                error = e
            } catch (e: KeyManagementException) {
                error = e
            } finally {
                try {
                    conn!!.disconnect()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
            eventHandler.connectFinished(resHead, res, error)
        }
    }
}