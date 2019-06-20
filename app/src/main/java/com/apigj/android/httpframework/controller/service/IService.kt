package com.apigj.android.httpframework.controller.service

import com.apigj.android.httpframework.Keep
import com.apigj.android.httpframework.controller.connect.NetworkConfiguration
import com.apigj.android.httpframework.controller.connect.ServiceStatus
import com.apigj.android.httpframework.uilistener.OnProgressListener

@Keep
interface IService {
    @Keep
    fun startRequest()

    @Keep
    fun startRequest(conf: NetworkConfiguration)

    @Keep
    fun getURL(): String

    @Keep
    fun getOnProgressListener(): OnProgressListener?

    @Keep
    fun setOnProgressListener(opl: OnProgressListener)

    @Keep
    fun progressUpdate(status: ServiceStatus, progress: Int)

}