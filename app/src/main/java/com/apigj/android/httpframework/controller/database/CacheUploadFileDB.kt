package com.apigj.android.httpframework.controller.database

import android.content.Context
import android.database.Cursor
import android.database.SQLException
import java.util.ArrayList

class CacheUploadFileDB(context: Context) : DataBaseHelper(context) {
    companion object {
        private val TABLE_NAME = "cacheUploadFile"
    }

    ///列名
    private val UPLOAD_URL = "up_url"//上传目标URL
    private val CACHE_PATH = "file_path"//文件本地路径
    private val DATE_TIME = "date_time"//上传的时间

    init{
        this.database_name = TABLE_NAME
    }

    //创建表
    fun createTable() {
        //建表的SQL语句
        val sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                UPLOAD_URL + " VARCHAR(1000) NOT NULL, " +
                CACHE_PATH + " VARCHAR(1000) NOT NULL, " +
                DATE_TIME + " DATETIME)"
        //执行SQL语句-创建表
        val db = writableDatabase
        db.execSQL(sql)
    }

    private fun queryData(uploadUrl: String, filePath: String): Cursor? {
        var filePath = filePath
        filePath = DataBaseHelper.sqliteEscape(filePath)
        val sql = "select * from $TABLE_NAME where $UPLOAD_URL = '$uploadUrl' and $CACHE_PATH = '$filePath'"
        var cursor: Cursor? = null
        try {
            cursor = qurey(sql, null)
        } catch (e: SQLException) {
            createTable()
        }

        return cursor
    }

    private fun queryData(): Cursor? {
        val sql = "select * from $TABLE_NAME"
        var cursor: Cursor? = null
        try {
            cursor = qurey(sql, null)
        } catch (e: SQLException) {
            createTable()
        }

        return cursor
    }

    fun queryDBData(): List<CacheUploadFile> {
        val res = ArrayList<CacheUploadFile>()
        val cursor = queryData()
        try {
            if (cursor != null && cursor.count > 0) {
                cursor.moveToFirst()
                while (!cursor.isAfterLast) {
                    val dateTime = cursor.getLong(2)
                    val filePath = cursor.getString(1)
                    val uploadUrl = cursor.getString(0)
                    res.add(CacheUploadFile(uploadUrl, filePath, dateTime))
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

    fun insertOrUpdate(uploadUrl: String, filePath: String, dateTime: Long): Boolean {
        val cursor = queryData(uploadUrl, filePath)
        try {
            if (cursor != null && cursor.count > 0) {
                val sql =
                    "update $TABLE_NAME Set $DATE_TIME = $dateTime where $UPLOAD_URL = '$uploadUrl' and $CACHE_PATH = '$filePath'"
                updata(sql, null)
            } else {
                val sql = "insert into $TABLE_NAME($UPLOAD_URL,$CACHE_PATH,$DATE_TIME) values(?,?,?)"
                insert(sql, arrayOf(uploadUrl, filePath, dateTime))
            }
            return true
        } catch (e: SQLException) {
            createTable()
            return insertOrUpdate(uploadUrl, filePath, dateTime)
        } finally {
            cursor?.close()
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

    fun remove(uploadUrl: String, filePath: String) {
        val sql = "DELETE FROM $TABLE_NAME where $UPLOAD_URL = '$uploadUrl' and $CACHE_PATH = '$filePath'"
        try {
            delete(sql, null)
        } catch (e: SQLException) {
            createTable()
        }

    }

    inner class CacheUploadFile(val uploadUrl: String, val filePath: String, val datetime: Long)
}