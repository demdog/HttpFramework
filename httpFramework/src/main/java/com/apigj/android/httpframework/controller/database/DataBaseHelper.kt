package com.apigj.android.httpframework.controller.database

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.jetbrains.annotations.Nullable
import java.io.File

open class DataBaseHelper(
    context: Context, name: String, factory: SQLiteDatabase.CursorFactory?,
    version: Int): SQLiteOpenHelper(CustomPathDatabaseContext(context, getDirPath(context)), name, factory, version) {
    companion object {
        //数据库当前的版本
        val DATABASE_VERSION = 1
        //数据库名
        val DATABASE_NAME = "appdb.db"


        /*
	 * 数据库基本数据类型
	 * */
        val INT = "INTEGER "
        val STRING = "VARCHAR"
        val TEXT = "TEXT"
        val FLOAT = "FLOAT"
        val DOUBLE = "DOUBLE"
        /**
         * 获取db文件在sd卡的路径
         * @return
         */
        fun getDirPath(context: Context): String {
            //这里返回存放db的文件夹的绝对路径
            var filePath: String? = context.getExternalFilesDir(null)!!.absolutePath + "/databases"
            if (filePath == null) {
                filePath = context.filesDir.absolutePath + "/databases"
            }
            val file = File(filePath)
            if (!file.exists()) {
                file.mkdirs()
            }
            return file.absolutePath
        }

        fun sqliteEscape(keyWord: String): String {
            var keyWord = keyWord
            keyWord = keyWord.replace("'", "''")
            return keyWord
        }
    }

    var database_name = ""
    private var mContext = context

    /**
     * 打开默认数据库
     */
    constructor(context: Context): this(
        CustomPathDatabaseContext(context, getDirPath(context)),
        DATABASE_NAME,
        null,
        DATABASE_VERSION
    ) {
    }


    protected fun getContext(): Context {
        return mContext
    }


    override fun onCreate(db: SQLiteDatabase) {

    }

    override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {

    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
    }

    /*
	 * 可进行增删改
	 * */
    fun execSQL(sql: String) {
        val db = writableDatabase
        db.execSQL(sql)
        db.close()
    }

    fun execSQL(sql: String, bindArgs: Array<Any>) {
        val db = writableDatabase
        db.execSQL(sql, bindArgs)
        db.close()
    }

    /*
	 * 进行查寻
	 */
    fun rawQuery(sql: String, selectionArgs: Array<String>?): Cursor {
        val db = readableDatabase
        return db.rawQuery(sql, selectionArgs)
        //		return getReadableDatabase().rawQuery(sql, selectionArgs);
    }

    @Synchronized
    override fun close() {
        super.close()
    }

    /**
     * 创建表格
     */
    fun createTable(sql: String) {
        execSQL(sql)
    }

    /**
     * 创建表格
     * @author LvYunXing
     * @param params 列数据类型数组
     * @param names 列名数组
     * @param isAutoIncrement 主键是否自动增长
     * @return null
     */
    fun createTable(TABLE_NAME: String, params: Array<String>, names: Array<String>, isAutoIncrement: Boolean) {
        val sql = StringBuffer()
        sql.append("CREATE TABLE [$TABLE_NAME]( ")
        if (isAutoIncrement) {
            sql.append("[_id] INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT")
        } else {
            sql.append("[_id] INTEGER NOT NULL PRIMARY KEY, ")
        }
        for (i in params.indices) {
            if (i == params.size - 1) {
                sql.append("[" + names[i] + "] " + params[i] + " )")
            } else {
                sql.append("[" + names[i] + "] " + params[i] + " ,")
            }
        }

        createTable(sql.toString())
    }

    /**
     * 查询所有
     */
    fun qureyAll(): Cursor {
        return qurey("select * from $database_name", null)
    }

    /**
     * 通过列名来查找
     */
    fun qureyByName(colsName: String, vlaue: String): Cursor {
        val sql = "select * from $database_name where $colsName = ?"
        return qurey(sql, arrayOf(vlaue))
    }

    /**
     * 查询
     */
    fun qurey(sql: String, params: Array<String>?): Cursor {
        return rawQuery(sql, params)
    }

    /**
     * 添加
     */
    fun insert(sql: String, bindArgs: Array<Any>?) {
        if (bindArgs == null) {
            execSQL(sql)
        } else {
            execSQL(sql, bindArgs)
        }
    }

    /**
     * 删除
     */
    fun delete(sql: String, bindArgs: Array<Any>?) {
        if (bindArgs == null) {
            execSQL(sql)
        } else {
            execSQL(sql, bindArgs)
        }
    }

    /**
     * 更新 */
    fun updata(sql: String, bindArgs: Array<Any>?) {
        if (bindArgs == null) {
            execSQL(sql)
        } else {
            execSQL(sql, bindArgs)
        }
    }



}