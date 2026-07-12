package app.vitalmorph.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class MoodEngineTest {
    private val generation = MonsterGeneration(
        generationNumber = 1,
        sex = MonsterSex.MALE,
        seasonStart = LocalDate.of(2026, 7, 1),
        mood = 50,
        bond = 10,
    )

    @Test
    fun `mood bands follow phase 3 thresholds`() {
        assertEquals(MoodBand.BAD, MoodEngine.moodBand(0))
        assertEquals(MoodBand.BAD, MoodEngine.moodBand(19))
        assertEquals(MoodBand.LOW, MoodEngine.moodBand(20))
        assertEquals(MoodBand.NORMAL, MoodEngine.moodBand(59))
        assertEquals(MoodBand.GOOD, MoodEngine.moodBand(60))
        assertEquals(MoodBand.GREAT, MoodEngine.moodBand(80))
        assertEquals(MoodBand.GREAT, MoodEngine.moodBand(100))
    }

    @Test
    fun `bond bands cover full range`() {
        assertEquals(BondBand.DISTANT, MoodEngine.bondBand(0))
        assertEquals(BondBand.FRIENDLY, MoodEngine.bondBand(20))
        assertEquals(BondBand.DEVOTED, MoodEngine.bondBand(60))
        assertEquals(BondBand.DEVOTED, MoodEngine.bondBand(100))
    }

    @Test
    fun `apply delta clamps mood into range`() {
        assertEquals(100, MoodEngine.applyDelta(generation.copy(mood = 99), 5, 0).mood)
        assertEquals(0, MoodEngine.applyDelta(generation.copy(mood = 1), -5, 0).mood)
    }

    @Test
    fun `bond never decreases through apply delta`() {
        val result = MoodEngine.applyDelta(generation.copy(bond = 30), 0, -10)
        assertEquals(30, result.bond)
        assertEquals(100, MoodEngine.applyDelta(generation.copy(bond = 99), 0, 5).bond)
    }

    @Test
    fun `daily mood rises with records and drops only without record and interaction`() {
        assertEquals(MoodEngine.RECORDED_DAY_MOOD, MoodEngine.dailyMoodDelta(recordedYesterday = true, interactedYesterday = false))
        assertEquals(MoodEngine.RECORDED_DAY_MOOD, MoodEngine.dailyMoodDelta(recordedYesterday = true, interactedYesterday = true))
        assertEquals(0, MoodEngine.dailyMoodDelta(recordedYesterday = false, interactedYesterday = true))
        assertEquals(MoodEngine.NEGLECTED_DAY_MOOD, MoodEngine.dailyMoodDelta(recordedYesterday = false, interactedYesterday = false))
    }
}
