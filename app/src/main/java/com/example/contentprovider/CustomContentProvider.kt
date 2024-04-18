package com.example.contentprovider

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import android.provider.BaseColumns
import android.text.TextUtils
import android.util.Log


class NotesContentProvider() : ContentProvider() {
    class NotesDBHelper(c: Context?) : SQLiteOpenHelper(
            c, NotesMetaData.DATABASE_NAME, null,
            NotesMetaData.DATABASE_VERSION
        ) {
        override fun onCreate(db: SQLiteDatabase) {
            Log.d("In NoteDb Helper", " In On Create")
            db.execSQL(SQL_QUERY_CREATE)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVer: Int, newVer: Int) {
            db.execSQL(SQL_QUERY_DROP)
            onCreate(db)
        }

        companion object {
            private val SQL_QUERY_CREATE = (((("CREATE TABLE "
                    + NotesMetaData.NotesTable.TABLE_NAME).toString() + " ("
                    + NotesMetaData.NotesTable.ID
                    ).toString() + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + NotesMetaData.NotesTable.TITLE).toString() + " TEXT NOT NULL, "
                    + NotesMetaData.NotesTable.CONTENT).toString() + " TEXT NOT NULL" + ");"
            private val SQL_QUERY_DROP = ("DROP TABLE IF EXISTS "
                    + NotesMetaData.NotesTable.TABLE_NAME).toString() + ";"
        }
    }

    // create a db helper object
    private var mDbHelper: NotesDBHelper? = null
    override fun onCreate(): Boolean {
        mDbHelper = NotesDBHelper(context)
        return true
    }

    override fun delete(uri: Uri, where: String?, whereArgs: Array<String>?): Int {
        val db = mDbHelper!!.writableDatabase
        var count = 0
        when (sUriMatcher!!.match(uri)) {
            NOTES_ALL -> count = db.delete(
                NotesMetaData.NotesTable.TABLE_NAME, where,
                whereArgs
            )

            NOTES_ONE -> {
                val rowId = uri.pathSegments[1]
                count = db.delete(
                    NotesMetaData.NotesTable.TABLE_NAME,
                    NotesMetaData.NotesTable.ID
                            + " = "
                            + rowId
                            + if (!TextUtils.isEmpty(where)) (" AND (" + where
                            + ")") else "", whereArgs
                )
            }

            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
        context!!.contentResolver.notifyChange(uri, null)
        return count
    }

    override fun getType(uri: Uri): String? {
        when (sUriMatcher!!.match(uri)) {
            NOTES_ALL -> return NotesMetaData.CONTENT_TYPE_NOTES_ALL
            NOTES_ONE -> return NotesMetaData.CONTENT_TYPE_NOTES_ONE
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {

        // you cannot insert a bunch of values at once so throw exception
        if (sUriMatcher!!.match(uri) != NOTES_ALL) {
            throw IllegalArgumentException(" Unknown URI: $uri")
        }

        // Insert once row
        val db = mDbHelper!!.writableDatabase
        val rowId = db.insert(
            NotesMetaData.NotesTable.TABLE_NAME, null,
            values
        )
        if (rowId > 0) {
            val notesUri = ContentUris.withAppendedId(
                NotesMetaData.CONTENT_URI, rowId
            )
            context!!.contentResolver.notifyChange(notesUri, null)
            return notesUri
        }
        throw IllegalArgumentException("<Illegal>Unknown URI: $uri")
    }

    // Get values from Content Provider
    override fun query(
        uri: Uri, projection: Array<String>?, selection: String?,
        selectionArgs: Array<String>?, sortOrder: String?
    ): Cursor? {
        val builder = SQLiteQueryBuilder()
        when (sUriMatcher!!.match(uri)) {
            NOTES_ALL -> {
                builder.tables = NotesMetaData.NotesTable.TABLE_NAME
                builder.projectionMap = sNotesColumnProjectionMap
            }

            NOTES_ONE -> {
                builder.tables = NotesMetaData.NotesTable.TABLE_NAME
                builder.projectionMap = sNotesColumnProjectionMap
                builder.appendWhere(
                    (NotesMetaData.NotesTable.ID + " = "
                            + uri.lastPathSegment)
                )
            }

            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
        val db = mDbHelper!!.readableDatabase
        val queryCursor = builder.query(
            db, projection, selection,
            selectionArgs, null, null, sortOrder
        )
        queryCursor.setNotificationUri(context!!.contentResolver, uri)
        return queryCursor
    }

    override fun update(
        uri: Uri, values: ContentValues?, where: String?,
        whereArgs: Array<String>?
    ): Int {
        val db = mDbHelper!!.writableDatabase
        var count = 0
        when (sUriMatcher!!.match(uri)) {
            NOTES_ALL -> count = db.update(
                NotesMetaData.NotesTable.TABLE_NAME, values,
                where, whereArgs
            )

            NOTES_ONE -> {
                val rowId = uri.lastPathSegment
                count = db
                    .update(
                        NotesMetaData.NotesTable.TABLE_NAME, values,
                        (NotesMetaData.NotesTable.ID + " = " + rowId +
                                (if (!TextUtils.isEmpty(where)) " AND (" + ")" else "")), whereArgs
                    )
            }

            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
        context!!.contentResolver.notifyChange(uri, null)
        return count
    }

    companion object {
        private var sUriMatcher: UriMatcher? = null
        private val NOTES_ALL = 1
        private val NOTES_ONE = 2

        init {
            sUriMatcher = UriMatcher(UriMatcher.NO_MATCH)
            sUriMatcher?.addURI(NotesMetaData.AUTHORITY, "notes", NOTES_ALL)
            sUriMatcher?.addURI(NotesMetaData.AUTHORITY, "notes/#", NOTES_ONE)
        }

        // Map table columns
        private var sNotesColumnProjectionMap: HashMap<String, String>? = null

        init {
            sNotesColumnProjectionMap = HashMap()
            sNotesColumnProjectionMap!![NotesMetaData.NotesTable.ID] =
                NotesMetaData.NotesTable.ID
            sNotesColumnProjectionMap!![NotesMetaData.NotesTable.TITLE] =
                NotesMetaData.NotesTable.TITLE
            sNotesColumnProjectionMap!![NotesMetaData.NotesTable.CONTENT] =
                NotesMetaData.NotesTable.CONTENT
        }
    }
}


object NotesMetaData {
    // A content URI is a URI that identifies data in a provider. Content URIs
    // include the symbolic name of the entire provider (its authority)
    const val AUTHORITY = "com.example.shubham.notescontentprovider"
    val CONTENT_URI = Uri.parse(
        "content://" + AUTHORITY
                + "/notes"
    )
    const val DATABASE_NAME = "notes.db"
    const val DATABASE_VERSION = 1
    const val CONTENT_TYPE_NOTES_ALL = "vnd.android.cursor.dir/vnd.example.shubham.notes"
    const val CONTENT_TYPE_NOTES_ONE = "vnd.android.cursor.item/vnd.example.shubham.notes"

    object NotesTable : BaseColumns {
        const val TABLE_NAME = "notes"
        const val ID = "_id"
        const val TITLE = "title"
        const val CONTENT = "content"
    }
}