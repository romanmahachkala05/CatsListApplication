package com.example.catslist.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

/** The production name. A test passes its own so it doesn't touch the real database. */
const val CAT_DATABASE_NAME = "cats_database"

/**
 * One place where the database is configured, so a test exercises the same upgrade
 * behaviour the app ships with rather than a copy that can drift from it.
 */
fun catDatabaseBuilder(
    context: Context,
    name: String = CAT_DATABASE_NAME,
): RoomDatabase.Builder<CatDatabase> =
    Room.databaseBuilder(context, CatDatabase::class.java, name)
        .addMigrations(MIGRATION_2_3)
        // v1 stored a different table (`favoriteCats`, with a `favourite` column) and was
        // only ever wiped rather than migrated — the original app shipped
        // `fallbackToDestructiveMigration()`. Registering only MIGRATION_2_3 turned that
        // silent wipe into `IllegalStateException: A migration from 1 to 3 was required but
        // not found`, so a v1 install would crash on launch instead of starting empty.
        // This restores the old outcome and, unlike the old blanket call, says so for
        // exactly one version: any other missing migration still fails loudly in review.
        // `dropAllTables` clears v1's `favoriteCats` too, which is not an entity any more.
        .fallbackToDestructiveMigrationFrom(true, 1)
