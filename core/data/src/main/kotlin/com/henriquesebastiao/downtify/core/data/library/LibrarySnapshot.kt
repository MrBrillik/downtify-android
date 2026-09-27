package com.henriquesebastiao.downtify.core.data.library

import com.henriquesebastiao.downtify.core.model.Album
import com.henriquesebastiao.downtify.core.model.Artist
import com.henriquesebastiao.downtify.core.model.LibraryGrouping
import com.henriquesebastiao.downtify.core.model.Track

/** The whole synced library, grouped once per change and shared by every screen. */
class LibrarySnapshot(val tracks: List<Track>) {
    val byId: Map<String, Track> = tracks.associateBy { it.id }
    val albums: List<Album> = LibraryGrouping.albums(tracks)
    val artists: List<Artist> = LibraryGrouping.artists(tracks)
    val albumsById: Map<String, Album> = albums.associateBy { it.id }
    val artistsById: Map<String, Artist> = artists.associateBy { it.id }

    fun tracks(ids: List<String>): List<Track> = ids.mapNotNull(byId::get)

    val isEmpty: Boolean get() = tracks.isEmpty()

    companion object {
        val Empty = LibrarySnapshot(emptyList())
    }
}
