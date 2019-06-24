package com.apigj.android.httpframework.controller.connect.serviceTask

import android.text.TextUtils
import com.apigj.android.httpframework.controller.connect.DataHttpSession
import com.apigj.android.httpframework.controller.connect.IHttpEventHandler
import com.apigj.android.httpframework.controller.connect.NetworkUtils
import com.apigj.android.httpframework.controller.connect.ServiceStatus
import com.apigj.android.httpframework.controller.exceptions.CacheLoaded
import com.apigj.android.httpframework.controller.exceptions.DescriptionException
import com.apigj.android.httpframework.controller.service.BaseService
import java.util.*

class PostDataTask(locale: Locale) : IRequestTask, IHttpEventHandler {

    private var mBaseReq: BaseService<*>? = null
    private var mLocale = locale

    override fun execute(vararg params: Any) {
        mBaseReq = params[0] as BaseService<*>
        mBaseReq!!.progressUpdate(ServiceStatus.PREEXECUTE, 0)
        NetworkUtils.threadPool.execute(this)
    }

    override fun run() {
        // 缓存请求
        if (mBaseReq!!.processLocalDataGot()) {
            if (mBaseReq!!.isLoadOnce()) {
                mBaseReq!!.processRemoteDataGot(null, null, CacheLoaded(mBaseReq!!.javaClass.name))
                mBaseReq!!.progressUpdate(ServiceStatus.END_PROGRESS, 0)
                return
            }
        }

        // 网络请求
        if (mBaseReq!!.getOnDataGotListener() != null) {
            if (TextUtils.isEmpty(mBaseReq!!.getParameter())) {
                connectFinished(
                    null,
                    null,
                    DescriptionException(mBaseReq!!.javaClass.name + " parameter create failed")
                )
                return
            }
            if (mBaseReq!!.setHandleReService()) {
                return
            }
            DataHttpSession.httpBytesExchange(
                mLocale,
                mBaseReq!!.getURL(),
                mBaseReq!!.getParameter()!!.toByteArray(),
                this,
                mBaseReq!!.getNetConfig()
            )
        }
    }

    override fun publishStatus(status: ServiceStatus, progress: Int) {
        mBaseReq!!.progressUpdate(status, progress)
    }

    override fun connectFinished(headFields: Map<String, String>?, data: ByteArray?, error: Exception?) {
        mBaseReq!!.progressUpdate(ServiceStatus.DISCONNECTED, 0)
        mBaseReq!!.processRemoteDataGot(headFields, data, error)
        mBaseReq!!.progressUpdate(ServiceStatus.END_PROGRESS, 0)
    }
}