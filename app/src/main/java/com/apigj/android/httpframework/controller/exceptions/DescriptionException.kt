package com.apigj.android.httpframework.controller.exceptions

import com.apigj.android.httpframework.Keep

@Keep
class DescriptionException(desc: String) : Exception(desc) {
}