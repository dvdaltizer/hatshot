package com.slowpixels.hatshot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.slowpixels.hatshot.Hub
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightBottomBar
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable

/**
 * A screen of the tool. Every screen sets the game up as it shows (whichever comes first), and the kit draws every
 * screen from the same spot, so what's remembered is keyed on the screen itself.
 */
abstract class HatScreen(protected val sealed: SealedLightActivity) : SimpleLightScreen<Unit>(sealed) {
    /** Names this screen for [Hub.closeUntil]. */
    open val tag = ""
    private val shown = mutableIntStateOf(0)

    override fun willShow() {
        Hub.start(lightContext.filesDir, sealed.vibrator)
        Hub.sound?.pause(false)
        shown.intValue++
    }

    /** The tool has gone to the background: not a sound until it's back. */
    override fun onAppPause() {
        Hub.sound?.pause(true)
    }

    @Composable
    override fun Content() {
        val n = shown.intValue
        LaunchedEffect(this, n) {
            val until = Hub.closeUntil
            if (until != null) { if (until == tag) Hub.closeUntil = null else goBack() }
        }
        key(this) { Body() }
    }

    @Composable
    abstract fun Body()
}

/** LightOS's theme on a full screen. */
@Composable
fun Themed(content: @Composable ColumnScope.() -> Unit) {
    val colors by LightThemeController.colors.collectAsState()
    LightTheme(colors = colors) {
        Column(Modifier.fillMaxSize().background(LightThemeTokens.colors.background), content = content)
    }
}

/** A sub-screen's top: a back arrow and title. */
@Composable
fun BackBar(title: String, back: () -> Unit, right: LightBarButton? = null) = LightTopBar(
    leftButton = LightBarButton.LightIcon(LightIcons.BACK, onClick = back),
    center = LightTopBarCenter.Text(title),
    rightButton = right,
    modifier = Modifier.padding(bottom = 0.5f.gridUnitsAsDp()),
)

/** A sub-screen: a back arrow and title over a scrolling list, and a bar along the foot if [bottom] is given. */
@Composable
fun ListPage(
    title: String,
    back: () -> Unit,
    bottom: List<LightBarButton?>? = null,
    rows: @Composable ColumnScope.() -> Unit,
) = Themed {
    BackBar(title, back)
    LightScrollView(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 1f.gridUnitsAsDp()), content = rows)
    if (bottom != null) LightBottomBar(bottom)
}

/** One choice in a list; the one in use is underlined, as LightOS's own settings lists do. */
@Composable
fun Choice(label: String, selected: Boolean = false, detail: String? = null, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().lightClickable(onClick = onClick).semantics(mergeDescendants = true) { this.selected = selected }
            .padding(vertical = 0.75f.gridUnitsAsDp()),
    ) {
        LightText(label.uppercase(), LightTextVariant.Copy, underline = selected, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (detail != null) LightText(detail, LightTextVariant.Fine, lighten = true, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** A row: what it is on the left and, if given, what it's set to on the right. Tappable when [onClick] is given. */
@Composable
fun Item(label: String, value: String? = null, lighten: Boolean = false, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.lightClickable(onClick = onClick) else Modifier)
            .semantics(mergeDescendants = true) {}.padding(vertical = 0.55f.gridUnitsAsDp()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LightText(label.uppercase(), LightTextVariant.Copy, Modifier.padding(end = 1f.gridUnitsAsDp()), lighten = lighten,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (value != null) LightText(value, LightTextVariant.Copy, Modifier.weight(1f), align = TextAlign.End, lighten = true,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** A paragraph of plain words. */
@Composable
fun Note(text: String, lighten: Boolean = false) =
    LightText(text, LightTextVariant.Paragraph, Modifier.padding(vertical = 0.5f.gridUnitsAsDp()), lighten = lighten)

fun onOff(on: Boolean) = if (on) "On" else "Off"
