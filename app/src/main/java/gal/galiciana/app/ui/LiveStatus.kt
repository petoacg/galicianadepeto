package gal.galiciana.app.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics

/** Status text that appears without user action; TalkBack reads it when it changes. */
fun Modifier.liveStatus(): Modifier = semantics { liveRegion = LiveRegionMode.Polite }
