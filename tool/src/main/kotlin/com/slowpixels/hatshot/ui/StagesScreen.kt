package com.slowpixels.hatshot.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.slowpixels.hatshot.Hub
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Dry Gulch's eight stages, as a Light list: the next one to win is underlined, as LightOS marks a choice, and each
 * stage won shows its stars. Every stage can be played, as on the Kompakt.
 */
class StagesScreen(sealed: SealedLightActivity) : HatScreen(sealed) {
    override val tag = "stages"

    @Composable
    override fun Body() {
        val world = Hub.world
        val save = Hub.save
        val next = world.stages.indexOfFirst { save.stars(world.id, it.key) == 0 }
        ListPage(world.name, back = { goBack() }) {
            world.stages.forEachIndexed { i, s ->
                val stars = save.stars(world.id, s.key)
                Row(
                    Modifier.fillMaxWidth().lightClickable { navigateTo({ GameScreen(it, i, story = true) }) }
                        .semantics(mergeDescendants = true) {
                            selected = i == next
                            contentDescription = "${s.title}, $stars of 3 stars"
                        }
                        .padding(vertical = 0.6f.gridUnitsAsDp()),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LightText(s.title.uppercase(), LightTextVariant.Copy, Modifier.weight(1f), underline = i == next,
                        lighten = stars == 0 && i != next, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (stars > 0) Stars(stars, 0.9f.gridUnitsAsDp())
                }
            }
        }
    }
}

/** Three stars in a row, [got] of them filled: Light's hollow shapes that fill when earned. */
@Composable
fun Stars(got: Int, each: Dp, gap: Dp = each / 3, color: Color = LightThemeTokens.colors.content) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        for (k in 0 until 3) {
            if (k > 0) Spacer(Modifier.width(gap))
            Canvas(Modifier.size(each)) { star(k < got, color) }
        }
    }
}

fun DrawScope.star(filled: Boolean, color: Color) {
    val r = size.minDimension / 2
    val cx = size.width / 2; val cy = size.height / 2 + r * 0.06f
    val p = Path()
    for (k in 0 until 10) {
        val a = -PI / 2 + k * PI / 5
        val rr = if (k % 2 == 0) r else r * 0.45f
        val x = cx + (rr * cos(a)).toFloat(); val y = cy + (rr * sin(a)).toFloat()
        if (k == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    if (filled) drawPath(p, color) else drawPath(p, color, style = Stroke(width = r * 0.14f))
}
