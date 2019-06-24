package com.apigj.android.httpframework.controller.formatTool

import com.apigj.android.httpframework.Keep
import com.google.gson.Gson
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.reflect.Type
@Keep
class JSONProcessor {
    companion object {
        private val gson = Gson()

        @Keep
        fun <T> toJSObject(st: String, typeOfT: Type): T {
            return gson.fromJson(st, typeOfT)
        }

        fun <T> toJSObject(stream: InputStreamReader, typeOfT: Type): T{
            return gson.fromJson(stream, typeOfT)
        }

        @Keep
        fun toJSString(ob: Any): String {
            return gson.toJson(ob)
        }
    }
}