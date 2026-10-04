package com.example.service

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import com.example.data.model.InventoryItem
import java.util.Locale
import java.util.regex.Pattern

data class ParsedItemDraft(
    val name: String,
    val category: String,
    val quantity: Double,
    val unit: String,
    val price: Double,
    val shelfLifeDays: Int,
    val sourceText: String
)

object SmsReader {

    // Preset realistic quick commerce SMS messages for instant verification
    val SAMPLE_SMS_MESSAGES = listOf(
        "Blinkit: Order #BK-8841 delivered! Amul Taaza Milk 1L (₹56), Fresh Paneer 200g (₹92), Farm Eggs 6pcs (₹48). Total paid: ₹196.",
        "Zepto order delivered: 500g Fresh Tomato (₹25), 1kg Hybrid Potato (₹38), 250g Green Coriander (₹20). Amount paid: ₹83.",
        "Swiggy Instamart: Your groceries delivered! 1kg Aashirvaad Shudh Chakki Atta (₹65), 500g Dahi (₹45), 100g Ginger (₹25). Total: ₹135.",
        "Zomato / Blinkit: Delivered 1L Mother Dairy Full Cream Milk (₹66), 400g Brown Bread (₹50), 200g Salted Butter (₹110). Paid ₹226.",
        "BigBasket BB-Now: Order delivered successfully. 1kg Basmati Rice (₹120), 500g Toor Dal (₹85), 250g Paneer (₹110). Total: ₹315."
    )

