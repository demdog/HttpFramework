package com.apigj.android.httpframework.controller.connect.reService

import com.apigj.android.httpframework.Keep
import com.apigj.android.httpframework.uilistener.OnDataGotListener

interface IRemoteHandler {

    fun handlerString(): String
    fun remoteHandle(resHeader: Map<String, String>?, remoteThing: Any?, error: Exception?)

}