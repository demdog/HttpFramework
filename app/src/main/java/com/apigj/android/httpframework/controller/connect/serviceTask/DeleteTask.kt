package com.apigj.android.httpframework.controller.connect.serviceTask

import com.apigj.android.httpframework.controller.connect.DataHttpSession
import com.apigj.android.httpframework.controller.connect.IHttpEventHandler
import com.apigj.android.httpframework.controller.connect.NetworkUtils
import com.apigj.android.httpframework.controller.connect.ServiceStatus
import com.apigj.android.httpframework.controller.service.FileDeleteService
import java.util.*

class DeleteTask(mLocale: Locale):IRequestTask, IHttpEventHandler {

    private var atoken: String? = null
    private var deleteReq: FileDeleteService? = null
    private var mLocale: Locale = mLocale

    private var uploadCommand: Int = 0
    private val uploadContinue = 0
    private val uploadCancel = 3

    override fun execute(vararg params: Any) {
        deleteReq = params[0] as FileDeleteService
        deleteReq!!.progressUpdate(ServiceStatus.PREEXECUTE, 0)
        NetworkUtils.threadPool.execute(this)
    }

    override fun run() {
        // 网络请求
        if (deleteReq!!.setHandleReService()) {
            return
        }
        DataHttpSession.httpFileDelete(mLocale, deleteReq!!.getURL(), this, deleteReq!!.getNetConfig())
    }

    fun cancelUpload() {
        uploadCommand = uploadCancel
    }

    override fun publishStatus(status: ServiceStatus, progress: Int) {
        deleteReq!!.progressUpdate(status, progress)
    }

    override fun connectFinished(headFields: Map<String, String>?, data: ByteArray?, error: Exception?) {

    }
}