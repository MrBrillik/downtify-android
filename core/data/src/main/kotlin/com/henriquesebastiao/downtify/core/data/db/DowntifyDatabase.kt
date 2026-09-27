package com.henriquesebastiao.downtify.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        TrackEntity::class,
        SyncStateEntity::class,
        PlaylistEntity::class,
        LikeEntity::class,
        RecentContextEntity::class,
        PendingListenEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class DowntifyDatabase : RoomDatabase() {
    abstract fun tracks(): TrackDao
    abstract fun playlists(): PlaylistDao
    abstract fun likes(): LikeDao
    abstract fun recents(): RecentContextDao
    abstract fun pendingListens(): PendingListenDao
}
