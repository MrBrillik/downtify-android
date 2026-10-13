package com.henriquesebastiao.downtify.core.player

import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Timeline

/** The order a [Player] will play its items in, which isn't index order when shuffle is on. */
internal object PlayOrder {
    /** The current item and everything after it, as it will play (repeat aside). */
    fun fromCurrent(player: Player): List<Int> {
        val timeline = player.currentTimeline
        if (timeline.isEmpty) return emptyList()
        return walk(timeline, player.currentMediaItemIndex, player.shuffleModeEnabled)
    }

    /** Every item in shuffled order, from the first. */
    fun shuffled(timeline: Timeline): List<Int> =
        if (timeline.isEmpty) emptyList() else walk(timeline, timeline.getFirstWindowIndex(true), true)

    private fun walk(timeline: Timeline, from: Int, shuffle: Boolean): List<Int> {
        val order = ArrayList<Int>()
        var index = from
        while (index != C.INDEX_UNSET && order.size < timeline.windowCount) {
            order += index
            index = timeline.getNextWindowIndex(index, Player.REPEAT_MODE_OFF, shuffle)
        }
        return order
    }
}
