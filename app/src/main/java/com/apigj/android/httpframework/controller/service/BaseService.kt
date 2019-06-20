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
import com.apigj.android.httpframework.controller.connect.serviceTask.PostDataTask
import com.apigj.android.httpframework.controller.database.CacheApiDataBase
import com.apigj.android.httpframework.controller.database.CacheUploadDataBase
import com.apigj.android.httpframework.controller.exceptions.*
import com.apigj.android.httpframework.controller.formatTool.JSONProcessor
import com.apigj.android.httpframework.controller.response.ICachableResponse
import com.apigj.android.httpframework.uilistener.OnCacheGotListener
import com.apigj.android.httpframework.uilistener.OnDataGotListener
import com.apigj.android.httpframework.uilistener.OnProgressListener
import com.apigj.android.httpframework.utils.LogTag
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import java.lang.reflect.Type
import java.util.*

@Keep
abstract class BaseService<T>(context: Context?) : IBaseService<T>, IRemoteHandler {
    protected var mLocale: Locale
    protected var mAppContext: Context

    private var ocgl: OnCacheGotListener<T>? = null
    private var odgl: OnDataGotListener<T>? = null
    private var opl: OnProgressListener? = null

    //    protected Context mContext;

    private var netConfig = NetworkConfiguration.getCurrentNetworkConfiguration()
    private var mRequestObj: Any? = null
    protected var para: String? = null

