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
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO favoriteCatsTable_new (id, url, width, height)
            SELECT id, url, width, height FROM favoriteCatsTable
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE favoriteCatsTable")
        db.execSQL("ALTER TABLE favoriteCatsTable_new RENAME TO favoriteCatsTable")
    }
}

/**
 * v3 -> v4: add the Paging 3 feed cache (`feedCatsTable`, `feedRemoteKeysTable`). Both are
 * brand new and start empty — nothing to copy, unlike [MIGRATION_2_3]. A destructive fallback
 * would have been just as safe here (there is no user data in either table), but a real
 * migration costs nothing extra and keeps every schema change in this file consistent.
 *
 * Kept although [MIGRATION_4_5] removes both tables again: a v3 install still has to reach v5,
 * and Room walks the chain one step at a time.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS feedCatsTable (
                id TEXT NOT NULL PRIMARY KEY,
                url TEXT NOT NULL,
                width INTEGER NOT NULL,
                height INTEGER NOT NULL,
                sortOrder INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS feedRemoteKeysTable (
                id INTEGER NOT NULL PRIMARY KEY,
                nextPage INTEGER
            )
            """.trimIndent(),
        )
    }
}

/**
 * v4 -> v5: drop the feed cache. The feed is paged straight from the network now, so nothing
 * persists it — only favorites are kept on disk.
 *
 * Dropping rather than preserving is the whole point: the cached cats existed to be rendered at
 * launch before a refresh replaced them, which is exactly the behaviour being removed. No user
 * data is lost, because none of it was ever the user's — favoriteCatsTable is untouched.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS feedCatsTable")
        db.execSQL("DROP TABLE IF EXISTS feedRemoteKeysTable")
    }
}
