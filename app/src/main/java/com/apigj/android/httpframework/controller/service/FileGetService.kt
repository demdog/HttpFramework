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
import com.apigj.android.httpframework.controller.connect.serviceTask.DownloadTask
import com.apigj.android.httpframework.controller.database.CacheFileDataBase
import com.apigj.android.httpframework.controller.response.ICachableResponse
import com.apigj.android.httpframework.uilistener.OnFileGotListener
import com.apigj.android.httpframework.uilistener.OnProgressListener
import com.apigj.android.httpframework.utils.CacheUtils
import java.io.File
import java.util.*

@Keep
class FileGetService(context: Context, url: String):IService, IRemoteHandler, ICachableResponse {

    private var ofgl: OnFileGotListener? = null
    private var opl: OnProgressListener? = null
    private var netConfig = NetworkConfiguration.getCurrentNetworkConfiguration()
    private var mUrl = url
    private var mFilePath: String? = null
    private var mRefleshCache = false
    private var mNeedCache = true
    //    private Context mContext;
    private var dlt: DownloadTask? = null
    private var mCachePath = CacheUtils.getTempFileDir(context)
    private var mLocale = context.resources.configuration.locale
    private var mAppContext = context.applicationContext
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

    private fun fileGetService() {
        dlt = DownloadTask(mAppContext, mCachePath, mLocale)
        dlt!!.execute(this)
    }

    @Keep
    override fun startRequest() {
        if (getOnFileGotListener() == null) {
            return
        }
        fileGetService()
    }

    @Keep
    fun cancelRequest() {
        dlt!!.cancelDownload()
        dlt = null
    }

    @Keep
    fun pauseRequest() {
        dlt!!.pauseDownload()
        dlt = null
    }

    override fun handlerString(): String {
        return getURL()
    }

    override fun remoteHandle(resHeader: Map<String, String>?, obj: Any?, error: Exception?) {
        remoteGot(resHeader, obj.toString(), error)
    }

    @Keep
    fun setCacheDisable() {
        mNeedCache = false
    }

    @Keep
    fun setCacheEnable() {
        mNeedCache = true
    }

    @Keep
    fun needCache(): Boolean {
        return mNeedCache
    }

    @Keep
    fun refleshCache() {
        mRefleshCache = true
    }

    @Keep
    fun getRefleshCache(): Boolean {
        return mRefleshCache
    }

    @Keep
    fun setFilePath(filePath: String) {
        mFilePath = filePath
    }

    @Keep
    fun getFilePath(): String? {
        return mFilePath
    }

    @Keep
    override fun getURL(): String {
        return mUrl
    }

    @Keep
    fun getOnFileGotListener(): OnFileGotListener? {
        return ofgl
    }

    @Keep
    fun setOnFileGotListener(ofgl: OnFileGotListener) {
        this.ofgl = ofgl
    }

    @Keep
    override fun getOnProgressListener(): OnProgressListener? {
        return opl
    }

    @Keep
    override fun setOnProgressListener(opl: OnProgressListener) {
        this.opl = opl
    }

    @Keep
    override fun progressUpdate(status: ServiceStatus, progress: Int) {
        if (opl != null) {
            handler.sendMessage(handler.obtainMessage(0, Runnable {
                if (opl != null) {
                    opl!!.onProgress(status, progress)
                }
            }))
        }
    }

    fun setHandleReService(): Boolean {
        if (ReServiceHandler.containsService(this)) {
            ReServiceHandler.putOnRemoteDataHandler(this)
            return true
        }
        ReServiceHandler.createQueue(this)
        return false
    }

    fun handleReService(resHeader: Map<String, String>?, filePath: String?, error: Exception?) {
        ReServiceHandler.handleOnRemoteData(this, resHeader, filePath, error)
        ReServiceHandler.eraseQueue(this)
    }

    fun fileGot(filePath: String?, error: Exception?) {
        if (getOnFileGotListener() != null) {
            handler.sendMessage(handler.obtainMessage(0, Runnable {
                if (ofgl != null) {
                    ofgl!!.onFileGot(filePath, error)
                }
            }))
        }
    }

