package com.apigj.android.httpframework.controller.connect.serviceTask

import com.apigj.android.httpframework.controller.connect.DataHttpSession
import com.apigj.android.httpframework.controller.connect.IHttpUploadEventHandler
import com.apigj.android.httpframework.controller.connect.NetworkUtils
import com.apigj.android.httpframework.controller.connect.ServiceStatus
import com.apigj.android.httpframework.controller.exceptions.DescriptionException
import com.apigj.android.httpframework.controller.exceptions.NetBrokenException
import com.apigj.android.httpframework.controller.exceptions.UserCancel
import com.apigj.android.httpframework.controller.service.FileUploadService
import java.io.File
import java.io.UnsupportedEncodingException
import java.util.*

class UploadTask(locale: Locale):IRequestTask, IHttpUploadEventHandler {

    private var uploadReq: FileUploadService? = null
    private var mLocale = locale

    private var uploadCommand: Int = 0
    private val uploadContinue = 0
    private val uploadCancel = 3

    override fun execute(vararg params: Any) {
        uploadReq = params[0] as FileUploadService
        uploadReq!!.progressUpdate(ServiceStatus.PREEXECUTE, 0)
        NetworkUtils.threadPool.execute(this)
    }

    override fun run() {
        // 网络请求
        if (uploadReq!!.getOnStringGotListener() != null) {
            if (uploadReq!!.setHandleReService()) {
                return
            }
            DataHttpSession.httpFileUpload(
                mLocale,
                uploadReq!!.getURL(),
                File(uploadReq!!.getFilePath()),
                this,
                uploadReq!!.getNetConfig()
            )
        }
    }

    fun cancelUpload() {
        uploadCommand = uploadCancel
    }

    override fun publishStatus(status: ServiceStatus, progress: Int) {
        uploadReq!!.progressUpdate(status, progress)
    }

    override fun connectFinished(headFields: Map<String, String>?, data: ByteArray?, error: Exception?) {}

    override fun upLoadFinished(resHeader: Map<String, String>?, data: ByteArray?) {
        var urlPath: String? = null
        var error: Exception? = null
        try {
            urlPath = String(data!!)
        } catch (e: UnsupportedEncodingException) {
            error = DescriptionException("respone datae UnsupportedEncoding UTF8")
        }

        uploadReq!!.deleteUploadFile(urlPath)
        if(urlPath != null)uploadReq!!.processFile(resHeader, urlPath)
        uploadReq!!.remoteGot(resHeader, urlPath, error)
        uploadReq!!.handleReService(resHeader, urlPath, error)
        uploadReq!!.progressUpdate(ServiceStatus.END_PROGRESS, 0)
    }

    override fun upLoadError(resHeader: Map<String, String>?, error: Exception?) {
        if (error != null) {
            if (error is NetBrokenException) {
                uploadReq!!.saveUploadFile()
            } else {
                uploadReq!!.deleteUploadFile(uploadReq!!.getFilePath())
            }
            uploadReq!!.remoteGot(resHeader, uploadReq!!.getFilePath(), error)
            uploadReq!!.handleReService(resHeader, uploadReq!!.getFilePath(), error)
            uploadReq!!.progressUpdate(ServiceStatus.END_PROGRESS, 0)
        }
    }

    @Throws(UserCancel::class)
    override fun canceluploadAction() {
        if (uploadCommand == uploadCancel) {
            //取消下载任务，把下载好的数据删除
            uploadReq!!.progressUpdate(ServiceStatus.PROGRESS_CANCELED, 0)
            throw UserCancel(uploadReq!!.getURL(), uploadCancel)
        }
    }
}