package app.vitalmorph.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.vitalmorph.R
import app.vitalmorph.domain.MonsterForm
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

enum class MonsterMotion {
    IDLE,
    ATTACK,
    VICTORY,
    HIT,
    // タッチ・会話・ミニゲーム用モーション(2026-07-12 Claude実装 / docs/COMPLETION_PLAN.md T1)。
    TOUCH_HAPPY,
    TOUCH_SHY,
    TOUCH_ANNOYED,
    TALK,
    SAD,
    MINIGAME_SUCCESS,
    // バトル演出強化用モーション(2026-07-12 Claude実装 / docs/RELEASE_1_1_PLAN.md U11)。
    // SPECIAL: 溜め(縮み)から前方へ強く踏み込む必殺・強打。GUARD_STANCE: わずかに沈んで構える防御。
    SPECIAL,
    GUARD_STANCE,
}

/**
 * キャラ本体の表情差分。現状はCodexによる表情画像が未制作のため、
 * 参照は必ずフォールバック(通常画像)へ解決される。画像が入り次第、
 * MonsterArtwork.expressionResources へ登録するだけで自動的に使われる。
 */
enum class MonsterExpression { NORMAL, HAPPY, SAD }

object MonsterArtwork {
    private val resources = mapOf(
        "morphy" to R.drawable.monster_morphy,
        "leafang" to R.drawable.monster_leafang,
        "solfeon" to R.drawable.monster_solfeon,
        "aquanel" to R.drawable.monster_aquanel,
        "astelion" to R.drawable.monster_astelion,
        "chronoleos" to R.drawable.monster_chronoleos,
        "miraflora" to R.drawable.monster_miraflora,
        "serenadia" to R.drawable.monster_serenadia,
        "galvol" to R.drawable.monster_galvol,
        "grandguard" to R.drawable.monster_grandguard,
        "fangrage" to R.drawable.monster_fangrage,
        "armagradian" to R.drawable.monster_armagradian,
        "bastion_rex" to R.drawable.monster_bastion_rex,
        "volcarion" to R.drawable.monster_volcarion,
        "ragna_fang" to R.drawable.monster_ragna_fang,
        "rapizel" to R.drawable.monster_rapizel,
        "velox" to R.drawable.monster_velox,
        "sky_rush" to R.drawable.monster_sky_rush,
        "zephyrion" to R.drawable.monster_zephyrion,
        "cross_gale" to R.drawable.monster_cross_gale,
        "volt_rave" to R.drawable.monster_volt_rave,
        "storm_arc" to R.drawable.monster_storm_arc,
        "motchigrow" to R.drawable.monster_motchigrow,
        "bulk_dome" to R.drawable.monster_bulk_dome,
        "ember_pot" to R.drawable.monster_ember_pot,
        "titan_bulwark" to R.drawable.monster_titan_bulwark,
        "gravi_dozer" to R.drawable.monster_gravi_dozer,
        "flare_glum" to R.drawable.monster_flare_glum,
        "magna_kiln" to R.drawable.monster_magna_kiln,
        "mossleep" to R.drawable.monster_mossleep,
        "moon_moss" to R.drawable.monster_moon_moss,
        "drift_mark" to R.drawable.monster_drift_mark,
        "luna_verde" to R.drawable.monster_luna_verde,
        "elder_moss" to R.drawable.monster_elder_moss,
        "nebra_shade" to R.drawable.monster_nebra_shade,
        "mist_wraith" to R.drawable.monster_mist_wraith,
        "runpact" to R.drawable.monster_runpact,
        "igni_dash" to R.drawable.monster_igni_dash,
        "crack_run" to R.drawable.monster_crack_run,
        "sol_blazer" to R.drawable.monster_sol_blazer,
        "prominence_gear" to R.drawable.monster_prominence_gear,
        "revenant_gear" to R.drawable.monster_revenant_gear,
        "phoenix_crest" to R.drawable.monster_phoenix_crest,
        "leon_saber_m" to R.drawable.monster_leon_saber_m,
        "twin_fang_m" to R.drawable.monster_twin_fang_m,
        "grand_breaker_m" to R.drawable.monster_grand_breaker_m,
        "volt_lancer_m" to R.drawable.monster_volt_lancer_m,
        "barrel_guard_m" to R.drawable.monster_barrel_guard_m,
        "rune_sage_m" to R.drawable.monster_rune_sage_m,
        "kagerou_m" to R.drawable.monster_kagerou_m,
        "sol_regnard_m" to R.drawable.monster_sol_regnard_m,
        "zero_dualion_m" to R.drawable.monster_zero_dualion_m,
        "titan_glaive_m" to R.drawable.monster_titan_glaive_m,
        "tempest_dragoon_m" to R.drawable.monster_tempest_dragoon_m,
        "arc_buster_m" to R.drawable.monster_arc_buster_m,
        "astra_magius_m" to R.drawable.monster_astra_magius_m,
        "mugen_shinobi_m" to R.drawable.monster_mugen_shinobi_m,
        "valeria_f" to R.drawable.monster_valeria_f,
        "lila_twin_f" to R.drawable.monster_lila_twin_f,
        "crim_arge_f" to R.drawable.monster_crim_arge_f,
        "celes_lancer_f" to R.drawable.monster_celes_lancer_f,
        "rouge_shell_f" to R.drawable.monster_rouge_shell_f,
        "mystica_f" to R.drawable.monster_mystica_f,
        "yoidzuki_f" to R.drawable.monster_yoidzuki_f,
        "val_rose_f" to R.drawable.monster_val_rose_f,
        "lumina_duella_f" to R.drawable.monster_lumina_duella_f,
        "grand_empress_f" to R.drawable.monster_grand_empress_f,
        "astra_reina_f" to R.drawable.monster_astra_reina_f,
        "nova_valeria_f" to R.drawable.monster_nova_valeria_f,
        "eclipsia_f" to R.drawable.monster_eclipsia_f,
        "tsukikage_hime_f" to R.drawable.monster_tsukikage_hime_f,
    )

