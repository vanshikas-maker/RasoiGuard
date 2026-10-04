package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.InventoryItem
import com.example.ui.theme.CriticalRed
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.*

data class DayTrend(
    val dayLabel: String,
    val dayDateStr: String,
    val dateMillis: Long,
    val savedAmount: Double,
    val lossAmount: Double,
    val consumedWeightKg: Double,
    val wastedWeightKg: Double
)

data class WeeklyReportData(
    val timeframeLabel: String,
    val totalConsumedWeightKg: Double,
    val totalWastedWeightKg: Double,
    val weightEfficiencyPercent: Int,
    val totalMoneyRescued: Double,
    val totalMoneyLost: Double,
    val netWeeklySavings: Double,
    val financialEfficiencyPercent: Int,
    val co2SavedKg: Double,
    val waterSavedLitres: Double,
    val dailyTrends: List<DayTrend>,
    val topRescuedItems: List<String>,
    val topWastedItems: List<String>,
    val kitchenGrade: String
)

@Composable
fun WeeklyEfficiencyReportDialog(
    items: List<InventoryItem>,
    onDismiss: () -> Unit
) {
    var selectedTimeframe by remember { mutableStateOf(0) } // 0: This Week (Last 7 Days), 1: Last 14 Days, 2: All Time
    val clipboardManager = LocalClipboardManager.current
    var showCopiedToast by remember { mutableStateOf(false) }

    val reportData = remember(items, selectedTimeframe) {
        calculateWeeklyReport(items, selectedTimeframe)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .testTag("weekly_report_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = SafeGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = "📊", fontSize = 22.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Weekly Efficiency Report",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Weight Wasted vs Consumed & Savings Trends",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_weekly_report_button")
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Timeframe Filter Chips
                val timeframes = listOf("This Week (7 Days)", "Last 14 Days", "All-Time")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    timeframes.forEachIndexed { index, label ->
                        FilterChip(
                            selected = selectedTimeframe == index,
                            onClick = { selectedTimeframe = index },
                            label = { Text(label, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f).testTag("timeframe_filter_$index")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Total Weight Wasted vs Consumed Card
                    WeightSummaryCard(reportData)

                    // 2. Financial Savings & Trends Card
                    FinancialSavingsTrendCard(reportData)

                    // 3. Environmental & Sustainability Impact Card
                    EcoImpactCard(reportData)

                    // 4. Item Highlights (Rescued vs Expired)
                    ItemHighlightsCard(reportData)
                }

                // Bottom Footer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val text = generateShareText(reportData)
                            clipboardManager.setText(AnnotatedString(text))
                            showCopiedToast = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("copy_report_button")
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (showCopiedToast) "Copied!" else "Share Summary")
                    }

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("done_report_button")
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

@Composable
fun WeightSummaryCard(data: WeeklyReportData) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "⚖️ Total Food Weight Analysis",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Weight conserved vs discarded",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = SafeGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SafeGreen.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "${data.weightEfficiencyPercent}% Saved",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = SafeGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Two Column Weight Comparison
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Consumed Weight
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(SafeGreen))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Food Consumed",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.getDefault(), "%.2f kg", data.totalConsumedWeightKg),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = SafeGreen
                    )
                    Text(
                        text = "Rescued from waste",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Wasted Weight
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(CriticalRed))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Food Wasted",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.getDefault(), "%.2f kg", data.totalWastedWeightKg),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (data.totalWastedWeightKg > 0) CriticalRed else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Expired / thrown",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stacked Weight Visual Proportion Bar
            val totalKg = data.totalConsumedWeightKg + data.totalWastedWeightKg
            val consumedFraction = if (totalKg > 0) (data.totalConsumedWeightKg / totalKg).toFloat() else 1f

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (consumedFraction > 0) {
                        Box(
                            modifier = Modifier
                                .weight(consumedFraction.coerceAtLeast(0.01f))
                                .fillMaxHeight()
                                .background(SafeGreen)
                        )
                    }
                    if (1f - consumedFraction > 0) {
                        Box(
                            modifier = Modifier
                                .weight((1f - consumedFraction).coerceAtLeast(0.01f))
                                .fillMaxHeight()
                                .background(CriticalRed)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FinancialSavingsTrendCard(data: WeeklyReportData) {
    var selectedDayIndex by remember { mutableStateOf<Int?>(null) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "📈 Weekly Savings Trends",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Daily money rescued vs lost",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Net Savings",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "₹${data.netWeeklySavings.toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (data.netWeeklySavings >= 0) SafeGreen else CriticalRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Financial Summary Metrics Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Rescued: ₹${data.totalMoneyRescued.toInt()}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = SafeGreen
                )
                Text(
                    text = "Lost: ₹${data.totalMoneyLost.toInt()}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = CriticalRed
                )
                Text(
                    text = "Efficiency: ${data.financialEfficiencyPercent}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Canvas-Drawn 7-Day Trend Chart
            Text(
                text = "7-Day Financial Bar Chart (Tap day to inspect):",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            val maxAmount = remember(data.dailyTrends) {
                val peak = data.dailyTrends.maxOfOrNull { maxOf(it.savedAmount, it.lossAmount) } ?: 100.0
                if (peak <= 0.0) 100.0 else peak
            }

            // Custom Canvas Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height - 24f // leave room for labels
                    val count = data.dailyTrends.size
                    val colWidth = width / count

                    for (i in 0 until count) {
                        val trend = data.dailyTrends[i]
                        val xCenter = i * colWidth + colWidth / 2f
                        val barWidth = (colWidth * 0.35f).coerceAtMost(18f)

                        // Saved Bar (Green)
                        val savedFraction = (trend.savedAmount / maxAmount).toFloat().coerceIn(0f, 1f)
                        val savedHeight = height * savedFraction
                        if (savedHeight > 2f) {
                            drawRoundRect(
                                color = SafeGreen,
                                topLeft = Offset(xCenter - barWidth - 1f, height - savedHeight),
                                size = Size(barWidth, savedHeight),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                        } else {
                            // Baseline marker
                            drawRoundRect(
                                color = SafeGreen.copy(alpha = 0.3f),
                                topLeft = Offset(xCenter - barWidth - 1f, height - 3f),
                                size = Size(barWidth, 3f),
                                cornerRadius = CornerRadius(2f, 2f)
                            )
                        }

                        // Lost Bar (Red)
                        val lossFraction = (trend.lossAmount / maxAmount).toFloat().coerceIn(0f, 1f)
                        val lossHeight = height * lossFraction
                        if (lossHeight > 2f) {
                            drawRoundRect(
                                color = CriticalRed,
                                topLeft = Offset(xCenter + 1f, height - lossHeight),
                                size = Size(barWidth, lossHeight),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                        }
                    }
                }

                // Interactive Overlays & Day Labels
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    data.dailyTrends.forEachIndexed { index, trend ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { selectedDayIndex = if (selectedDayIndex == index) null else index }
                        ) {
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = trend.dayLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontWeight = if (selectedDayIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedDayIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Tooltip inspection if a day is clicked
            selectedDayIndex?.let { idx ->
                val trend = data.dailyTrends[idx]
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${trend.dayDateStr}:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Rescued: ₹${trend.savedAmount.toInt()} (${String.format(Locale.getDefault(), "%.2f", trend.consumedWeightKg)} kg)",
                            style = MaterialTheme.typography.labelSmall,
                            color = SafeGreen
                        )
                        Text(
                            text = "Lost: ₹${trend.lossAmount.toInt()} (${String.format(Locale.getDefault(), "%.2f", trend.wastedWeightKg)} kg)",
                            style = MaterialTheme.typography.labelSmall,
                            color = CriticalRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SafeGreen))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "₹ Rescued (Consumed)", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                Spacer(modifier = Modifier.width(16.dp))
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CriticalRed))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "₹ Lost (Expired)", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun EcoImpactCard(data: WeeklyReportData) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🌱 Environmental Impact & Eco-Score",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // CO2 Saved
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "🌍 CO₂e Avoided", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f kg", data.co2SavedKg),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(text = "Methane & landfill offset", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                }

                // Water Conserved
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "💧 Water Saved", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = String.format(Locale.getDefault(), "%.0f L", data.waterSavedLitres),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF38BDF8)
                    )
                    Text(text = "Virtual agricultural water", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                }

                // Kitchen Grade
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(text = "⭐ Kitchen Rating", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = data.kitchenGrade,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = SafeGreen
                    )
                    Text(text = "Zero-Waste Index", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
fun ItemHighlightsCard(data: WeeklyReportData) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🥗 Item Level Breakdown",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Rescued Items
            Text(
                text = "Top Rescued Before Expiry:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = SafeGreen
            )
            if (data.topRescuedItems.isEmpty()) {
                Text(
                    text = "No items consumed yet in this window.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                data.topRescuedItems.take(4).forEach { item ->
                    Text(
                        text = "• $item",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 6.dp, top = 2.dp)
                    )
                }
            }

            if (data.topWastedItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Items Expired (Loss Recorded):",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = CriticalRed
                )
                data.topWastedItems.take(4).forEach { item ->
                    Text(
                        text = "• $item",
                        style = MaterialTheme.typography.bodySmall,
                        color = CriticalRed,
                        modifier = Modifier.padding(start = 6.dp, top = 2.dp)
                    )
                }
            }
        }
    }
}

