package com.apigj.android.httpframework.controller.service

import android.content.Context
import android.os.Handler
import android.os.Message
import com.apigj.android.httpframework.Keep
import com.apigj.android.httpframework.controller.connect.NetworkConfiguration
import com.apigj.android.httpframework.controller.connect.NetworkUtils
import com.apigj.android.httpframework.controller.connect.ServiceStatus
import com.apigj.android.httpframework.controller.connect.reService.IRemoteHandler
import com.apigj.android.httpframework.controller.connect.reService.ReServiceHandler
import com.apigj.android.httpframework.controller.connect.serviceTask.ObjectDataTask
import com.apigj.android.httpframework.controller.database.CacheApiDataBase
import com.apigj.android.httpframework.controller.database.CacheUploadDataBase
import com.apigj.android.httpframework.controller.exceptions.*
import com.apigj.android.httpframework.controller.formatTool.JSONProcessor
import com.apigj.android.httpframework.controller.response.ICachableResponse
import com.apigj.android.httpframework.uilistener.OnCacheGotListener
import com.apigj.android.httpframework.uilistener.OnDataGotListener
import com.apigj.android.httpframework.uilistener.OnProgressListener
import com.apigj.android.httpframework.utils.FrameworkConfiguration
import com.apigj.android.httpframework.utils.LogTag
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import java.lang.reflect.Type
import java.util.*
@Keep
class ObjectGetService<T>(context: Context, url: String): IRemoteHandler, IBaseService<T> {

    private var ocgl: OnCacheGotListener<T>? = null
    private var odgl: OnDataGotListener<T>? = null
    private var opl: OnProgressListener? = null

