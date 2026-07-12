package app.vitalmorph.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.random.Random

class GenerationPlannerTest {
    private val seasonStart = LocalDate.of(2026, 6, 20)

    @Test
    fun `deterministic sex is stable across repeated calls`() {
        val first = SexAssigner.deterministicFor(seasonStart)
        repeat(100) {
            assertEquals(first, SexAssigner.deterministicFor(seasonStart))
        }
    }

    @Test
    fun `deterministic sex varies across different season starts`() {
        val results = (0L until 60L)
            .map { SexAssigner.deterministicFor(seasonStart.plusDays(it)) }
            .toSet()
        assertEquals(setOf(MonsterSex.MALE, MonsterSex.FEMALE), results)
    }

    @Test
    fun `random hatch is roughly fifty percent`() {
        val random = Random(42)
        val males = (0 until 1_000).count { SexAssigner.randomHatch(random) == MonsterSex.MALE }
        assertTrue("males=$males", males in 400..600)
    }

    @Test
    fun `keeps existing generation for the same season without rerolling sex`() {
        val existing = MonsterGeneration(
            generationId = 7,
            generationNumber = 3,
            sex = MonsterSex.FEMALE,
            seasonStart = seasonStart,
        )
        repeat(50) { attempt ->
            val plan = GenerationPlanner.plan(
                currentGeneration = existing,
                hasAnyGenerationHistory = true,
                seasonStart = seasonStart,
                completedSeasons = 2,
                random = Random(attempt),
            )
            assertTrue(plan is GenerationPlan.Keep)
            assertEquals(MonsterSex.FEMALE, (plan as GenerationPlan.Keep).generation.sex)
        }
    }

    @Test
    fun `first migration creates generation with deterministic sex and preserved numbering`() {
        val plan = GenerationPlanner.plan(
            currentGeneration = null,
            hasAnyGenerationHistory = false,
            seasonStart = seasonStart,
            completedSeasons = 5,
            random = Random(1),
        )
        assertTrue(plan is GenerationPlan.Create)
        val created = (plan as GenerationPlan.Create).create
        assertEquals(SexAssigner.deterministicFor(seasonStart), created.sex)
        assertEquals(6, created.generationNumber)
        assertEquals(seasonStart, created.seasonStart)
        assertEquals(null, created.seasonEnd)
    }

    @Test
    fun `migration result does not depend on random source`() {
        val sexes = (0 until 20).map { seed ->
            val plan = GenerationPlanner.plan(
                currentGeneration = null,
                hasAnyGenerationHistory = false,
                seasonStart = seasonStart,
                completedSeasons = 0,
                random = Random(seed),
            ) as GenerationPlan.Create
            plan.create.sex
        }.toSet()
        assertEquals(1, sexes.size)
    }

    @Test
    fun `new hatch after completed season uses random sex`() {
        val newStart = seasonStart.plusDays(28)
        val results = (0 until 200).map { seed ->
            val plan = GenerationPlanner.plan(
                currentGeneration = null,
                hasAnyGenerationHistory = true,
                seasonStart = newStart,
                completedSeasons = 1,
                random = Random(seed),
            ) as GenerationPlan.Create
            plan.create.sex
        }
        assertTrue(results.contains(MonsterSex.MALE))
        assertTrue(results.contains(MonsterSex.FEMALE))
    }

    @Test
    fun `stale open generation is closed before creating the next one`() {
        val stale = MonsterGeneration(
            generationId = 3,
            generationNumber = 1,
            sex = MonsterSex.MALE,
            seasonStart = seasonStart.minusDays(28),
        )
        val plan = GenerationPlanner.plan(
            currentGeneration = stale,
            hasAnyGenerationHistory = true,
            seasonStart = seasonStart,
            completedSeasons = 1,
            random = Random(9),
        )
        assertTrue(plan is GenerationPlan.CloseAndCreate)
        val closeAndCreate = plan as GenerationPlan.CloseAndCreate
        assertEquals(seasonStart, closeAndCreate.toClose.seasonEnd)
        assertEquals(2, closeAndCreate.create.generationNumber)
        assertEquals(seasonStart, closeAndCreate.create.seasonStart)
    }
}
