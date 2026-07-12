package app.vitalmorph.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class EvolutionEngineTest {
    private val start = LocalDate.of(2026, 1, 1)
    private val goals = UserGoals()

    private fun balancedDay(offset: Long, transform: (DailyHealthData) -> DailyHealthData = { it }) =
        transform(
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
            ),
        )

    private fun balancedSeason(days: Long = 27L) = (0L..days).map { balancedDay(it) }

    @Test
    fun `season changes stage every seven days`() {
        val days = balancedSeason()
        assertEquals(MonsterStage.BABY, EvolutionEngine.evaluate(days, goals, start, start.plusDays(6), MonsterSex.MALE).form.stage)
        assertEquals(MonsterStage.FAMILY, EvolutionEngine.evaluate(days, goals, start, start.plusDays(7), MonsterSex.MALE).form.stage)
        assertEquals(MonsterStage.INTERMEDIATE, EvolutionEngine.evaluate(days, goals, start, start.plusDays(14), MonsterSex.MALE).form.stage)
        assertEquals(MonsterStage.FINAL, EvolutionEngine.evaluate(days, goals, start, start.plusDays(21), MonsterSex.MALE).form.stage)
    }

    @Test
    fun `third week mature form is humanoid and split by sex`() {
        val days = balancedSeason(20L)
        val male = EvolutionEngine.evaluate(days, goals, start, start.plusDays(14), MonsterSex.MALE)
        val female = EvolutionEngine.evaluate(days, goals, start, start.plusDays(14), MonsterSex.FEMALE)

        assertEquals("leon_saber_m", male.form.id)
        assertEquals("valeria_f", female.form.id)
        assertNotEquals(male.form.id, female.form.id)
        assertTrue(male.form.id.endsWith("_m"))
        assertTrue(female.form.id.endsWith("_f"))
    }

    @Test
    fun `mature stage exposes humanoid and animal final candidates`() {
        val days = balancedSeason(20L)
        val result = EvolutionEngine.evaluate(days, goals, start, start.plusDays(20), MonsterSex.MALE)
        assertEquals(MonsterStage.INTERMEDIATE, result.form.stage)
        assertEquals(2, result.finalCandidates.size)
        assertTrue(result.finalCandidates.all { it.stage == MonsterStage.FINAL })
        assertTrue(result.finalCandidates.any { it.id == "sol_regnard_m" })
        assertTrue(result.finalCandidates.any { it.id == "astelion" })
    }

    @Test
    fun `strength weeks lead to greatsword job`() {
        val days = (0L..20L).map { offset ->
            balancedDay(offset) { it.copy(workoutTag = if (offset % 2 == 0L) WorkoutTag.STRENGTH else WorkoutTag.NONE) }
        }
        val result = EvolutionEngine.evaluate(days, goals, start, start.plusDays(14), MonsterSex.FEMALE)
        assertEquals("crim_arge_f", result.form.id)
    }

    @Test
    fun `step heavy speed weeks lead to dual blade and workout heavy to lancer`() {
        // 8,500歩は歩数目標(8,000)以上・過活動判定(9,600)未満 → 俊足系統の歩数型。
        val stepDays = (0L..20L).map { offset -> balancedDay(offset) { it.copy(steps = 8_500) } }
        val dualBlade = EvolutionEngine.evaluate(stepDays, goals, start, start.plusDays(14), MonsterSex.MALE)
        assertEquals("twin_fang_m", dualBlade.form.id)

        // 第1週は有酸素で俊足系統になり、第2週は歩数目標に届かない → 槍使い。
        val cardioDays = (0L..20L).map { offset ->
            balancedDay(offset) {
                if (offset <= 6L) it.copy(workoutTag = if (offset % 3 == 0L) WorkoutTag.CARDIO else WorkoutTag.NONE)
                else it.copy(steps = 6_000)
            }
        }
        val lancer = EvolutionEngine.evaluate(cardioDays, goals, start, start.plusDays(14), MonsterSex.MALE)
        assertEquals("volt_lancer_m", lancer.form.id)
    }

    @Test
    fun `quiet weeks lead to mage when consistent and ninja when irregular`() {
        fun restDay(offset: Long, recorded: Boolean) = DailyHealthData(
            date = start.plusDays(offset),
            calories = if (recorded) 1_900.0 else 0.0,
            proteinGrams = if (recorded) 75.0 else 0.0,
            carbsGrams = if (recorded) 240.0 else 0.0,
            fatGrams = if (recorded) 55.0 else 0.0,
            steps = 2_000,
            exerciseMinutes = 0,
            hasNutrition = recorded,
            hasActivity = recorded,
        )

        val consistent = (0L..20L).map { restDay(it, recorded = true) }
        val mage = EvolutionEngine.evaluate(consistent, goals, start, start.plusDays(14), MonsterSex.FEMALE)
        assertEquals("mystica_f", mage.form.id)

        val irregular = (0L..20L).map { restDay(it, recorded = it % 2 == 0L) }
        val ninja = EvolutionEngine.evaluate(irregular, goals, start, start.plusDays(14), MonsterSex.FEMALE)
        assertEquals("yoidzuki_f", ninja.form.id)
    }

    @Test
    fun `high mood or bond leads to upper humanoid final`() {
        val days = balancedSeason()
        val highMood = EvolutionEngine.evaluate(days, goals, start, start.plusDays(27), MonsterSex.MALE, mood = 80, bond = 0)
        assertEquals("sol_regnard_m", highMood.form.id)

        val highBond = EvolutionEngine.evaluate(days, goals, start, start.plusDays(27), MonsterSex.FEMALE, mood = 30, bond = 60)
        assertEquals("val_rose_f", highBond.form.id)
    }

    @Test
    fun `low mood and bond leads to animal final by sex`() {
        val days = balancedSeason()
        val male = EvolutionEngine.evaluate(days, goals, start, start.plusDays(27), MonsterSex.MALE, mood = 40, bond = 10)
        assertEquals("astelion", male.form.id)

        val female = EvolutionEngine.evaluate(days, goals, start, start.plusDays(27), MonsterSex.FEMALE, mood = 40, bond = 10)
        assertEquals("miraflora", female.form.id)
    }

    @Test
    fun `poor consistency in third week never reaches upper humanoid`() {
        // 第3週(15〜21日目)の記録が無い → 継続性0 → 機嫌が高くても動物系最終形態。
        val days = (0L..27L).filter { it < 14L || it > 20L }.map { balancedDay(it) }
        val result = EvolutionEngine.evaluate(days, goals, start, start.plusDays(27), MonsterSex.MALE, mood = 100, bond = 100)
        assertEquals("astelion", result.form.id)
    }

    @Test
    fun `all humanoid ids stay within approved roster`() {
        val approved = setOf(
            "leon_saber_m", "sol_regnard_m", "valeria_f", "val_rose_f",
            "twin_fang_m", "zero_dualion_m", "lila_twin_f", "lumina_duella_f",
            "grand_breaker_m", "titan_glaive_m", "crim_arge_f", "grand_empress_f",
            "volt_lancer_m", "tempest_dragoon_m", "celes_lancer_f", "astra_reina_f",
            "barrel_guard_m", "arc_buster_m", "rouge_shell_f", "nova_valeria_f",
            "rune_sage_m", "astra_magius_m", "mystica_f", "eclipsia_f",
            "kagerou_m", "mugen_shinobi_m", "yoidzuki_f", "tsukikage_hime_f",
        )
        assertEquals(28, HumanoidRoster.all.size)
        assertEquals(approved, HumanoidRoster.all.map { it.id }.toSet())
    }

    @Test
    fun `male routes never produce female ids and vice versa`() {
        for (job in HumanoidJob.entries) {
            assertTrue(HumanoidRoster.mature(job, MonsterSex.MALE).id.endsWith("_m"))
            assertTrue(HumanoidRoster.finalForm(job, MonsterSex.MALE).id.endsWith("_m"))
            assertTrue(HumanoidRoster.mature(job, MonsterSex.FEMALE).id.endsWith("_f"))
            assertTrue(HumanoidRoster.finalForm(job, MonsterSex.FEMALE).id.endsWith("_f"))
        }
    }

    @Test
    fun `opponent pools match the player growth stage per week`() {
        // 第1・2週は成長体6種。
        assertEquals(6, EvolutionEngine.opponentPoolFor(1).size)
        assertEquals(6, EvolutionEngine.opponentPoolFor(2).size)
        assertTrue(EvolutionEngine.opponentPoolFor(1).all { it.stage == MonsterStage.FAMILY })
        // 第3週は成熟体26種(人型14 + 動物12)。
        assertEquals(26, EvolutionEngine.opponentPoolFor(3).size)
        assertTrue(EvolutionEngine.opponentPoolFor(3).all { it.stage == MonsterStage.INTERMEDIATE })
        // 第4週は最終形態38種(人型14 + 動物24)。
        assertEquals(38, EvolutionEngine.opponentPoolFor(4).size)
        assertTrue(EvolutionEngine.opponentPoolFor(4).all { it.stage == MonsterStage.FINAL })
    }

    @Test
    fun `morphy is never used as an opponent`() {
        for (week in 1..4) {
            assertTrue(EvolutionEngine.opponentPoolFor(week).none { it.id == "morphy" })
        }
    }
}
