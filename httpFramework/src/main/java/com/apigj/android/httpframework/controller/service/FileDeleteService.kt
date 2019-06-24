package com.apigj.android.httpframework.controller.service

import android.content.Context
import android.os.Handler
import android.os.Message
import com.apigj.android.httpframework.Keep
import com.apigj.android.httpframework.controller.connect.NetworkConfiguration
import com.apigj.android.httpframework.controller.connect.ServiceStatus
import com.apigj.android.httpframework.controller.connect.reService.IRemoteHandler
import com.apigj.android.httpframework.controller.connect.reService.ReServiceHandler
import com.apigj.android.httpframework.controller.connect.serviceTask.DeleteTask
import com.apigj.android.httpframework.uilistener.OnProgressListener
import com.apigj.android.httpframework.uilistener.OnSuccessListener

@Keep
class FileDeleteService(context: Context, url: String):IService, IRemoteHandler {

    private var osl: OnSuccessListener? = null
    private var opl: OnProgressListener? = null
    private var netConfig = NetworkConfiguration.getCurrentNetworkConfiguration()
    private var mUrl = url
    private var dt: DeleteTask? = null
    private var mLocale = context.resources.configuration.locale
    private val handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            (msg.obj as Runnable).run()
        }
    }

    @Keep
    fun getNetConfig(): NetworkConfiguration {
        return netConfig
    }

    @Keep
    override fun startRequest(conf: NetworkConfiguration) {
        netConfig = conf
        startRequest()
    }

    private fun fileDeleteService() {
        dt = DeleteTask(mLocale)
        dt!!.execute(this)
    }

    @Keep
    override fun startRequest() {
        fileDeleteService()
    }

    override fun handlerString(): String {
        return getURL()
    }

    override fun remoteHandle(resHeader: Map<String, String>?, remoteThing: Any?, error: Exception?) {
        remoteGot(resHeader, error)
    }

    fun remoteGot(resHeader: Map<String, String>?, error: Exception?) {
        if (osl != null) {
            handler.sendMessage(handler.obtainMessage(0, Runnable {
                if (osl != null) {
                    osl!!.OnSuccess(error)
                }
            }))
        }
    }

    @Keep
    override fun getURL(): String {
        return mUrl
    }

    @Keep
    override fun getOnProgressListener(): OnProgressListener? {
        return opl
    }

    @Keep
    override fun setOnProgressListener(opl: OnProgressListener) {
        this.opl = opl
    }

    @Keep
    fun getOnSuccessListener(): OnSuccessListener? {
        return osl
    }

    @Keep
    fun setOnSuccessListener(onSuccessListener: OnSuccessListener) {
        osl = onSuccessListener
    }

    override fun progressUpdate(status: ServiceStatus, progress: Int) {
        if (opl != null) {
            handler.sendMessage(handler.obtainMessage(0, Runnable {
                if (opl != null) {
                    opl!!.onProgress(status, progress)
                }
            }))
        }
    }

    fun setHandleReService(): Boolean {
        if (ReServiceHandler.containsService(this)) {
            ReServiceHandler.putOnRemoteDataHandler(this)
            return true
        }
        ReServiceHandler.createQueue(this)
        return false
    }

    fun handleReService(resHeader: Map<String, String>?, urlPath: String?, error: Exception?) {
        ReServiceHandler.handleOnRemoteData(this, resHeader, urlPath, error)
        ReServiceHandler.eraseQueue(this)
    }
}