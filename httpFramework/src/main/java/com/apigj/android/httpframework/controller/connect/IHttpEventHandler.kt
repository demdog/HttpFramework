package com.apigj.android.httpframework.controller.connect

interface IHttpEventHandler {

    fun publishStatus(status: ServiceStatus, progress: Int)

    fun connectFinished(headFields: Map<String, String>?, data: ByteArray?, error: Exception?)
}