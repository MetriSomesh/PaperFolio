package com.paperfolio.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paperfolio.domain.model.ChartPoint
import com.paperfolio.ui.theme.LossRed
import com.paperfolio.ui.theme.ProfitGreen
import com.paperfolio.ui.theme.TextSecondary
import com.paperfolio.ui.theme.TextTertiary

@Composable
fun StockLineChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    isPositive: Boolean = true,
    previousClose: Double? = null,
    onPointSelected: ((ChartPoint?) -> Unit)? = null
) {
    if (points.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No chart data available for this timeframe", color = TextTertiary, fontSize = 13.sp)
        }
        return
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val lineColor = if (isPositive) ProfitGreen else LossRed
    val gradientColor = if (isPositive) Color(0x3800D09C) else Color(0x38EB5B56)

    val minPrice = points.minOf { it.price }
    val maxPrice = points.maxOf { it.price }
    val isFlat = (maxPrice - minPrice) <= 0.0001
    val priceRange = if (isFlat) 1.0 else (maxPrice - minPrice)

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(points) {
                    detectTapGestures(
                        onPress = { offset ->
                            val xFraction = (offset.x / size.width).coerceIn(0f, 1f)
                            val index = (xFraction * (points.size - 1).coerceAtLeast(0)).toInt().coerceIn(points.indices)
                            selectedIndex = index
                            onPointSelected?.invoke(points[index])
                            tryAwaitRelease()
                            selectedIndex = null
                            onPointSelected?.invoke(null)
                        }
                    )
                }
                .pointerInput(points) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val xFraction = (offset.x / size.width).coerceIn(0f, 1f)
                            val index = (xFraction * (points.size - 1).coerceAtLeast(0)).toInt().coerceIn(points.indices)
                            selectedIndex = index
                            onPointSelected?.invoke(points[index])
                        },
                        onDragEnd = {
                            selectedIndex = null
                            onPointSelected?.invoke(null)
                        },
                        onDragCancel = {
                            selectedIndex = null
                            onPointSelected?.invoke(null)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val xFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                            val index = (xFraction * (points.size - 1).coerceAtLeast(0)).toInt().coerceIn(points.indices)
                            if (index != selectedIndex) {
                                selectedIndex = index
                                onPointSelected?.invoke(points[index])
                            }
                        }
                    )
                }
        ) {
            val width = size.width
            val height = size.height
            val verticalPadding = 20.dp.toPx()
            val usableHeight = height - (verticalPadding * 2)

            fun getY(price: Double): Float {
                if (isFlat) {
                    return height / 2f
                }
                val normalized = (price - minPrice) / priceRange
                return (height - verticalPadding) - (normalized * usableHeight).toFloat()
            }

            // Draw dashed previous close line if present
            if (previousClose != null && !isFlat && previousClose in minPrice..maxPrice) {
                val prevCloseY = getY(previousClose)
                drawLine(
                    color = Color.Gray.copy(alpha = 0.35f),
                    start = Offset(0f, prevCloseY),
                    end = Offset(width, prevCloseY),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                )
            }

            if (points.size == 1) {
                // Single point case: draw horizontal dashed line and center dot
                val cy = height / 2f
                drawLine(
                    color = lineColor,
                    start = Offset(0f, cy),
                    end = Offset(width, cy),
                    strokeWidth = 2.dp.toPx()
                )
                drawCircle(
                    color = lineColor,
                    radius = 5.dp.toPx(),
                    center = Offset(width / 2f, cy)
                )
            } else {
                // Build smooth curve path
                val strokePath = Path()
                val fillPath = Path()

                val stepX = width / (points.size - 1).coerceAtLeast(1)

                for (i in points.indices) {
                    val x = i * stepX
                    val y = getY(points[i].price)

                    if (i == 0) {
                        strokePath.moveTo(x, y)
                        fillPath.moveTo(x, height)
                        fillPath.lineTo(x, y)
                    } else {
                        val prevX = (i - 1) * stepX
                        val prevY = getY(points[i - 1].price)
                        val controlX1 = prevX + (stepX / 2f)
                        val controlX2 = prevX + (stepX / 2f)
                        strokePath.cubicTo(controlX1, prevY, controlX2, y, x, y)
                        fillPath.cubicTo(controlX1, prevY, controlX2, y, x, y)
                    }
                }

                fillPath.lineTo(width, height)
                fillPath.close()

                // Draw gradient fill under curve
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(gradientColor, Color.Transparent),
                        startY = 0f,
                        endY = height
                    )
                )

                // Draw main price line
                drawPath(
                    path = strokePath,
                    color = lineColor,
                    style = Stroke(
                        width = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )

                // Draw touch crosshair & glowing marker dot when dragging/tapping
                selectedIndex?.let { index ->
                    if (index in points.indices) {
                        val x = index * stepX
                        val y = getY(points[index].price)

                        // Vertical dashed guideline
                        drawLine(
                            color = TextSecondary.copy(alpha = 0.5f),
                            start = Offset(x, 0f),
                            end = Offset(x, height),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                        )

                        // Outer halo
                        drawCircle(
                            color = lineColor.copy(alpha = 0.25f),
                            radius = 9.dp.toPx(),
                            center = Offset(x, y)
                        )

                        // Inner solid dot with white border
                        drawCircle(
                            color = Color.White,
                            radius = 4.5.dp.toPx(),
                            center = Offset(x, y)
                        )
                        drawCircle(
                            color = lineColor,
                            radius = 3.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }
                }
            }
        }
    }
}
