package com.lsing.timego.ui.log

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.lsing.timego.ui.theme.Hairline
import com.lsing.timego.ui.theme.LedgerFigureValue
import com.lsing.timego.ui.theme.PlateBlue
import com.lsing.timego.ui.theme.PlateGreen
import com.lsing.timego.ui.theme.PlateYellow

/** Which number the stepper strip is adjusting. */
enum class EntryField { WEIGHT, REPS }

/** One number inside the set tile: a large figure you can tap to type, with its unit underneath. The selected one gets the action outline. */
@Composable
private fun NumberCell(
    unit: String,
    text: String,
    onTextChange: (String) -> Unit,
    placeholder: String?,
    placeholderIsTarget: Boolean,
    selected: Boolean,
    onFocused: () -> Unit,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    focusRequester: FocusRequester,
    isError: Boolean,
    filterInput: (String) -> String,
    modifier: Modifier = Modifier,
) {
    // An empty cell is an empty slot: a solid grey outline. Selected is the action colour, an error is red.
    val outline = when {
        isError -> MaterialTheme.colorScheme.error
        selected -> MaterialTheme.colorScheme.primary
        text.isEmpty() -> MaterialTheme.colorScheme.outline
        else -> Color.Transparent
    }
    // The unit sits under the box, not inside it: the box is only the number you are entering.
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(if (selected || isError) 2.dp else 1.5.dp, outline, RoundedCornerShape(12.dp))
                .padding(horizontal = 4.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            BasicTextField(
                value = text,
                onValueChange = { onTextChange(filterInput(it)) },
                singleLine = true,
                textStyle = LedgerFigureValue.copy(
                    fontSize = 48.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { if (it.isFocused) onFocused() }
                    .semantics { contentDescription = unit },
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.Center) {
                        if (text.isEmpty() && placeholder != null) {
                            Text(
                                placeholder,
                                style = LedgerFigureValue.copy(
                                    fontSize = 48.sp,
                                    textAlign = TextAlign.Center,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Light,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                        inner()
                    }
                },
            )
        }
        Text(unit.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Weight and reps as one object: "62.5 x 8" in a single raised tile with a one-line caption (change since last time, the target).
 * Tapping a number selects it for the [StepperStrip] below and opens the keyboard on it.
 */
@Composable
fun SetEntryTile(
    weightUnit: String,
    weightText: String,
    onWeightChange: (String) -> Unit,
    weightPlaceholder: String?,
    weightError: Boolean,
    repsText: String,
    onRepsChange: (String) -> Unit,
    repsPlaceholder: String?,
    placeholderIsTarget: Boolean,
    repsError: Boolean,
    selected: EntryField,
    onSelect: (EntryField) -> Unit,
    caption: String?,
    captionColor: Color,
    weightKeyboardActions: KeyboardActions,
    repsKeyboardActions: KeyboardActions,
    weightFocus: FocusRequester,
    repsFocus: FocusRequester,
    modifier: Modifier = Modifier,
    secondUnit: String = "reps",
    secondDecimal: Boolean = false,
    separator: String = "x",
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .border(1.dp, Hairline, RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            NumberCell(
                unit = weightUnit,
                text = weightText,
                onTextChange = onWeightChange,
                placeholder = weightPlaceholder,
                placeholderIsTarget = placeholderIsTarget,
                selected = selected == EntryField.WEIGHT,
                onFocused = { onSelect(EntryField.WEIGHT) },
                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                keyboardActions = weightKeyboardActions,
                focusRequester = weightFocus,
                isError = weightError,
                filterInput = ::filterDecimal,
                modifier = Modifier.weight(1f),
            )
            Text(separator, style = LedgerFigureValue.copy(fontSize = 20.sp), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 6.dp))
            NumberCell(
                unit = secondUnit,
                text = repsText,
                onTextChange = onRepsChange,
                placeholder = repsPlaceholder,
                placeholderIsTarget = placeholderIsTarget,
                selected = selected == EntryField.REPS,
                onFocused = { onSelect(EntryField.REPS) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (secondDecimal) androidx.compose.ui.text.input.KeyboardType.Decimal else androidx.compose.ui.text.input.KeyboardType.Number,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                ),
                keyboardActions = repsKeyboardActions,
                focusRequester = repsFocus,
                isError = repsError,
                filterInput = if (secondDecimal) ::filterDecimal else { raw -> filterWholeNumber(raw) },
                modifier = Modifier.weight(1f),
            )
        }
        Box(modifier = Modifier.height(20.dp), contentAlignment = Alignment.Center) {
            if (caption != null) Text(caption, style = MaterialTheme.typography.labelMedium, color = captionColor)
        }
    }
}

/** One number in a raised tile with its unit under the box: hold seconds, or cardio km while a timer runs. */
@Composable
fun SingleEntryTile(
    unit: String,
    text: String,
    onTextChange: (String) -> Unit,
    placeholder: String?,
    placeholderIsTarget: Boolean,
    caption: String?,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    focusRequester: FocusRequester,
    isError: Boolean,
    filterInput: (String) -> String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .border(1.dp, Hairline, RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NumberCell(
            unit = unit,
            text = text,
            onTextChange = onTextChange,
            placeholder = placeholder,
            placeholderIsTarget = placeholderIsTarget,
            selected = true,
            onFocused = {},
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            focusRequester = focusRequester,
            isError = isError,
            filterInput = filterInput,
            modifier = Modifier.fillMaxWidth(0.6f),
        )
        Box(modifier = Modifier.height(20.dp), contentAlignment = Alignment.Center) {
            if (caption != null) Text(caption, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** One wide −/+ strip for whichever number is selected; the middle shows what it is adjusting and the step. */
@Composable
fun StepperStrip(
    adjusting: String,
    step: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    minusDescription: String,
    plusDescription: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, Hairline, RoundedCornerShape(12.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onMinus, modifier = Modifier.weight(1f).height(56.dp)) {
            Icon(Icons.Filled.Remove, contentDescription = minusDescription)
        }
        Box(modifier = Modifier.width(1.dp).height(56.dp).background(Hairline))
        Column(modifier = Modifier.weight(1.4f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(adjusting, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(step, style = LedgerFigureValue.copy(fontSize = 15.sp), color = MaterialTheme.colorScheme.onSurface)
        }
        Box(modifier = Modifier.width(1.dp).height(56.dp).background(Hairline))
        IconButton(onClick = onPlus, modifier = Modifier.weight(1f).height(56.dp)) {
            Icon(Icons.Filled.Add, contentDescription = plusDescription)
        }
    }
}

/** Keeps only digits, at most [maxDigits] of them: reps are whole numbers. */
fun filterWholeNumber(raw: String, maxDigits: Int = 3): String = raw.filter { it.isDigit() }.take(maxDigits)

/** Keeps a plain decimal: digits and one point (a comma becomes the point), at most two decimals and five characters. */
fun filterDecimal(raw: String): String {
    val cleaned = raw.replace(',', '.').filter { it.isDigit() || it == '.' }
    val point = cleaned.indexOf('.')
    val single = if (point < 0) cleaned else cleaned.substring(0, point + 1) + cleaned.substring(point + 1).replace(".", "")
    val limited = if (point < 0) single else single.substring(0, minOf(single.length, point + 3))
    return limited.take(6)
}

/** One line of dots for the sets of this exercise: done (green tick), the one you are on (ring), and the rest of the plan (dashed). */
@Composable
fun SetTimeline(done: Int, planned: Int?, modifier: Modifier = Modifier) {
    val total = maxOf(planned ?: (done + 1), done + 1)
    val label = if (planned != null) "Set ${done + 1} of $total" else "Set ${done + 1}"
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            repeat(total) { index ->
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .width(12.dp)
                            .height(2.dp)
                            .background(if (index <= done) PlateGreen else Hairline),
                    )
                }
                val dotModifier = Modifier.size(24.dp)
                when {
                    index < done -> Box(
                        modifier = dotModifier.clip(CircleShape).background(PlateGreen),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(15.dp)) }
                    index == done -> Box(
                        modifier = dotModifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface)) }
                    else -> Box(modifier = dotModifier.border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape))
                }
            }
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** RPE 5 to 10 as one snapping slider. Optional: until you touch it the thumb is muted and nothing is saved. */
@Composable
fun RpeSlider(value: Int?, defaultValue: Int, onValueChange: (Int) -> Unit, onClear: () -> Unit, modifier: Modifier = Modifier) {
    val shown = value ?: defaultValue
    val left = 10 - shown
    val active = if (value != null) PlateYellow else MaterialTheme.colorScheme.outline
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("RPE (optional)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (value == null) {
                Text("drag to rate", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(
                    if (left <= 0) "$value, nothing left" else "$value, about $left rep${if (left == 1) "" else "s"} left",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.clickable(onClickLabel = "Clear RPE", onClick = onClear),
                )
            }
        }
        Slider(
            value = shown.toFloat(),
            onValueChange = { onValueChange(it.toInt().coerceIn(5, 10)) },
            valueRange = 5f..10f,
            steps = 4,
            colors = SliderDefaults.colors(
                thumbColor = active,
                activeTrackColor = active,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                activeTickColor = MaterialTheme.colorScheme.surface,
                inactiveTickColor = MaterialTheme.colorScheme.outline,
            ),
            modifier = Modifier.semantics { contentDescription = "RPE" },
        )
    }
}
