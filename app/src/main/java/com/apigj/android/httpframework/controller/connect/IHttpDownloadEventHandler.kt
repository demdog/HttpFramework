package com.apigj.android.httpframework.controller.connect

import com.apigj.android.httpframework.controller.exceptions.UserCancel
import java.io.File

interface IHttpDownloadEventHandler:IHttpEventHandler {

    fun downLoadFinished(resHeader: Map<String, String>, file: File)

    fun downLoadError(resHeader: Map<String, String>?, file: File, error: Exception?)

    @Throws(UserCancel::class)
    fun cancelDownloadAction(resHeader: Map<String, String>, file: File)

    @Throws(UserCancel::class)
    fun pauseDownloadAction(resHeader: Map<String, String>, file: File)

    fun saveResumeData(resHeader: Map<String, String>, resumeData: File)
}