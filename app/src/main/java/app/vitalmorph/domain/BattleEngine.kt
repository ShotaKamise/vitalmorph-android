package app.vitalmorph.domain

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

object BattleEngine {
    private const val MAX_ENERGY = 3
    private val rounds = listOf("準々決勝", "準決勝", "決勝")
    private val opponentNames = listOf(
        "アイアンホーン", "クリムゾウル", "ナイトグライド",
        "コバルトスク", "ロックバイター", "ミラージュパウ",
    )

    private val healingItem = BattleItem("vita_tonic", "ヴィータトニック", "HPを40回復する")
    private val energyItem = BattleItem("core_cell", "コアセル", "エネルギーを2回復する")
    private val guardItem = BattleItem("pulse_guard", "パルスガード", "次に受ける攻撃を半減する")

    fun startTournament(monster: MonsterForm, metrics: EvolutionMetrics, seed: Int): TurnBattleState {
        val stageBonus = monster.stage.ordinal * 12
        val maxHp = 120 + metrics.consistencyScore / 2 + metrics.nutritionScore / 3 + stageBonus
        val attack = 18 + metrics.activityScore.coerceAtMost(130) / 8 + metrics.nutritionScore / 20 + stageBonus / 3
        val defense = 10 + metrics.nutritionScore / 12 + metrics.consistencyScore / 15 + stageBonus / 4
        val speed = 10 + metrics.activityScore.coerceAtMost(130) / 10 + metrics.stepGoalDays * 2
        return createRound(
            roundIndex = 0,
            playerName = monster.name,
            playerHp = maxHp,
            playerMaxHp = maxHp,
            playerAttack = attack,
            playerDefense = defense,
            playerSpeed = speed,
            moves = movesFor(monster.family),
            items = listOf(
                BattleItemStock(healingItem, 2),
                BattleItemStock(energyItem, 1),
                BattleItemStock(guardItem, 1),
            ),
            completedMatches = emptyList(),
            seed = seed,
        )
    }

    fun useMove(state: TurnBattleState, moveId: String): TurnBattleState {
        if (state.outcome != BattleOutcome.IN_PROGRESS) return state
        val move = state.moves.firstOrNull { it.id == moveId } ?: return state
        if (state.playerEnergy < move.energyCost) return state.copy(
            log = appendLog(state.log, "エネルギーが足りない！"),
        )
        return resolveTurn(state, PlayerAction.Move(move))
    }

    fun useItem(state: TurnBattleState, itemId: String): TurnBattleState {
        if (state.outcome != BattleOutcome.IN_PROGRESS) return state
        val stock = state.items.firstOrNull { it.item.id == itemId } ?: return state
        if (stock.remaining <= 0) return state.copy(log = appendLog(state.log, "そのアイテムはもう残っていない。"))
        return resolveTurn(state, PlayerAction.Item(stock.item))
    }

    fun nextRound(state: TurnBattleState): TurnBattleState {
        if (state.outcome != BattleOutcome.ROUND_WON || state.roundIndex >= rounds.lastIndex) return state
        val recoveredHp = min(state.playerMaxHp, state.playerHp + state.playerMaxHp * 35 / 100)
        return createRound(
            roundIndex = state.roundIndex + 1,
            playerName = state.playerName,
            playerHp = recoveredHp,
            playerMaxHp = state.playerMaxHp,
            playerAttack = state.playerAttack,
            playerDefense = state.playerDefense,
            playerSpeed = state.playerSpeed,
            moves = state.moves,
            items = state.items,
            completedMatches = state.completedMatches,
            seed = state.seed,
        ).copy(log = listOf("${rounds[state.roundIndex + 1]}開始！ HPが少し回復した。"))
    }

    fun resultFor(state: TurnBattleState): TournamentResult? = when (state.outcome) {
        BattleOutcome.TOURNAMENT_WON -> TournamentResult(1, 10, state.completedMatches)
        BattleOutcome.PLAYER_LOST -> {
            val placement = when (state.roundIndex) {
                0 -> 8
                1 -> 4
                else -> 2
            }
            val points = when (placement) {
                2 -> 7
                4 -> 4
                else -> 2
            }
            TournamentResult(placement, points, state.completedMatches)
        }
        else -> null
    }

