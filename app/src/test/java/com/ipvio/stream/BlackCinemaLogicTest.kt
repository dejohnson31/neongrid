package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlackCinemaLogicTest {
    private fun mv(id: String, title: String, year: Int?) =
        Item(id = id, kind = Kind.MOVIE, title = title, year = year, url = "http://x/$id", logo = null, group = "VOD")

    private val shaft = BlackCinemaLogic.Entry("Shaft", 1971, listOf("BLX"))
    private val friday = BlackCinemaLogic.Entry("Friday", 1995, listOf("COM", "HOOD"))

    @Test fun exactYearAndLanguageTagMatch() {
        val m = BlackCinemaLogic.match(listOf(shaft), listOf(mv("a", "EN - Shaft", 1971)))
        assertEquals(1, m.size)
        assertEquals("a", m[0].item.id)
    }

    @Test fun yearOffByOneIsAccepted_butNotTwo() {
        assertEquals(1, BlackCinemaLogic.match(listOf(friday), listOf(mv("a", "Friday", 1996))).size)
        assertTrue(BlackCinemaLogic.match(listOf(friday), listOf(mv("a", "Friday", 2000))).isEmpty())
    }

    @Test fun exactYearBeatsRemake() {
        val m = BlackCinemaLogic.match(listOf(shaft), listOf(mv("new", "Shaft", 2019), mv("old", "Shaft", 1971)))
        assertEquals("old", m[0].item.id)
    }

    @Test fun noYearOnlyWhenSingleCandidate() {
        assertEquals(1, BlackCinemaLogic.match(listOf(friday), listOf(mv("a", "Friday", null))).size)
        assertTrue(BlackCinemaLogic.match(listOf(friday), listOf(mv("a", "Friday", null), mv("b", "Friday", null))).isEmpty())
    }

    @Test fun seriesAreIgnored() {
        val s = Item("s", Kind.SERIES, "Friday", 1995, "u", null, "g")
        assertTrue(BlackCinemaLogic.match(listOf(friday), listOf(s)).isEmpty())
    }

    @Test fun decadeAndCategoryRows() {
        val ms = BlackCinemaLogic.match(listOf(shaft, friday), listOf(mv("a", "Shaft", 1971), mv("b", "Friday", 1995)))
        assertEquals(listOf("1970s", "1990s"), BlackCinemaLogic.byDecade(ms).map { it.first })
        assertEquals(listOf("COM"), BlackCinemaLogic.byCategory(ms, listOf("BLX_X", "COM")).map { it.first })
        assertNull(BlackCinemaLogic.byCategory(ms, listOf("HOR")).firstOrNull())
    }

    @Test fun preSeventiesAreClassics() {
        assertEquals("Classics", BlackCinemaLogic.decadeLabel(1960))
        assertEquals("2000s", BlackCinemaLogic.decadeLabel(2000))
    }
}
