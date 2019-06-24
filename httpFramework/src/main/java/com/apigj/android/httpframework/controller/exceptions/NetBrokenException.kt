package com.apigj.android.httpframework.controller.exceptions

import com.apigj.android.httpframework.Keep
import java.lang.Exception

@Keep
class NetBrokenException : Exception("Network is Broken Suddenly.") {
}