    fun remoteGot(resHeader: Map<String, String>?, filePath: String?, error: Exception?) {
        var error = error
        if (mNeedCache) {
            if (error == null) {
                val cfdb = CacheFileDataBase(mAppContext)
                try {
                    cfdb.deleteCacheDataWithPrefix(getURL(), CacheUtils.getPartialPath(mCachePath))
                    if (filePath != null) {
                        if (filePath.startsWith(CacheUtils.getCachePath(mCachePath))) {
                            val cfData = cfdb.queryDBDataWithPrefix(getURL(), CacheUtils.getCachePath(mCachePath))
                            if (cfData != null) {
                                if (filePath != cfData!!.filePath) {
                                    CacheUtils.deleteFile(cfData!!.filePath)
                                    cfdb.deleteCacheData(getURL(), cfData!!.filePath)
                                }
                            }
                        }
                        var ctime = -1
                        if (resHeader != null && resHeader[NetworkUtils.MODEL_CACHE_TIME] != null) {
                            try {
                                ctime = Integer.parseInt(resHeader[NetworkUtils.MODEL_CACHE_TIME]!!)
                            } catch (e: NumberFormatException) {
                            }

                        }
                        cfdb.deleteCacheData(getURL(), filePath)
                        cfdb.insertOrUpdatebyUrl(getURL(), filePath, System.currentTimeMillis(), ctime)
                    }
                } catch (e: Exception) {
                    error = e
                } finally {
                    if (cfdb != null) {
                        cfdb.close()
                    }
                }
            }
        }
        fileGot(filePath, error)
    }

    fun savePartialFile(resHeader: Map<String, String>?, filePath: String) {
        val cfdb = CacheFileDataBase(mAppContext)
        try {
            val cfData = cfdb.queryDBDataWithPrefix(getURL(), CacheUtils.getPartialPath(mCachePath))
            if (cfData != null) {
                if (filePath !== cfData!!.filePath) {
                    CacheUtils.deleteFile(cfData!!.filePath)
                    cfdb.deleteCacheData(getURL(), cfData!!.filePath)
                }
            }
            var ctime = -1
            if (resHeader != null && resHeader[NetworkUtils.MODEL_CACHE_TIME] != null) {
                try {
                    ctime = Integer.parseInt(resHeader[NetworkUtils.MODEL_CACHE_TIME]!!)
                } catch (e: NumberFormatException) {
                }

            }
            cfdb.insertOrUpdate(getURL(), filePath, System.currentTimeMillis(), ctime)
        } finally {
            if (cfdb != null) {
                cfdb.close()
            }
        }
    }

    @Keep
    fun isCached(context: Context, url: String): Boolean {
        return isCached(context, url, null)
    }

    @Keep
    fun isCached(context: Context, url: String, filePath: String?): Boolean {
        val cfdb = CacheFileDataBase(context)
        try {
            var cfData = cfdb.queryDBDataWithPrefix(url, CacheUtils.getPartialPath(context.cacheDir))
            if (cfData != null) {
                if (CacheUtils.fileExists(cfData!!.filePath)) {
                    return false
                } else {
                    cfdb.deleteCacheData(url, cfData!!.filePath)
                }
            }
            cfData = cfdb.queryCachePath(url, filePath)
            if (cfData != null) {
                if (!CacheUtils.fileExists(cfData!!.filePath)) {
                    cfdb.deleteCacheData(url, cfData!!.filePath)
                } else {
                    return true
                }
            }
        } finally {
            if (cfdb != null) {
                cfdb.close()
            }
        }
        return false
    }

    @Keep
    fun getCachedPath(context: Context, url: String): String? {
        return getCachedPath(context, url, null)
    }

    @Keep
    fun getCachedPath(context: Context, url: String, filePath: String?): String? {
        val cfdb = CacheFileDataBase(context)
        try {
            var cfData = cfdb.queryDBDataWithPrefix(url, CacheUtils.getPartialPath(context.cacheDir))
            if (cfData != null) {
                if (CacheUtils.fileExists(cfData!!.filePath)) {
                    return null
                } else {
                    cfdb.deleteCacheData(url, cfData!!.filePath)
                }
            }
            cfData = cfdb.queryCachePath(url, filePath)
            if (cfData != null) {
                if (CacheUtils.fileExists(cfData!!.filePath)) {
                    return cfData!!.filePath
                } else {
                    cfdb.deleteCacheData(url, cfData!!.filePath)
                }
            }
        } finally {
            if (cfdb != null) {
                cfdb.close()
            }
        }
        return null
    }

