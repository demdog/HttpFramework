package com.apigj.android.httpframework.controller.service

import com.apigj.android.httpframework.Keep

@Keep
interface ICachableService:IService {
    @Keep
    fun putDataUploadTime(uploadTime: Long)

    @Keep
    fun getDataUploadTime(): Long

    @Keep
    fun isCache(): Boolean

    @Keep
    fun getConflictIntervention(): ConflictIntervention

    @Keep
    fun setConflictIntervention(intervention: ConflictIntervention)

    @Keep
    fun getCacheTime(): Int

    @Keep
    fun setCacheTime(cacheTime: Int)
}