fun calculateWeeklyReport(items: List<InventoryItem>, timeframeIndex: Int): WeeklyReportData {
    val now = System.currentTimeMillis()
    val dayMillis = 24 * 60 * 60 * 1000L

    val daysWindow = when (timeframeIndex) {
        0 -> 7
        1 -> 14
        else -> 90
    }
    val cutoffTime = now - (daysWindow * dayMillis)

    val windowItems = if (timeframeIndex == 2) items else items.filter {
        (it.consumedDate ?: it.purchaseDate) >= cutoffTime || it.purchaseDate >= cutoffTime
    }

    var consumedKg = 0.0
    var wastedKg = 0.0
    var moneyRescued = 0.0
    var moneyLost = 0.0

    val rescuedNames = mutableListOf<String>()
    val wastedNames = mutableListOf<String>()

    for (item in windowItems) {
        val kg = itemToWeightKg(item)
        when (item.status) {
            "CONSUMED" -> {
                consumedKg += kg
                moneyRescued += item.purchasePrice
                rescuedNames.add("${item.name} (${item.quantity} ${item.unit} - ₹${item.purchasePrice.toInt()})")
            }
            "EXPIRED" -> {
                wastedKg += kg
                moneyLost += if (item.wastageLoss > 0) item.wastageLoss else item.purchasePrice
                wastedNames.add("${item.name} (${item.quantity} ${item.unit} - ₹${item.purchasePrice.toInt()})")
            }
        }
    }

    val totalKg = consumedKg + wastedKg
    val weightEff = if (totalKg <= 0.0) 100 else ((consumedKg / totalKg) * 100).toInt().coerceIn(0, 100)

    val totalMoney = moneyRescued + moneyLost
    val finEff = if (totalMoney <= 0.0) 100 else (((moneyRescued - moneyLost) / totalMoney) * 100).toInt().coerceIn(0, 100)
    val netSavings = moneyRescued - moneyLost

    // Compute 7-day daily trend
    val dailyTrends = mutableListOf<DayTrend>()
    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
    val dateDisplayFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

    for (i in 6 downTo 0) {
        val targetDayCal = Calendar.getInstance().apply {
            timeInMillis = now - (i * dayMillis)
        }
        val targetDay = targetDayCal.get(Calendar.DAY_OF_YEAR)
        val targetYear = targetDayCal.get(Calendar.YEAR)

        var daySaved = 0.0
        var dayLost = 0.0
        var dayConsumedKg = 0.0
        var dayWastedKg = 0.0

        for (item in items) {
            val itemCal = Calendar.getInstance()
            if (item.status == "CONSUMED" && item.consumedDate != null) {
                itemCal.timeInMillis = item.consumedDate
                if (itemCal.get(Calendar.DAY_OF_YEAR) == targetDay && itemCal.get(Calendar.YEAR) == targetYear) {
                    daySaved += item.purchasePrice
                    dayConsumedKg += itemToWeightKg(item)
                }
            } else if (item.status == "EXPIRED") {
                itemCal.timeInMillis = item.expiryDate
                if (itemCal.get(Calendar.DAY_OF_YEAR) == targetDay && itemCal.get(Calendar.YEAR) == targetYear) {
                    dayLost += item.purchasePrice
                    dayWastedKg += itemToWeightKg(item)
                }
            }
        }

        dailyTrends.add(
            DayTrend(
                dayLabel = dayFormat.format(targetDayCal.time),
                dayDateStr = dateDisplayFormat.format(targetDayCal.time),
                dateMillis = targetDayCal.timeInMillis,
                savedAmount = daySaved,
                lossAmount = dayLost,
                consumedWeightKg = dayConsumedKg,
                wastedWeightKg = dayWastedKg
            )
        )
    }

    val co2 = consumedKg * 2.5
    val water = consumedKg * 800.0

    val grade = when {
        weightEff >= 95 -> "A+ (Master)"
        weightEff >= 80 -> "A (Eco Star)"
        weightEff >= 65 -> "B (Good)"
        weightEff >= 50 -> "C (Fair)"
        else -> "D (Needs Focus)"
    }

    return WeeklyReportData(
        timeframeLabel = when (timeframeIndex) {
            0 -> "This Week"
            1 -> "Last 14 Days"
            else -> "All-Time"
        },
        totalConsumedWeightKg = consumedKg,
        totalWastedWeightKg = wastedKg,
        weightEfficiencyPercent = weightEff,
        totalMoneyRescued = moneyRescued,
        totalMoneyLost = moneyLost,
        netWeeklySavings = netSavings,
        financialEfficiencyPercent = finEff,
        co2SavedKg = co2,
        waterSavedLitres = water,
        dailyTrends = dailyTrends,
        topRescuedItems = rescuedNames,
        topWastedItems = wastedNames,
        kitchenGrade = grade
    )
}

