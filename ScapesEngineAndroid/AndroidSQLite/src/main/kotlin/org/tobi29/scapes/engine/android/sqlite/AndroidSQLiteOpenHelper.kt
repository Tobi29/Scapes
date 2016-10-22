package org.tobi29.scapes.engine.android.sqlite

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.tobi29.scapes.engine.utils.io.filesystem.FilePath

class AndroidSQLiteOpenHelper : SQLiteOpenHelper {
    constructor(context: Context, path: FilePath) : this(context,
            path.toAbsolutePath().toString()) {
    }

    constructor(context: Context, name: String) : super(context, name, null,
            1) {
    }

    override fun onCreate(db: SQLiteDatabase) {
    }

    override fun onUpgrade(db: SQLiteDatabase,
                           oldVersion: Int,
                           newVersion: Int) {
    }
}
