package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.ParsedItemDraft
import com.example.service.SmsReader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemsModal(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    hasSmsPermission: Boolean,
    hasCameraPermission: Boolean,
    onRequestSmsPermission: () -> Unit,
    onRequestCameraPermission: () -> Unit,
    onAddManualItem: (String, String, Double, String, Double, Int) -> Unit,
    onAddParsedDrafts: (List<ParsedItemDraft>, String, String) -> Unit
) {
    if (!isOpen) return

    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("SMS & Push", "Camera OCR", "WhatsApp Bot", "Quick Manual")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.fillMaxHeight(0.92f).testTag("add_items_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📥 Ingest Kitchen Items",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_add_items_modal")) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("ingest_tab_$index")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (selectedTab) {
                0 -> SmsIngestionTab(
                    hasSmsPermission = hasSmsPermission,
                    onRequestSmsPermission = onRequestSmsPermission,
                    onIngestDrafts = { drafts, source ->
                        onAddParsedDrafts(drafts, source, "SMS / Notification Parse")
                        onDismiss()
                    }
                )
                1 -> CameraScannerView(
                    hasCameraPermission = hasCameraPermission,
                    onRequestCameraPermission = onRequestCameraPermission,
                    onDetectedItem = { draft ->
                        onAddParsedDrafts(listOf(draft), "OCR", "Packaging Scan")
                        onDismiss()
                    }
                )
                2 -> WhatsAppSimulatorTab(
                    onIngestItems = { drafts ->
                        onAddParsedDrafts(drafts, "WHATSAPP", "WhatsApp Forwarder")
                        onDismiss()
                    }
                )
                3 -> ManualAddTab(
                    onAddItem = { name, cat, qty, unit, price, shelfLife ->
                        onAddManualItem(name, cat, qty, unit, price, shelfLife)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
fun SmsIngestionTab(
    hasSmsPermission: Boolean,
    onRequestSmsPermission: () -> Unit,
    onIngestDrafts: (List<ParsedItemDraft>, String) -> Unit
) {
    val context = LocalContext.current
    var pastedText by remember { mutableStateOf("") }
    var notificationListenerActive by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        // Native Permission & Hook Status Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Native READ_SMS Hook",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (hasSmsPermission) "Active: Listening for Swiggy/Zepto/Blinkit orders" else "Disabled: Grant permission to auto-read incoming SMS",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (hasSmsPermission) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }

                    if (!hasSmsPermission) {
                        Button(
                            onClick = onRequestSmsPermission,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("grant_sms_button")
                        ) {
                            Text("Grant", style = MaterialTheme.typography.labelSmall)
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(10.dp))

                // Notification Listener Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Push Notification Order Sniffer",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Captures instant delivery push notifications from Zomato & Swiggy",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = notificationListenerActive,
                        onCheckedChange = { notificationListenerActive = it },
                        modifier = Modifier.testTag("notification_sniffer_switch")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Preset Indian Quick Commerce Test Samples
        Text(
            text = "⚡ Instant Test Samples (Click to Parse):",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(6.dp))

        SmsReader.SAMPLE_SMS_MESSAGES.forEachIndexed { idx, sample ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable {
                        val parsed = SmsReader.parseMessageToDrafts(sample)
                        onIngestDrafts(parsed, "SMS Sample #${idx + 1}")
                    }
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = sample,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Sync",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Manual Paste Box
        Text(
            text = "Or Paste Order SMS / Bill Text:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = pastedText,
            onValueChange = { pastedText = it },
            placeholder = { Text("e.g. Blinkit: Delivered 1L Amul Milk ₹60, 200g Paneer ₹95") },
            modifier = Modifier.fillMaxWidth().testTag("paste_sms_text_field"),
            minLines = 3
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (pastedText.isNotBlank()) {
                    val parsed = SmsReader.parseMessageToDrafts(pastedText)
                    onIngestDrafts(parsed, "Pasted SMS")
                }
            },
            enabled = pastedText.isNotBlank(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("parse_pasted_sms_button")
        ) {
            Text("Parse & Sync into Active Inventory")
        }
    }
}

@Composable
fun WhatsAppSimulatorTab(
    onIngestItems: (List<ParsedItemDraft>) -> Unit
) {
    var messages by remember {
        mutableStateOf(
            listOf(
                "BOT: 🤖 Connected to RasoiGuard WhatsApp Business Webhook (+91 98765 43210). Forward any grocery bill, photo or text order here!"
            )
        )
    }
    var inputText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    val quickForwards = listOf(
        "Forwarded: Zepto bill - 1L Amul Buffalo Milk (₹70), 500g Fresh Curd (₹45), 6 Eggs (₹48)",
        "Forwarded: BigBasket receipt - 1kg Aashirvaad Atta (₹65), 500g Moong Dal (₹75)",
        "Forwarded: Local Sabzi Mandi - 1kg Tomato ₹30, 2kg Potato ₹70, 250g Palak ₹20"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        // WhatsApp Chat Surface
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(bottom = 10.dp)
        ) {
            LazyColumn(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { msg ->
                    val isBot = msg.startsWith("BOT:")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isBot) Arrangement.Start else Arrangement.End
                    ) {
                        Surface(
                            color = if (isBot) MaterialTheme.colorScheme.surface else Color(0xFF005C4B),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Text(
                                text = msg.removePrefix("BOT: "),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isBot) MaterialTheme.colorScheme.onSurface else Color.White,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = "⚡ Quick Forward Presets:",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))

        quickForwards.forEach { preset ->
            FilledTonalButton(
                onClick = {
                    val updated = messages.toMutableList()
                    updated.add(preset)
                    messages = updated

                    coroutineScope.launch {
                        delay(600)
                        val drafts = SmsReader.parseMessageToDrafts(preset)
                        val updated2 = messages.toMutableList()
                        updated2.add("BOT: ✅ Parsed ${drafts.size} items! Ingesting to your active Rasoi...")
                        messages = updated2
                        delay(500)
                        onIngestItems(drafts)
                    }
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            ) {
                Text(
                    text = preset.take(45) + "...",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Forward receipt message...") },
                modifier = Modifier.weight(1f).testTag("whatsapp_input_field"),
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        val text = inputText
                        inputText = ""
                        val updated = messages.toMutableList()
                        updated.add(text)
                        messages = updated
                        coroutineScope.launch {
                            delay(500)
                            val drafts = SmsReader.parseMessageToDrafts(text)
                            val updated2 = messages.toMutableList()
                            updated2.add("BOT: ✅ Detected ${drafts.size} items from message. Added to pantry!")
                            messages = updated2
                            delay(400)
                            onIngestItems(drafts)
                        }
                    }
                },
                modifier = Modifier.testTag("whatsapp_send_button")
            ) {
                Icon(Icons.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualAddTab(
    onAddItem: (String, String, Double, String, Double, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Vegetables") }
    var quantityStr by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("kg") }
    var priceStr by remember { mutableStateOf("40") }
    var shelfLifeDays by remember { mutableStateOf(3) }

    val categories = listOf("Vegetables", "Dairy", "Fruits", "Bakery", "Staples", "Meat/Fish", "Spices", "Beverages")
    val units = listOf("kg", "g", "L", "ml", "pcs", "packets")
    val shelfLifePresets = listOf(
        1 to "1 Day (Critical)",
        2 to "2 Days",
        3 to "3 Days (Safe)",
        7 to "1 Week",
        14 to "2 Weeks",
        60 to "2 Months"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Item Name *") },
            placeholder = { Text("e.g. Fresh Paneer, Amul Butter, Tomatoes") },
            modifier = Modifier.fillMaxWidth().testTag("manual_item_name_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Category Chips
        Text(text = "Category:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
            items(categories) { cat ->
                FilterChip(
                    selected = category == cat,
                    onClick = { category = cat },
                    label = { Text(cat) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Qty & Unit & Price
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = quantityStr,
                onValueChange = { quantityStr = it },
                label = { Text("Qty") },
                modifier = Modifier.weight(1f).testTag("manual_qty_input")
            )

            // Unit Selector
            Box(modifier = Modifier.weight(1f)) {
                var unitExpanded by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = unit,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Unit") },
                    trailingIcon = {
                        IconButton(onClick = { unitExpanded = true }) {
                            Icon(Icons.Filled.ArrowDropDown, null)
                        }
                    },
                    modifier = Modifier.clickable { unitExpanded = true }
                )
                DropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                    units.forEach { u ->
                        DropdownMenuItem(
                            text = { Text(u) },
                            onClick = {
                                unit = u
                                unitExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = priceStr,
                onValueChange = { priceStr = it },
                label = { Text("Price (₹)") },
                modifier = Modifier.weight(1f).testTag("manual_price_input")
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Shelf Life Presets
        Text(
            text = "Shelf-Life / Expiry Window:",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 6.dp)) {
            items(shelfLifePresets) { (days, label) ->
                FilterChip(
                    selected = shelfLifeDays == days,
                    onClick = { shelfLifeDays = days },
                    label = { Text(label) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (name.isNotBlank()) {
                    val qty = quantityStr.toDoubleOrNull() ?: 1.0
                    val price = priceStr.toDoubleOrNull() ?: 0.0
                    onAddItem(name, category, qty, unit, price, shelfLifeDays)
                }
            },
            enabled = name.isNotBlank(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("save_manual_item_button")
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Item to Rasoi")
        }
    }
}
