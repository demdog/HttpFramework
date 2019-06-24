package com.apigj.android.httpframework.controller.exceptions

import com.apigj.android.httpframework.Keep
import java.lang.Exception

@Keep
class DataConflictException(obj: Any) : Exception (obj.javaClass.getName() + " DataConflict in database") {

    private val mConfliceObject = obj
    @Keep
    fun getConflictObject(): Any {
        return mConfliceObject
    }
}