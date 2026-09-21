package com.example.catslist.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

/** The production name. A test passes its own so it doesn't touch the real database. */
const val CAT_DATABASE_NAME = "cats_database"

/** One place the database is configured, so tests exercise the build the app ships. */
fun catDatabaseBuilder(context: Context, name: String = CAT_DATABASE_NAME): RoomDatabase.Builder<CatDatabase> =
    Room.databaseBuilder(context, CatDatabase::class.java, name)
        .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
        // v1 shipped a different table and was only ever wiped, never migrated. Scoping the
        // fallback to that one version keeps a v1 install from crashing on launch while any
        // other missing migration still fails loudly.
        .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1)
