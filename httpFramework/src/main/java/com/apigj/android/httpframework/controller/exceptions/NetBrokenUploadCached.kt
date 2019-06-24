package com.apigj.android.httpframework.controller.exceptions

import com.apigj.android.httpframework.Keep
import java.lang.Exception
@Keep
class NetBrokenUploadCached(obj: Any?) : Exception("Network is Broken! Upload data cached, will upload while network is fixed") {

    private var uploadData = obj
    @Keep
    fun getUploadData(): Any? {
        return uploadData
    }
}