    private val cpuFinals = listOf(
        R.drawable.monster_armagradian,
        R.drawable.monster_zephyrion,
        R.drawable.monster_titan_bulwark,
        R.drawable.monster_nebra_shade,
        R.drawable.monster_prominence_gear,
        R.drawable.monster_serenadia,
    )

    /**
     * 表情差分の画像リソース。キーは "<formId>_happy" / "<formId>_sad"。
     * 現状は画像が未制作のため空。Codexへ: 画像追加時は
     * drawable-nodpi/monster_<formId>_<expression>.webp を置き、このマップへ登録する。
     */
    private val expressionResources: Map<String, Int> = emptyMap()

    fun resourceFor(formId: String): Int = resources[formId] ?: R.drawable.monster_morphy

    /**
     * 表情差分を考慮した画像リソースを返す。該当する表情画像が無い場合(現状は常に)、
     * 通常画像へフォールバックする。expression==NORMAL も通常画像。
     */
    fun resourceFor(formId: String, expression: MonsterExpression): Int {
        if (expression == MonsterExpression.NORMAL) return resourceFor(formId)
        val suffix = when (expression) {
            MonsterExpression.HAPPY -> "happy"
            MonsterExpression.SAD -> "sad"
            MonsterExpression.NORMAL -> return resourceFor(formId)
        }
        return expressionResources["${formId}_$suffix"] ?: resourceFor(formId)
    }

    fun resourceForOpponent(name: String): Int = cpuFinals[Math.floorMod(name.hashCode(), cpuFinals.size)]
}

@Composable
fun MonsterVisual(
    form: MonsterForm,
    modifier: Modifier = Modifier,
    motion: MonsterMotion = MonsterMotion.IDLE,
    facingRight: Boolean = true,
    showAura: Boolean = true,
    expression: MonsterExpression = MonsterExpression.NORMAL,
) {
    MonsterSprite(
        drawableRes = MonsterArtwork.resourceFor(form.id, expression),
        contentDescription = form.name,
        accent = Color(form.accent),
        modifier = modifier,
        motion = motion,
        facingRight = facingRight,
        showAura = showAura,
    )
}

