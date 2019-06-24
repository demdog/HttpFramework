package com.apigj.android.httpframework.controller.connect.serviceTask

import com.apigj.android.httpframework.controller.connect.DataHttpSession
import com.apigj.android.httpframework.controller.connect.IHttpEventHandler
import com.apigj.android.httpframework.controller.connect.NetworkUtils
import com.apigj.android.httpframework.controller.connect.ServiceStatus
import com.apigj.android.httpframework.controller.exceptions.CacheLoaded
import com.apigj.android.httpframework.controller.service.StringGetService
import java.util.*

class GetDataTask(locale: Locale) : IRequestTask, IHttpEventHandler {

    private var mLocale = locale
    private var atoken: String? = null
    private var mBaseReq: StringGetService? = null

    override fun execute(vararg params: Any) {
        mBaseReq = params[0] as StringGetService
        mBaseReq!!.progressUpdate(ServiceStatus.PREEXECUTE, 0)
        NetworkUtils.threadPool.execute(this)
    }

    fun setAccessToken(accessToken: String) {
        atoken = accessToken
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
        //		mBaseReq.processLocalDataGot();
        // 网络请求
        if (mBaseReq!!.getOnStringGotListener() != null) {
            if (mBaseReq!!.setHandleReService()) {
                return
            }
            DataHttpSession.httpGetBytes(mLocale, mBaseReq!!.getURL(), this, mBaseReq!!.getNetConfig())
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