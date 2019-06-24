package com.apigj.android.httpframework.uilistener

import com.apigj.android.httpframework.Keep

@Keep
interface OnDataGotListener<T> {
    @Keep
    fun onRequestGot(
        br: T?,
        error: Exception?
    )
}