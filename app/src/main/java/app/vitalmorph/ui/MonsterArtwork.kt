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

enum class MonsterMotion {
    IDLE,
    ATTACK,
    VICTORY,
    HIT,
}

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
    )

    private val cpuFinals = listOf(
        R.drawable.monster_armagradian,
        R.drawable.monster_zephyrion,
        R.drawable.monster_titan_bulwark,
        R.drawable.monster_nebra_shade,
        R.drawable.monster_prominence_gear,
        R.drawable.monster_serenadia,
    )

    fun resourceFor(formId: String): Int = resources[formId] ?: R.drawable.monster_morphy

    fun resourceForOpponent(name: String): Int = cpuFinals[Math.floorMod(name.hashCode(), cpuFinals.size)]
}

@Composable
fun MonsterVisual(
    form: MonsterForm,
    modifier: Modifier = Modifier,
    motion: MonsterMotion = MonsterMotion.IDLE,
    facingRight: Boolean = true,
    showAura: Boolean = true,
) {
    MonsterSprite(
        drawableRes = MonsterArtwork.resourceFor(form.id),
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
    val scale = when (motion) {
        MonsterMotion.IDLE -> 0.985f + phase * 0.03f
        MonsterMotion.ATTACK -> 1f + action * 0.06f
        MonsterMotion.VICTORY -> 1f + action * 0.09f
        MonsterMotion.HIT -> 1f - action * 0.05f
    }
    val translationX = when (motion) {
        MonsterMotion.ATTACK -> action * 34f * density
        MonsterMotion.HIT -> -action * 14f * density
        else -> 0f
    }
    val translationY = when (motion) {
        MonsterMotion.VICTORY -> baseBob - action * 22f * density
        MonsterMotion.HIT -> baseBob + action * 4f * density
        else -> baseBob
    }
    val rotation = when (motion) {
        MonsterMotion.IDLE -> (phase - 0.5f) * 2f
        MonsterMotion.ATTACK -> action * 5f
        MonsterMotion.VICTORY -> (phase - 0.5f) * 5f
        MonsterMotion.HIT -> -action * 7f
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
                .alpha(if (motion == MonsterMotion.HIT) 0.78f + phase * 0.22f else 1f)
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
