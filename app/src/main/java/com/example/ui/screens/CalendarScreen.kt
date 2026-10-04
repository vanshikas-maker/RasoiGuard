package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActivityLog
import com.example.data.model.InventoryItem
import com.example.ui.theme.CriticalRed
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.FinancialStats
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendarScreen(
    items: List<InventoryItem>,
    logs: List<ActivityLog>,
    stats: FinancialStats,
    onOpenAddModal: () -> Unit,
    onOpenWeeklyReport: () -> Unit
) {
    val calendar = remember { Calendar.getInstance() }
    var currentYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var currentMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH)) } // 0-based
    var selectedDay by remember { mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }

    val monthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val displayCalendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, currentYear)
        set(Calendar.MONTH, currentMonth)
        set(Calendar.DAY_OF_MONTH, 1)
    }

    // Dynamic Financial Engine stats
    val efficiencyColor = when {
        stats.efficiencyPercent >= 80 -> SafeGreen
        stats.efficiencyPercent >= 50 -> WarningAmber
        else -> CriticalRed
    }

    // Quick calculations for weekly weight and savings
    val consumedKg = remember(items) {
        items.filter { it.status == "CONSUMED" }.sumOf { item ->
            val q = item.quantity
            val u = item.unit.lowercase(Locale.ROOT)
            when {
                u == "kg" -> q
                u == "g" -> q / 1000.0
                u == "l" -> q * 1.03
                u == "ml" -> (q * 1.03) / 1000.0
                else -> q * 0.25
            }
        }
    }
    val wastedKg = remember(items) {
        items.filter { it.status == "EXPIRED" }.sumOf { item ->
            val q = item.quantity
            val u = item.unit.lowercase(Locale.ROOT)
            when {
                u == "kg" -> q
                u == "g" -> q / 1000.0
                u == "l" -> q * 1.03
                u == "ml" -> (q * 1.03) / 1000.0
                else -> q * 0.25
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Weekly Efficiency Report Visual Banner Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            ),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp)
                .clickable { onOpenWeeklyReport() }
                .testTag("weekly_report_banner_card")
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Insights,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Weekly Efficiency Report",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Weight wasted vs consumed & savings trends",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = onOpenWeeklyReport,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("open_weekly_report_button")
                    ) {
                        Text("View Report", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Weight & Savings Snapshot
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).padding(end = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SafeGreen))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(text = "Rescued", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = String.format(Locale.getDefault(), "%.2f kg", consumedKg),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SafeGreen
                                )
                            }
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CriticalRed))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(text = "Wasted", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = String.format(Locale.getDefault(), "%.2f kg", wastedKg),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (wastedKg > 0) CriticalRed else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).padding(start = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "₹", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(text = "Efficiency", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "${stats.efficiencyPercent}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = efficiencyColor
                                )
                            }
                        }
                    }
                }
            }
        }
        // Dynamic Financial Engine Header Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Kitchen Efficiency Rating",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Dynamic zero-waste financial performance",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        color = efficiencyColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, efficiencyColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "${stats.efficiencyPercent}%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = efficiencyColor,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { stats.efficiencyPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = efficiencyColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Spend vs Loss Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Total Spend",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "₹${stats.totalSpend.toInt()}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Wastage Loss",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "₹${stats.wastageLoss.toInt()}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CriticalRed
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Active Items",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${stats.activeItemCount}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Month Navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (currentMonth == 0) {
                        currentMonth = 11
                        currentYear -= 1
                    } else {
                        currentMonth -= 1
                    }
                },
                modifier = Modifier.testTag("prev_month_button")
            ) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous Month")
            }

            Text(
                text = monthFormat.format(displayCalendar.time),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = {
                    if (currentMonth == 11) {
                        currentMonth = 0
                        currentYear += 1
                    } else {
                        currentMonth += 1
                    }
                },
                modifier = Modifier.testTag("next_month_button")
            ) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Next Month")
            }
        }

        // Calendar Day Headers
        val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        Row(modifier = Modifier.fillMaxWidth()) {
            dayNames.forEach { dayName ->
                Text(
                    text = dayName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Calendar Grid
        val firstDayOfWeek = displayCalendar.get(Calendar.DAY_OF_WEEK) - 1 // 0-based for Sunday
        val daysInMonth = displayCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val totalCells = ((firstDayOfWeek + daysInMonth + 6) / 7) * 7

        Column(modifier = Modifier.fillMaxWidth()) {
            for (row in 0 until (totalCells / 7)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val dayNumber = cellIndex - firstDayOfWeek + 1
                        val isValidDay = dayNumber in 1..daysInMonth

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.1f)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isValidDay && dayNumber == selectedDay) MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent
                                )
                                .clickable(enabled = isValidDay) {
                                    if (isValidDay) selectedDay = dayNumber
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isValidDay) {
                                val cellCal = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, currentYear)
                                    set(Calendar.MONTH, currentMonth)
                                    set(Calendar.DAY_OF_MONTH, dayNumber)
                                }
                                val cellDate = cellCal.timeInMillis

                                // Check purchases, consumptions, and expiries on this date
                                val hasPurchase = items.any { isSameDay(it.purchaseDate, cellDate) }
                                val hasExpiry = items.any { it.status == "ACTIVE" && isSameDay(it.expiryDate, cellDate) }
                                val hasConsumed = items.any { it.status == "CONSUMED" && it.consumedDate != null && isSameDay(it.consumedDate, cellDate) }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$dayNumber",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (dayNumber == selectedDay) FontWeight.Bold else FontWeight.Normal,
                                        color = if (dayNumber == selectedDay) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    // Indicators Row
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        if (hasPurchase) {
                                            Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color(0xFF3B82F6)))
                                        }
                                        if (hasConsumed) {
                                            Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(SafeGreen))
                                        }
                                        if (hasExpiry) {
                                            Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(CriticalRed))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(10.dp))

        // Selected Date Log / Inspector
        val selectedDateCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonth)
            set(Calendar.DAY_OF_MONTH, selectedDay)
        }
        val selectedDateMillis = selectedDateCal.timeInMillis
        val selectedDateStr = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(selectedDateCal.time)

        Text(
            text = "Records for $selectedDateStr:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        val dayPurchases = items.filter { isSameDay(it.purchaseDate, selectedDateMillis) }
        val dayExpiries = items.filter { it.status == "ACTIVE" && isSameDay(it.expiryDate, selectedDateMillis) }
        val dayConsumed = items.filter { it.status == "CONSUMED" && it.consumedDate != null && isSameDay(it.consumedDate, selectedDateMillis) }

        if (dayPurchases.isEmpty() && dayExpiries.isEmpty() && dayConsumed.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No purchase or expiry activity recorded on this date.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 70.dp)
            ) {
                // Expiry Alerts for this day
                items(dayExpiries) { item ->
                    ActivityItemCard(
                        type = "EXPIRY",
                        title = "🚨 Expiry Warning: ${item.name}",
                        details = "${item.quantity} ${item.unit} • Worth ₹${item.purchasePrice.toInt()}",
                        color = CriticalRed
                    )
                }

                // Consumed items
                items(dayConsumed) { item ->
                    ActivityItemCard(
                        type = "CONSUMED",
                        title = "🎉 Zero-Waste Win: ${item.name}",
                        details = "Consumed in full • Saved ₹${item.purchasePrice.toInt()}",
                        color = SafeGreen
                    )
                }

                // Purchases
                items(dayPurchases) { item ->
                    ActivityItemCard(
                        type = "PURCHASE",
                        title = "🛒 Purchased: ${item.name}",
                        details = "${item.quantity} ${item.unit} • Paid ₹${item.purchasePrice.toInt()} (${item.source})",
                        color = Color(0xFF3B82F6)
                    )
                }
            }
        }
    }
}

@Composable
fun ActivityItemCard(
    type: String,
    title: String,
    details: String,
    color: Color
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun isSameDay(time1: Long, time2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
