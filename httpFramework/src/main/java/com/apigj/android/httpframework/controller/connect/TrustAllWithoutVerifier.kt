package com.apigj.android.httpframework.controller.connect

import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSession

class TrustAllWithoutVerifier: HostnameVerifier {
    override fun verify(p0: String?, p1: SSLSession?): Boolean {
        return true
    }
}