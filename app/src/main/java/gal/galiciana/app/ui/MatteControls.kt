package gal.galiciana.app.ui

import androidx.compose.material3.MaterialTheme

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import gal.galiciana.app.ui.theme.MatteFill

val MatteShape = RoundedCornerShape(12.dp)
val ScreenGutter = 20.dp
val FieldGap = 12.dp
val CardInk: Color
    @Composable get() = MaterialTheme.colorScheme.onSurface
val CardMute: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
val CardRule = Color(0xFFD7CDB8)
val CardPaper = Color(0xFFF7F1E6)

@Composable
fun matteFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    disabledContainerColor = Color(0xFFD4D8DC),
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    disabledLabelColor = Color(0xFF8B9298),
    disabledTextColor = Color(0xFF8B9298),
    disabledPlaceholderColor = Color(0xFF9AA1A7),
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
)

@Composable
fun ClearableField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    label: String? = null,
    imeSearch: Boolean = true,
    onSearch: (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        label = label?.let { { Text(it) } },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        enabled = enabled,
        shape = MatteShape,
        colors = matteFieldColors(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType,
            imeAction = if (imeSearch) ImeAction.Search else ImeAction.Done),
        keyboardActions = KeyboardActions(
            onSearch = { onSearch?.invoke(); keyboard?.hide() },
            onDone = { keyboard?.hide() },
        ),
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }, enabled = enabled) {
                    Icon(Icons.Filled.Close, contentDescription = "Borrar ${label ?: placeholder}")
                }
            }
        },
    )
}
