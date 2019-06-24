package com.apigj.android.httpframework.controller.database

import android.content.Context
import android.database.Cursor
import android.database.SQLException
import com.apigj.android.httpframework.utils.CacheUtils
import java.util.ArrayList

class CacheFileDataBase(context: Context) : DataBaseHelper(context) {

    companion object {
        private val TABLE_NAME = "cacheFileTable"
    }

    //列名
    private val URL_ADD = "urladd"//URL
    private val CACHE_PATH = "file_path"//返回的内容
    private val DATE_TIME = "date_time"//获取的时间
    private val CACHE_TIME_UP = "cache_time"//缓存的时间

    init {
        this.database_name = TABLE_NAME
    }

    fun createTable() {
        val sql = "CREATE TABLE IF NOT EXISTS  " + TABLE_NAME + "( " +
                URL_ADD + " VARCHAR(1024) NOT NULL, " +
                CACHE_PATH + " VARCHAR(1024), " +
                DATE_TIME + " DATETIME," +
                CACHE_TIME_UP + " INTEGER)"
        val db = writableDatabase
        db.execSQL(sql)
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

    private fun queryDataWithPrefix(url: String, perfix: String): Cursor? {
        val sql = "select * from $TABLE_NAME where $URL_ADD = '$url' and $CACHE_PATH like '$perfix%'"
        var cursor: Cursor? = null
        try {
            cursor = qurey(sql, null)
        } catch (e: SQLException) {
            createTable()
        }

        return cursor
    }

    private fun queryPath(url: String, filePath: String?): Cursor? {
        var sql = "select * from $TABLE_NAME where $URL_ADD = '$url'"
        if (filePath != null) sql += "and $CACHE_PATH = '$filePath'"
        var cursor: Cursor? = null
        try {
            cursor = qurey(sql, null)
        } catch (e: SQLException) {
            createTable()
        }

        return cursor
    }

    fun queryAllCachePath(url: String): ArrayList<CacheFileItem> {
        val res = ArrayList<CacheFileItem>()
        val cursor = queryData(url)
        try {
            if (cursor != null && cursor.count > 0) {
                cursor.moveToFirst()
                while (!cursor.isAfterLast) {
                    val catchtime = cursor.getInt(3)
                    val datetime = cursor.getLong(2)
                    val filePath = cursor.getString(1)
                    if (catchtime == 0) {
                        deleteCacheData(url, filePath)
                        continue
                    } else if (catchtime > 0) {
                        if (System.currentTimeMillis() < datetime + catchtime) {
                            deleteCacheData(url, filePath)
                            continue
                        }
                    }
                    res.add(CacheFileItem(filePath, datetime, catchtime))
                    cursor.moveToNext()
                }
                return res
            }
        } catch (e: SQLException) {
            createTable()
        } finally {
            cursor?.close()
        }
        return res
    }

    fun queryDBDataWithPrefix(url: String, prefix: String): CacheFileItem? {
        val cursor = queryDataWithPrefix(url, prefix)
        try {
            if (cursor != null && cursor.count > 0) {
                cursor.moveToFirst()
                val catchtime = cursor.getInt(3)
                val datetime = cursor.getLong(2)
                val filePath = cursor.getString(1)
                if (catchtime == 0) {
                    deleteCacheData(url, filePath)
                    return null
                } else if (catchtime > 0) {
                    if (System.currentTimeMillis() < datetime + catchtime) {
                        deleteCacheData(url, filePath)
                        return null
                    }
                }
                return CacheFileItem(filePath, datetime, catchtime)
            }
        } catch (e: SQLException) {
            createTable()
        } finally {
            cursor?.close()
        }
        return null
    }

    fun checkCachePath(url: String, filePath: String): Boolean {
        var cursor: Cursor? = null
        if (filePath == null) {
            cursor = queryData(url)
        } else {
            cursor = queryPath(url, filePath)
        }
        try {
            if (cursor != null && cursor.count > 0) {
                cursor.moveToFirst()
                val catchtime = cursor.getInt(3)
                val datetime = cursor.getLong(2)
                if (catchtime == 0) {
                    deleteCacheData(url, filePath)
                    return false
                } else if (catchtime > 0) {
                    if (System.currentTimeMillis() < datetime + catchtime) {
                        deleteCacheData(url, filePath)
                        return false
                    }
                }
                return true
            }
        } catch (e: SQLException) {
            createTable()
        } finally {
            cursor?.close()
        }
        //        ArrayList<CacheFileItem> list = queryAllCachePath(url);
        //        if(!list.isEmpty()){
        //            for(int i = 0;i < list.size();i++) {
        //                if (CacheUtils.copyFile(list.get(i).getFilePath(), filePath)) {
        //                    insertOrUpdatebyUrl(url, filePath, list.get(i).getDatetime(), list.get(i).getCacheTime());
        //                    return true;
        //                }
        //            }
        //        }
        return false
    }

    fun queryCachePath(url: String, filePath: String?): CacheFileItem? {
        var filePath = filePath
        val cursor = queryPath(url, filePath)
        try {
            if (cursor != null && cursor.count > 0) {
                cursor.moveToFirst()
                val catchtime = cursor.getInt(3)
                val datetime = cursor.getLong(2)
                filePath = cursor.getString(1)
                if (catchtime == 0) {
                    deleteCacheData(url, filePath)
                    return null
                } else if (catchtime > 0) {
                    if (System.currentTimeMillis() < datetime + catchtime) {
                        deleteCacheData(url, filePath)
                        return null
                    }
                }
                return CacheFileItem(filePath, datetime, catchtime)
            }
        } catch (e: SQLException) {
            createTable()
        } finally {
            cursor?.close()
        }
        return null
    }

    fun queryCachePath(url: String): CacheFileItem? {
        val cursor = queryData(url)
        try {
            if (cursor != null && cursor.count > 0) {
                cursor.moveToFirst()
                val catchtime = cursor.getInt(3)
                val datetime = cursor.getLong(2)
                val filePath = cursor.getString(1)
                if (catchtime == 0) {
                    deleteCacheData(url, filePath)
                    return null
                } else if (catchtime > 0) {
                    if (System.currentTimeMillis() < datetime + catchtime) {
                        deleteCacheData(url, filePath)
                        return null
                    }
                }
                return CacheFileItem(filePath, datetime, catchtime)
            }
        } catch (e: SQLException) {
            createTable()
        } finally {
            cursor?.close()
        }
        return null
    }

    fun insertOrUpdate(url: String, filePath: String, datatime: Long, cacheTime: Int): Boolean {
        if (cacheTime == 0) {
            return false
        }
        var cursor: Cursor? = null
        try {
            cursor = queryPath(url, filePath)
            if (cursor != null && cursor.count > 0) {
                val sql =
                    "update $TABLE_NAME Set $DATE_TIME = $datatime, $CACHE_TIME_UP = $cacheTime where $URL_ADD = '$url' and $CACHE_PATH = '$filePath'"
                updata(sql, null)
            } else {
                val sql = "insert into $TABLE_NAME($URL_ADD,$CACHE_PATH,$DATE_TIME,$CACHE_TIME_UP) values(?,?,?,?)"
                insert(sql, arrayOf(url, filePath, datatime, cacheTime))
            }
            return true
        } catch (e: SQLException) {
            createTable()
            return insertOrUpdate(url, filePath, datatime, cacheTime)
        } finally {
            cursor?.close()
        }
    }

    fun insertOrUpdatebyUrl(url: String, filePath: String, datatime: Long, cacheTime: Int): Boolean {
        if (cacheTime == 0) {
            return false
        }
        var cursor: Cursor? = null
        var res = false
        try {
            cursor = queryData(url)
            if (cursor != null && cursor.count > 0) {
                cursor.moveToFirst()
                while (!cursor.isAfterLast) {
                    val cachedfilePath = cursor.getString(1)
                    if (cachedfilePath !== filePath) {
                        if (CacheUtils.copyFile(filePath, cachedfilePath)) {
                            val sql =
                                "update $TABLE_NAME Set $DATE_TIME = $datatime, $CACHE_TIME_UP = $cacheTime where $URL_ADD = '$url' and $CACHE_PATH = '$cachedfilePath'"
                            updata(sql, null)
                        }
                    } else {
                        val sql =
                            "update $TABLE_NAME Set $DATE_TIME = $datatime, $CACHE_TIME_UP = $cacheTime where $URL_ADD = '$url' and $CACHE_PATH = '$cachedfilePath'"
                        updata(sql, null)
                        res = true
                    }
                    cursor.moveToNext()
                }
            }
        } catch (e: SQLException) {
            createTable()
            return insertOrUpdatebyUrl(url, filePath, datatime, cacheTime)
        } finally {
            cursor?.close()
        }
        if (!res) {
            val sql = "insert into $TABLE_NAME($URL_ADD,$CACHE_PATH,$DATE_TIME,$CACHE_TIME_UP) values(?,?,?,?)"
            insert(sql, arrayOf(url, filePath, datatime, cacheTime))
            return true
        }
        return res
    }

    fun deleteCacheData(url: String) {
        val sql = "DELETE FROM $TABLE_NAME where $URL_ADD = '$url'"
        try {
            delete(sql, null)
        } catch (e: SQLException) {
            createTable()
        }

    }

    fun deleteCacheData(url: String, filePath: String) {
        var filePath = filePath
        filePath = DataBaseHelper.sqliteEscape(filePath)
        val sql = "DELETE FROM $TABLE_NAME where $URL_ADD = '$url' and $CACHE_PATH='$filePath'"
        try {
            delete(sql, null)
        } catch (e: SQLException) {
            createTable()
        }

    }

    fun deleteCacheDataWithPrefix(url: String, prefix: String) {
        var prefix = prefix
        prefix = DataBaseHelper.sqliteEscape(prefix)
        val sql = "DELETE FROM $TABLE_NAME where $URL_ADD = '$url' and $CACHE_PATH like '$prefix%'"
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

    inner class CacheFileItem internal constructor(val filePath: String, val datetime: Long, val cacheTime: Int)
}