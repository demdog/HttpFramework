package com.apigj.android.httpframework.controller.exceptions

import com.apigj.android.httpframework.Keep
import java.lang.Exception
@Keep
class ModelParseFailed(modelName:String, jsonString:String) : Exception("$modelName cannot parse.") {

    private val jsonString = modelName

    @Keep
    fun getJsonString(): String {
        return jsonString
    }
}