@Composable
fun MonsterSprite(
    drawableRes: Int,
    contentDescription: String,
    accent: Color,
    modifier: Modifier = Modifier,
    motion: MonsterMotion = MonsterMotion.IDLE,
    facingRight: Boolean = true,
    showAura: Boolean = true,
) {
    val transition = rememberInfiniteTransition(label = "monster-$contentDescription-$motion")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathing",
    )
    val action by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2_400
                0f at 0
                0f at 550
                1f at 760
                0f at 1_060
                0f at 2_400
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "action",
    )
    val density = LocalDensity.current.density
    val baseBob = (phase - 0.5f) * 10f * density
    // 2回跳ねる動き用に、actionの山を絶対値sinで2山へ変換する(TOUCH_HAPPY)。
    val doubleHop = abs(sin(action.toDouble() * PI * 2.0)).toFloat()
    // 首を振る速い左右揺れ用に、phaseから高周波の振動を作る(TOUCH_ANNOYED / TALK)。
    val quickShake = sin(phase.toDouble() * PI * 4.0).toFloat()
    val scale = when (motion) {
        MonsterMotion.IDLE -> 0.985f + phase * 0.03f
        MonsterMotion.ATTACK -> 1f + action * 0.06f
        MonsterMotion.VICTORY -> 1f + action * 0.09f
        MonsterMotion.HIT -> 1f - action * 0.05f
        MonsterMotion.TOUCH_HAPPY -> 1.01f + action * 0.04f
        MonsterMotion.TOUCH_SHY -> 0.94f + phase * 0.01f
        MonsterMotion.TOUCH_ANNOYED -> 1f
        MonsterMotion.TALK -> 0.99f + phase * 0.02f
        MonsterMotion.SAD -> 0.97f
        MonsterMotion.MINIGAME_SUCCESS -> 1f + action * 0.10f
        // 溜め局面(action前半)で少し縮み、踏み込みで通常サイズへ戻す。
        MonsterMotion.SPECIAL -> 0.92f + action * 0.14f
        MonsterMotion.GUARD_STANCE -> 0.97f - phase * 0.01f
    }
    val translationX = when (motion) {
        MonsterMotion.ATTACK -> action * 34f * density
        MonsterMotion.HIT -> -action * 14f * density
        MonsterMotion.TOUCH_ANNOYED -> -4f * density - action * 6f * density
        // 通常攻撃より強い前方への踏み込み。
        MonsterMotion.SPECIAL -> action * 52f * density
        else -> 0f
    }
    val translationY = when (motion) {
        MonsterMotion.VICTORY -> baseBob - action * 22f * density
        MonsterMotion.HIT -> baseBob + action * 4f * density
        MonsterMotion.TOUCH_HAPPY -> baseBob - doubleHop * 12f * density
        MonsterMotion.TALK -> quickShake * 4f * density
        MonsterMotion.SAD -> baseBob * 0.4f + 8f * density
        MonsterMotion.MINIGAME_SUCCESS -> baseBob - action * 40f * density
        // わずかに沈んで構える。揺れは最小限。
        MonsterMotion.GUARD_STANCE -> baseBob * 0.3f + 4f * density
        else -> baseBob
    }
    val rotation = when (motion) {
        MonsterMotion.IDLE -> (phase - 0.5f) * 2f
        MonsterMotion.ATTACK -> action * 5f
        MonsterMotion.VICTORY -> (phase - 0.5f) * 5f
        MonsterMotion.HIT -> -action * 7f
        MonsterMotion.TOUCH_HAPPY -> (phase - 0.5f) * 2f
        MonsterMotion.TOUCH_SHY -> (phase - 0.5f) * 6f
        MonsterMotion.TOUCH_ANNOYED -> quickShake * 10f
        MonsterMotion.TALK -> 3.5f + (phase - 0.5f) * 2f
        MonsterMotion.SAD -> -4f + (phase - 0.5f) * 1f
        MonsterMotion.MINIGAME_SUCCESS -> action * 25f
        MonsterMotion.SPECIAL -> action * 12f
        MonsterMotion.GUARD_STANCE -> (phase - 0.5f) * 1f
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (showAura) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    color = accent.copy(alpha = 0.10f + phase * 0.08f),
                    radius = size.minDimension * (0.40f + phase * 0.04f),
                    center = center,
                )
                drawCircle(
                    color = accent.copy(alpha = 0.34f - phase * 0.12f),
                    radius = size.minDimension * (0.36f + phase * 0.07f),
                    center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
                )
            }
        }
        Image(
            painter = painterResource(drawableRes),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
                .alpha(
                    when (motion) {
                        MonsterMotion.HIT -> 0.78f + phase * 0.22f
                        MonsterMotion.SAD -> 0.88f
                        else -> 1f
                    },
                )
                .graphicsLayer {
                    this.translationX = if (facingRight) translationX else -translationX
                    this.translationY = translationY
                    rotationZ = if (facingRight) rotation else -rotation
                    scaleX = if (facingRight) scale else -scale
                    scaleY = scale
                },
        )
    }
}
