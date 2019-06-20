package com.apigj.android.httpframework.controller.exceptions

import com.apigj.android.httpframework.Keep

@Keep
class HttpCodeException(httpCode:Int, httpUrl:String) : Exception("$httpUrl has error code:$httpCode") {
}