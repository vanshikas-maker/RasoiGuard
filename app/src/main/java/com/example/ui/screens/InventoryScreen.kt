package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.example.data.model.ExpiryLevel
import com.example.data.model.InventoryItem
import com.example.ui.components.ItemCard
import com.example.ui.theme.CriticalRed
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.WarningAmber

@Composable
fun InventoryScreen(
    items: List<InventoryItem>,
    isCommercial: Boolean,
    onOpenAddModal: () -> Unit,
    onMarkConsumed: (InventoryItem) -> Unit,
    onMarkExpired: (InventoryItem) -> Unit,
    onShowRecipe: (InventoryItem) -> Unit,
    onDeleteItem: (InventoryItem) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, CRITICAL, WARNING, SAFE, CONSUMED

    val activeItems = items.filter { it.status == "ACTIVE" }
    val criticalCount = activeItems.count { it.getExpiryLevel() == ExpiryLevel.CRITICAL || it.getExpiryLevel() == ExpiryLevel.EXPIRED }
    val warningCount = activeItems.count { it.getExpiryLevel() == ExpiryLevel.WARNING }
    val safeCount = activeItems.count { it.getExpiryLevel() == ExpiryLevel.SAFE }
    val consumedCount = items.count { it.status == "CONSUMED" }

    val filteredItems = items.filter { item ->
        val matchesSearch = item.name.contains(searchQuery, ignoreCase = true) ||
                item.category.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedFilter) {
            "CRITICAL" -> item.status == "ACTIVE" && (item.getExpiryLevel() == ExpiryLevel.CRITICAL || item.getExpiryLevel() == ExpiryLevel.EXPIRED)
            "WARNING" -> item.status == "ACTIVE" && item.getExpiryLevel() == ExpiryLevel.WARNING
            "SAFE" -> item.status == "ACTIVE" && item.getExpiryLevel() == ExpiryLevel.SAFE
            "CONSUMED" -> item.status == "CONSUMED"
            else -> item.status == "ACTIVE" // ALL defaults to all active items
        }
        matchesSearch && matchesFilter
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Summary Quick-Stats Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = "Critical",
                count = criticalCount.toString(),
                color = CriticalRed,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Warning",
                count = warningCount.toString(),
                color = WarningAmber,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Safe",
                count = safeCount.toString(),
                color = SafeGreen,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Saved",
                count = consumedCount.toString(),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }

        // Search Bar & Filter Chips
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search pantry items by name or category...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear")
                    }
                }
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("inventory_search_field")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Filter Chips Row
        val filters = listOf(
            "ALL" to "Active (${activeItems.size})",
            "CRITICAL" to "🚨 Critical ($criticalCount)",
            "WARNING" to "⚠️ Warning ($warningCount)",
            "SAFE" to "✅ Safe ($safeCount)",
            "CONSUMED" to "🎉 Consumed ($consumedCount)"
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) {
            items(filters) { (key, label) ->
                FilterChip(
                    selected = selectedFilter == key,
                    onClick = { selectedFilter = key },
                    label = { Text(label, fontSize = 11.sp) },
                    modifier = Modifier.testTag("filter_chip_$key")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // List or Clean Slate Empty State
        if (items.isEmpty() || filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "🥗", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (items.isEmpty()) "Pantry is Clean & Empty" else "No matching items",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (items.isEmpty())
                                "Start fresh! Ingest items automatically via SMS, snap receipt via Camera OCR, forward via WhatsApp, or add manually."
                            else
                                "Try searching for a different ingredient or switch the filter.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = onOpenAddModal,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("add_first_item_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+ Add First Item")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    ItemCard(
                        item = item,
                        isCommercial = isCommercial,
                        onMarkConsumed = { onMarkConsumed(item) },
                        onMarkExpired = { onMarkExpired(item) },
                        onShowRecipe = { onShowRecipe(item) },
                        onDeleteItem = { onDeleteItem(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    count: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}
