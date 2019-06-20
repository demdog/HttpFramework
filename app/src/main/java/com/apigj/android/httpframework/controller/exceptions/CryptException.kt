package com.apigj.android.httpframework.controller.exceptions

import com.apigj.android.httpframework.Keep

@Keep
class CryptException constructor(message: String?, crypt: ByteArray) : Exception(message) {

    private val mContent: ByteArray = crypt

    @Keep
    fun getContent(): ByteArray {
        return mContent
    }
}