    fun movesFor(family: MonsterFamily): List<BattleMove> {
        val familyMove = when (family) {
            MonsterFamily.BALANCE -> BattleMove("family", "ハーモニーパルス", "攻撃しながらHPを少し回復", BattleMoveKind.RECOVERY, 26, 1, heal = 8)
            MonsterFamily.POWER -> BattleMove("family", "ブレイクナックル", "重い一撃を叩き込む", BattleMoveKind.ATTACK, 40, 2)
            MonsterFamily.SPEED -> BattleMove("family", "ゲイルラッシュ", "素早く先制しやすい", BattleMoveKind.ATTACK, 29, 1, priority = 1)
            MonsterFamily.STORAGE -> BattleMove("family", "シェルバッシュ", "攻撃後に防御姿勢を取る", BattleMoveKind.GUARD, 25, 1, priority = 1)
            MonsterFamily.REST -> BattleMove("family", "ムーンリカバー", "月の力で大きく回復", BattleMoveKind.RECOVERY, 12, 1, heal = 22)
            MonsterFamily.OVERDRIVE -> BattleMove("family", "オーバーバーン", "反動と引き換えの超火力", BattleMoveKind.ATTACK, 48, 2, recoil = 8)
        }
        return listOf(
            BattleMove("core_strike", "コアストライク", "安定した基本攻撃", BattleMoveKind.ATTACK, 22, 0),
            familyMove,
            BattleMove("guard_shift", "ガードシフト", "先に構えて次の攻撃を半減", BattleMoveKind.GUARD, 0, 0, priority = 2),
            BattleMove("life_burst", "ライフバースト", "生命コアを解放する必殺技", BattleMoveKind.ATTACK, 55, 3),
        )
    }

    private fun createRound(
        roundIndex: Int,
        playerName: String,
        playerHp: Int,
        playerMaxHp: Int,
        playerAttack: Int,
        playerDefense: Int,
        playerSpeed: Int,
        moves: List<BattleMove>,
        items: List<BattleItemStock>,
        completedMatches: List<BattleMatch>,
        seed: Int,
    ): TurnBattleState {
        val difficulty = roundIndex * 6
        val opponentHp = playerMaxHp - 8 + difficulty * 2
        return TurnBattleState(
            roundIndex = roundIndex,
            roundLabel = rounds[roundIndex],
            playerName = playerName,
            opponentName = opponentNames[Math.floorMod(seed + roundIndex, opponentNames.size)],
            playerHp = playerHp,
            playerMaxHp = playerMaxHp,
            opponentHp = opponentHp,
            opponentMaxHp = opponentHp,
            playerEnergy = MAX_ENERGY,
            opponentEnergy = MAX_ENERGY,
            playerAttack = playerAttack,
            playerDefense = playerDefense,
            playerSpeed = playerSpeed,
            opponentAttack = playerAttack - 2 + difficulty,
            opponentDefense = playerDefense - 1 + difficulty / 2,
            opponentSpeed = playerSpeed - 2 + difficulty,
            playerGuarding = false,
            opponentGuarding = false,
            opponentPotions = 1,
            moves = moves,
            items = items,
            turn = 1,
            seed = seed,
            outcome = BattleOutcome.IN_PROGRESS,
            log = listOf("${rounds[roundIndex]}！ ${opponentNames[Math.floorMod(seed + roundIndex, opponentNames.size)]}が現れた。"),
            completedMatches = completedMatches,
        )
    }

