package com.apigj.android.httpframework.utils

import android.content.Context
import com.apigj.android.httpframework.Keep
import com.apigj.android.httpframework.controller.database.CacheApiDataBase
import com.apigj.android.httpframework.controller.database.CacheFileDataBase
import com.apigj.android.httpframework.controller.database.CacheUploadDataBase
import com.apigj.android.httpframework.controller.database.CacheUploadFileDB

class FrameworkConfiguration {

    companion object {
        private var isPublish = false
        private var Code = "code"
        private var SuccessCode = 0
        //    public final static String t = "fuguapp201612051";
        @Keep
        var t = ""

        @Keep
        fun setPublish(isPublish: Boolean) {
            FrameworkConfiguration.isPublish = isPublish
        }

        @Keep
        fun setCodeString(codeString: String) {
            Code = codeString
        }

        @Keep
        fun setSuccessCode(successCode: Int) {
            SuccessCode = successCode
        }

        @Keep
        fun getCodeString(): String {
            return Code
        }

        @Keep
        fun getSuccessCode(): Int {
            return SuccessCode
        }

        @Keep
        fun cleanDataBase(context: Context) {
            var cufd: CacheUploadFileDB? = null
            var cudb: CacheUploadDataBase? = null
            var cfdb: CacheFileDataBase? = null
            var cadb: CacheApiDataBase? = null
            try {
                cufd = CacheUploadFileDB(context)
                cufd.removeAll()
                cudb = CacheUploadDataBase(context)
                cudb.removeAll()
                cfdb = CacheFileDataBase(context)
                cfdb.removeAll()
                cadb = CacheApiDataBase(context)
                cadb.removeAll()
            } finally {
                if (cufd != null) {
                    cufd.close()
                }
                if (cudb != null) {
                    cudb.close()
                }
                if (cfdb != null) {
                    cfdb.close()
                }
                if (cadb != null) {
                    cadb.close()
                }
            }
        }

        fun isPublish(): Boolean {
            return isPublish
        }

    }
}