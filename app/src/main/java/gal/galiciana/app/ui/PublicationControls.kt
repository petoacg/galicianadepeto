package gal.galiciana.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicationYearSelector(years: List<String>, selected: String?, enabled: Boolean = true, onSelect: (String) -> Unit) {
    var choosing by rememberSaveable { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf("") }
    TextButton(onClick = { filter = ""; choosing = true }, enabled = enabled && years.isNotEmpty(),
        modifier = Modifier.heightIn(min = 48.dp)) {
        Icon(Icons.Outlined.CalendarMonth, null)
        Spacer(Modifier.width(8.dp))
        Text("Año ${selected ?: "…"} · Cambiar")
    }
    if (choosing) ModalBottomSheet(onDismissRequest = { choosing = false },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Text("Elegir año", style = MaterialTheme.typography.headlineSmall)
            ClearableField(filter, { filter = it }, "Escribe un año", modifier = Modifier.padding(vertical = 16.dp),
                imeSearch = false, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            LazyColumn(Modifier.heightIn(max = 440.dp)) {
                items(years.filter { it.contains(filter.trim()) }) { year ->
                    ListItem(headlineContent = { Text(year) },
                        trailingContent = { if (selected == year) Text("Seleccionado", color = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable { choosing = false; onSelect(year) })
                }
            }
        }
    }
}
