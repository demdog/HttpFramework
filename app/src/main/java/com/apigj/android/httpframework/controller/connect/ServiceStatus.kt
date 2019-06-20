package com.apigj.android.httpframework.controller.connect

import com.apigj.android.httpframework.Keep

@Keep
enum class ServiceStatus {
    @Keep
    STARTING_PROGRESS,
    @Keep
    STARTING_CONNECT,
    @Keep
    STARTING_SEND_DATA,
    @Keep
    STARTING_GET_DATA,
    @Keep
    STARTING_PARSER,
    @Keep
    DISCONNECTED,
    @Keep
    PREEXECUTE,
    @Keep
    END_PROGRESS,
    @Keep
    PROGRESS_CANCELED,
    @Keep
    PROGRESS_PAUSED,
    @Keep
    PROGRESS_RESUME
}