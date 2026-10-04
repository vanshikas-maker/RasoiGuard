package com.example.ui.components

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.service.ParsedItemDraft
import kotlinx.coroutines.delay

@Composable
fun CameraScannerView(
    hasCameraPermission: Boolean,
    onRequestCameraPermission: () -> Unit,
    onDetectedItem: (ParsedItemDraft) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isScanning by remember { mutableStateOf(false) }
    var detectedDraft by remember { mutableStateOf<ParsedItemDraft?>(null) }
    var scanStep by remember { mutableStateOf(0) }

    val simulatedReceiptPresets = listOf(
        ParsedItemDraft("Fresh Paneer", "Dairy", 200.0, "g", 92.0, 3, "OCR: AMUL FRESH PANEER 200g USE BY 07/OCT"),
        ParsedItemDraft("Full Cream Milk", "Dairy", 1.0, "L", 66.0, 2, "OCR: MOTHER DAIRY 1L EXP 06/OCT ₹66"),
        ParsedItemDraft("Brown Bread", "Bakery", 400.0, "g", 50.0, 3, "OCR: ENGLISH OVEN BROWN BREAD USE BY 3 DAYS"),
        ParsedItemDraft("Hybrid Tomatoes", "Vegetables", 1.0, "kg", 35.0, 4, "OCR: ZEPRO GROCERY FRESH TOMATOES 1KG")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!hasCameraPermission) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "📷", fontSize = 36.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Camera Permission Required",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "RasoiGuard uses device camera to scan grocery packaging, printed dates, and grocery store receipts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onRequestCameraPermission,
                        modifier = Modifier.testTag("request_camera_perm_button")
                    ) {
                        Text("Grant Camera Access")
                    }
                }
            }
        } else {
            // Camera Preview Container with OCR Targeting Reticle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                // Real Android CameraX Preview
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                            } catch (_: Exception) {
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Augmented Scanner Reticle & Bounding Box Overlay
                Box(
                    modifier = Modifier
                        .size(width = 240.dp, height = 150.dp)
                        .border(2.dp, if (detectedDraft != null) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = if (isScanning) "⚡ Scanning OCR stream..." else "Align barcode or expiry text here",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                    )
                }

                // Detected OCR Bounding Box Label
                if (detectedDraft != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text(
                                text = "Detected: ${detectedDraft?.name} (${detectedDraft?.quantity} ${detectedDraft?.unit})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Expiry: ${detectedDraft?.shelfLifeDays} Days • Price: ₹${detectedDraft?.price?.toInt()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        isScanning = true
                        val preset = simulatedReceiptPresets[scanStep % simulatedReceiptPresets.size]
                        scanStep++
                        detectedDraft = preset
                        isScanning = false
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("snap_and_read_ocr_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Camera, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("📸 Snap & Read Expiry")
                }

                if (detectedDraft != null) {
                    Spacer(modifier = Modifier.width(10.dp))
                    FilledTonalButton(
                        onClick = {
                            detectedDraft?.let { onDetectedItem(it) }
                            detectedDraft = null
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("confirm_ocr_detected_item")
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add to Pantry")
                    }
                }
            }
        }
    }
}
