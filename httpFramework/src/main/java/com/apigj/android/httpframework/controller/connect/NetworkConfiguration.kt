package com.apigj.android.httpframework.controller.connect

import com.apigj.android.httpframework.Keep
import java.util.HashMap
import java.util.concurrent.Executors
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.TrustManager

@Keep
class NetworkConfiguration:Cloneable {

    private var timeout = 30000//请求超时时间
    private var hostnameVerifier: HostnameVerifier = TrustAllWithoutVerifier()//https安全验证的实现
    private var trustManager: TrustManager = EmptyTrustManager()
    private val extraHead = HashMap<String, String>()
    private var truckSize = 4096
    private var gzip = false

    @Throws(CloneNotSupportedException::class)
    public override fun clone(): Any {
        val res = NetworkConfiguration()
        res.timeout = this.timeout//请求超时时间
        res.hostnameVerifier = this.hostnameVerifier//https安全验证的实现
        res.trustManager = this.trustManager
        res.extraHead.putAll(this.extraHead)
        res.truckSize = this.truckSize
        res.gzip = this.gzip
        return res
    }

    @Keep
    fun setHostnameVerifier(hostnameVerifier: HostnameVerifier) {
        this.hostnameVerifier = hostnameVerifier
    }

    fun getHostVerifier(): HostnameVerifier {
        return hostnameVerifier
    }

    fun getTrustManager(): TrustManager {
        return trustManager
    }

    @Keep
    fun setTimeOut(timeout: Int) {
        this.timeout = timeout
    }

    fun getTimeOut(): Int {
        return timeout
    }

    fun getExtraHead(): Map<String, String> {
        return extraHead
    }

    @Keep
    fun putExtraHead(key: String, content: String) {
        extraHead[key] = content
    }

    @Keep
    fun setTruckSize(truckSize: Int) {
        this.truckSize = truckSize
    }

    fun getTruckSize(): Int {
        return truckSize
    }

    @Keep
    fun setGzip(gzip: Boolean) {
        this.gzip = gzip
    }

    fun getGzip(): Boolean {
        return gzip
    }

    fun getCachePartialFileSize(): Long {
        return 5242880
    }

    companion object {
        val USER_AGENT = "android_APP"
        private var instance = NetworkConfiguration()
        @Keep
        fun setThreadPoolSize(poolSize: Int) {
            NetworkUtils.threadPool = Executors
                .newFixedThreadPool(poolSize)
        }

        @Keep
        fun setCurrentNetworkConfiguration(conf: NetworkConfiguration) {
            instance = conf
        }

        @Keep
        fun getDefaultNetworkConfiguration(): NetworkConfiguration {
            return NetworkConfiguration()
        }

        @Keep
        fun getCurrentNetworkConfiguration(): NetworkConfiguration {
            return instance
        }

        @Keep
        @Throws(CloneNotSupportedException::class)
        fun copyCurrentNetworkConfiguration(): NetworkConfiguration {
            return instance.clone() as NetworkConfiguration
        }
    }
}