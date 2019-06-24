package com.apigj.android.httpframework.controller.exceptions

import com.apigj.android.httpframework.Keep
import java.lang.Exception
@Keep
class ModelSynthesizeFailed(modelName: String, obj: Any) : Exception("$modelName cannot Synthesize.") {

    private val obj = obj
    @Keep
    fun getObject(): Any {
        return obj
    }
}