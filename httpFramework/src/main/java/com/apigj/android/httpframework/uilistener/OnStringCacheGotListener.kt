package com.apigj.android.httpframework.uilistener

import com.apigj.android.httpframework.Keep

@Keep
interface OnStringCacheGotListener {

    @Keep
    fun onStringCacheGot(
        string: String?,
        cacheTime: Long?,
        error: Exception?
    )
}