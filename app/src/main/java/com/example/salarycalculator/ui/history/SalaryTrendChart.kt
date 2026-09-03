package com.example.salarycalculator.ui.history

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.salarycalculator.domain.MonthlySalaryRecord
import com.example.salarycalculator.theme.Emerald60
import com.example.salarycalculator.theme.Slate600
import kotlin.math.max

@Composable
fun SalaryTrendChart(
    history: List<MonthlySalaryRecord>,
    modifier: Modifier = Modifier
) {
    if (history.isEmpty()) return

    // Sort chronologically ascending for timeline
    val sortedRecords = remember(history) {
        history.sortedBy { it.timestamp }.takeLast(8)
    }

    val maxGross = remember(sortedRecords) {
        max(100.0, sortedRecords.maxOfOrNull { it.grossPay } ?: 100.0) * 1.15
    }

    val avgNet = remember(sortedRecords) {
        if (sortedRecords.isNotEmpty()) sortedRecords.sumOf { it.netPay } / sortedRecords.size else 0.0
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header & Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Earnings Trend",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Emerald60))
                        Text("Take-Home", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Slate600))
                        Text("Gross", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                    }
                }
            }

            // Canvas Chart Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                val avgLineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val bottomPadding = 24.dp.toPx()
                    val chartHeight = height - bottomPadding

                    // Draw 3 horizontal grid lines
                    val gridSteps = 3
                    for (i in 0..gridSteps) {
                        val y = chartHeight - (chartHeight * (i.toFloat() / gridSteps))
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1f
                        )
                    }

                    // Average Net dashed line
                    if (avgNet > 0 && maxGross > 0) {
                        val avgY = (chartHeight - (chartHeight * (avgNet / maxGross).toFloat())).coerceIn(0f, chartHeight)
                        drawLine(
                            color = avgLineColor,
                            start = Offset(0f, avgY),
                            end = Offset(width, avgY),
                            strokeWidth = 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }

                    // Draw Bars per record
                    val count = sortedRecords.size
                    val slotWidth = width / count
                    val barWidth = minOf(36.dp.toPx(), slotWidth * 0.55f)

                    sortedRecords.forEachIndexed { index, record ->
                        val centerX = (index * slotWidth) + (slotWidth / 2f)
                        val left = centerX - (barWidth / 2f)

                        val grossFraction = (record.grossPay / maxGross).toFloat().coerceIn(0.02f, 1f)
                        val netFraction = (record.netPay / maxGross).toFloat().coerceIn(0.01f, 1f)

                        val grossBarHeight = chartHeight * grossFraction
                        val netBarHeight = chartHeight * netFraction

                        // 1. Gross Bar (Background container)
                        drawRoundRect(
                            color = Color(0xFF334155).copy(alpha = 0.5f),
                            topLeft = Offset(left, chartHeight - grossBarHeight),
                            size = Size(barWidth, grossBarHeight),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )

                        // 2. Net Take-Home Bar (Emerald Foreground)
                        drawRoundRect(
                            color = Emerald60,
                            topLeft = Offset(left, chartHeight - netBarHeight),
                            size = Size(barWidth, netBarHeight),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )

                        // 3. Month Label Text
                        val shortMonth = record.monthYear.split(" ").firstOrNull()?.take(3) ?: "M${index + 1}"
                        drawContext.canvas.nativeCanvas.apply {
                            val textPaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.GRAY
                                textSize = 22f
                                textAlign = android.graphics.Paint.Align.CENTER
                                isAntiAlias = true
                            }
                            drawText(shortMonth, centerX, height - 4f, textPaint)
                        }
                    }
                }
            }
        }
    }
}
