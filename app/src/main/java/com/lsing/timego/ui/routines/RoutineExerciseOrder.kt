package com.lsing.timego.ui.routines

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.lsing.timego.ui.theme.Spacing
import kotlin.math.roundToInt

/** Returns [items] with the element at [from] moved to [to]; out-of-range or equal indices return it unchanged. */
internal fun <T> moveItem(items: List<T>, from: Int, to: Int): List<T> {
    if (from == to || from !in items.indices || to !in items.indices) return items
    return items.toMutableList().apply { add(to, removeAt(from)) }
}

/** The routine's exercises in workout order; drag the handle to reorder. */
@Composable
internal fun RoutineExerciseOrder(
    ids: List<Long>,
    nameOf: (Long) -> String,
    onReorder: (List<Long>) -> Unit,
) {
    val currentIds by rememberUpdatedState(ids)
    val currentOnReorder by rememberUpdatedState(onReorder)
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var rowHeightPx by remember { mutableFloatStateOf(0f) }
    val rowGapPx = with(LocalDensity.current) { Spacing.ExtraSmall.toPx() }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)) {
        ids.forEachIndexed { index, id ->
            androidx.compose.runtime.key(id) {
                val dragging = draggingId == id
                Surface(
                    tonalElevation = if (dragging) 6.dp else 1.dp,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(if (dragging) 1f else 0f)
                        .offset { IntOffset(0, if (dragging) dragOffset.roundToInt() else 0) }
                        .onSizeChanged { if (rowHeightPx == 0f) rowHeightPx = it.height.toFloat() },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = Spacing.Small, vertical = Spacing.ExtraSmall),
                    ) {
                        Text(
                            "${index + 1}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = Spacing.Small),
                        )
                        Text(
                            nameOf(id),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            Icons.Filled.DragHandle,
                            contentDescription = "Drag to reorder ${nameOf(id)}",
                            modifier = Modifier
                                .size(40.dp)
                                .padding(8.dp)
                                .pointerInput(id) {
                                    detectDragGestures(
                                        onDragStart = { draggingId = id; dragOffset = 0f },
                                        onDragEnd = { draggingId = null; dragOffset = 0f },
                                        onDragCancel = { draggingId = null; dragOffset = 0f },
                                    ) { change, amount ->
                                        change.consume()
                                        dragOffset += amount.y
                                        val step = rowHeightPx + rowGapPx
                                        val list = currentIds
                                        val from = list.indexOf(id)
                                        if (step > 0f && from >= 0) {
                                            val to = (from + (dragOffset / step).roundToInt()).coerceIn(list.indices)
                                            if (to != from) {
                                                currentOnReorder(moveItem(list, from, to))
                                                dragOffset -= (to - from) * step
                                            }
                                        }
                                    }
                                },
                        )
                    }
                }
            }
        }
    }
}
