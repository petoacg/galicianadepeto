package gal.galiciana.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics

/** Sort option that shows and announces whether it is the order in use. */
@Composable
fun SortMenuItem(label: String, active: Boolean, detail: String? = null, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(if (active && detail != null) "$label · $detail" else label) },
        trailingIcon = if (active) {
            { Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary) }
        } else null,
        modifier = Modifier.semantics { selected = active },
        onClick = onClick,
    )
}
