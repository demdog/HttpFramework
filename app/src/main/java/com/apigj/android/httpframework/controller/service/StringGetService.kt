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
import com.apigj.android.httpframework.controller.connect.serviceTask.GetDataTask
import com.apigj.android.httpframework.controller.database.CacheApiDataBase
import com.apigj.android.httpframework.controller.exceptions.DescriptionException
import com.apigj.android.httpframework.uilistener.OnProgressListener
import com.apigj.android.httpframework.uilistener.OnStringCacheGotListener
import com.apigj.android.httpframework.uilistener.OnStringGotListener
import java.io.UnsupportedEncodingException
import java.util.*

@Keep
class StringGetService(context: Context, url: String):IService, IRemoteHandler {

    private var ocgl: OnStringCacheGotListener? = null
    private var odgl: OnStringGotListener? = null
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
        val pdt = GetDataTask(mLocale)
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

    internal fun handleReService(resHeader: Map<String, String>?, response: String?, error: Exception?) {
        ReServiceHandler.handleOnRemoteData(this, resHeader, response, error)
        ReServiceHandler.eraseQueue(this)
    }

    fun processLocalDataGot(): Boolean {
        if (getOnStringCacheGotListener() == null) {
            return false
        }
        val cadb = CacheApiDataBase(mAppContext)
        try {
            val caData = cadb.queryCacheData(getURL(), " ")
            if (caData != null) {
                cacheGot(caData!!.jsonData, caData!!.datetime, null)
                return true
            } else {
                cacheGot(null, 0, DescriptionException(getURL() + " is not found in cache database."))
            }
        } catch (error: Exception) {
            cacheGot(null, 0, error)
        } finally {
            if (cadb != null) {
                cadb.close()
            }
        }
        return false
    }

    fun processRemoteDataGot(resHeader: Map<String, String>?, data: ByteArray?, error: Exception?) {
        var error = error
        var obj: String? = null
        var er: Exception? = error
        if (er == null) {
            try {
                obj = String(data!!)
            } catch (e: UnsupportedEncodingException) {
                error = e
            }

            if (obj != null) {
                var ctime = -1
                if (resHeader != null && resHeader[NetworkUtils.MODEL_CACHE_TIME] != null) {
                    try {
                        ctime = Integer.parseInt(resHeader[NetworkUtils.MODEL_CACHE_TIME]!!)
                    } catch (e: NumberFormatException) {
                    }

                }
                var cadb: CacheApiDataBase? = null
                try {
                    cadb = CacheApiDataBase(mAppContext)
                    cadb!!.insertOrUpdate(getURL(), " ", obj, System.currentTimeMillis(), ctime)
                } catch (e: Exception) {
                    er = e
                } finally {
                    if (cadb != null) {
                        cadb!!.close()
                    }
                }
            }
        }
        remoteGot(obj, er)
        handleReService(resHeader, obj, er)
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

    internal fun cacheGot(str: String?, dateTime: Long?, error: Exception?) {
        if (ocgl != null) {
            handler.sendMessage(handler.obtainMessage(0, Runnable {
                if (ocgl != null) {
                    ocgl!!.onStringCacheGot(str, dateTime, error)
                }
            }))
        }
    }

    internal fun remoteGot(str: String?, error: Exception?) {
        if (odgl != null) {
            handler.sendMessage(handler.obtainMessage(0, Runnable {
                if (odgl != null) {
                    odgl!!.onStringGot(str, error)
                }
            }))
        }
    }

    @Keep
    fun getOnStringCacheGotListener(): OnStringCacheGotListener? {
        return ocgl
    }

    @Keep
    fun getOnStringGotListener(): OnStringGotListener? {
        return odgl
    }

    @Keep
    override fun getURL(): String {
        return mUrl
    }

    @Keep
    override fun getOnProgressListener(): OnProgressListener? {
        return opl
    }

    @Keep
    fun setOnStringCacheGotListener(ocgl: OnStringCacheGotListener) {
        this.ocgl = ocgl
    }

    @Keep
    fun setOnStringGotListener(odgl: OnStringGotListener) {
        this.odgl = odgl
    }

    @Keep
    override fun setOnProgressListener(opl: OnProgressListener) {
        this.opl = opl
    }

    override fun handlerString(): String {
        return getURL()
    }

    override fun remoteHandle(resHeader: Map<String, String>?, ob: Any?, error: Exception?) {
        remoteGot(ob as String, error)
    }

    @Keep
    fun setLoadOnce(loadOnce: Boolean) {
        mLoadOnce = loadOnce
    }

    fun isLoadOnce(): Boolean {
        return mLoadOnce
    }

}