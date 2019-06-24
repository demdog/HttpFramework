package com.apigj.android.httpframework.controller.formatTool

import android.text.TextUtils
import android.webkit.MimeTypeMap
import com.apigj.android.httpframework.controller.connect.IHttpUploadEventHandler
import com.apigj.android.httpframework.controller.connect.ServiceStatus
import com.apigj.android.httpframework.controller.exceptions.MultiFormException
import com.apigj.android.httpframework.controller.exceptions.UserCancel
import com.apigj.android.httpframework.utils.CacheUtils
import java.io.*
import java.util.*

class MultipartFormData {

    internal interface EncodingCharacters {
        companion object {
            val crlf = "\r\n"
        }
    }

    internal inner class BodyPart(
        var headers: Map<String, String>,
        var bodyStream: InputStream?,
        var bodyContentLength: Long
    ) {
        var hasInitialBoundary = false
        var hasFinalBoundary = false
    }

    // MARK: - Properties

    /// The `Content-Type` header value containing the boundary used to generate the `multipart/form-data`.
    fun getContentType(): String {
        return "multipart/form-data; boundary=$boundary"
    }

    /// The content length of all body parts used to generate the `multipart/form-data` not including the boundaries.
    fun contentLength(): Long {
        var contentLenght = 0
        for (bp in bodyParts) {
            contentLenght += bp.bodyContentLength.toInt()
        }
        return contentLenght.toLong()
    }

    /// The boundary used to separate the body parts in the encoded form data.
    var boundary = randomBoundary()

    private val bodyParts: MutableList<BodyPart> = ArrayList()
    private var bodyPartError: MultiFormException? = null
    private var streamBufferSize = 1024

    // MARK: - Body Parts

    /// Creates a body part from the data and appends it to the multipart form data object.
    ///
    /// The body part data will be encoded using the following format:
    ///
    /// - `Content-Disposition: form-data; name=#{name}` (HTTP Header)
    /// - Encoded data
    /// - Multipart form boundary
    ///
    /// - parameter data: The data to encode into the multipart form data.
    /// - parameter name: The name to associate with the data in the `Content-Disposition` HTTP header.
    fun addBodyParts(data: ByteArray, name: String) {
        val headers = contentHeaders(name)
        val stream = ByteArrayInputStream(data)
        val length = data.size.toLong()

        addBodyParts(stream, length, headers)
    }

    /// Creates a body part from the data and appends it to the multipart form data object.
    ///
    /// The body part data will be encoded using the following format:
    ///
    /// - `Content-Disposition: form-data; name=#{name}` (HTTP Header)
    /// - `Content-Type: #{generated mimeType}` (HTTP Header)
    /// - Encoded data
    /// - Multipart form boundary
    ///
    /// - parameter data:     The data to encode into the multipart form data.
    /// - parameter name:     The name to associate with the data in the `Content-Disposition` HTTP header.
    /// - parameter mimeType: The MIME type to associate with the data content type in the `Content-Type` HTTP header.
    fun addBodyParts(data: ByteArray, name: String, mimeType: String) {
        val headers = contentHeaders(name, mimeType)
        val stream = ByteArrayInputStream(data)
        val length = data.size.toLong()

        addBodyParts(stream, length, headers)
    }

    /// Creates a body part from the data and appends it to the multipart form data object.
    ///
    /// The body part data will be encoded using the following format:
    ///
    /// - `Content-Disposition: form-data; name=#{name}; filename=#{filename}` (HTTP Header)
    /// - `Content-Type: #{mimeType}` (HTTP Header)
    /// - Encoded file data
    /// - Multipart form boundary
    ///
    /// - parameter data:     The data to encode into the multipart form data.
    /// - parameter name:     The name to associate with the data in the `Content-Disposition` HTTP header.
    /// - parameter fileName: The filename to associate with the data in the `Content-Disposition` HTTP header.
    /// - parameter mimeType: The MIME type to associate with the data in the `Content-Type` HTTP header.
    fun addBodyParts(data: ByteArray, name: String, fileName: String, mimeType: String) {
        val headers = contentHeaders(name, fileName, mimeType)
        val stream = ByteArrayInputStream(data)
        val length = data.size.toLong()

        addBodyParts(stream, length, headers)
    }