    fun queryDeviceSms(context: Context): List<String> {
        val messages = mutableListOf<String>()
        try {
            val uri: Uri = Telephony.Sms.Inbox.CONTENT_URI
            val projection = arrayOf(Telephony.Sms.BODY, Telephony.Sms.ADDRESS, Telephony.Sms.DATE)
            val cursor = context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC LIMIT 30"
            )
            cursor?.use {
                val bodyIndex = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val addressIndex = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                while (it.moveToNext()) {
                    val body = it.getString(bodyIndex)
                    val sender = it.getString(addressIndex) ?: ""
                    val lower = (body + sender).lowercase(Locale.ROOT)
                    if (lower.contains("order") || lower.contains("blinkit") || lower.contains("zepto") ||
                        lower.contains("swiggy") || lower.contains("zomato") || lower.contains("bigbasket") ||
                        lower.contains("delivered") || lower.contains("grocery") || lower.contains("milk")
                    ) {
                        messages.add(body)
                    }
                }
            }
        } catch (_: Exception) {
            // Permission denied or provider unavailable
        }
        return messages
    }

    fun parseMessageToDrafts(messageText: String): List<ParsedItemDraft> {
        val drafts = mutableListOf<ParsedItemDraft>()
        val lines = messageText.split("\n", ";", ",")
            .flatMap { it.split(Pattern.compile("(?<=\\)),|(?<=\\d\\))")) }
            .map { it.trim() }
            .filter { it.isNotBlank() }

        // Regex patterns to capture: (qty)(unit) Name (price)
        // e.g. "Amul Taaza Milk 1L (₹56)", "1kg Hybrid Potato (₹38)", "Fresh Paneer 200g - ₹92", "400g Brown Bread"
        val itemPattern = Pattern.compile(
            "(?:(\\d+(?:\\.\\d+)?)\\s*(kg|g|l|ml|pcs|packet|packets)?\\s+)?([A-Za-z\\s]+?)(?:\\s+(\\d+(?:\\.\\d+)?)\\s*(kg|g|l|ml|pcs|packet|packets))?(?:[\\s\\(:-]*(?:₹|Rs\\.?|INR)?\\s*(\\d+(?:\\.\\d+)?)\\)?)?",
            Pattern.CASE_INSENSITIVE
        )

        for (part in lines) {
            val trimmed = part.replace("delivered!", "").replace("order delivered:", "")
                .replace("Total paid:", "").replace("Amount paid:", "").trim()
            if (trimmed.startsWith("total", ignoreCase = true) ||
                trimmed.startsWith("paid", ignoreCase = true) ||
                trimmed.startsWith("amount", ignoreCase = true)
            ) {
                continue
            }

            val matcher = itemPattern.matcher(trimmed)
            if (matcher.find()) {
                val preQty = matcher.group(1)?.toDoubleOrNull()
                val preUnit = matcher.group(2)
                var name = matcher.group(3)?.trim() ?: ""
                val postQty = matcher.group(4)?.toDoubleOrNull()
                val postUnit = matcher.group(5)
                val price = matcher.group(6)?.toDoubleOrNull() ?: estimatePrice(name)

                // Clean name from delivery prefixes
                name = name.replace(Pattern.compile("^(Blinkit|Zepto|Swiggy|Zomato|Instamart|BigBasket|Order|Delivered)\\s*[:-]?\\s*", Pattern.CASE_INSENSITIVE).toRegex(), "").trim()

                if (name.length >= 3 && !name.equals("order", ignoreCase = true) && !name.equals("total", ignoreCase = true)) {
                    val qty = preQty ?: postQty ?: 1.0
                    val unit = preUnit ?: postUnit ?: inferUnit(name)
                    val category = inferCategory(name)
                    val shelfLife = inferShelfLifeDays(name, category)

                    drafts.add(
                        ParsedItemDraft(
                            name = name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
                            category = category,
                            quantity = qty,
                            unit = unit,
                            price = price,
                            shelfLifeDays = shelfLife,
                            sourceText = trimmed
                        )
                    )
                }
            }
        }

        // If regex didn't parse items cleanly, fallback to token-based matching
        if (drafts.isEmpty()) {
            val fallbackDrafts = parseFallback(messageText)
            drafts.addAll(fallbackDrafts)
        }

        return drafts
    }

    private fun parseFallback(text: String): List<ParsedItemDraft> {
        val keywords = listOf(
            "Milk" to ("Dairy" to 2),
            "Paneer" to ("Dairy" to 3),
            "Curd" to ("Dairy" to 4),
            "Dahi" to ("Dairy" to 4),
            "Bread" to ("Bakery" to 3),
            "Tomato" to ("Vegetables" to 4),
            "Potato" to ("Vegetables" to 14),
            "Onion" to ("Vegetables" to 14),
            "Coriander" to ("Vegetables" to 2),
            "Spinach" to ("Vegetables" to 2),
            "Eggs" to ("Dairy" to 7),
            "Atta" to ("Staples" to 60),
            "Rice" to ("Staples" to 90),
            "Dal" to ("Staples" to 60),
            "Butter" to ("Dairy" to 14),
            "Cheese" to ("Dairy" to 10),
            "Ginger" to ("Vegetables" to 10),
            "Banana" to ("Fruits" to 3),
            "Apple" to ("Fruits" to 7)
        )

        val results = mutableListOf<ParsedItemDraft>()
        for ((keyword, meta) in keywords) {
            if (text.contains(keyword, ignoreCase = true)) {
                results.add(
                    ParsedItemDraft(
                        name = "Fresh $keyword",
                        category = meta.first,
                        quantity = 1.0,
                        unit = if (meta.first == "Dairy" && keyword == "Milk") "L" else if (meta.first == "Staples") "kg" else "pcs",
                        price = estimatePrice(keyword),
                        shelfLifeDays = meta.second,
                        sourceText = text.take(60)
                    )
                )
            }
        }
        return results
    }

    fun inferCategory(name: String): String {
        val lower = name.lowercase(Locale.ROOT)
        return when {
            lower.contains("milk") || lower.contains("paneer") || lower.contains("curd") ||
            lower.contains("dahi") || lower.contains("cheese") || lower.contains("butter") ||
            lower.contains("ghee") || lower.contains("cream") -> "Dairy"

            lower.contains("tomato") || lower.contains("potato") || lower.contains("onion") ||
            lower.contains("coriander") || lower.contains("spinach") || lower.contains("palak") ||
            lower.contains("ginger") || lower.contains("garlic") || lower.contains("chilli") ||
            lower.contains("bhindi") || lower.contains("gobi") || lower.contains("cabbage") -> "Vegetables"

            lower.contains("apple") || lower.contains("banana") || lower.contains("mango") ||
            lower.contains("orange") || lower.contains("papaya") || lower.contains("grapes") -> "Fruits"

            lower.contains("bread") || lower.contains("bun") || lower.contains("croissant") ||
            lower.contains("toast") || lower.contains("pav") -> "Bakery"

            lower.contains("atta") || lower.contains("rice") || lower.contains("dal") ||
            lower.contains("flour") || lower.contains("besan") || lower.contains("sooji") ||
            lower.contains("oil") || lower.contains("sugar") || lower.contains("salt") -> "Staples"

            lower.contains("chicken") || lower.contains("mutton") || lower.contains("fish") ||
            lower.contains("prawn") || lower.contains("meat") -> "Meat/Fish"

            lower.contains("egg") -> "Dairy"
            else -> "Pantry"
        }
    }

    fun inferUnit(name: String): String {
        val lower = name.lowercase(Locale.ROOT)
        return when {
            lower.contains("milk") || lower.contains("oil") || lower.contains("juice") -> "L"
            lower.contains("egg") || lower.contains("lemon") -> "pcs"
            lower.contains("paneer") || lower.contains("butter") || lower.contains("ginger") -> "g"
            lower.contains("atta") || lower.contains("rice") || lower.contains("potato") || lower.contains("onion") -> "kg"
            else -> "pcs"
        }
    }

    fun inferShelfLifeDays(name: String, category: String): Int {
        val lower = name.lowercase(Locale.ROOT)
        return when {
            lower.contains("milk") -> 2
            lower.contains("paneer") -> 3
            lower.contains("coriander") || lower.contains("spinach") || lower.contains("palak") -> 2
            lower.contains("bread") || lower.contains("pav") -> 3
            lower.contains("curd") || lower.contains("dahi") -> 4
            lower.contains("tomato") -> 4
            lower.contains("banana") -> 3
            lower.contains("chicken") || lower.contains("fish") -> 1
            lower.contains("egg") -> 7
            lower.contains("potato") || lower.contains("onion") -> 14
            category == "Staples" -> 60
            category == "Dairy" -> 4
            category == "Vegetables" -> 5
            category == "Bakery" -> 3
            else -> 7
        }
    }

    private fun estimatePrice(name: String): Double {
        val lower = name.lowercase(Locale.ROOT)
        return when {
            lower.contains("milk") -> 60.0
            lower.contains("paneer") -> 95.0
            lower.contains("bread") -> 45.0
            lower.contains("tomato") -> 30.0
            lower.contains("potato") -> 35.0
            lower.contains("atta") -> 70.0
            lower.contains("rice") -> 110.0
            lower.contains("egg") -> 55.0
            lower.contains("dahi") || lower.contains("curd") -> 40.0
            else -> 50.0
        }
    }
}
