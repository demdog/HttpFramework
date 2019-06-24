package com.apigj.android.httpframework.uilistener

import com.apigj.android.httpframework.Keep

@Keep
interface OnStringGotListener {
    @Keep
    fun onStringGot(
        string: String?,
        error: Exception?
    )
}