package com.apigj.android.httpframework.controller.exceptions

import com.apigj.android.httpframework.Keep
import java.lang.Exception
@Keep
class UserCancel(content: String, cancelType: Int): Exception("$content has been canceled.") {
    private val mCancelType = cancelType

    @Keep
    fun getCancelType(): Int {
        return mCancelType
    }
}