package app.vitalmorph.domain

import org.json.JSONArray
import org.json.JSONObject

/**
 * 進行中の大会状態(`TurnBattleState`)をJSON文字列へ相互変換する純ロジック。
 *
 * アプリ終了後も試合途中から再開できるよう、`GameStore` がSharedPreferencesへ保存する。
 * 全フィールドを保存し、往復でデータクラスの等価性を保つ(スキーマ省略はしない)。
 * 破損データや将来のフォーマット変更でアプリを落とさないよう、`fromJson` は失敗時にnullを返す。
 */
object BattleStateCodec {
    /** 保存フォーマットのバージョン。互換性を壊す変更のたびに上げ、旧データは安全に破棄する。 */
    const val VERSION = 2

    fun toJson(state: TurnBattleState): String {
        val json = JSONObject()
        json.put("version", VERSION)
        json.put("roundIndex", state.roundIndex)
        json.put("roundLabel", state.roundLabel)
        json.put("playerName", state.playerName)
        json.put("opponentName", state.opponentName)
        json.put("opponentFormId", state.opponentFormId)
        json.put("week", state.week)
        json.put("practice", state.practice)
        json.put("playerHp", state.playerHp)
        json.put("playerMaxHp", state.playerMaxHp)
        json.put("opponentHp", state.opponentHp)
        json.put("opponentMaxHp", state.opponentMaxHp)
        json.put("playerEnergy", state.playerEnergy)
        json.put("opponentEnergy", state.opponentEnergy)
        json.put("playerAttack", state.playerAttack)
        json.put("playerDefense", state.playerDefense)
        json.put("playerSpeed", state.playerSpeed)
        json.put("opponentAttack", state.opponentAttack)
        json.put("opponentDefense", state.opponentDefense)
        json.put("opponentSpeed", state.opponentSpeed)
        json.put("playerGuarding", state.playerGuarding)
        json.put("opponentGuarding", state.opponentGuarding)
        json.put("opponentPotions", state.opponentPotions)
        json.put("moves", movesToJson(state.moves))
        json.put("items", itemsToJson(state.items))
        json.put("turn", state.turn)
        json.put("seed", state.seed)
        json.put("outcome", state.outcome.name)
        json.put("log", JSONArray(state.log))
        json.put("completedMatches", matchesToJson(state.completedMatches))
        json.put("playerStartEnergy", state.playerStartEnergy)
        json.put("playerStartShield", state.playerStartShield)
        return json.toString()
    }

    fun fromJson(json: String): TurnBattleState? = runCatching {
        val root = JSONObject(json)
        if (root.getInt("version") != VERSION) return null
        TurnBattleState(
            roundIndex = root.getInt("roundIndex"),
            roundLabel = root.getString("roundLabel"),
            playerName = root.getString("playerName"),
            opponentName = root.getString("opponentName"),
            opponentFormId = root.getString("opponentFormId"),
            week = root.getInt("week"),
            practice = root.getBoolean("practice"),
            playerHp = root.getInt("playerHp"),
            playerMaxHp = root.getInt("playerMaxHp"),
            opponentHp = root.getInt("opponentHp"),
            opponentMaxHp = root.getInt("opponentMaxHp"),
            playerEnergy = root.getInt("playerEnergy"),
            opponentEnergy = root.getInt("opponentEnergy"),
            playerAttack = root.getInt("playerAttack"),
            playerDefense = root.getInt("playerDefense"),
            playerSpeed = root.getInt("playerSpeed"),
            opponentAttack = root.getInt("opponentAttack"),
            opponentDefense = root.getInt("opponentDefense"),
            opponentSpeed = root.getInt("opponentSpeed"),
            playerGuarding = root.getBoolean("playerGuarding"),
            opponentGuarding = root.getBoolean("opponentGuarding"),
            opponentPotions = root.getInt("opponentPotions"),
            moves = movesFromJson(root.getJSONArray("moves")),
            items = itemsFromJson(root.getJSONArray("items")),
            turn = root.getInt("turn"),
            seed = root.getInt("seed"),
            outcome = BattleOutcome.valueOf(root.getString("outcome")),
            log = stringsFromJson(root.getJSONArray("log")),
            completedMatches = matchesFromJson(root.getJSONArray("completedMatches")),
            playerStartEnergy = root.getInt("playerStartEnergy"),
            playerStartShield = root.getBoolean("playerStartShield"),
        )
    }.getOrNull()

    private fun movesToJson(moves: List<BattleMove>): JSONArray {
        val array = JSONArray()
        moves.forEach { move ->
            array.put(
                JSONObject()
                    .put("id", move.id)
                    .put("name", move.name)
                    .put("description", move.description)
                    .put("kind", move.kind.name)
                    .put("power", move.power)
                    .put("energyCost", move.energyCost)
                    .put("priority", move.priority)
                    .put("heal", move.heal)
                    .put("recoil", move.recoil),
            )
        }
        return array
    }

    private fun movesFromJson(array: JSONArray): List<BattleMove> = List(array.length()) { index ->
        val obj = array.getJSONObject(index)
        BattleMove(
            id = obj.getString("id"),
            name = obj.getString("name"),
            description = obj.getString("description"),
            kind = BattleMoveKind.valueOf(obj.getString("kind")),
            power = obj.getInt("power"),
            energyCost = obj.getInt("energyCost"),
            priority = obj.getInt("priority"),
            heal = obj.getInt("heal"),
            recoil = obj.getInt("recoil"),
        )
    }

    private fun itemsToJson(items: List<BattleItemStock>): JSONArray {
        val array = JSONArray()
        items.forEach { stock ->
            array.put(
                JSONObject()
                    .put("id", stock.item.id)
                    .put("name", stock.item.name)
                    .put("description", stock.item.description)
                    .put("remaining", stock.remaining),
            )
        }
        return array
    }

    private fun itemsFromJson(array: JSONArray): List<BattleItemStock> = List(array.length()) { index ->
        val obj = array.getJSONObject(index)
        BattleItemStock(
            item = BattleItem(
                id = obj.getString("id"),
                name = obj.getString("name"),
                description = obj.getString("description"),
            ),
            remaining = obj.getInt("remaining"),
        )
    }

    private fun matchesToJson(matches: List<BattleMatch>): JSONArray {
        val array = JSONArray()
        matches.forEach { match ->
            array.put(
                JSONObject()
                    .put("round", match.round)
                    .put("opponent", match.opponent)
                    .put("playerScore", match.playerScore)
                    .put("opponentScore", match.opponentScore)
                    .put("won", match.won),
            )
        }
        return array
    }

    private fun matchesFromJson(array: JSONArray): List<BattleMatch> = List(array.length()) { index ->
        val obj = array.getJSONObject(index)
        BattleMatch(
            round = obj.getString("round"),
            opponent = obj.getString("opponent"),
            playerScore = obj.getInt("playerScore"),
            opponentScore = obj.getInt("opponentScore"),
            won = obj.getBoolean("won"),
        )
    }

    private fun stringsFromJson(array: JSONArray): List<String> =
        List(array.length()) { index -> array.getString(index) }
}
