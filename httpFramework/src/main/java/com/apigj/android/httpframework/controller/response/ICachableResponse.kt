package com.apigj.android.httpframework.controller.response

import com.apigj.android.httpframework.Keep

@Keep
interface ICachableResponse {
    @Keep
    fun putDataLastUpdateTime(updateTime: Long)

    @Keep
    fun getDataLastUpdateTime(): Long

    @Keep
    fun isCache(): Boolean

    @Keep
    fun getCacheTime(): Int

    @Keep
    fun setCacheTime(cacheTime: Int)
}