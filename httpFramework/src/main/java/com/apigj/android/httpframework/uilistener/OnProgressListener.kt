package com.apigj.android.httpframework.uilistener

import com.apigj.android.httpframework.Keep
import com.apigj.android.httpframework.controller.connect.ServiceStatus

@Keep
interface OnProgressListener {
    @Keep
    fun onProgress(
        status: ServiceStatus,
        progress: Int
    )

}