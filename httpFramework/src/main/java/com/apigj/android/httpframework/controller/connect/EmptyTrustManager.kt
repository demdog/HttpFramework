package com.apigj.android.httpframework.controller.connect

import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

class EmptyTrustManager: X509TrustManager {

    override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}