    //    private Context mContext;
    private var netConfig = NetworkConfiguration.getCurrentNetworkConfiguration()
    private var mUrl = url
    private var mLocale = context.resources.configuration.locale
    private var mAppContext = context.applicationContext
    private var mLoadOnce: Boolean = false
    private val handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            (msg.obj as Runnable).run()
        }
    }

    @Keep
    fun getNetConfig(): NetworkConfiguration {
        return netConfig
    }

    @Keep
    override fun startRequest(conf: NetworkConfiguration) {
        netConfig = conf
        startRequest()
    }

    override fun startRequest() {
        val pdt = ObjectDataTask(mLocale)
        pdt.execute(this)
    }

    fun setHandleReRequest(): Boolean {
        if (ReServiceHandler.containsService(this)) {
            ReServiceHandler.putOnRemoteDataHandler(this)
            return true
        }
        ReServiceHandler.createQueue(this)
        return false
    }

    internal fun handleReService(resHeader: Map<String, String>?, response: Any?, error: Exception?) {
        ReServiceHandler.handleOnRemoteData(this, resHeader, response, error)
        ReServiceHandler.eraseQueue(this)
    }

    fun processLocalDataGot(): Boolean {
        if (getOnCacheGotListener() == null) {
            return false
        }
        var responseClass: Type? = null
        val cadb = CacheApiDataBase(mAppContext)
        val caData = cadb.queryCacheData(getURL(), " ")
        if (caData != null) {
            try {
                //                JSONObject jso = new JSONObject(caData.getJsonData());
                responseClass = getResponseModuleName(false)
                //                jso = FrameworkConfiguration.decodeJosn(jso, responseClass);
                val resObj = JSONProcessor.toJSObject<T>(caData!!.jsonData, responseClass!!)
                (resObj as ICachableResponse).putDataLastUpdateTime(caData!!.datetime)
                cacheGot(resObj, null)
                return true
            } catch (e: Exception) {
                cacheGot(null, e)
            }

        } else {
            cacheGot(null, DescriptionException(getURL() + " is not found in cache database."))
        }
        if (cadb != null) {
            cadb.close()
        }
        return false
    }

    @Throws(DataConflictException::class, ModelParseFailed::class, ModelSynthesizeFailed::class)
    private fun processNetworkError(): Boolean {
        if (this is ICachableService) {
            val caReq = this as ICachableService
            //            if(!caReq.isCache()){
            val caUDB = CacheUploadDataBase(mAppContext)
            caUDB.insertOrUpdate(
                caReq.javaClass.name,
                this.getAssemblerShortURL()!!,
                " ",
                System.currentTimeMillis(),
                caReq.getCacheTime(),
                caReq.getConflictIntervention()
            )
            if (caUDB != null) {
                caUDB.close()
            }
            //            }
            return true
        }
        return false
    }

    fun processRemoteDataGot(resHeader: Map<String, String>?, data: ByteArray?, error: Exception?) {
        var error = error
        var obj: T? = null
        if (error != null) {
            if (error is NetBrokenException) {
                var cached = false
                try {
                    cached = processNetworkError()
                } catch (e: Exception) {
                    error = e
                }
                if(cached) error = NetBrokenUploadCached(" ")
            }
        } else {
            progressUpdate(ServiceStatus.STARTING_PARSER, 0)
            var jsonString: String? = null
            try {
                jsonString = String(data!!)
            } catch (e: UnsupportedEncodingException) {
                error = e
            }

            if (jsonString != null) {
                LogTag.debug(jsonString)
                val json: JSONObject? = null
                var cadb: CacheApiDataBase? = null
                try {
                    try {
                        //                        json = new JSONObject(jsonString);
                        val responseClass = getResponseModuleName(false)
                        //                        json = FrameworkConfiguration.decodeJosn(json, responseClass);
                        obj = JSONProcessor.toJSObject<T>(jsonString, responseClass!!)
                    } catch (e: Exception) {
                        throw ModelParseFailed(getURL() + " cannot parse response", json!!.toString())
                    }

                    if (this.getOnCacheGotListener() != null && obj is ICachableResponse) {
                        if (resHeader != null && resHeader[NetworkUtils.MODEL_CACHE_TIME] != null) {
                            var cacheTime = -1
                            try {
                                cacheTime = Integer.parseInt(resHeader[NetworkUtils.MODEL_CACHE_TIME]!!)
                            } catch (e: NumberFormatException) {
                            }

                            (obj as ICachableResponse).setCacheTime(cacheTime)
                        }
                        cadb = CacheApiDataBase(mAppContext)
                        cadb!!.insertOrUpdate(
                            getURL(),
                            " ",
                            jsonString,
                            System.currentTimeMillis(),
                            (obj as ICachableResponse).getCacheTime()
                        )
                    }
                } catch (e: ModelParseFailed) {
                    error = e
                } finally {
                    if (cadb != null) {
                        cadb!!.close()
                    }
                }
            }
        }
        remoteGot(obj, error)
        handleReService(resHeader, obj, error)
    }

    override fun progressUpdate(status: ServiceStatus, progress: Int) {
        if (opl != null) {
            handler.sendMessage(handler.obtainMessage(0, Runnable {
                if (opl != null) {
                    opl!!.onProgress(status, progress)
                }
            }))
        }
    }

    internal fun cacheGot(br: T?, error: Exception?) {
        if (ocgl != null) {
            handler.sendMessage(handler.obtainMessage(0, Runnable {
                if (ocgl != null) {
                    ocgl!!.onCacheGot(br, error)
                }
            }))
        }
    }

    internal fun remoteGot(br: T?, error: Exception?) {
        if (odgl != null) {
            handler.sendMessage(handler.obtainMessage(0, Runnable {
                if (odgl != null) {
                    odgl!!.onRequestGot(br, error)
                }
            }))
        }
    }

    @Keep
    override fun getURL(): String {
        return mUrl
    }

    @Keep
    override fun getOnProgressListener(): OnProgressListener? {
        return opl
    }

    override fun getResponseModuleName(isAssSuccessed: Boolean): Type? {
        return null
    }

    override fun getAssemblerShortURL(): String? {
        return null
    }

    override fun getVersion(): String? {
        return null
    }

    @Keep
    override fun getOnCacheGotListener(): OnCacheGotListener<T>? {
        return ocgl
    }

    @Keep
    override fun getOnDataGotListener(): OnDataGotListener<T>? {
        return odgl
    }

    @Keep
    override fun setOnCacheGotListener(ocgl: OnCacheGotListener<T>) {
        this.ocgl = ocgl
    }

    @Keep
    override fun setOnDataGotListener(odgl: OnDataGotListener<T>) {
        this.odgl = odgl
    }

    override fun setRequestParaObject(requestParaObj: Any) {

    }

    override fun getRequestParaObject(): Any {
        return " "
    }

    @Keep
    fun setLoadOnce(loadOnce: Boolean) {
        mLoadOnce = loadOnce
    }

    fun isLoadOnce(): Boolean {
        return mLoadOnce
    }

    @Keep
    override fun setOnProgressListener(opl: OnProgressListener) {
        this.opl = opl
    }

    override fun handlerString(): String {
        return getURL()
    }

    override fun remoteHandle(resHeader: Map<String, String>?, ob: Any?, error: Exception?) {
        remoteGot(ob as T?, error)
    }
}