    @Keep
    fun clearCachedPath(context: Context, url: String) {
        val cfdb = CacheFileDataBase(context)
        try {
            val arr = cfdb.queryAllCachePath(url)
            if (arr != null && !arr!!.isEmpty()) {
                for (cfi in arr!!) {
                    CacheUtils.deleteFile(cfi.filePath)
                }
            }
            cfdb.deleteCacheData(url)
        } finally {
            if (cfdb != null) {
                cfdb.close()
            }
        }
    }

    @Keep
    fun clearCachedPath(context: Context, url: String, filePath: String) {
        val cfdb = CacheFileDataBase(context)
        try {
            val cfData = cfdb.queryCachePath(url, filePath)
            if (cfData != null) {
                CacheUtils.deleteFile(cfData!!.filePath)
                cfdb.deleteCacheData(url, cfData!!.filePath)
            }
        } finally {
            if (cfdb != null) {
                cfdb.close()
            }
        }
    }

    fun processCache(): String? {
        if (mRefleshCache) {
            val cfdb = CacheFileDataBase(mAppContext)
            try {
                var cfData = cfdb.queryDBDataWithPrefix(getURL(), CacheUtils.getPartialPath(mCachePath))
                if (cfData != null) {
                    CacheUtils.deleteFile(cfData!!.filePath)
                }
                cfdb.deleteCacheDataWithPrefix(getURL(), CacheUtils.getPartialPath(mCachePath))
                if (mFilePath == null) {
                    cfData = cfdb.queryDBDataWithPrefix(getURL(), CacheUtils.getCachePath(mCachePath))
                    if (cfData != null) {
                        CacheUtils.deleteFile(cfData!!.filePath)
                    }
                    cfdb.deleteCacheDataWithPrefix(getURL(), CacheUtils.getCachePath(mCachePath))
                } else {
                    CacheUtils.deleteFile(mFilePath!!)
                    cfdb.deleteCacheData(getURL(), mFilePath!!)
                }
            } finally {
                if (cfdb != null) {
                    cfdb.close()
                }
            }
            return null
        }
        if (getOnFileGotListener() == null) {
            return ""
        }
        val cfdb = CacheFileDataBase(mAppContext)
        try {
            //查找断点文件
            var cfData = cfdb.queryDBDataWithPrefix(getURL(), CacheUtils.getPartialPath(mCachePath))
            if (cfData != null) {
                if (CacheUtils.fileExists(cfData!!.filePath)) {
                    return cfData!!.filePath
                } else {
                    cfdb.deleteCacheData(getURL(), cfData!!.filePath)
                }
            }
            //是否指定文件位置
            if (mFilePath == null) {
                val cfDataList = cfdb.queryAllCachePath(getURL())
                for (tcfData in cfDataList) {
                    if (CacheUtils.fileExists(tcfData.filePath)) {
                        return tcfData.filePath
                    } else {
                        cfdb.deleteCacheData(getURL(), tcfData.filePath)
                    }
                }
            } else {
                //查找filePath位置的文件是否下载
                cfData = cfdb.queryCachePath(getURL(), mFilePath!!)
                if (cfData != null) {
                    if (CacheUtils.fileExists(cfData!!.filePath)) {
                        return cfData!!.filePath
                    } else {
                        cfdb.deleteCacheData(getURL(), cfData!!.filePath)
                    }
                }
                //查找其它位置是否有缓存
                val cfDataList = cfdb.queryAllCachePath(getURL())
                for (tcfData in cfDataList) {
                    if (CacheUtils.fileExists(tcfData.filePath)) {
                        CacheUtils.copyFile(tcfData.filePath, mFilePath!!)
                        cfdb.insertOrUpdate(getURL(), mFilePath!!, tcfData.datetime, tcfData.cacheTime)
                        return mFilePath
                    } else {
                        cfdb.deleteCacheData(getURL(), tcfData.filePath)
                    }
                }
            }
        } finally {
            if (cfdb != null) {
                cfdb.close()
            }
        }
        return null
    }

    private var mUpdateTime: Long = 0
    private var mIsCache: Boolean = false
    private var mCacheTime = -1
    override fun putDataLastUpdateTime(updateTime: Long) {
        mUpdateTime = updateTime
        mIsCache = true
    }

    @Keep
    override fun getDataLastUpdateTime(): Long {
        return mUpdateTime
    }

    @Keep
    override fun isCache(): Boolean {
        return mIsCache
    }

    override fun getCacheTime(): Int {
        return mCacheTime
    }

    @Keep
    override fun setCacheTime(cacheTime: Int) {
        this.mCacheTime = cacheTime
    }
}