fun itemToWeightKg(item: InventoryItem): Double {
    val q = item.quantity
    val u = item.unit.lowercase(Locale.ROOT)
    val name = item.name.lowercase(Locale.ROOT)
    return when {
        u == "kg" -> q
        u == "g" -> q / 1000.0
        u == "l" -> q * 1.03
        u == "ml" -> (q * 1.03) / 1000.0
        u == "pcs" || u == "packet" || u == "packets" -> {
            when {
                name.contains("egg") -> q * 0.05
                name.contains("bread") -> q * 0.4
                name.contains("lemon") -> q * 0.06
                name.contains("butter") -> q * 0.2
                name.contains("paneer") -> q * 0.2
                else -> q * 0.25
            }
        }
        else -> q * 0.5
    }
}

fun generateShareText(data: WeeklyReportData): String {
    return """
        🥗 *RasoiGuard Weekly Efficiency Report* (${data.timeframeLabel})
        ------------------------------------------
        ⚖️ *Weight Rescued:* ${String.format(Locale.getDefault(), "%.2f", data.totalConsumedWeightKg)} kg
        🗑️ *Weight Wasted:* ${String.format(Locale.getDefault(), "%.2f", data.totalWastedWeightKg)} kg
        📊 *Weight Efficiency:* ${data.weightEfficiencyPercent}%
        
        💰 *Money Rescued:* ₹${data.totalMoneyRescued.toInt()}
        ❌ *Financial Loss:* ₹${data.totalMoneyLost.toInt()}
        ✨ *Net Savings:* ₹${data.netWeeklySavings.toInt()}
        
        🌱 *CO₂e Offset:* ${String.format(Locale.getDefault(), "%.1f", data.co2SavedKg)} kg
        💧 *Water Conserved:* ${String.format(Locale.getDefault(), "%.0f", data.waterSavedLitres)} Litres
        ⭐ *Kitchen Eco-Rating:* ${data.kitchenGrade}
        
        #RasoiGuard #ZeroWasteKitchen #SmartRasoi
    """.trimIndent()
}
