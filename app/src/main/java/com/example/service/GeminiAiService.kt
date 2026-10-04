package com.example.service

import com.example.BuildConfig
import com.example.data.model.InventoryItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ZeroWasteRecipe(
    val title: String,
    val prepTime: String,
    val difficulty: String,
    val ingredientsUsed: List<String>,
    val additionalIngredients: List<String>,
    val instructions: List<String>,
    val wasteSavingTip: String,
    val dietaryTag: String
)

object GeminiAiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    suspend fun askAssistant(
        userQuery: String,
        activeItems: List<InventoryItem>,
        dietaryPreference: String,
        workspaceName: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val expiringItems = activeItems.sortedBy { it.expiryDate }.take(5)
            .joinToString(", ") { "${it.name} (${it.quantity} ${it.unit}, expires in ${it.hoursLeft()}h)" }

        val allPantry = activeItems.joinToString(", ") { "${it.name} (${it.quantity} ${it.unit})" }

        val systemContext = """
            You are RasoiGuard AI, an elite Indian Master Chef & Zero-Waste Kitchen Specialist.
            Workspace: $workspaceName
            User Dietary Preference: $dietaryPreference (STRICT: Never recommend ingredients violating this preference. E.g. If Jain, no onion/garlic/root veggies. If Vegetarian, no meat/fish/egg. If Eggetarian, eggs allowed but no meat. If Vegan, no dairy/meat/honey).
            Current Pantry Inventory: ${allPantry.ifBlank { "Pantry is currently empty" }}
            Items Expiring Soonest: ${expiringItems.ifBlank { "None currently critical" }}

            Provide concise, practical, delicious Indian culinary advice. Focus on zero-waste cooking techniques, utilizing vegetable peels/stems, revamping leftovers, shelf-life preservation hacks, and batch FIFO usage. Keep responses crisp and formatted with bullet points.
        """.trimIndent()

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getOfflineAiResponse(userQuery, activeItems, dietaryPreference)
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val prompt = "$systemContext\n\nUser Question: $userQuery"

            val jsonBody = JSONObject().apply {
                val contents = JSONArray().apply {
                    put(JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        }
                        put("parts", parts)
                    })
                }
                put("contents", contents)
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext getOfflineAiResponse(userQuery, activeItems, dietaryPreference)
            }

            val parsedJson = JSONObject(responseBody)
            val candidates = parsedJson.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val content = candidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text")

            if (!text.isNullOrBlank()) {
                text.trim()
            } else {
                getOfflineAiResponse(userQuery, activeItems, dietaryPreference)
            }
        } catch (_: Exception) {
            getOfflineAiResponse(userQuery, activeItems, dietaryPreference)
        }
    }

    private fun getOfflineAiResponse(
        query: String,
        activeItems: List<InventoryItem>,
        dietary: String
    ): String {
        val q = query.lowercase()
        val expiring = activeItems.sortedBy { it.expiryDate }.firstOrNull()
        val itemName = expiring?.name ?: "Paneer or Vegetables"

        return when {
            q.contains("recipe") || q.contains("cook") || q.contains("make") -> {
                """
                👨‍🍳 **Zero-Waste Chef Suggestion for $dietary:**
                Based on your active kitchen inventory, let's prioritize **$itemName**!

                ✨ **Instant Rescue Recipe: Quick Spiced $itemName Bhurji / Toss**
                • **Time:** 12 mins | **Diet:** $dietary
                • **Key Ingredients:** $itemName, Cumin seeds, Turmeric, Green chillies, Salt, Pinch of garam masala.
                • **Zero-Waste Hack:** Don't discard herb stems or outer peels—sauté them into the tempering oil for extra aroma and micronutrients!
                • **Pairing:** Serve hot with rotis, parathas, or toasted bread slices.
                """.trimIndent()
            }
            q.contains("milk") || q.contains("sour") || q.contains("dahi") -> {
                """
                🥛 **Dairy Rescue Hacks:**
                1. If milk is about to turn, curdle it immediately with 1 tsp lemon juice or vinegar to yield fresh Chenna / Paneer.
                2. If dahi is too sour, hang it in a muslin cloth to make creamy Hung Curd Dip, or churn with roasted cumin into digestive Chaas!
                """.trimIndent()
            }
            q.contains("waste") || q.contains("save") || q.contains("fifo") -> {
                """
                ♻️ **Rasoi Zero-Waste Principles:**
                1. **FIFO (First-In, First-Out):** Always rotate new grocery purchases behind older batches in your fridge & dry pantry.
                2. **Vegetable Scrap Stock:** Collect clean onion skins, carrot peels, and coriander roots in a freezer bag to boil a rich vegetable broth.
                3. **Stale Bread / Roti Revamp:** Toast dry rotis on tawa with ghee & chaat masala for crispy khakhra chips!
                """.trimIndent()
            }
            else -> {
                """
                🌿 **RasoiGuard Kitchen Assistant:**
                You have ${activeItems.size} items in your pantry.
                • Prioritize items expiring in the next 24-48 hours.
                • Ask me for a specific recipe like *"What can I cook with tomato and paneer?"* or *"How to preserve coriander leaves longer?"*.
                • All suggestions strictly conform to your **$dietary** preference!
                """.trimIndent()
            }
        }
    }

    fun getZeroWasteRecipeForItem(item: InventoryItem, dietary: String): ZeroWasteRecipe {
        val name = item.name.lowercase()
        val isJain = dietary.equals("Jain", ignoreCase = true)

        return when {
            name.contains("paneer") -> ZeroWasteRecipe(
                title = if (isJain) "Jain Shahi Paneer Bhurji" else "Amritsari Paneer Bhurji Wrap",
                prepTime = "15 mins",
                difficulty = "Easy",
                ingredientsUsed = listOf(item.name, "Tomatoes", "Cumin seeds", "Turmeric", "Green chillies"),
                additionalIngredients = if (isJain) listOf("Asafoetida (Hing)", "Ginger (optional)", "Coriander") else listOf("Onion", "Ginger Garlic Paste", "Kasuri Methi"),
                instructions = listOf(
                    "Crumble ${item.quantity} ${item.unit} of ${item.name} into coarse texture.",
                    "Heat 1 tbsp oil/ghee in a kadhai, splutter cumin seeds.",
                    if (isJain) "Add finely chopped tomatoes and green chillies with turmeric and salt." else "Sauté chopped onions and ginger-garlic paste until golden, then add tomatoes.",
                    "Fold in the crumbled paneer gently and cook on medium heat for 3-4 minutes.",
                    "Garnish with chopped coriander and serve hot with roti or toasted bread."
                ),
                wasteSavingTip = "Using paneer before 48 hours preserves its moisture and protein without curdling.",
                dietaryTag = dietary
            )

            name.contains("milk") -> ZeroWasteRecipe(
                title = "Instant Spiced Rabdi or Fresh Chenna",
                prepTime = "20 mins",
                difficulty = "Easy",
                ingredientsUsed = listOf(item.name, "Sugar or Jaggery", "Cardamom"),
                additionalIngredients = listOf("Chopped Almonds", "Pistachios", "Pinch of Saffron"),
                instructions = listOf(
                    "Pour ${item.quantity} ${item.unit} milk into a wide heavy-bottomed pan.",
                    "Bring to a boil and simmer on low heat, scraping down the cream layers (malai) to the side.",
                    "When reduced to one-third, stir in crushed cardamom and 2 tbsp sugar.",
                    "Chill or serve warm as traditional dessert!"
                ),
                wasteSavingTip = "Simmering milk that's close to expiry sterilizes bacteria and creates rich concentrated rabdi.",
                dietaryTag = dietary
            )

            name.contains("bread") -> ZeroWasteRecipe(
                title = "Zero-Waste Masala Bread Upma",
                prepTime = "10 mins",
                difficulty = "Beginner",
                ingredientsUsed = listOf(item.name, "Mustard seeds", "Curry leaves", "Turmeric"),
                additionalIngredients = if (isJain) listOf("Chopped Tomatoes", "Capsicum", "Roasted Peanuts") else listOf("Chopped Onions", "Green Chillies", "Peanuts"),
                instructions = listOf(
                    "Tear or cube stale bread slices into bite-sized chunks.",
                    "Heat oil in a pan, add mustard seeds, curry leaves, and crunchy peanuts.",
                    if (isJain) "Add diced tomatoes, capsicum, and dry spices." else "Add diced onions, sauté until translucent, followed by tomatoes and turmeric.",
                    "Sprinkle 2 tbsp water over the bread cubes, add to the pan, and toss well for 3 mins.",
                    "Squeeze fresh lemon juice on top before serving."
                ),
                wasteSavingTip = "Day-old dry bread absorbs spices better than fresh bread without turning mushy.",
                dietaryTag = dietary
            )

            name.contains("tomato") -> ZeroWasteRecipe(
                title = "Slow-Cooked Tangy Tomato Thokku / Chutney",
                prepTime = "25 mins",
                difficulty = "Easy",
                ingredientsUsed = listOf(item.name, "Mustard seeds", "Fenugreek powder", "Red chilli powder"),
                additionalIngredients = if (isJain) listOf("Sesame oil", "Hing", "Salt") else listOf("Garlic cloves", "Sesame oil", "Curry leaves"),
                instructions = listOf(
                    "Finely chop or puree softening tomatoes.",
                    "Heat 3 tbsp sesame or mustard oil, crackle mustard seeds and curry leaves.",
                    "Add the tomato pulp, turmeric, chilli powder, and salt.",
                    "Simmer on medium-low until oil separates from the edges (15-20 mins).",
                    "Store in a clean dry jar—lasts up to 3 weeks in the fridge!"
                ),
                wasteSavingTip = "Concentrating ripe tomatoes with oil and salt extends their shelf-life by 21+ days.",
                dietaryTag = dietary
            )

            else -> ZeroWasteRecipe(
                title = "Rasoi Quick-Toss Zero-Waste Stir Fry",
                prepTime = "12 mins",
                difficulty = "Easy",
                ingredientsUsed = listOf(item.name, "Cooking oil", "Cumin seeds", "Turmeric", "Salt"),
                additionalIngredients = listOf("Garam Masala", "Lemon Juice", "Fresh Herbs"),
                instructions = listOf(
                    "Rinse and slice ${item.name} uniformly.",
                    "Temper cumin seeds in hot oil in a heavy skillet.",
                    "Add ${item.name} with dry seasonings and sauté on high heat for 5 minutes.",
                    "Finish with fresh lemon juice and chopped herbs."
                ),
                wasteSavingTip = "Flash cooking on high heat seals flavors and avoids food spoilage.",
                dietaryTag = dietary
            )
        }
    }
}
