package app.vitalmorph.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DialogueEngineTest {
    private val base = DialogueContext(
        trainerName = "ショウタ",
        stage = MonsterStage.INTERMEDIATE,
        mood = 50,
        bond = 30,
        timeOfDay = TimeOfDay.MORNING,
        seasonDay = 16,
        recordedToday = true,
    )

    @Test
    fun `trainer name is inserted into text and replies`() {
        for (seed in 0..30) {
            val line = DialogueEngine.greeting(base, seed)
            assertFalse(line.text.contains("{name}"))
            assertTrue(line.choices.none { it.reply.contains("{name}") || it.text.contains("{name}") })
        }
        val named = DialogueEngine.greeting(base, 0)
        assertTrue((0..10).map { DialogueEngine.greeting(base, it).text }.any { it.contains("ショウタ") })
        assertEquals(named.text, DialogueEngine.greeting(base, 0).text)
    }

    @Test
    fun `missing trainer name falls back to generic call`() {
        val context = base.copy(trainerName = null)
        val texts = (0..10).map { DialogueEngine.greeting(context, it).text }
        assertTrue(texts.none { it.contains("{name}") })
        assertTrue(texts.any { it.contains(DialogueEngine.FALLBACK_TRAINER_NAME) })
    }

    @Test
    fun `same seed and context produce the same line`() {
        assertEquals(
            DialogueEngine.greeting(base, 7).text,
            DialogueEngine.greeting(base, 7).text,
        )
    }

    @Test
    fun `step goal achievement can be praised`() {
        val context = base.copy(stepsToday = 9_000, stepGoal = 8_000)
        val texts = (0..40).map { DialogueEngine.greeting(context, it).text }
        assertTrue(texts.any { it.contains("歩数目標") })
    }

    @Test
    fun `choice deltas stay small and bond never negative`() {
        for (bond in listOf(0, 30, 80)) {
            val line = DialogueEngine.greeting(base.copy(bond = bond), 0)
            assertEquals(2, line.choices.size)
            assertTrue(line.choices.all { it.moodDelta in -3..3 })
            assertTrue(line.choices.all { it.bondDelta in 0..2 })
        }
    }

    @Test
    fun `low mood line asks for company instead of blaming`() {
        val context = base.copy(mood = 5)
        val texts = (0..40).map { DialogueEngine.greeting(context, it).text }
        assertTrue(texts.any { it.contains("そばにいてくれる") })
        assertTrue(texts.none { it.contains("ダメ") || it.contains("サボ") })
    }

    @Test
    fun `time of day mapping covers all hours`() {
        assertEquals(TimeOfDay.MORNING, TimeOfDay.fromHour(5))
        assertEquals(TimeOfDay.MORNING, TimeOfDay.fromHour(10))
        assertEquals(TimeOfDay.DAYTIME, TimeOfDay.fromHour(11))
        assertEquals(TimeOfDay.DAYTIME, TimeOfDay.fromHour(16))
        assertEquals(TimeOfDay.EVENING, TimeOfDay.fromHour(17))
        assertEquals(TimeOfDay.EVENING, TimeOfDay.fromHour(21))
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.fromHour(22))
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.fromHour(0))
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.fromHour(4))
    }
}
