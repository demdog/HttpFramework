package com.apigj.android.httpframework.controller.database

import android.content.Context
import android.database.Cursor
import android.database.SQLException
import com.apigj.android.httpframework.controller.exceptions.DataConflictException
import com.apigj.android.httpframework.controller.exceptions.ModelParseFailed
import com.apigj.android.httpframework.controller.exceptions.ModelSynthesizeFailed
import com.apigj.android.httpframework.controller.service.ConflictIntervention
import com.apigj.android.httpframework.controller.service.ConflictIntervention.*
import org.json.JSONException
import org.json.JSONObject
import java.util.ArrayList

class CacheUploadDataBase(context: Context) : DataBaseHelper(context) {

    companion object {
        private val TABLE_NAME = "cacheUploadTable"
    }

    ///列名
    private val REQUSET_CLASS = "fullClassName"//URL
    private val PARAMETER = "para"//参数
    private val SIMPLE_URL = "s_url"
    private val DATE_TIME = "date_time"//上传的时间
    private val CACHE_TIME_UP = "cache_time"//缓存的时间

    init {
        this.database_name = TABLE_NAME
    }

    fun createTable() {
        val sql = "CREATE TABLE IF NOT EXISTS  " + TABLE_NAME + "( " +
                REQUSET_CLASS + " VARCHAR(500) NOT NULL, " +
                SIMPLE_URL + " VARCHAR(500), " +
                PARAMETER + " VARCHAR(100000)," +
                DATE_TIME + " DATETIME," +
                CACHE_TIME_UP + " INTEGER)"
        val db = writableDatabase
        db.execSQL(sql)
    }

