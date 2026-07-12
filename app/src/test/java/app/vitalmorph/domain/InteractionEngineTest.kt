package app.vitalmorph.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class InteractionEngineTest {
    private val today: LocalDate = LocalDate.of(2026, 7, 12)
    private val baseTime = 1_000_000L

    @Test
    fun `head touch with good mood is happy and rewards mood and bond`() {
        val result = InteractionEngine.onTouch(InteractionState(), baseTime, today, TouchArea.HEAD, mood = 70)
        assertEquals(TouchReactionType.HAPPY, result.reaction)
        assertEquals(MoodEngine.TOUCH_HAPPY_MOOD, result.moodDelta)
        assertEquals(MoodEngine.TOUCH_BOND, result.bondDelta)
        assertTrue(result.rewarded)
        assertEquals(1, result.state.touchRewardCountToday)
    }

    @Test
    fun `head touch with normal mood is shy`() {
        val result = InteractionEngine.onTouch(InteractionState(), baseTime, today, TouchArea.HEAD, mood = 50)
        assertEquals(TouchReactionType.SHY, result.reaction)
        assertEquals(MoodEngine.TOUCH_SHY_MOOD, result.moodDelta)
    }

    @Test
    fun `touch during cooldown does nothing`() {
        val first = InteractionEngine.onTouch(InteractionState(), baseTime, today, TouchArea.HEAD, mood = 70)
        val second = InteractionEngine.onTouch(first.state, baseTime + 100, today, TouchArea.HEAD, mood = 70)
        assertEquals(TouchReactionType.NONE, second.reaction)
        assertEquals(0, second.moodDelta)
        assertEquals(first.state, second.state)
    }

    @Test
    fun `rapid consecutive touches become annoying`() {
        var state = InteractionState()
        var time = baseTime
        var lastReaction = TouchReactionType.NONE
        repeat(InteractionEngine.RAPID_TOUCH_LIMIT + 1) {
            val result = InteractionEngine.onTouch(state, time, today, TouchArea.HEAD, mood = 70)
            state = result.state
            lastReaction = result.reaction
            time += InteractionEngine.TOUCH_COOLDOWN_MS + 1
        }
        assertEquals(TouchReactionType.ANNOYED, lastReaction)
    }

    /** 指定した性格で連続タッチをくり返し、初めてANNOYEDになるまでのタッチ回数を返す。 */
    private fun touchesUntilAnnoyed(personality: Personality): Int {
        var state = InteractionState()
        var time = baseTime
        var count = 0
        repeat(50) {
            count++
            val result = InteractionEngine.onTouch(
                state, time, today, TouchArea.HEAD, mood = 70, personality = personality,
            )
            state = result.state
            if (result.reaction == TouchReactionType.ANNOYED) return count
            time += InteractionEngine.TOUCH_COOLDOWN_MS + 1
        }
        return -1
    }

    @Test
    fun `affectionate tolerates exactly two more rapid touches than default`() {
        val default = touchesUntilAnnoyed(Personality.HARDWORKER)
        val affectionate = touchesUntilAnnoyed(Personality.AFFECTIONATE)
        assertEquals(default + 2, affectionate)
    }

    @Test
    fun `cool tolerates exactly two fewer rapid touches than default`() {
        val default = touchesUntilAnnoyed(Personality.HARDWORKER)
        val cool = touchesUntilAnnoyed(Personality.COOL)
        assertEquals(default - 2, cool)
    }

    @Test
    fun `rapid touch limit shifts by personality`() {
        assertEquals(InteractionEngine.RAPID_TOUCH_LIMIT + 2, InteractionEngine.rapidTouchLimitFor(Personality.AFFECTIONATE))
        assertEquals(InteractionEngine.RAPID_TOUCH_LIMIT - 2, InteractionEngine.rapidTouchLimitFor(Personality.COOL))
        assertEquals(InteractionEngine.RAPID_TOUCH_LIMIT, InteractionEngine.rapidTouchLimitFor(Personality.HARDWORKER))
    }

    @Test
    fun `calm pause resets consecutive counter`() {
        var state = InteractionState()
        var time = baseTime
        repeat(InteractionEngine.RAPID_TOUCH_LIMIT) {
            state = InteractionEngine.onTouch(state, time, today, TouchArea.HEAD, mood = 70).state
            time += InteractionEngine.TOUCH_COOLDOWN_MS + 1
        }
        val afterPause = InteractionEngine.onTouch(
            state,
            time + InteractionEngine.RAPID_WINDOW_MS + 1,
            today,
            TouchArea.HEAD,
            mood = 70,
        )
        assertEquals(TouchReactionType.HAPPY, afterPause.reaction)
        assertEquals(1, afterPause.state.consecutiveTouches)
    }

    @Test
    fun `touch rewards stop at daily cap but reaction continues`() {
        var state = InteractionState(touchRewardCountToday = MoodEngine.TOUCH_REWARDS_PER_DAY, lastDailyResetDate = today)
        val result = InteractionEngine.onTouch(state, baseTime, today, TouchArea.HEAD, mood = 70)
        assertEquals(TouchReactionType.HAPPY, result.reaction)
        assertFalse(result.rewarded)
        assertEquals(0, result.moodDelta)
        assertEquals(0, result.bondDelta)
        assertEquals(MoodEngine.TOUCH_REWARDS_PER_DAY, result.state.touchRewardCountToday)
    }

    @Test
    fun `new day resets counters`() {
        val state = InteractionState(
            touchRewardCountToday = 8,
            conversationCountToday = 3,
            lastDailyResetDate = today.minusDays(1),
        )
        val reset = InteractionEngine.resetIfNewDay(state, today)
        assertEquals(0, reset.touchRewardCountToday)
        assertEquals(0, reset.conversationCountToday)
        assertEquals(today, reset.lastDailyResetDate)
    }

    @Test
    fun `moving clock backwards does not reset counters`() {
        val state = InteractionState(
            touchRewardCountToday = MoodEngine.TOUCH_REWARDS_PER_DAY,
            lastDailyResetDate = today,
        )
        val rolledBack = InteractionEngine.resetIfNewDay(state, today.minusDays(1))
        assertEquals(MoodEngine.TOUCH_REWARDS_PER_DAY, rolledBack.touchRewardCountToday)
        assertEquals(today, rolledBack.lastDailyResetDate)
    }

    @Test
    fun `conversation rewards stop at daily cap`() {
        var state = InteractionState(lastDailyResetDate = today)
        var rewardedCount = 0
        repeat(MoodEngine.DIALOGUE_REWARDS_PER_DAY + 2) { index ->
            val result = InteractionEngine.onConversation(state, baseTime + index * 10_000L, today)
            state = result.state
            if (result.rewarded) rewardedCount++
        }
        assertEquals(MoodEngine.DIALOGUE_REWARDS_PER_DAY, rewardedCount)
        assertEquals(MoodEngine.DIALOGUE_REWARDS_PER_DAY, state.conversationCountToday)
    }
}
