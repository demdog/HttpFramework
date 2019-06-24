package com.apigj.android.httpframework.controller.connect

import com.apigj.android.httpframework.controller.exceptions.UserCancel

interface IHttpUploadEventHandler:IHttpEventHandler {

    fun upLoadFinished(resHeader: Map<String, String>?, data: ByteArray?)

    fun upLoadError(resHeader: Map<String, String>?, error: Exception?)

    @Throws(UserCancel::class)
    fun canceluploadAction()
}