    private fun queryData(className: String, parameter: String): Cursor? {
        var parameter = parameter
        parameter = DataBaseHelper.sqliteEscape(parameter)
        val sql = "select * from $TABLE_NAME where $REQUSET_CLASS = '$className' and $PARAMETER = '$parameter'"
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

    private fun queryData(className: String): Cursor? {
        val sql = "select * from $TABLE_NAME where $REQUSET_CLASS = '$className'"
        var cursor: Cursor? = null
        try {
            cursor = qurey(sql, null)
        } catch (e: SQLException) {
            createTable()
        }

        return cursor
    }

    private fun queryDataInclude(content: String): Cursor? {
        var content = content
        content = DataBaseHelper.sqliteEscape(content)
        val sql = "select * from $TABLE_NAME where $PARAMETER like '%$content%'"
        var cursor: Cursor? = null
        try {
            cursor = qurey(sql, null)
        } catch (e: SQLException) {
            createTable()
        }

        return cursor
    }

    fun queryDBData(className: String, parameter: String): CacheUploadItem? {
        val cursor = queryData(className, parameter)
        try {
            if (cursor != null && cursor.count > 0) {
                cursor.moveToFirst()
                val cacheTime = cursor.getInt(4)
                val dateTime = cursor.getLong(3)
                val simpleClass = cursor.getString(1)
                if (cacheTime == 0) {
                    deleteCacheUploadData(className, parameter)
                    return null
                } else if (cacheTime > 0) {
                    if (System.currentTimeMillis() < dateTime + cacheTime) {
                        deleteCacheUploadData(className, parameter)
                        return null
                    }
                }
                return CacheUploadItem(className, simpleClass, parameter, dateTime)
            }
        } catch (e: SQLException) {
            createTable()
        } finally {
            cursor?.close()
        }
        return null
    }

    fun queryDBData(className: String): List<CacheUploadItem> {
        val res = ArrayList<CacheUploadItem>()
        val cursor = queryData(className)
        try {
            if (cursor != null && cursor.count > 0) {
                cursor.moveToFirst()
                while (!cursor.isAfterLast) {
                    val cacheTime = cursor.getInt(4)
                    val dateTime = cursor.getLong(3)
                    val parameter = cursor.getString(2)
                    val simpleClass = cursor.getString(1)
                    if (cacheTime == 0) {
                        deleteCacheUploadData(className, parameter)
                        continue
                    } else if (cacheTime > 0) {
                        if (System.currentTimeMillis() < dateTime + cacheTime) {
                            deleteCacheUploadData(className, parameter)
                            continue
                        }
                    }
                    res.add(CacheUploadItem(className, simpleClass, parameter, dateTime))
                    cursor.moveToNext()
                }
            }
        } catch (e: SQLException) {
            createTable()
        } finally {
            cursor?.close()
        }
        return res
    }

    fun queryDBData(): List<CacheUploadItem> {
        val res = ArrayList<CacheUploadItem>()
        val cursor = queryData()
        try {
            if (cursor != null && cursor.count > 0) {
                cursor.moveToFirst()
                while (!cursor.isAfterLast) {
                    val cacheTime = cursor.getInt(4)
                    val dateTime = cursor.getLong(3)
                    val parameter = cursor.getString(2)
                    val simpleClass = cursor.getString(1)
                    val className = cursor.getString(0)
                    if (cacheTime == 0) {
                        deleteCacheUploadData(className, parameter)
                        continue
                    } else if (cacheTime > 0) {
                        if (System.currentTimeMillis() < dateTime + cacheTime) {
                            deleteCacheUploadData(className, parameter)
                            continue
                        }
                    }
                    res.add(CacheUploadItem(className, simpleClass, parameter, dateTime))
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

    fun deleteCacheUploadData(className: String, parameter: String) {
        var parameter = parameter
        parameter = DataBaseHelper.sqliteEscape(parameter)
        val sql = "DELETE FROM $TABLE_NAME where $REQUSET_CLASS = '$className' and $PARAMETER='$parameter'"
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

    @Throws(DataConflictException::class, ModelParseFailed::class, ModelSynthesizeFailed::class)
    fun insertOrUpdate(
        className: String,
        simpleUrl: String,
        parameter: String,
        dateTime: Long,
        cacheTime: Int,
        conflict: ConflictIntervention
    ): Boolean {
        if (cacheTime == 0) {
            return true
        }
        when (conflict) {
            KEEP_ALL -> return insertOrUpdate(className, simpleUrl, parameter, dateTime, cacheTime)
            OVER_COVER -> return insertOrUpdateClassName(className, simpleUrl, parameter, dateTime, cacheTime)
            MERGE -> return mergeClassName(className, simpleUrl, parameter, dateTime, cacheTime)
        }
        return false
    }

    fun insertOrUpdate(
        className: String,
        simpleUrl: String,
        parameter: String,
        dateTime: Long,
        cacheTime: Int
    ): Boolean {
        val cursor = queryData(className, parameter)
        try {
            if (cursor != null && cursor.count > 0) {
                val sql =
                    "update $TABLE_NAME Set $DATE_TIME = $dateTime, $CACHE_TIME_UP = $cacheTime where $REQUSET_CLASS = '$className' and $PARAMETER = '$parameter'"
                updata(sql, null)
            } else {
                val sql =
                    "insert into $TABLE_NAME($REQUSET_CLASS,$SIMPLE_URL,$PARAMETER,$DATE_TIME,$CACHE_TIME_UP) values(?,?,?,?,?)"
                insert(sql, arrayOf(className, simpleUrl, parameter, dateTime, cacheTime))
            }
            return true
        } catch (e: SQLException) {
            createTable()
            return insertOrUpdate(className, simpleUrl, parameter, dateTime, cacheTime)
        } finally {
            cursor?.close()
        }
    }

    @Throws(DataConflictException::class)
    fun insertOrUpdateClassName(
        className: String,
        simpleUrl: String,
        parameter: String,
        dateTime: Long,
        cacheTime: Int
    ): Boolean {
        var parameter = parameter
        val cursor = queryData(className)
        parameter = DataBaseHelper.sqliteEscape(parameter)
        try {
            if (cursor != null && cursor.count == 1) {
                val sql =
                    "update $TABLE_NAME Set $DATE_TIME = $dateTime, $PARAMETER = '$parameter', $CACHE_TIME_UP = '$cacheTime' where $REQUSET_CLASS = '$className'"
                updata(sql, null)
            } else if (cursor == null || cursor.count == 0) {
                val sql =
                    "insert into $TABLE_NAME($REQUSET_CLASS,$SIMPLE_URL,$PARAMETER,$DATE_TIME,$CACHE_TIME_UP) values(?,?,?,?,?)"
                insert(sql, arrayOf(className, simpleUrl, parameter, dateTime, cacheTime))
            } else {
                throw DataConflictException(CacheUploadItem(className, simpleUrl, parameter, dateTime))
            }
            return true
        } catch (e: SQLException) {
            createTable()
            return insertOrUpdateClassName(className, simpleUrl, parameter, dateTime, cacheTime)
        } finally {
            cursor?.close()
        }
    }

    @Throws(DataConflictException::class, ModelParseFailed::class, ModelSynthesizeFailed::class)
    fun mergeClassName(
        className: String,
        simpleUrl: String,
        parameter: String,
        dateTime: Long,
        cacheTime: Int
    ): Boolean {
        val cursor = queryData(className)
        try {
            if (cursor != null && cursor.count == 1) {
                cursor.moveToFirst()
                var oldjson: JSONObject? = null
                var newjson: JSONObject? = null
                try {
                    oldjson = JSONObject(cursor.getString(2))
                } catch (e: JSONException) {
                    throw ModelParseFailed("JSON", cursor.getString(2))
                }

                try {
                    newjson = JSONObject(parameter)
                } catch (e: JSONException) {
                    throw ModelParseFailed("JSON", parameter)
                }

                val mergedJson = mergeJson(oldjson, newjson)
                var para: String? = null
                para = DataBaseHelper.sqliteEscape(mergedJson!!.toString())
                val sql =
                    "update $TABLE_NAME Set $DATE_TIME = $dateTime, $PARAMETER = '$para', $CACHE_TIME_UP = $cacheTime where $REQUSET_CLASS = '$className'"
                updata(sql, null)
            } else if (cursor == null || cursor.count == 0) {
                val sql =
                    "insert into $TABLE_NAME($REQUSET_CLASS,$SIMPLE_URL,$PARAMETER,$DATE_TIME,$CACHE_TIME_UP) values(?,?,?,?,?)"
                insert(sql, arrayOf(className, simpleUrl, parameter, dateTime, cacheTime))
            } else {
                throw DataConflictException(CacheUploadItem(className, simpleUrl, parameter, dateTime))
            }
            return true
        } catch (e: SQLException) {
            createTable()
            return insertOrUpdateClassName(className, simpleUrl, parameter, dateTime, cacheTime)
        } finally {
            cursor?.close()
        }
    }

    @Throws(ModelParseFailed::class)
    fun mergeJson(oldJson: JSONObject?, newJson: JSONObject?): JSONObject? {
        if (oldJson == null) {
            return newJson
        }
        if (newJson == null) {
            return oldJson
        }
        val it = newJson.keys()
        while (it.hasNext()) {
            val key = it.next().toString()
            try {
                val tempOld = oldJson.getJSONObject(key)
                val tempnew = newJson.getJSONObject(key)
                mergeJson(tempOld, tempnew)
            } catch (e: JSONException) {
                try {
                    oldJson.put(key, newJson.get(key))
                } catch (e1: JSONException) {
                    throw ModelParseFailed("JSON key not found, Key:$key", newJson.toString())
                }

            }

        }
        return oldJson
    }

    fun updateContent(filePath: String, urlPath: String) {
        val cursor = queryDataInclude(filePath)
        try {
            if (cursor != null && cursor.count > 0) {
                cursor.moveToFirst()
                while (!cursor.isAfterLast) {
                    val cacheTime = cursor.getInt(4)
                    val dateTime = cursor.getLong(3)
                    val parameter = cursor.getString(2)
                    val simpleClass = cursor.getString(1)
                    val className = cursor.getString(0)
                    if (cacheTime == 0) {
                        deleteCacheUploadData(className, parameter)
                        continue
                    } else if (cacheTime > 0) {
                        if (System.currentTimeMillis() < dateTime + cacheTime) {
                            deleteCacheUploadData(className, parameter)
                            continue
                        }
                    }
                    val sql = "update " + TABLE_NAME + " Set " + PARAMETER + " = '" + parameter.replace(
                        filePath.toRegex(),
                        urlPath
                    ) + "' where " + REQUSET_CLASS + " = '" + className + "' and " + PARAMETER + " = '" + parameter + "'"
                    updata(sql, null)
                    cursor.moveToNext()
                }
            }
        } catch (e: SQLException) {
            createTable()
        } finally {
            cursor?.close()
        }
    }

    fun deleteContent(filePath: String) {
        var filePath = filePath
        filePath = DataBaseHelper.sqliteEscape(filePath)
        val sql = "delete from $TABLE_NAME where $PARAMETER like '%$filePath%'"
        try {
            delete(sql, null)
        } catch (e: SQLException) {
            createTable()
        }

        //		Cursor cursor = queryDataInclude(filePath);
        //		try{
        //			if(cursor!=null&&cursor.getCount()>0){
        //				cursor.moveToFirst();
        //				while (!cursor.isAfterLast()) {
        //					String parameter = cursor.getString(2);
        //					String className = cursor.getString(0);
        //					deleteCacheUploadData(className, parameter);
        //					cursor.moveToNext();
        //				}
        //			}
        //		}catch(SQLException e){
        //			createTable();
        //		}finally{
        //			if(cursor != null){
        //				cursor.close();
        //			}
        //		}
    }

    inner class CacheUploadItem(val className: String, val simpleUrl: String, val parameter: String, dateTime: Long) {
        val datetime: Long

        init {
            this.datetime = dateTime
        }
    }
}