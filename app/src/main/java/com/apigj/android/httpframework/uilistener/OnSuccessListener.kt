package com.apigj.android.httpframework.uilistener

import com.apigj.android.httpframework.Keep

@Keep
interface OnSuccessListener {
    @Keep
    fun OnSuccess(
        error: Exception?
    )
}