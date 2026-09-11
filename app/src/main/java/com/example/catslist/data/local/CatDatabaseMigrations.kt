package com.example.catslist.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v2 -> v3: drop the `favorite` column. Every row in favoriteCatsTable is a
 * favorite by definition, so the column was always redundant (see
 * [CatEntity]). SQLite on this project's minSdk can't ALTER TABLE ... DROP
 * COLUMN, so recreate the table instead — the standard Room migration
 * pattern for a column removal.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS favoriteCatsTable_new (
                id TEXT NOT NULL PRIMARY KEY,
                url TEXT NOT NULL,
                width INTEGER NOT NULL,
                height INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO favoriteCatsTable_new (id, url, width, height)
            SELECT id, url, width, height FROM favoriteCatsTable
            """.trimIndent()
        )
        db.execSQL("DROP TABLE favoriteCatsTable")
        db.execSQL("ALTER TABLE favoriteCatsTable_new RENAME TO favoriteCatsTable")
    }
}
