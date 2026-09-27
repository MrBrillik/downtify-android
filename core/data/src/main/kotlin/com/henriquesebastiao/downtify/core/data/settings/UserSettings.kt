package com.henriquesebastiao.downtify.core.data.settings

import com.henriquesebastiao.downtify.core.model.StreamQuality

enum class ThemeMode { System, Dark, Light }

enum class LibraryLayout { Grid, List }

enum class LibrarySort { RecentlyAdded, Alphabetical, Artist }

data class UserSettings(
    val theme: ThemeMode = ThemeMode.System,
    /** Material You wallpaper colors instead of Downtify green. Off by default. */
    val dynamicColor: Boolean = false,
    val wifiQuality: StreamQuality = StreamQuality.Original,
    val mobileQuality: StreamQuality = StreamQuality.MobileDefault,
    val libraryLayout: LibraryLayout = LibraryLayout.Grid,
    val librarySort: LibrarySort = LibrarySort.RecentlyAdded,
)
