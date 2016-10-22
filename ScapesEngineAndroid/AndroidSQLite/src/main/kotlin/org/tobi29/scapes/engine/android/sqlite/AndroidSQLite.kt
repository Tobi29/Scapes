package org.tobi29.scapes.engine.android.sqlite

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import org.tobi29.scapes.engine.sql.SQLColumn
import org.tobi29.scapes.engine.sql.SQLDatabase
import org.tobi29.scapes.engine.sql.SQLQuery
import org.tobi29.scapes.engine.sql.SQLType
import java.util.*

class AndroidSQLite(private val connection: SQLiteDatabase) : SQLDatabase {
    override fun replace(table: String,
                         columns: Array<String>,
                         rows: List<Array<Any>>) {
        for (row in rows) {
            val content = ContentValues()
            for (i in columns.indices) {
                resolveObject(row[i], columns[i], content)
            }
            connection.replace(table, null, content)
        }
    }

    override fun insert(table: String,
                        columns: Array<String>,
                        rows: List<Array<Any>>) {
        for (row in rows) {
            val content = ContentValues()
            for (i in columns.indices) {
                resolveObject(row[i], columns[i], content)
            }
            connection.insertWithOnConflict(table, null, content,
                    SQLiteDatabase.CONFLICT_IGNORE)
        }
    }

    override fun compileQuery(table: String,
                              columns: Array<String>,
                              matches: List<String>): SQLQuery {
        val where = StringBuilder(columns.size shl 5)
        var first = true
        for (match in matches) {
            if (first) {
                first = false
            } else {
                where.append(',')
            }
            where.append(match).append("=?")
        }
        val compiledWhere = where.toString()
        return object : SQLQuery {
            override fun run(values: List<Any>): List<Array<Any?>> {
                val argsWhere = arrayOfNulls<String>(values.size)
                var i = 0
                for (`object` in values) {
                    if (`object` is ByteArray) {
                        throw IllegalArgumentException(
                                "Byte array value not supported")
                    }
                    argsWhere[i++] = `object`.toString()
                }
                val result = ArrayList<Array<Any?>>()
                connection.query(table, columns, compiledWhere, argsWhere, null,
                        null, null).use { cursor ->
                    while (cursor.moveToNext()) {
                        result.add(resolveResult(cursor))
                    }
                }
                return result
            }
        }
    }

    override fun delete(table: String,
                        matches: List<Pair<String, Any>>) {
        val argsWhere = arrayOfNulls<String>(matches.size)
        val where = StringBuilder(64)
        var first = true
        var i = 0
        for (match in matches) {
            if (match.second is ByteArray) {
                throw IllegalArgumentException(
                        "Byte array value not supported")
            }
            argsWhere[i++] = match.second.toString()
            if (first) {
                first = false
            } else {
                where.append(',')
            }
            where.append(match.first).append("=?")
        }
        val compiledWhere = where.toString()
        connection.delete(table, compiledWhere, argsWhere)
    }

    override fun createTable(name: String,
                             primaryKey: String?,
                             columns: List<SQLColumn>) {
        val sql = StringBuilder(64)
        sql.append("CREATE TABLE IF NOT EXISTS ").append(name).append(" (")
        var first = true
        for (column in columns) {
            if (first) {
                first = false
            } else {
                sql.append(',')
            }
            sql.append(column.name).append(' ')
            sql.append(resolveType(column.type, column.extra))
        }
        if (primaryKey != null) {
            sql.append(", PRIMARY KEY (").append(primaryKey).append(')')
        }
        sql.append(");")
        val compiled = sql.toString()
        val statement = connection.compileStatement(compiled)
        statement.execute()
    }

    override fun dropTable(name: String) {
        val compiled = "DROP TABLE IF EXISTS $name;"
        val statement = connection.compileStatement(compiled)
        statement.execute()
    }

    fun dispose() {
        connection.close()
    }

    private fun resolveObject(`object`: Any?,
                              column: String,
                              content: ContentValues) {
        if (`object` is Byte) {
            content.put(column, `object` as Byte?)
        } else if (`object` is Short) {
            content.put(column, `object` as Short?)
        } else if (`object` is Int) {
            content.put(column, `object` as Int?)
        } else if (`object` is Long) {
            content.put(column, `object` as Long?)
        } else if (`object` is Float) {
            content.put(column, `object` as Float?)
        } else if (`object` is Double) {
            content.put(column, `object` as Double?)
        } else if (`object` is ByteArray) {
            content.put(column, `object` as ByteArray?)
        } else if (`object` is String) {
            content.put(column, `object` as String?)
        } else if (`object` == null) {
            content.putNull(column)
        }
    }

    private fun resolveResult(cursor: Cursor): Array<Any?> {
        val columns = cursor.columnCount
        val row = arrayOfNulls<Any>(columns)
        for (i in 0..columns - 1) {
            when (cursor.getType(i)) {
                Cursor.FIELD_TYPE_NULL -> row[i] = null
                Cursor.FIELD_TYPE_INTEGER -> row[i] = cursor.getLong(i)
                Cursor.FIELD_TYPE_FLOAT -> row[i] = cursor.getDouble(i)
                Cursor.FIELD_TYPE_BLOB -> row[i] = cursor.getBlob(i)
                Cursor.FIELD_TYPE_STRING -> row[i] = cursor.getString(i)
            }
        }
        return row
    }

    private fun resolveType(type: SQLType,
                            extra: String?): String {
        val typeStr = type.toString()
        if (extra != null) {
            return "$typeStr($extra)"
        }
        return typeStr
    }
}
