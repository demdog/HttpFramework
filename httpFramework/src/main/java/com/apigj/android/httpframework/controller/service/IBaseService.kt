package com.apigj.android.httpframework.controller.service

import com.apigj.android.httpframework.Keep
import com.apigj.android.httpframework.uilistener.OnCacheGotListener
import com.apigj.android.httpframework.uilistener.OnDataGotListener
import java.lang.reflect.Type
@Keep
interface IBaseService<T>: IService {

    @Keep
    fun getResponseModuleName(assResSuccess: Boolean): Type?

    @Keep
    fun getAssemblerShortURL(): String?

    @Keep
    fun getVersion(): String?

    @Keep
    fun getOnCacheGotListener(): OnCacheGotListener<T>?

    @Keep
    fun getOnDataGotListener(): OnDataGotListener<T>?

    @Keep
    fun setOnCacheGotListener(ocgl: OnCacheGotListener<T>)

    @Keep
    fun setOnDataGotListener(odgl: OnDataGotListener<T>)

    @Keep
    fun setRequestParaObject(requestParaObj: Any)

    fun getRequestParaObject(): Any?

}