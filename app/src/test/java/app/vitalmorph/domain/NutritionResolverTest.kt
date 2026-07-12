package app.vitalmorph.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class NutritionResolverTest {
    private val start = LocalDate.of(2026, 7, 1)
    private val local = DayNutrition(1_800.0, 70.0, 50.0, 230.0)
    private val external = DayNutrition(2_100.0, 85.0, 60.0, 260.0)

    private fun entry(date: LocalDate, calories: Double) = FoodEntry(
        entryId = 1,
        date = date,
        mealSlot = MealSlot.LUNCH,
        foodName = "テスト",
        amount = 100.0,
        amountUnit = "g",
        calories = calories,
        proteinGrams = 10.0,
        fatGrams = 5.0,
        carbsGrams = 20.0,
        clientRecordId = "test",
        createdAt = 0,
        updatedAt = 0,
    )

    @Test
    fun `vitamorph first prefers local records`() {
        assertEquals(local, NutritionResolver.resolveDay(NutritionSource.VITALMORPH_FIRST, local, external))
        assertEquals(external, NutritionResolver.resolveDay(NutritionSource.VITALMORPH_FIRST, null, external))
        assertNull(NutritionResolver.resolveDay(NutritionSource.VITALMORPH_FIRST, null, null))
    }

    @Test
    fun `asken first prefers external records`() {
        assertEquals(external, NutritionResolver.resolveDay(NutritionSource.ASKEN_FIRST, local, external))
        assertEquals(local, NutritionResolver.resolveDay(NutritionSource.ASKEN_FIRST, local, null))
    }

    @Test
    fun `sources are never summed`() {
        val resolved = NutritionResolver.resolveDay(NutritionSource.VITALMORPH_FIRST, local, external)
        assertEquals(1_800.0, resolved!!.calories, 0.001)
    }

    @Test
    fun `merge overlays local entries on health connect days`() {
        val base = listOf(
            DailyHealthData(date = start, calories = 2_100.0, steps = 5_000, hasNutrition = true, hasActivity = true),
            DailyHealthData(date = start.plusDays(1), steps = 6_000, hasActivity = true),
        )
        val entries = mapOf(start to listOf(entry(start, 500.0), entry(start, 700.0)))
        val merged = NutritionResolver.mergeDays(base, entries, NutritionSource.VITALMORPH_FIRST, start, start.plusDays(1))

        assertEquals(2, merged.size)
        // ローカル記録がある日はローカルの合計。歩数などの活動データは維持される。
        assertEquals(1_200.0, merged[0].calories, 0.001)
        assertEquals(5_000, merged[0].steps)
        assertTrue(merged[0].hasNutrition)
        // ローカルが無い日は栄養なし(あすけん等の記録も無いケース)。
        assertFalse(merged[1].hasNutrition)
        assertEquals(6_000, merged[1].steps)
    }

    @Test
    fun `merge works without health connect data`() {
        val entries = mapOf(start.plusDays(2) to listOf(entry(start.plusDays(2), 800.0)))
        val merged = NutritionResolver.mergeDays(emptyList(), entries, NutritionSource.VITALMORPH_FIRST, start, start.plusDays(3))
        assertEquals(4, merged.size)
        assertEquals(800.0, merged[2].calories, 0.001)
        assertTrue(merged[2].hasNutrition)
        assertFalse(merged[0].hasNutrition)
    }

    @Test
    fun `select per day follows the stored choice and falls back to recorded side`() {
        // その日の選択がASKENなら外部を使う。
        assertEquals(
            external,
            NutritionResolver.resolveDay(NutritionSource.SELECT_PER_DAY, local, external, DayNutritionChoice.ASKEN),
        )
        // VITALMORPH選択ならローカル。
        assertEquals(
            local,
            NutritionResolver.resolveDay(NutritionSource.SELECT_PER_DAY, local, external, DayNutritionChoice.VITALMORPH),
        )
        // 未選択の日は記録した側(ローカル優先)。
        assertEquals(
            local,
            NutritionResolver.resolveDay(NutritionSource.SELECT_PER_DAY, local, external, null),
        )
        // 選んだ側に記録が無ければもう一方へフォールバック。
        assertEquals(
            local,
            NutritionResolver.resolveDay(NutritionSource.SELECT_PER_DAY, local, null, DayNutritionChoice.ASKEN),
        )
    }

    @Test
    fun `merge days applies per date choices`() {
        val base = listOf(
            DailyHealthData(date = start, calories = 2_100.0, hasNutrition = true),
            DailyHealthData(date = start.plusDays(1), calories = 2_100.0, hasNutrition = true),
        )
        val entries = mapOf(
            start to listOf(entry(start, 1_500.0)),
            start.plusDays(1) to listOf(entry(start.plusDays(1), 1_500.0)),
        )
        val merged = NutritionResolver.mergeDays(
            base,
            entries,
            NutritionSource.SELECT_PER_DAY,
            start,
            start.plusDays(1),
            dayChoices = mapOf(start to DayNutritionChoice.ASKEN),
        )
        assertEquals(2_100.0, merged[0].calories, 0.001)
        assertEquals(1_500.0, merged[1].calories, 0.001)
    }

    @Test
    fun `asken first keeps external totals even with local entries`() {
        val base = listOf(
            DailyHealthData(date = start, calories = 2_100.0, proteinGrams = 85.0, fatGrams = 60.0, carbsGrams = 260.0, hasNutrition = true),
        )
        val entries = mapOf(start to listOf(entry(start, 500.0)))
        val merged = NutritionResolver.mergeDays(base, entries, NutritionSource.ASKEN_FIRST, start, start)
        assertEquals(2_100.0, merged[0].calories, 0.001)
    }
}
