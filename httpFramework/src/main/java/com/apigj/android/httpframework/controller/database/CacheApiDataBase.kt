package com.apigj.android.httpframework.controller.database

import android.content.Context
import android.database.Cursor
import android.database.SQLException
import java.util.ArrayList

class CacheApiDataBase(context: Context) : DataBaseHelper(context) {
    companion object {
        private val TABLE_NAME = "cacheTable"
    }

    ///列名
    private val URL_ADD = "urladd"//URL
    private val PARAMETER = "para"//参数
    private val JSONDATA = "jsonData"//返回的内容
    private val DATE_TIME = "date_time"//获取的时间
    private val CACHE_TIME_UP = "cache_time"//缓存的时间

    init{
        database_name = TABLE_NAME
    }

    fun createTable() {
        val sql = "CREATE TABLE IF NOT EXISTS  " + TABLE_NAME + "( " +
                URL_ADD + " VARCHAR(1024) NOT NULL, " +
                PARAMETER + " VARCHAR(100000), " +
                JSONDATA + " VARCHAR(100000), " +
                DATE_TIME + " DATETIME," +
                CACHE_TIME_UP + " INTEGER)"
        val db = writableDatabase
        db.execSQL(sql)
    }

    //添加数据
    fun insertOrUpdate(url: String, parameter: String, jsonData: String, datatime: Long, cacheTime: Int): Boolean {
        var parameter = parameter
        var jsonData = jsonData
        if (cacheTime == 0) {
            return false
        }
        jsonData = DataBaseHelper.sqliteEscape(jsonData)
        parameter = DataBaseHelper.sqliteEscape(parameter)
        var cursor: Cursor? = null
        try {
            cursor = queryData(url, parameter)
            if (cursor != null && cursor.count > 0) {
                cursor.moveToFirst()
                val sql =
                    "update $TABLE_NAME Set $DATE_TIME = $datatime, $JSONDATA = '$jsonData' where $URL_ADD = '$url' and $PARAMETER = '$parameter'"
                updata(sql, null)
            } else {
                val sql =
                    "insert into $TABLE_NAME($URL_ADD,$PARAMETER,$JSONDATA,$DATE_TIME,$CACHE_TIME_UP) values(?,?,?,?,?)"
                insert(sql, arrayOf(url, parameter, jsonData, datatime, cacheTime))
            }
            return true
        } catch (e: SQLException) {
            createTable()
            return insertOrUpdate(url, parameter, jsonData, datatime, cacheTime)
        } finally {
            cursor?.close()
        }
    }

    private fun queryData(url: String, parameter: String): Cursor? {
        var parameter = parameter
        parameter = DataBaseHelper.sqliteEscape(parameter)
        val sql = "select * from $TABLE_NAME where $URL_ADD = '$url' and $PARAMETER = '$parameter'"
        var cursor: Cursor? = null
        try {
            cursor = qurey(sql, null)
        } catch (e: SQLException) {
            createTable()
        }

        return cursor
    }

    private fun queryData(url: String): Cursor? {
        val sql = "select * from $TABLE_NAME where $URL_ADD = '$url'"
        var cursor: Cursor? = null
        try {
            cursor = qurey(sql, null)
        } catch (e: SQLException) {
            createTable()
        }

        return cursor
    }

    fun queryCacheData(url: String): List<UrlCacheItem> {
        val res = ArrayList<UrlCacheItem>()
        val cursor = queryData(url)
        try {
            if (cursor != null && cursor.count > 0) {
                cursor.moveToFirst()
                while (!cursor.isAfterLast) {
                    val cacheTime = cursor.getInt(4)
                    val dateTime = cursor.getLong(3)
                    val parameter = cursor.getString(1)
                    val jsonData = cursor.getString(2)
                    if (cacheTime == 0) {
                        deleteCacheData(url, parameter)
                        continue
                    } else if (cacheTime > 0) {
                        if (System.currentTimeMillis() < dateTime + cacheTime) {
                            deleteCacheData(url, parameter)
                            continue
                        }
                    }
                    res.add(UrlCacheItem(parameter, jsonData, dateTime))
                    cursor.moveToNext()
                }
                return res
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        return res
    }

    fun queryCacheData(url: String, parameter: String): UrlCacheItem? {
        val cursor = queryData(url, parameter)
        try {
            if (cursor != null && cursor.count > 0) {
                cursor.moveToFirst()
                val cacheTime = cursor.getInt(4)
                val dateTime = cursor.getLong(3)
                val jsonData = cursor.getString(2)
                if (cacheTime == 0) {
                    deleteCacheData(url, parameter)
                    return null
                } else if (cacheTime > 0) {
                    if (System.currentTimeMillis() < dateTime + cacheTime) {
                        deleteCacheData(url, parameter)
                        return null
                    }
                }
                return UrlCacheItem(parameter, jsonData, dateTime)
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        return null
    }

    fun deleteCacheData(url: String) {
        val sql = "DELETE FROM $TABLE_NAME where $URL_ADD = '$url'"
        try {
            delete(sql, null)
        } catch (e: SQLException) {
            createTable()
        }

    }

    fun deleteCacheData(url: String, parameter: String) {
        var parameter = parameter
        parameter = DataBaseHelper.sqliteEscape(parameter)
        val sql = "DELETE FROM $TABLE_NAME where $URL_ADD = '$url' and $PARAMETER='$parameter'"
        try {
            delete(sql, null)
        } catch (e: SQLException) {
            createTable()
        }

    }

    fun removeAll() {
        val sql = "DELETE FROM $TABLE_NAME"
        try {
            delete(sql, null)
        } catch (e: SQLException) {
            createTable()
        }

    }

    inner class UrlCacheItem(val parameter: String, val jsonData: String, val datetime: Long)
}