    /// Creates a body part from the file and appends it to the multipart form data object.
    ///
    /// The body part data will be encoded using the following format:
    ///
    /// - `Content-Disposition: form-data; name=#{name}; filename=#{generated filename}` (HTTP Header)
    /// - `Content-Type: #{generated mimeType}` (HTTP Header)
    /// - Encoded file data
    /// - Multipart form boundary
    ///
    /// The filename in the `Content-Disposition` HTTP header is generated from the last path component of the
    /// `fileURL`. The `Content-Type` HTTP header MIME type is generated by mapping the `fileURL` extension to the
    /// system associated MIME type.
    ///
    /// - parameter fileURL: The URL of the file whose content will be encoded into the multipart form data.
    /// - parameter name:    The name to associate with the file content in the `Content-Disposition` HTTP header.
    fun addBodyParts(filePath: String, name: String) {
        val file = File(filePath)
        val fileName = file.name
        val pathExtension = CacheUtils.getExtensionName(file.name)

        if (!TextUtils.isEmpty(fileName) && !TextUtils.isEmpty(pathExtension)) {
            val mime = mimeType(pathExtension)
            addBodyParts(filePath, name, fileName, mime)
        } else {
            this.bodyPartError = MultiFormException("bodyPartFilenameInvalid $filePath")
        }
    }

    /// Creates a body part from the file and appends it to the multipart form data object.
    ///
    /// The body part data will be encoded using the following format:
    ///
    /// - Content-Disposition: form-data; name=#{name}; filename=#{filename} (HTTP Header)
    /// - Content-Type: #{mimeType} (HTTP Header)
    /// - Encoded file data
    /// - Multipart form boundary
    ///
    /// - parameter fileURL:  The URL of the file whose content will be encoded into the multipart form data.
    /// - parameter name:     The name to associate with the file content in the `Content-Disposition` HTTP header.
    /// - parameter fileName: The filename to associate with the file content in the `Content-Disposition` HTTP header.
    /// - parameter mimeType: The MIME type to associate with the file content in the `Content-Type` HTTP header.
    fun addBodyParts(filePath: String, name: String, fileName: String, mimeType: String?) {
        val headers = contentHeaders(name, fileName, mimeType)
        val file = File(filePath)
        if (!file.exists()) {
            this.bodyPartError = MultiFormException("bodyPartFilePathInvalid $filePath")
            return
        }
        if (!file.isFile) {
            this.bodyPartError = MultiFormException("bodyPartFileIsDirectory $filePath")
            return
        }
        //        //============================================================
        //        //                 Check 1 - is file URL?
        //        //============================================================
        //
        //        if(filePath.isFileURL) {
        //            throw new MultiFormException("bodyPartURLInvalid "+ filePath);
        //        }
        //
        //        //============================================================
        //        //              Check 2 - is file URL reachable?
        //        //============================================================
        //        boolean isReachable = false;
        //        try {
        //            isReachable = fileURL.checkPromisedItemIsReachable();
        //        } catch (Exception e){
        //            throw new MultiFormException("bodyPartFileNotReachable "+ filePath);
        //        }
        //        if(!isReachable) {
        //            throw new MultiFormException("bodyPartFileNotReachable "+ filePath);
        //        }


        //============================================================
        //            Check 3 - is file URL a directory?
        //============================================================

        //        boolean isDirectory = false;
        //        String path = fileURL.path;
        //
        //        guard FileManager.default.fileExists(atPath: path, isDirectory: &isDirectory) && !isDirectory.boolValue else {
        //            throw new MultiFormException("bodyPartFileIsDirectory "+ filePath);
        //        }

        //============================================================
        //          Check 4 - can the file size be extracted?
        //============================================================

        val bodyContentLength = file.length()

        if (bodyContentLength == 0L) {
            this.bodyPartError = MultiFormException("bodyPartFileSizeQueryFailedWithError $filePath")
            return
        }
        //        try {
        //            guard let fileSize = try FileManager.default.attributesOfItem(atPath: path)[.size] as? NSNumber else {
        //                setBodyPartError(withReason: .bodyPartFileSizeNotAvailable(at: fileURL))
        //                return
        //            }
        //
        //            bodyContentLength = file.length();
        //        }
        //        catch {
        //            throw new MultiFormException("bodyPartFileSizeQueryFailedWithError "+ filePath);
        //        }

        //============================================================
        //       Check 5 - can a stream be created from file URL?
        //============================================================
        var stream: InputStream? = null
        try {
            stream = FileInputStream(file)
            addBodyParts(stream, bodyContentLength, headers)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }

    }

