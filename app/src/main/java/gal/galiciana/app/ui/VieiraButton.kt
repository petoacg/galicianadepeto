package gal.galiciana.app.ui

import androidx.compose.ui.semantics.clearAndSetSemantics
import gal.galiciana.app.i18n.tr
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

val VieiraCeleste = Color(0xFF7EC8E3)

@Composable
fun VieiraButton(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    forcedCeleste: Boolean = false,
    contentLabel: String? = null,
    interactive: Boolean = true,
) {
    val action = contentLabel ?: if (forcedCeleste) tr("Abrir Gardados", "Abrir Guardados") else tr("Gardar páxina", "Guardar página")
    val interaction = if (!interactive) Modifier else if (forcedCeleste) Modifier.clickable(role = Role.Button, onClick = onClick)
        else Modifier.toggleable(value = selected, role = Role.Checkbox, onValueChange = { onClick() })
    val ink = if (selected || forcedCeleste) Color(0xFF12384F) else androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
    // On its own screen the brand mark is decoration: nothing to press, nothing to announce.
    Box(modifier.size(maxOf(48.dp, size)).then(interaction).then(if (!interactive) Modifier.clearAndSetSemantics { } else Modifier).semantics {
        contentDescription = action
        if (!forcedCeleste) stateDescription = if (selected) tr("Gardada", "Guardada") else tr("Sen gardar", "Sin guardar")
    }, contentAlignment = Alignment.Center) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h * 0.46f
        val r = minOf(w, h) * 0.42f
        val lobes = 7
        val path = Path()
        for (i in 0..lobes) {
            val a0 = Math.PI + i * Math.PI / lobes
            val x = cx + r * cos(a0).toFloat()
            val y = cy + r * 0.92f * sin(a0).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.lineTo(w * 0.72f, h * 0.78f)
        path.quadraticTo(cx, h * 0.98f, w * 0.28f, h * 0.78f)
        path.close()
        val fill = if (forcedCeleste || selected) VieiraCeleste else Color.Transparent
        if (fill != Color.Transparent) drawPath(path, fill, style = Fill)
        drawPath(
            path,
            ink,
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        val base = Offset(cx, h * 0.78f)
        for (i in -3..3) {
            val ang = Math.PI + (i + 3) * Math.PI / 6.2
            val tip = Offset(
                cx + r * 0.78f * cos(ang).toFloat(),
                cy + r * 0.72f * sin(ang).toFloat(),
            )
            drawLine(ink, base, tip, strokeWidth = 1.dp.toPx(), cap = StrokeCap.Round)
        }
    }
}
}
