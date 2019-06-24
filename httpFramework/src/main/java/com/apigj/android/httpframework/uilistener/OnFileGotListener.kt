package com.apigj.android.httpframework.uilistener

import com.apigj.android.httpframework.Keep

@Keep
interface OnFileGotListener {
    @Keep
    fun onFileGot(
        path: String?,
        error: Exception?
    )
}