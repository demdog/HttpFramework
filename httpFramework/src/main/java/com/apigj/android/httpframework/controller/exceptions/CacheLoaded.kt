package com.apigj.android.httpframework.controller.exceptions

import android.os.Parcel
import android.os.Parcelable
import com.apigj.android.httpframework.Keep
import java.lang.Exception

@Keep
class CacheLoaded(message: String?) : Exception("$message is working in LoadOnce Mode, will not reflush data in that mode"){

}
