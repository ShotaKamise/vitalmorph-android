package app.vitalmorph.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TournamentScheduleTest {
    @Test
    fun `tournament days are the 7th 14th 21st 28th`() {
        assertFalse(TournamentSchedule.isTournamentDay(6))
        assertTrue(TournamentSchedule.isTournamentDay(7))
        assertFalse(TournamentSchedule.isTournamentDay(8))
        assertTrue(TournamentSchedule.isTournamentDay(14))
        assertTrue(TournamentSchedule.isTournamentDay(21))
        assertTrue(TournamentSchedule.isTournamentDay(28))
        assertFalse(TournamentSchedule.isTournamentDay(1))
    }

    @Test
    fun `weekOf maps season days to 1 through 4`() {
        assertEquals(1, TournamentSchedule.weekOf(1))
        assertEquals(1, TournamentSchedule.weekOf(7))
        assertEquals(2, TournamentSchedule.weekOf(8))
        assertEquals(2, TournamentSchedule.weekOf(14))
        assertEquals(3, TournamentSchedule.weekOf(15))
        assertEquals(4, TournamentSchedule.weekOf(28))
    }

    @Test
    fun `daysUntilTournament counts down to the next tournament day`() {
        assertEquals(0, TournamentSchedule.daysUntilTournament(7))
        assertEquals(6, TournamentSchedule.daysUntilTournament(8))
        assertEquals(6, TournamentSchedule.daysUntilTournament(1))
        assertEquals(0, TournamentSchedule.daysUntilTournament(14))
        assertEquals(1, TournamentSchedule.daysUntilTournament(13))
    }

    @Test
    fun `week multipliers scale from 60 to 115 percent`() {
        assertEquals(60, TournamentSchedule.weekMultiplierPercent(1))
        assertEquals(80, TournamentSchedule.weekMultiplierPercent(2))
        assertEquals(100, TournamentSchedule.weekMultiplierPercent(3))
        assertEquals(115, TournamentSchedule.weekMultiplierPercent(4))
    }

    @Test
    fun `season points average the four weeks with a fixed denominator`() {
        assertEquals(0, TournamentSchedule.seasonTournamentPoints(emptyMap()))
        // 10/4 = 2.5 → 四捨五入で3。
        assertEquals(3, TournamentSchedule.seasonTournamentPoints(mapOf(1 to 10)))
        assertEquals(10, TournamentSchedule.seasonTournamentPoints(mapOf(1 to 10, 2 to 10, 3 to 10, 4 to 10)))
    }

    @Test
    fun `season points round half up`() {
        // (10 + 0 + 0 + 0)/4 = 2.5 → 3。
        assertEquals(3, TournamentSchedule.seasonTournamentPoints(mapOf(2 to 10)))
        // (7 + 4)/4 = 2.75 → 3。
        assertEquals(3, TournamentSchedule.seasonTournamentPoints(mapOf(1 to 7, 2 to 4)))
        // (7 + 2)/4 = 2.25 → 2。
        assertEquals(2, TournamentSchedule.seasonTournamentPoints(mapOf(1 to 7, 2 to 2)))
    }
}
