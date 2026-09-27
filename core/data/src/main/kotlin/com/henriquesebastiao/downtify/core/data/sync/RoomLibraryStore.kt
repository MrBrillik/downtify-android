package com.henriquesebastiao.downtify.core.data.sync

import androidx.room.withTransaction
import com.henriquesebastiao.downtify.core.data.db.DowntifyDatabase
import com.henriquesebastiao.downtify.core.data.db.SyncStateEntity
import com.henriquesebastiao.downtify.core.data.db.TrackEntity
import com.henriquesebastiao.downtify.core.model.Track
import javax.inject.Inject

class RoomLibraryStore @Inject constructor(private val db: DowntifyDatabase) : LibraryLocalStore {
    private val tracks get() = db.tracks()

    override suspend fun syncState(): SyncState? = tracks.syncState()?.let { SyncState(it.serverId, it.cursor) }

    override suspend fun replaceAll(serverId: String, tracks: List<Track>, cursor: Long) = db.withTransaction {
        this.tracks.deleteAll()
        tracks.chunked(CHUNK).forEach { chunk -> this.tracks.upsert(chunk.map(TrackEntity::from)) }
        this.tracks.setSyncState(
            SyncStateEntity(serverId = serverId, cursor = cursor, syncedAt = System.currentTimeMillis()),
        )
    }

    override suspend fun applyChanges(serverId: String, tracks: List<Track>, deleted: List<String>, cursor: Long) =
        db.withTransaction {
            tracks.chunked(CHUNK).forEach { chunk -> this.tracks.upsert(chunk.map(TrackEntity::from)) }
            deleted.chunked(CHUNK).forEach { this.tracks.delete(it) }
            this.tracks.setSyncState(
                SyncStateEntity(serverId = serverId, cursor = cursor, syncedAt = System.currentTimeMillis()),
            )
        }

    /** Everything that belongs to one server: before pairing with another. */
    suspend fun clearServerData() = db.withTransaction {
        tracks.deleteAll()
        tracks.clearSyncState()
        db.playlists().deleteAll()
        db.likes().deleteAll()
        db.recents().deleteAll()
    }

    private companion object {
        /** SQLite's variable limit is 999 on older Androids. */
        const val CHUNK = 500
    }
}
