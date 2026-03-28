package org.meow.autistic.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class DailyTasksScreenTest {

    @Test
    fun `formatTime midnight is 12_00 AM`() {
        assertEquals("12:00 AM", formatTime(0))
    }

    @Test
    fun `formatTime noon is 12_00 PM`() {
        assertEquals("12:00 PM", formatTime(720))
    }

    @Test
    fun `formatTime 8_30 AM`() {
        assertEquals("8:30 AM", formatTime(510))
    }

    @Test
    fun `formatTime 1_05 PM`() {
        assertEquals("1:05 PM", formatTime(785))
    }

    @Test
    fun `formatTime 11_59 PM`() {
        assertEquals("11:59 PM", formatTime(1439))
    }

    @Test
    fun `formatTime pads single-digit minutes`() {
        assertEquals("9:05 AM", formatTime(545))
    }
}