    /// Creates a body part from the stream and appends it to the multipart form data object.
    ///
    /// The body part data will be encoded using the following format:
    ///
    /// - `Content-Disposition: form-data; name=#{name}; filename=#{filename}` (HTTP Header)
    /// - `Content-Type: #{mimeType}` (HTTP Header)
    /// - Encoded stream data
    /// - Multipart form boundary
    ///
    /// - parameter stream:   The input stream to encode in the multipart form data.
    /// - parameter length:   The content length of the stream.
    /// - parameter name:     The name to associate with the stream content in the `Content-Disposition` HTTP header.
    /// - parameter fileName: The filename to associate with the stream content in the `Content-Disposition` HTTP header.
    /// - parameter mimeType: The MIME type to associate with the stream content in the `Content-Type` HTTP header.
    fun addBodyParts(
        stream: InputStream,
        length: Long,
        name: String,
        fileName: String,
        mimeType: String
    ) {
        val headers = contentHeaders(name, fileName, mimeType)
        addBodyParts(stream, length, headers)
    }

    /// Creates a body part with the headers, stream and length and appends it to the multipart form data object.
    ///
    /// The body part data will be encoded using the following format:
    ///
    /// - HTTP headers
    /// - Encoded stream data
    /// - Multipart form boundary
    ///
    /// - parameter stream:  The input stream to encode in the multipart form data.
    /// - parameter length:  The content length of the stream.
    /// - parameter headers: The HTTP headers for the body part.
    fun addBodyParts(stream: InputStream, length: Long, headers: Map<String, String>) {
        val bodyPart = BodyPart(headers, stream, length)
        bodyParts.add(bodyPart)
    }


    // MARK: - Private - Mime Type

    private fun mimeType(pathExtension: String): String? {
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(pathExtension)
        //        return "application/octet-stream";
    }

    // MARK: - Private - Content Headers

    private fun contentHeaders(name: String, fileName: String?, mimeType: String?): Map<String, String> {
        var disposition = "form-data; name=\"$name\""
        if (fileName != null) {
            disposition += "; filename=\"$fileName\""
        }

        val headers = HashMap<String, String>()
        headers["Content-Disposition"] = disposition
        if (mimeType != null) {
            headers["Content-Type"] = mimeType
        }
        return headers
    }

    private fun contentHeaders(name: String, fileName: String): Map<String, String> {
        return this.contentHeaders(name, fileName, null)
    }

    private fun contentHeaders(name: String): Map<String, String> {
        return this.contentHeaders(name, null, null)
    }

    internal enum class BoundaryType {
        initial, encapsulated, finall
    }
    companion object {
        private fun randomBoundary(): String {
            return String.format(
                "fugumobile.boundary.%08x%08x",
                Math.abs(Random().nextInt()),
                Math.abs(Random().nextInt())
            )
        }

        private fun boundaryData(boundaryType: BoundaryType, boundary: String): ByteArray? {
            var boundaryText: String? = null
            when (boundaryType) {
                MultipartFormData.BoundaryType.initial -> boundaryText = "--" + boundary + EncodingCharacters.crlf
                MultipartFormData.BoundaryType.encapsulated -> boundaryText =
                    EncodingCharacters.crlf + "--" + boundary + EncodingCharacters.crlf
                MultipartFormData.BoundaryType.finall -> boundaryText =
                    EncodingCharacters.crlf + "--" + boundary + "--" + EncodingCharacters.crlf
            }
            return boundaryText?.toByteArray()
        }
    }

    @Throws(MultiFormException::class, IOException::class, UserCancel::class)
    fun writeBodyParts(os: OutputStream, uploadEvent: IHttpUploadEventHandler) {
        if (bodyPartError != null) {
            throw bodyPartError!!
        }
        bodyParts[0].hasInitialBoundary = true
        bodyParts[bodyParts.size - 1].hasFinalBoundary = true
        val eachpart = 100 / bodyParts.size.toDouble()
        var complete = 0
        for (bodyPart in bodyParts) {
            write(bodyPart, os, uploadEvent, complete.toDouble(), eachpart)
            complete += eachpart.toInt()
            uploadEvent.publishStatus(ServiceStatus.STARTING_PROGRESS, complete)
        }
    }

    // MARK: - Data Encoding

