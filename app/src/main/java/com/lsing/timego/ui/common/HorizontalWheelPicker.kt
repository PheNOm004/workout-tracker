package com.lsing.timego.ui.common

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** A horizontally-scrolling, snap-to-center item picker -- the item nearest the center is
 *  visually emphasized (larger, primary-colored) and reported via [onSelectedIndexChange] once
 *  scrolling settles. The side content padding is derived from the actual measured container
 *  width via BoxWithConstraints (not a hardcoded guess) so the centered item lands in the real
 *  visual center regardless of device screen width. A hairline center tick below the wheel
 *  is the Training Ledger's one concrete addition here. */
@Composable
fun HorizontalWheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemWidth: Dp = 140.dp,
) {
    val listState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(listState)

    LaunchedEffect(selectedIndex) {
        if (!listState.isScrollInProgress) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    val centeredIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            layoutInfo.visibleItemsInfo.minByOrNull { itemInfo ->
                kotlin.math.abs((itemInfo.offset + itemInfo.size / 2) - viewportCenter)
            }?.index
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            centeredIndex?.let { if (it != selectedIndex) onSelectedIndexChange(it) }
        }
    }

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val sidePadding = ((maxWidth - itemWidth) / 2).coerceAtLeast(0.dp)
            LazyRow(
                state = listState,
                flingBehavior = flingBehavior,
                contentPadding = PaddingValues(horizontal = sidePadding),
            ) {
                itemsIndexed(items) { index, label ->
                    val isSelected = index == centeredIndex
                    val itemScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.12f else 0.88f,
                        animationSpec = spring(
                            dampingRatio = 0.82f,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                        label = "wheelItemScale_$index",
                    )
                    val itemAlpha by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0.35f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                        label = "wheelItemAlpha_$index",
                    )

                    Box(
                        modifier = Modifier
                            .width(itemWidth)
                            .padding(vertical = 12.dp)
                            .graphicsLayer {
                                scaleX = itemScale
                                scaleY = itemScale
                                alpha = itemAlpha
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            style = if (isSelected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                }
            }
        }
        val indicatorColor = MaterialTheme.colorScheme.primary
        Canvas(
            modifier = Modifier
                .padding(top = 2.dp)
                .width(28.dp)
                .height(3.dp),
        ) {
            drawRoundRect(
                color = indicatorColor,
                cornerRadius = CornerRadius(size.height / 2f),
            )
        }
    }
}
