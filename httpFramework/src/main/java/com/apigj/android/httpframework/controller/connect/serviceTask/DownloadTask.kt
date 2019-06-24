package com.apigj.android.httpframework.controller.connect.serviceTask

import android.content.Context
import com.apigj.android.httpframework.controller.connect.DataHttpSession
import com.apigj.android.httpframework.controller.connect.IHttpDownloadEventHandler
import com.apigj.android.httpframework.controller.connect.NetworkUtils
import com.apigj.android.httpframework.controller.connect.ServiceStatus
import com.apigj.android.httpframework.controller.exceptions.UserCancel
import com.apigj.android.httpframework.controller.service.FileGetService
import com.apigj.android.httpframework.utils.CacheUtils
import java.io.File
import java.util.*

class DownloadTask(appContext: Context, cachePath: File, locale: Locale) : IRequestTask, IHttpDownloadEventHandler {

    private var fileStringReq: FileGetService? = null

    private var downloadCommand: Int = 0
    private val downloadContinue = 0
    private val downloadCancel = 1
    private val downloadPause = 2
    private var mCachePath = cachePath
    private var mLocale = locale
    private var mAppContext = appContext

    override fun execute(vararg params: Any) {
        fileStringReq = params[0] as FileGetService
        fileStringReq!!.progressUpdate(ServiceStatus.PREEXECUTE, 0)
        NetworkUtils.threadPool.execute(this)
    }

    override fun publishStatus(status: ServiceStatus, progress: Int) {
        fileStringReq!!.progressUpdate(status, progress)
    }

    override fun connectFinished(headFields: Map<String, String>?, data: ByteArray?, error: Exception?) {}

    override fun downLoadFinished(resHeader: Map<String, String>, file: File) {
        fileStringReq!!.progressUpdate(ServiceStatus.DISCONNECTED, 0)
        //下载结束
        //输出下载文件原来的存放目录
        val locationPath = file.absolutePath
        try {
            var filePath = fileStringReq!!.getFilePath()
            if (filePath == null) {
                filePath = CacheUtils.getCachePath(mCachePath) + CacheUtils.getTempName(fileStringReq!!.getURL())
            } else if (filePath!!.endsWith("/")) {
                filePath = filePath!! + CacheUtils.getTempName(fileStringReq!!.getURL())
            }
            //拷贝到用户目录
            //创建文件管理器
            //        if(!CacheUtils.dirExists(dir: CacheUtils.getCachePath())){
            //            CacheUtils.createDir(dir: CacheUtils.getCachePath())
            //        }

            if (CacheUtils.fileExists(filePath)) {
                CacheUtils.deleteFile(filePath)
            }
            CacheUtils.moveFile(locationPath, filePath)
            fileStringReq!!.remoteGot(resHeader, filePath, null)
            fileStringReq!!.handleReService(resHeader, filePath, null)
        } catch (error: Exception) {
            fileStringReq!!.remoteGot(resHeader, null, error)
            fileStringReq!!.handleReService(resHeader, null, error)
        }

        fileStringReq!!.progressUpdate(ServiceStatus.END_PROGRESS, 0)
    }

    override fun downLoadError(resHeader: Map<String, String>?, file: File, error: Exception?) {
        if (error != null) {
            if (error is UserCancel && (error as UserCancel).getCancelType() === downloadCancel) {
                deleteResumeData(file)
            } else {
                saveResumeData(resHeader!!, file)
            }
            fileStringReq!!.remoteGot(resHeader, null, error)
            fileStringReq!!.handleReService(resHeader, null, error)
            fileStringReq!!.progressUpdate(ServiceStatus.END_PROGRESS, 0)
        }
    }

    override fun run() {
        val tempFilePath = fileStringReq!!.processCache()
        var partialData: File? = null
        if (tempFilePath != null) {
            if (tempFilePath === "") {
                fileStringReq!!.progressUpdate(ServiceStatus.END_PROGRESS, 0)
                return
            }
            if (CacheUtils.isDownloadFinished(mCachePath, tempFilePath)) {
                fileStringReq!!.fileGot(tempFilePath, null)
                fileStringReq!!.progressUpdate(ServiceStatus.END_PROGRESS, 0)
                return
            } else {
                partialData = File(tempFilePath)
                if (!partialData!!.exists()) {
                    partialData = null
                }
            }
        }
        if (fileStringReq!!.setHandleReService()) {
            return
        }
        if (partialData == null) {
            partialData = File(CacheUtils.getPartialPath(mCachePath) + CacheUtils.getTempName(fileStringReq!!.getURL()))
        }
        DataHttpSession.httpFileDownload(
            mLocale,
            fileStringReq!!.getURL(),
            partialData,
            this,
            fileStringReq!!.getNetConfig()
        )
    }

    fun cancelDownload() {
        downloadCommand = downloadCancel
    }

    fun pauseDownload() {
        downloadCommand = downloadPause
    }

    @Throws(UserCancel::class)
    override fun cancelDownloadAction(resHeader: Map<String, String>, file: File) {
        if (downloadCommand == downloadCancel) {
            //取消下载任务，把下载好的数据删除
            fileStringReq!!.progressUpdate(ServiceStatus.PROGRESS_CANCELED, 0)
            throw UserCancel(fileStringReq!!.getURL(), downloadCancel)
        }
    }

    @Throws(UserCancel::class)
    override fun pauseDownloadAction(resHeader: Map<String, String>, file: File) {
        if (downloadCommand == downloadPause) {
            //取消下载任务，把下载好的数据存起来
            fileStringReq!!.progressUpdate(ServiceStatus.PROGRESS_PAUSED, 0)
            throw UserCancel(fileStringReq!!.getURL(), downloadPause)
        }
    }

    override fun saveResumeData(resHeader: Map<String, String>, resumeData: File) {
        if (resumeData != null && resumeData.exists()) {
            fileStringReq!!.savePartialFile(resHeader, resumeData.absolutePath)
        }
    }

    private fun deleteResumeData(file: File) {
        fileStringReq!!.clearCachedPath(mAppContext, fileStringReq!!.getURL(), file.absolutePath)
    }
}