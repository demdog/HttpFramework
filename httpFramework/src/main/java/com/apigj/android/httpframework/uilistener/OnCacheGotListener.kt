package com.apigj.android.httpframework.uilistener

import com.apigj.android.httpframework.Keep

@Keep
interface OnCacheGotListener<T> {
    @Keep
    fun onCacheGot(
        br:T?,
        error: Exception?
    )
}