    /// Encodes all the appended body parts into a single `Data` value.
    ///
    /// It is important to note that this method will load all the appended body parts into memory all at the same
    /// time. This method should only be used when the encoded data will have a small memory footprint. For large data
    /// cases, please use the `writeEncodedDataToDisk(fileURL:completionHandler:)` method.
    ///
    /// - throws: An `AFError` if encoding encounters an error.
    ///
    /// - returns: The encoded `Data` if encoding is successful.
    @Throws(MultiFormException::class, IOException::class)
    fun encode(): ByteArray {
        if (bodyPartError != null) {
            throw bodyPartError!!
        }

        val encoded = ByteArrayOutputStream()

        bodyParts[0].hasInitialBoundary = true
        bodyParts[bodyParts.size - 1].hasFinalBoundary = true

        for (bodyPart in bodyParts) {
            val encodedData = encode(bodyPart)
            encoded.write(encodedData)
        }

        return encoded.toByteArray()
    }

    /// Writes the appended body parts into the given file URL.
    ///
    /// This process is facilitated by reading and writing with input and output streams, respectively. Thus,
    /// this approach is very memory efficient and should be used for large body part data.
    ///
    /// - parameter fileURL: The file URL to write the multipart form data into.
    ///
    /// - throws: An `AFError` if encoding encounters an error.
    @Throws(MultiFormException::class)
    fun writeEncodedData(filePath: String) {
        if (bodyPartError != null) {
            throw bodyPartError!!
        }
        val file = File(filePath)
        if (!file.exists()) {
            throw MultiFormException("bodyPartFilePathInvalid $filePath")
        }
        if (!file.isFile) {
            throw MultiFormException("bodyPartFileIsDirectory $filePath")
        }
        var outputStream: OutputStream? = null
        try {
            outputStream = FileOutputStream(file)

            bodyParts[0].hasInitialBoundary = true
            bodyParts[bodyParts.size - 1].hasFinalBoundary = true

            for (bodyPart in bodyParts) {
                write(bodyPart, outputStream)
            }
        } catch (e: FileNotFoundException) {
            throw MultiFormException("bodyPartFilePathInvalid $filePath")
        } catch (e: IOException) {
            throw MultiFormException("bodyPartFileIOException $filePath")
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
        }
    }

    // MARK: - Private - Body Part Encoding

    @Throws(IOException::class)
    private fun encode(bodyPart: BodyPart): ByteArray {
        val encoded = ByteArrayOutputStream()

        val initialData = if (bodyPart.hasInitialBoundary) initialBoundaryData() else encapsulatedBoundaryData()
        encoded.write(initialData!!)

        val headerData = encodeHeaders(bodyPart)
        encoded.write(headerData)

        val bodyStreamData = encodeBodyStream(bodyPart)
        encoded.write(bodyStreamData)

        if (bodyPart.hasFinalBoundary) {
            encoded.write(finalBoundaryData()!!)
        }

        return encoded.toByteArray()
    }

    private fun encodeHeaders(bodyPart: BodyPart): ByteArray {
        var headerText = ""

        for ((key, value) in bodyPart.headers) {
            headerText += key + ": " + value + EncodingCharacters.crlf
        }

        headerText += EncodingCharacters.crlf

        return headerText.toByteArray()
    }

    @Throws(IOException::class)
    private fun encodeBodyStream(bodyPart: BodyPart): ByteArray {
        val inputStream = bodyPart.bodyStream
        var encoded: ByteArrayOutputStream? = null
        try {
            encoded = ByteArrayOutputStream()

            while (true) {
                var buffer = ByteArray(streamBufferSize)
                val bytesRead = inputStream!!.read(buffer)
                if (bytesRead > 0) {
                    if (bytesRead != streamBufferSize) {
                        val tbuffer = ByteArray(bytesRead)
                        System.arraycopy(buffer, 0, tbuffer, 0, tbuffer.size)
                        buffer = tbuffer
                    }
                    encoded.write(buffer, 0, bytesRead)
                } else {
                    break
                }
            }
            return encoded.toByteArray()
        } finally {
            inputStream?.close()
            encoded?.close()
        }
    }

    @Throws(IOException::class, UserCancel::class)
    private fun write(
        bodyPart: BodyPart,
        outputStream: OutputStream,
        uploadEvent: IHttpUploadEventHandler,
        complete: Double,
        eachpart: Double
    ) {
        writeInitialBoundaryData(bodyPart, outputStream)
        writeHeaderData(bodyPart, outputStream)
        writeBodyStream(bodyPart, outputStream, uploadEvent, complete, eachpart)
        writeFinalBoundaryData(bodyPart, outputStream)
    }

