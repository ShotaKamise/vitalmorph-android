package app.vitalmorph.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.random.Random

class BeastRouteTest {
    private val start = LocalDate.of(2026, 1, 1)
    private val goals = UserGoals()

    private fun balancedSeason() = (0L..27L).map { offset ->
        DailyHealthData(
            date = start.plusDays(offset),
            calories = 2_000.0,
            proteinGrams = 80.0,
            carbsGrams = 250.0,
            fatGrams = 60.0,
            steps = 7_000,
            exerciseMinutes = 15,
            hasNutrition = true,
            hasActivity = true,
        )
    }

    @Test
    fun `beast route third week is a sex assigned animal intermediate`() {
        val days = balancedSeason()
        val male = EvolutionEngine.evaluate(days, goals, start, start.plusDays(14), MonsterSex.MALE, route = EvolutionRoute.BEAST)
        val female = EvolutionEngine.evaluate(days, goals, start, start.plusDays(14), MonsterSex.FEMALE, route = EvolutionRoute.BEAST)
        // 調和系統: ♂はソルフェオン、♀はアクアネル。
        assertEquals("solfeon", male.form.id)
        assertEquals("aquanel", female.form.id)
    }

    @Test
    fun `beast route final inherits parent sex`() {
        val days = balancedSeason()
        for (sex in MonsterSex.entries) {
            val result = EvolutionEngine.evaluate(days, goals, start, start.plusDays(27), sex, route = EvolutionRoute.BEAST)
            assertEquals(MonsterStage.FINAL, result.form.stage)
            assertEquals("最終形態 ${result.form.id} の性別", sex, EvolutionEngine.animalFormSex[result.form.id])
        }
    }

    @Test
    fun `humanoid route is unchanged by default`() {
        val days = balancedSeason()
        val result = EvolutionEngine.evaluate(days, goals, start, start.plusDays(14), MonsterSex.MALE)
        assertEquals("leon_saber_m", result.form.id)
    }

    @Test
    fun `every family has one male and one female intermediate`() {
        for (family in MonsterFamily.entries) {
            val male = EvolutionEngine.beastIntermediateFor(family, MonsterSex.MALE)
            val female = EvolutionEngine.beastIntermediateFor(family, MonsterSex.FEMALE)
            assertTrue(male.id != female.id)
            assertEquals(family, male.family)
            assertEquals(family, female.family)
            assertEquals(MonsterSex.MALE, EvolutionEngine.animalFormSex[male.id])
            assertEquals(MonsterSex.FEMALE, EvolutionEngine.animalFormSex[female.id])
        }
    }

    @Test
    fun `sex assignment covers 36 animal forms and finals match roster hints`() {
        // 成熟体12 + 最終形態24 = 36。モルフィと成長体6体は共通のため含まれない。
        assertEquals(36, EvolutionEngine.animalFormSex.size)
        assertEquals(18, EvolutionEngine.animalFormSex.values.count { it == MonsterSex.MALE })
        assertEquals(18, EvolutionEngine.animalFormSex.values.count { it == MonsterSex.FEMALE })
        // ロスター既存ヒントとの一致。
        assertEquals(MonsterSex.MALE, EvolutionEngine.animalFormSex["astelion"])
        assertEquals(MonsterSex.MALE, EvolutionEngine.animalFormSex["bastion_rex"])
        assertEquals(MonsterSex.MALE, EvolutionEngine.animalFormSex["zephyrion"])
        assertEquals(MonsterSex.FEMALE, EvolutionEngine.animalFormSex["miraflora"])
        assertEquals(MonsterSex.FEMALE, EvolutionEngine.animalFormSex["luna_verde"])
        assertEquals(MonsterSex.FEMALE, EvolutionEngine.animalFormSex["phoenix_crest"])
    }

    @Test
    fun `all 71 forms are reachable across routes and sexes`() {
        val reachable = mutableSetOf<String>()
        // 共通: 幼生+成長体6。
        reachable += "morphy"
        reachable += listOf("leafang", "galvol", "rapizel", "motchigrow", "mossleep", "runpact")
        // 人型ルート: 職業×性別の成熟体・最終形態と、第4週の動物系6体。
        for (job in HumanoidJob.entries) {
            for (sex in MonsterSex.entries) {
                reachable += HumanoidRoster.mature(job, sex).id
                reachable += HumanoidRoster.finalForm(job, sex).id
                reachable += EvolutionEngine.animalFinalFor(job.family, sex).id
            }
        }
        // 動物ルート: 系統×性別の成熟体と、その2候補の最終形態。
        for (family in MonsterFamily.entries) {
            for (sex in MonsterSex.entries) {
                val intermediate = EvolutionEngine.beastIntermediateFor(family, sex)
                reachable += intermediate.id
                val lowMetrics = EvolutionMetrics()
                val highMetrics = EvolutionMetrics(
                    nutritionScore = 100, activityScore = 120, consistencyScore = 100,
                    calorieRatio = 1.0, carbsRatio = 1.2, fatRatio = 0.8,
                    stepGoalDays = 7, strengthDays = 7, highActivityDays = 7, restDays = 7,
                    weekdayWeekendVariance = 0.5,
                )
                // 脂質過多・活動急増の変化パターンも加えて、両候補の到達を網羅する。
                val altMetrics = EvolutionMetrics(fatRatio = 1.5, carbsRatio = 0.5, activityTrend = 1.0)
                reachable += EvolutionEngine.chooseBeastFinal(intermediate, lowMetrics).id
                reachable += EvolutionEngine.chooseBeastFinal(intermediate, highMetrics).id
                reachable += EvolutionEngine.chooseBeastFinal(intermediate, altMetrics).id
            }
        }
        val all = EvolutionEngine.allForms.map { it.id }.toSet()
        assertEquals(71, all.size)
        val unreachable = all - reachable
        assertTrue("到達不能: $unreachable", unreachable.isEmpty())
    }

    @Test
    fun `route assignment is deterministic for migration and roughly even for hatches`() {
        assertEquals(RouteAssigner.deterministicFor(start), RouteAssigner.deterministicFor(start))
        val hatches = (0 until 1000).map { RouteAssigner.randomHatch(Random(it)) }
        val humanoid = hatches.count { it == EvolutionRoute.HUMANOID }
        assertTrue("偏りすぎ: $humanoid/1000", humanoid in 400..600)
    }
}
