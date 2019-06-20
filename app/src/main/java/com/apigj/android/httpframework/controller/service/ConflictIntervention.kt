package com.apigj.android.httpframework.controller.service

import com.apigj.android.httpframework.Keep

@Keep
enum class ConflictIntervention {
    @Keep
    MERGE,
    @Keep
    OVER_COVER,
    @Keep
    KEEP_ALL
}