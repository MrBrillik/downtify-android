package com.henriquesebastiao.downtify.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class LrcParserTest {

    @Test
    fun `parses stamps, repeated stamps, metadata and offset`() {
        val lines = LrcParser.parse(
            """
            [ar:Portishead]
            [offset:+500]
            [00:12.50]Can't anybody see
            [00:20.1][01:05.123]We've got a war to fight
            not a lyric line
            [01:10]
            """.trimIndent(),
        )
        assertEquals(
            listOf(
                LyricLine(12_000, "Can't anybody see"),
                LyricLine(19_600, "We've got a war to fight"),
                LyricLine(64_623, "We've got a war to fight"),
                LyricLine(69_500, ""),
            ),
            lines,
        )
    }

    @Test
    fun `finds the active line`() {
        val lines = listOf(LyricLine(1_000, "a"), LyricLine(5_000, "b"), LyricLine(9_000, "c"))
        assertEquals(-1, LrcParser.activeIndex(lines, 500))
        assertEquals(0, LrcParser.activeIndex(lines, 1_000))
        assertEquals(1, LrcParser.activeIndex(lines, 8_999))
        assertEquals(2, LrcParser.activeIndex(lines, 60_000))
    }
}
