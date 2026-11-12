package com.slowpixels.hatshot.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.slowpixels.hatshot.game.Frame

/** A [Frame] and the bitmap it's shown through. [show] copies the frame in and asks for it to be drawn. */
class Screenful(val frame: Frame) {
    private val bitmap: Bitmap = Bitmap.createBitmap(frame.w, frame.h, Bitmap.Config.ARGB_8888)
    val image = bitmap.asImageBitmap()
    val version = mutableIntStateOf(0)

    fun show() {
        bitmap.setPixels(frame.px, 0, frame.w, 0, 0, frame.w, frame.h)
        version.intValue++
    }

    /** Screen pixels to a game pixel, for a canvas [width] across: a whole number, so every pixel is the same size. */
    fun scale(width: Float) = (width / frame.w).toInt().coerceAtLeast(1)
}

/**
 * Draws a [Screenful] blown up with no smoothing, so every game pixel lands as a crisp square, and reports where a
 * finger goes down in game pixels. Every new finger counts, so two quick taps with two fingers are two shots.
 */
@Composable
fun Pixels(s: Screenful, modifier: Modifier = Modifier, onDown: ((Int, Int) -> Unit)? = null) {
    val down = rememberUpdatedState(onDown)
    val touch = if (onDown == null) Modifier else Modifier.pointerInput(s) {
        awaitPointerEventScope {
            while (true) {
                val e = awaitPointerEvent()
                for (c in e.changes) if (c.changedToDown()) {
                    val scale = s.scale(size.width.toFloat())
                    val ox = (size.width - s.frame.w * scale) / 2
                    down.value?.invoke(((c.position.x - ox) / scale).toInt(), (c.position.y / scale).toInt())
                    c.consume()
                }
            }
        }
    }
    Canvas(modifier.fillMaxSize().then(touch)) {
        s.version.intValue
        val scale = s.scale(size.width)
        val ox = ((size.width - s.frame.w * scale) / 2).toInt()
        drawImage(
            s.image,
            dstOffset = IntOffset(ox, 0),
            dstSize = IntSize(s.frame.w * scale, s.frame.h * scale),
            filterQuality = FilterQuality.None,
        )
    }
}
