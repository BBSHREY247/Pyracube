package com.pyracube.music.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SongEntity::class,
        DownloadTaskEntity::class,
        PlaybackQueueEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun songDao(): SongDao

    abstract fun downloadTaskDao(): DownloadTaskDao

    abstract fun playbackQueueDao(): PlaybackQueueDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(
            context: Context
        ): AppDatabase {

            return INSTANCE
                ?: synchronized(this) {

                    val instance =
                        Room.databaseBuilder(
                            context.applicationContext,
                            AppDatabase::class.java,
                            "pyracube.db"
                        )
                            .fallbackToDestructiveMigration()
                            .build()

                    INSTANCE = instance

                    instance
                }
        }
    }
}