    @Throws(IOException::class, UserCancel::class)
    private fun writeBodyStream(
        bodyPart: BodyPart,
        outputStream: OutputStream,
        uploadEvent: IHttpUploadEventHandler,
        complete: Double,
        eachpart: Double
    ) {
        val inputStream = bodyPart.bodyStream
        val totalsize = inputStream!!.available()
        var bytessent = 0
        var precentage = 0
        var tprecentage = 0
        try {
            while (true) {
                var buffer = ByteArray(streamBufferSize)
                val bytesRead = inputStream.read(buffer, 0, streamBufferSize)

                if (bytesRead > 0) {
                    if (bytesRead != streamBufferSize) {
                        val tbuffer = ByteArray(bytesRead)
                        System.arraycopy(buffer, 0, tbuffer, 0, tbuffer.size)
                        buffer = tbuffer
                    }
                    writeBuffer(buffer, outputStream)
                    bytessent += bytesRead
                    tprecentage = (complete + eachpart * bytessent / totalsize).toInt()
                    if (precentage != tprecentage) {
                        precentage = tprecentage
                        uploadEvent.publishStatus(ServiceStatus.STARTING_PROGRESS, precentage)
                    }
                } else {
                    break
                }
                uploadEvent.canceluploadAction()
            }
        } finally {
            inputStream?.close()
        }
    }
    // MARK: - Private - Writing Body Part to Output Stream

    @Throws(IOException::class)
    private fun write(bodyPart: BodyPart, outputStream: OutputStream) {
        writeInitialBoundaryData(bodyPart, outputStream)
        writeHeaderData(bodyPart, outputStream)
        writeBodyStream(bodyPart, outputStream)
        writeFinalBoundaryData(bodyPart, outputStream)
    }

    @Throws(IOException::class)
    private fun writeInitialBoundaryData(bodyPart: BodyPart, outputStream: OutputStream) {
        val initialData = if (bodyPart.hasInitialBoundary) initialBoundaryData() else encapsulatedBoundaryData()
        writeData(initialData!!, outputStream)
    }

    @Throws(IOException::class)
    private fun writeHeaderData(bodyPart: BodyPart, outputStream: OutputStream) {
        val headerData = encodeHeaders(bodyPart)
        writeData(headerData, outputStream)
    }

    @Throws(IOException::class)
    private fun writeBodyStream(bodyPart: BodyPart, outputStream: OutputStream) {
        val inputStream = bodyPart.bodyStream

        try {
            while (true) {
                var buffer = ByteArray(streamBufferSize)
                val bytesRead = inputStream!!.read(buffer, 0, streamBufferSize)

                if (bytesRead > 0) {
                    if (bytesRead != streamBufferSize) {
                        val tbuffer = ByteArray(bytesRead)
                        System.arraycopy(buffer, 0, tbuffer, 0, tbuffer.size)
                        buffer = tbuffer
                    }
                    writeBuffer(buffer, outputStream)
                } else {
                    break
                }
            }
        } finally {
            inputStream?.close()
        }
    }

    @Throws(IOException::class)
    private fun writeFinalBoundaryData(bodyPart: BodyPart, outputStream: OutputStream) {
        if (bodyPart.hasFinalBoundary) {
            writeData(finalBoundaryData()!!, outputStream)
        }
    }

    // MARK: - Private - Writing Buffered Data to Output Stream

    @Throws(IOException::class)
    private fun writeData(data: ByteArray, outputStream: OutputStream) {
        val buffer = ByteArray(data.size)
        System.arraycopy(data, 0, buffer, 0, buffer.size)

        writeBuffer(buffer, outputStream)
    }

    @Throws(IOException::class)
    private fun writeBuffer(buffer: ByteArray, outputStream: OutputStream) {
        val bytesToWrite = buffer.size
        if (bytesToWrite == 0) {
            return
        }
        outputStream.write(buffer)
    }

    // MARK: - Private - Boundary Encoding

    private fun initialBoundaryData(): ByteArray? {
        return boundaryData(BoundaryType.initial, boundary)
    }

    private fun encapsulatedBoundaryData(): ByteArray? {
        return boundaryData(BoundaryType.encapsulated, boundary)
    }

    private fun finalBoundaryData(): ByteArray? {
        return boundaryData(BoundaryType.finall, boundary)
    }

}