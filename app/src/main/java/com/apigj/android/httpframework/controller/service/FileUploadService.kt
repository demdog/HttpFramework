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
import com.apigj.android.httpframework.controller.connect.serviceTask.UploadTask
import com.apigj.android.httpframework.controller.database.CacheFileDataBase
import com.apigj.android.httpframework.controller.database.CacheUploadDataBase
import com.apigj.android.httpframework.controller.database.CacheUploadFileDB
import com.apigj.android.httpframework.uilistener.OnProgressListener
import com.apigj.android.httpframework.uilistener.OnStringGotListener
import com.apigj.android.httpframework.utils.CacheUtils
import java.io.File
import java.util.*

@Keep
class FileUploadService(context: Context, url: String, filePath: String):IService, IRemoteHandler {
    private var osgl: OnStringGotListener? = null
    private var opl: OnProgressListener? = null
    private var netConfig = NetworkConfiguration.getCurrentNetworkConfiguration()
    private var mUrl = url
    private var mFilePath = filePath
    private var ult: UploadTask? = null
    //    private MultipartFormData formData;
    private var mSaveLocal: Boolean = false
    private var mLocale = context.resources.configuration.locale
    private var mAppContext = context.applicationContext
    private var cacheDir = CacheUtils.getTempFileDir(context)
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

    private fun fileSentService() {
        ult = UploadTask(mLocale)
        ult!!.execute(this)
    }

    @Keep
    override fun startRequest() {
        if (getOnStringGotListener() == null) {
            return
        }
        fileSentService()
    }

    @Keep
    fun saveLocalCopy() {
        mSaveLocal = true
    }

    override fun handlerString(): String {
        return getURL() + " " + getFilePath()
    }

    override fun remoteHandle(resHeader: Map<String, String>?, remoteThing: Any?, error: Exception?) {
        remoteGot(resHeader, remoteThing.toString(), error)
    }

    fun remoteGot(resHeader: Map<String, String>?, urlPath: String?, error: Exception?) {
        if (osgl != null) {
            handler.sendMessage(handler.obtainMessage(0, Runnable {
                if (osgl != null) {
                    osgl!!.onStringGot(urlPath, error)
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

    @Keep
    override fun setOnProgressListener(opl: OnProgressListener) {
        this.opl = opl
    }

    @Keep
    fun getOnStringGotListener(): OnStringGotListener? {
        return osgl
    }

    @Keep
    fun setOnStringGotListener(onStringGotListener: OnStringGotListener) {
        osgl = onStringGotListener
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

    @Keep
    fun cancelRequest() {
        ult!!.cancelUpload()
        ult = null
    }

    @Keep
    fun getFilePath(): String {
        return mFilePath
    }

    fun setHandleReService(): Boolean {
        if (ReServiceHandler.containsService(this)) {
            ReServiceHandler.putOnRemoteDataHandler(this)
            return true
        }
        ReServiceHandler.createQueue(this)
        return false
    }

    fun handleReService(resHeader: Map<String, String>?, urlPath: String?, error: Exception?) {
        ReServiceHandler.handleOnRemoteData(this, resHeader, urlPath, error)
        ReServiceHandler.eraseQueue(this)
    }

    fun saveUploadFile() {
        val cufb = CacheUploadFileDB(mAppContext)
        cufb.insertOrUpdate(getURL(), getFilePath(), System.currentTimeMillis())
        cufb.close()
    }

    //删除FileUploadRequest缓存，更新API缓存中的对应Path，如果非HTTP开头则删除API缓存
    fun deleteUploadFile(urlPath: String?) {
        var cufb: CacheUploadFileDB? = null
        var cudb: CacheUploadDataBase? = null
        try {
            cufb = CacheUploadFileDB(mAppContext)
            cufb!!.remove(getURL(), getFilePath())
            cudb = CacheUploadDataBase(mAppContext)
            if (urlPath != null && urlPath.startsWith("http")) {
                cudb!!.updateContent(getFilePath(), urlPath)
            } else {
                cudb!!.deleteContent(getFilePath())
            }
        } finally {
            if (cufb != null) {
                cufb!!.close()
            }
            if (cudb != null) {
                cudb!!.close()
            }
        }
    }

    fun processFile(resHeader: Map<String, String>?, urlPath: String) {
        if (mSaveLocal) {
            val cfdb = CacheFileDataBase(mAppContext)
            try {
                val localFilePath = CacheUtils.getCachePath(cacheDir) + CacheUtils.getTempName(urlPath)
                if (localFilePath != null) {
                    if (localFilePath !== getFilePath()) {
                        if (CacheUtils.fileExists(localFilePath)) {
                            CacheUtils.deleteFile(localFilePath)
                        }
                        CacheUtils.moveFile(getFilePath(), localFilePath)
                    }
                    var cacheTime = -1
                    if (resHeader != null && resHeader[NetworkUtils.MODEL_CACHE_TIME] != null) {
                        try {
                            cacheTime = Integer.parseInt(resHeader[NetworkUtils.MODEL_CACHE_TIME]!!)
                        } catch (e: NumberFormatException) {
                        }

                    }
                    cfdb.insertOrUpdatebyUrl(urlPath, localFilePath, System.currentTimeMillis(), cacheTime)
                }
            } finally {
                if (cfdb != null) {
                    cfdb.close()
                }
            }
        } else {
            //            CacheUtils.deleteFile(getFilePath());
        }
    }

    @Keep
    fun getCachedFileUploadService(context: Context): List<FileUploadService> {
        val res = ArrayList<FileUploadService>()
        val cufb = CacheUploadFileDB(context)
        try {
            val files = cufb.queryDBData()
            if (files != null) {
                for (file in files!!) {
                    res.add(FileUploadService(context, file.uploadUrl, file.filePath))
                }
            }
        } finally {
            if (cufb != null) {
                cufb.close()
            }
        }
        return res
    }
}