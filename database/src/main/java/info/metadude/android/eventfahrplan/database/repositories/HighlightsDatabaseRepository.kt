package info.metadude.android.eventfahrplan.database.repositories

import android.content.ContentValues
import android.database.sqlite.SQLiteException
import info.metadude.android.eventfahrplan.database.contract.FahrplanContract.HighlightsTable
import info.metadude.android.eventfahrplan.database.contract.FahrplanContract.HighlightsTable.Columns.EVENT_ID
import info.metadude.android.eventfahrplan.database.contract.FahrplanContract.HighlightsTable.Columns.HIGHLIGHT
import info.metadude.android.eventfahrplan.database.contract.FahrplanContract.HighlightsTable.Values.HIGHLIGHT_STATE_OFF
import info.metadude.android.eventfahrplan.database.contract.FahrplanContract.HighlightsTable.Values.HIGHLIGHT_STATE_ON
import info.metadude.android.eventfahrplan.database.extensions.*
import info.metadude.android.eventfahrplan.database.models.Highlight
import info.metadude.android.eventfahrplan.database.sqliteopenhelper.HighlightDBOpenHelper

class HighlightsDatabaseRepository(

        private val sqLiteOpenHelper: HighlightDBOpenHelper

) {

    fun update(list: List<ContentValues>, eventIds: List<String>) = with(sqLiteOpenHelper) {
        writableDatabase.transaction {
            eventIds.forEach { eventId ->
                delete(HighlightsTable.NAME, EVENT_ID, eventId)
            }
            list.forEach { contentValues ->
                insert(HighlightsTable.NAME, contentValues)
            }
        }
    }

    fun update(values: ContentValues, eventId: String) = with(sqLiteOpenHelper) {
        writableDatabase.upsert({
            delete(HighlightsTable.NAME, EVENT_ID, eventId)
        }, {
            insert(HighlightsTable.NAME, values)
        })
    }

    fun query(): List<Highlight> {
        val highlights = mutableListOf<Highlight>()
        val database = sqLiteOpenHelper.readableDatabase

        val cursor = try {
            database.read(HighlightsTable.NAME, orderBy = EVENT_ID)
        } catch (e: SQLiteException) {
            e.printStackTrace()
            return highlights.toList()
        }

        return cursor.map {
            val eventIdString = cursor.getString(EVENT_ID)
            val eventId = Integer.parseInt(eventIdString)
            val highlightState = cursor.getInt(HIGHLIGHT)
            val isHighlighted = highlightState == HIGHLIGHT_STATE_ON

            Highlight(eventId, isHighlighted)
        }
    }

    fun queryByEventId(eventId: Int): Highlight? {
        val database = sqLiteOpenHelper.readableDatabase
        val cursor = try {
            database.read(
                tableName = HighlightsTable.NAME,
                selection = "$EVENT_ID=?",
                selectionArgs = arrayOf(eventId.toString())
            )
        } catch (e: SQLiteException) {
            return null
        }

        return cursor.use {
            if (cursor.moveToFirst()) {
                val highlightState = cursor.getInt(HIGHLIGHT)
                val isHighlighted = highlightState == HIGHLIGHT_STATE_ON
                Highlight(eventId, isHighlighted)
            } else {
                null
            }
        }
    }

    /**
     * Resets the value of the [HIGHLIGHT] column to [`false`][HIGHLIGHT_STATE_OFF] for each row.
     * Rows are not removed.
     */
    fun deleteAll() = with(sqLiteOpenHelper) {
        writableDatabase.transaction {
            updateRows(
                    tableName = HighlightsTable.NAME,
                    contentValues = ContentValues().apply {
                        put(HIGHLIGHT, HIGHLIGHT_STATE_OFF)
                    })
        }
    }

}
