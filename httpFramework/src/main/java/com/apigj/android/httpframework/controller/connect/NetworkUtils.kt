package com.apigj.android.httpframework.controller.connect

import android.content.Context
import android.net.ConnectivityManager
import com.apigj.android.httpframework.Keep
import java.util.concurrent.Executors

@Keep
class NetworkUtils {
    companion object {
        @Keep
        val MODEL_CACHE_TIME = "ModelCacheTime"

        private val DEFAULT_LOADING_THREADS = 4

        var threadPool = Executors
            .newFixedThreadPool(DEFAULT_LOADING_THREADS)

        //判断是否有网络
        @Keep
        fun isNetworkConnected(context: Context?): Boolean {
            if (context != null) {
                val mConnectivityManager = context
                    .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val mNetworkInfo = mConnectivityManager.activeNetworkInfo
                if (mNetworkInfo != null) {
                    return mNetworkInfo.isAvailable
                }
            }
            return false
        }

        //判断是否用wifi
        @Keep
        fun isWifiConnected(context: Context?): Boolean {
            if (context != null) {
                val mConnectivityManager = context
                    .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val mWiFiNetworkInfo = mConnectivityManager
                    .getNetworkInfo(ConnectivityManager.TYPE_WIFI)
                if (mWiFiNetworkInfo != null) {
                    return mWiFiNetworkInfo.isAvailable
                }
            }
            return false
        }

        //判断MOBILE网络是否可用
        @Keep
        fun isMobileConnected(context: Context?): Boolean {
            if (context != null) {
                val mConnectivityManager = context
                    .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val mMobileNetworkInfo = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
                if (mMobileNetworkInfo != null) {
                    return mMobileNetworkInfo.isAvailable
                }
            }
            return false
        }

        //获取当前网络连接的类型信息
        @Keep
        fun getConnectedType(context: Context?): Int {
            if (context != null) {
                val mConnectivityManager = context
                    .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val mNetworkInfo = mConnectivityManager.activeNetworkInfo
                if (mNetworkInfo != null && mNetworkInfo.isAvailable) {
                    return mNetworkInfo.type
                }
            }
            return -1
        }
    }
}