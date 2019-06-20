package com.apigj.android.httpframework.controller.connect.serviceTask

interface IRequestTask:Runnable {

    fun execute(vararg params: Any)
}