    private var mLoadOnce: Boolean = false
    private val handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            (msg.obj as Runnable).run()
        }
    }

    init{
        if (context == null) {
            throw NullPointerException()
        }
        mLocale = context.resources.configuration.locale
        mAppContext = context.applicationContext
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

//    @Override
//    public String getURL() {
//        return null;
//    }

    @Keep
    override fun startRequest() {
        val pdt = PostDataTask(mLocale)
        //        if(TextUtils.isEmpty(getParameter())){
        //            Exception e = new DescriptionException("Parameter create failed");
        //            cacheGot(null, e);
        //            remoteGot(null, e);
        //            return;
        //        }
//        if (mRequestObj is ICompleteValidate) {
//            val le = (mRequestObj as ICompleteValidate).checkVarRequire()
//            if (le != null) {
//                cacheGot(null, le)
//                remoteGot(null, le)
//                return
//            }
//
//        }
        pdt.execute(this)
    }

    fun setHandleReService(): Boolean {
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

    @Throws(DataConflictException::class, ModelParseFailed::class, ModelSynthesizeFailed::class)
    private fun processNetworkError(): Boolean {
        var para = getParameter()
        para?: return false
        if (this is ICachableService) {
            val caReq = this as ICachableService
            //            if(!caReq.isCache()){
            val caUDB = CacheUploadDataBase(mAppContext)
            caUDB.insertOrUpdate(
                caReq.javaClass.name,
                this.getAssemblerShortURL()!!,
                para,
                System.currentTimeMillis(),
                caReq.getCacheTime(),
                caReq.getConflictIntervention()
            )
            caUDB.close()
            return true
        }
        return false
    }

    //来自后台线程LocalDataHandler的回调，Delegate需同步主线程
    fun processLocalDataGot(): Boolean {
        if (getOnCacheGotListener() == null) {
            return false
        }
        var responseClass: Type? = null
        val cadb = CacheApiDataBase(mAppContext)
        var para = getParameter()
        para ?: return false
        val caData = cadb.queryCacheData(getURL(), para)
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
            cacheGot(null, DescriptionException(getURL() + " " + getParameter() + " is not found in cache database."))
        }
        if (cadb != null) {
            cadb.close()
        }
        return false
    }

    //来自后台线程RemoteDataHandler的回调，Delegate需同步主线程
    fun processRemoteDataGot(resHeader: Map<String, String>?, jsonData: ByteArray?, error: Exception?) {
        var error = error
        var obj:T? = null
        if (error != null) {
            if (error is NetBrokenException) {
                var cached = false
                try {
                    cached = processNetworkError()
                } catch (e: Exception) {
                    error = e
                }
                if(cached) error = NetBrokenUploadCached(this.getParameter())
            }
        } else {
            progressUpdate(ServiceStatus.STARTING_PARSER, 0)
            var jsonString: String? = null
            try {
                jsonString = String(jsonData!!)
            } catch (e: UnsupportedEncodingException) {
                error = e
            }

            if (jsonString != null) {
                LogTag.debug(jsonString)
                val json: JSONObject? = null
                var cadb: CacheApiDataBase? = null
                var cudb: CacheUploadDataBase? = null
                try {
                    try {
                        //                        json = new JSONObject(jsonString);
                        val responseClass = getResponseModuleName(false)
                        //                        json = FrameworkConfiguration.decodeJosn(json, responseClass);
                        obj = JSONProcessor.toJSObject<T>(jsonString, responseClass!!)
                    } catch (e: Exception) {
                        throw ModelParseFailed(getURL() + " cannot parse response", json!!.toString())
                    }
                    //save cache response
                    if (getOnCacheGotListener() != null && obj is ICachableResponse) {
                        if (resHeader != null && resHeader[NetworkUtils.MODEL_CACHE_TIME] != null) {
                            var cacheTime = -1
                            try {
                                cacheTime = Integer.parseInt(resHeader[NetworkUtils.MODEL_CACHE_TIME])
                            } catch (e: NumberFormatException) {
                            }

                            (obj as ICachableResponse).setCacheTime(cacheTime)
                        }
                        cadb = CacheApiDataBase(mAppContext)
                        cadb!!.insertOrUpdate(
                            getURL(),
                            getParameter()!!,
                            jsonString,
                            System.currentTimeMillis(),
                            (obj as ICachableResponse).getCacheTime()
                        )
                    }
                    if (this is ICachableService) {
                        if ((this as ICachableService).isCache()) {
                            cudb = CacheUploadDataBase(mAppContext)
                            cudb!!.deleteCacheUploadData(this.javaClass.name, getParameter()!!)
                        }
                    }
                } catch (e: ModelParseFailed) {
                    error = e
                } finally {
                    if (cadb != null) {
                        cadb!!.close()
                    }
                    if (cudb != null) {
                        cudb!!.close()
                    }
                }
            }
        }
        remoteGot(obj, error)
        handleReService(resHeader, obj, error)
    }

    fun getParameter(): String? {
        if (para == null) {
            para = JSONProcessor.toJSString(mRequestObj!!)
            LogTag.debug("URL:" + getURL() + " Parameter:" + para)
        }
        if (para == null) {
            LogTag.debug("URL:" + getURL() + " Parameter is null")
        }
        return para
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

    internal fun cacheGot(obj: T?, error: Exception?) {
        if (ocgl != null) {
            handler.sendMessage(handler.obtainMessage(0, Runnable {
                if (ocgl != null) {
                    ocgl!!.onCacheGot(obj, error)
                }
            }))
        }
    }

    internal fun remoteGot(obj: T?, error: Exception?) {
        if (odgl != null) {
            handler.sendMessage(handler.obtainMessage(0, Runnable {
                if (odgl != null) {
                    odgl!!.onRequestGot(obj, error)
                }
            }))
        }
    }

    @Keep
    fun setLoadOnce(loadOnce: Boolean) {
        mLoadOnce = loadOnce
    }

    fun isLoadOnce(): Boolean {
        return mLoadOnce
    }

//    @Override
//    public String getResponseModuleName(JSONObject js){
//        return null;
//    }
//
//    @Override
//    public String getAssemblerShortURL() {
//        return null;
//    }
//
//    @Override
//    public String getVersion() {
//        return null;
//    }

    @Keep
    override fun getOnCacheGotListener(): OnCacheGotListener<T>? {
        return ocgl
    }

    @Keep
    override fun getOnDataGotListener(): OnDataGotListener<T>? {
        return odgl
    }

    @Keep
    override fun getOnProgressListener(): OnProgressListener? {
        return opl
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
        this.mRequestObj = requestParaObj
    }

    override fun getRequestParaObject(): Any? {
        return this.mRequestObj
    }

    @Keep
    override fun setOnProgressListener(opl: OnProgressListener) {
        this.opl = opl
    }

    override fun handlerString(): String {
        return getURL() + " " + getParameter()
    }

    override fun remoteHandle(resHeader: Map<String, String>?, ob: Any?, error: Exception?) {
        remoteGot(ob as T?, error)
    }

}