    private fun resolveTurn(state: TurnBattleState, playerAction: PlayerAction): TurnBattleState {
        var playerHp = state.playerHp
        var opponentHp = state.opponentHp
        var playerEnergy = state.playerEnergy
        var opponentEnergy = state.opponentEnergy
        var playerGuarding = state.playerGuarding
        var opponentGuarding = state.opponentGuarding
        var opponentPotions = state.opponentPotions
        var items = state.items
        val messages = mutableListOf("TURN ${state.turn}")
        val random = Random(state.seed + state.roundIndex * 997 + state.turn * 37)
        val cpuAction = chooseCpuAction(state, random)

        fun playerPriority(): Int = when (playerAction) {
            is PlayerAction.Item -> 3
            is PlayerAction.Move -> playerAction.move.priority
        }
        fun cpuPriority(): Int = when (cpuAction) {
            CpuAction.Heal -> 3
            CpuAction.Guard -> 2
            is CpuAction.Attack -> cpuAction.priority
        }

        fun applyPlayerAction() {
            if (playerHp <= 0 || opponentHp <= 0) return
            when (playerAction) {
                is PlayerAction.Item -> {
                    items = items.map {
                        if (it.item.id == playerAction.item.id) it.copy(remaining = it.remaining - 1) else it
                    }
                    when (playerAction.item.id) {
                        healingItem.id -> {
                            val healed = min(40, state.playerMaxHp - playerHp)
                            playerHp += healed
                            messages += "トレーナーは${playerAction.item.name}を使用。HPが${healed}回復！"
                        }
                        energyItem.id -> {
                            val restored = min(2, MAX_ENERGY - playerEnergy)
                            playerEnergy += restored
                            messages += "トレーナーは${playerAction.item.name}を使用。エネルギー+$restored！"
                        }
                        guardItem.id -> {
                            playerGuarding = true
                            messages += "トレーナーは${playerAction.item.name}を使用。防御膜を展開！"
                        }
                    }
                }
                is PlayerAction.Move -> {
                    val move = playerAction.move
                    playerEnergy -= move.energyCost
                    if (move.kind == BattleMoveKind.GUARD) {
                        playerGuarding = true
                        messages += "${state.playerName}の${move.name}！ 防御姿勢を取った。"
                    }
                    if (move.power > 0) {
                        var damage = damage(move.power, state.playerAttack, state.opponentDefense, random)
                        if (opponentGuarding) {
                            damage = max(1, damage / 2)
                            opponentGuarding = false
                            messages += "相手のガードがダメージを抑えた。"
                        }
                        opponentHp = max(0, opponentHp - damage)
                        messages += "${state.playerName}の${move.name}！ ${damage}ダメージ。"
                    }
                    if (move.heal > 0) {
                        val healed = min(move.heal, state.playerMaxHp - playerHp)
                        playerHp += healed
                        messages += "HPが${healed}回復。"
                    }
                    if (move.recoil > 0 && opponentHp > 0) {
                        playerHp = max(1, playerHp - move.recoil)
                        messages += "反動で${move.recoil}ダメージ。"
                    }
                }
            }
        }

        fun applyCpuAction() {
            if (playerHp <= 0 || opponentHp <= 0) return
            when (cpuAction) {
                CpuAction.Heal -> {
                    val healed = min(32, state.opponentMaxHp - opponentHp)
                    opponentHp += healed
                    opponentPotions--
                    messages += "CPUトレーナーはリペアミストを使用。相手のHPが${healed}回復！"
                }
                CpuAction.Guard -> {
                    opponentGuarding = true
                    opponentEnergy = min(MAX_ENERGY, opponentEnergy + 1)
                    messages += "${state.opponentName}はガードし、力を溜めている。"
                }
                is CpuAction.Attack -> {
                    opponentEnergy -= cpuAction.cost
                    var dealt = damage(cpuAction.power, state.opponentAttack, state.playerDefense, random)
                    if (playerGuarding) {
                        dealt = max(1, dealt / 2)
                        playerGuarding = false
                        messages += "ガードがダメージを抑えた。"
                    }
                    playerHp = max(0, playerHp - dealt)
                    messages += "${state.opponentName}の${cpuAction.name}！ ${dealt}ダメージ。"
                }
            }
        }

        val playerFirst = playerPriority() > cpuPriority() ||
            (playerPriority() == cpuPriority() && state.playerSpeed >= state.opponentSpeed)
        if (playerFirst) {
            applyPlayerAction()
            applyCpuAction()
        } else {
            applyCpuAction()
            applyPlayerAction()
        }

        val outcome = when {
            opponentHp <= 0 && state.roundIndex == rounds.lastIndex -> BattleOutcome.TOURNAMENT_WON
            opponentHp <= 0 -> BattleOutcome.ROUND_WON
            playerHp <= 0 -> BattleOutcome.PLAYER_LOST
            else -> BattleOutcome.IN_PROGRESS
        }
        val matches = if (outcome != BattleOutcome.IN_PROGRESS) {
            state.completedMatches + BattleMatch(
                round = state.roundLabel,
                opponent = state.opponentName,
                playerScore = playerHp,
                opponentScore = opponentHp,
                won = opponentHp <= 0,
            )
        } else state.completedMatches
        when (outcome) {
            BattleOutcome.ROUND_WON -> messages += "${state.roundLabel}勝利！ 次の試合へ進める。"
            BattleOutcome.TOURNAMENT_WON -> messages += "大会優勝！"
            BattleOutcome.PLAYER_LOST -> messages += "敗北。戦術を変えて再挑戦しよう。"
            BattleOutcome.IN_PROGRESS -> Unit
        }

        return state.copy(
            playerHp = playerHp,
            opponentHp = opponentHp,
            playerEnergy = if (outcome == BattleOutcome.IN_PROGRESS) min(MAX_ENERGY, playerEnergy + 1) else playerEnergy,
            opponentEnergy = if (outcome == BattleOutcome.IN_PROGRESS) min(MAX_ENERGY, opponentEnergy + 1) else opponentEnergy,
            playerGuarding = playerGuarding,
            opponentGuarding = opponentGuarding,
            opponentPotions = opponentPotions,
            items = items,
            turn = state.turn + 1,
            outcome = outcome,
            log = appendLog(state.log, *messages.toTypedArray()),
            completedMatches = matches,
        )
    }

    private fun chooseCpuAction(state: TurnBattleState, random: Random): CpuAction {
        if (state.opponentHp * 100 / state.opponentMaxHp <= 35 && state.opponentPotions > 0) return CpuAction.Heal
        val roll = random.nextInt(100)
        return when {
            roll < 18 -> CpuAction.Guard
            state.opponentEnergy >= 2 && roll < 70 -> CpuAction.Attack("コアブレイカー", 36, 2)
            else -> CpuAction.Attack("パルスアタック", 20, 0)
        }
    }

    private fun damage(power: Int, attack: Int, defense: Int, random: Random): Int =
        max(1, power + attack - defense / 2 + random.nextInt(-4, 5))

    private fun appendLog(current: List<String>, vararg messages: String): List<String> =
        (current + messages).takeLast(18)

    private sealed interface PlayerAction {
        data class Move(val move: BattleMove) : PlayerAction
        data class Item(val item: BattleItem) : PlayerAction
    }

    private sealed interface CpuAction {
        data object Heal : CpuAction
        data object Guard : CpuAction
        data class Attack(val name: String, val power: Int, val cost: Int, val priority: Int = 0) : CpuAction
    }
}
