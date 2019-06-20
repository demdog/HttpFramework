package com.apigj.android.httpframework.controller.connect.reService

import java.util.*

class ReServiceHandler {
    companion object {
        private val requestQueue = Hashtable<String, ArrayList<IRemoteHandler>>()

        fun removeAllRequestHandler() {
            synchronized(requestQueue) {
                requestQueue.clear()
            }
        }

        fun containsService(remoteHandler: IRemoteHandler?): Boolean {
            return if (remoteHandler != null) {
                requestQueue[remoteHandler.handlerString()] != null
            } else false
        }

        fun createQueue(remoteHandler: IRemoteHandler) {
            synchronized(requestQueue) {
                val url = remoteHandler.handlerString()
                if (containsService(remoteHandler)) {
                    return
                }
                requestQueue.put(url, ArrayList())
            }
        }

        fun eraseQueue(remoteHandler: IRemoteHandler) {
            synchronized(requestQueue) {
                val url = remoteHandler.handlerString()
                if (requestQueue[url] == null) {
                    return
                }
                requestQueue.remove(url)
            }
        }

        fun putOnRemoteDataHandler(remoteHandler: IRemoteHandler) {
            synchronized(requestQueue) {
                val url = remoteHandler.handlerString()
                if (requestQueue[url] != null) {
                    requestQueue[url]!!.add(remoteHandler)
                }
            }
        }

        fun handleOnRemoteData(
            remoteHandler: IRemoteHandler,
            resHeader: Map<String, String>?,
            obj: Any?,
            error: Exception?
        ) {
            synchronized(requestQueue) {
                val url = remoteHandler.handlerString()
                if (requestQueue[url] == null) {
                    return
                }
                val aa = requestQueue[url]
                for (i in aa!!.indices) {
                    aa[i].remoteHandle(resHeader, obj, error)
                }
            }
        }
    }
}