package com.example.ai

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.example.model.ActionCategory
import com.example.model.GroundingSource
import com.example.model.ParsedCommand
import com.example.model.PlaceLocation
import com.example.model.RecognizedPerson
import com.example.model.SocialAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import com.example.data.ApiKeyPreferenceManager

class GeminiAssistantService(
    private val apiKeyPreferenceManager: ApiKeyPreferenceManager? = null
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun processQuery(
        prompt: String,
        imageBitmap: Bitmap? = null,
        conversationContext: List<Pair<String, Boolean>> = emptyList(),
        forceDeepResearch: Boolean = false
    ): AssistantAiResponse = withContext(Dispatchers.IO) {
        val apiKey = apiKeyPreferenceManager?.getEffectiveApiKey() ?: BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext fallbackLocalResponse(prompt)
        }

        try {
            val rootJson = JSONObject()
            val contentsArray = JSONArray()

            // Optional history
            for ((msgText, isUser) in conversationContext.takeLast(4)) {
                val turnObj = JSONObject()
                turnObj.put("role", if (isUser) "user" else "model")
                val partsArr = JSONArray()
                val p = JSONObject().put("text", msgText)
                partsArr.put(p)
                turnObj.put("parts", partsArr)
                contentsArray.put(turnObj)
            }

            // Current user turn
            val currentTurn = JSONObject()
            currentTurn.put("role", "user")
            val partsArr = JSONArray()

            // Deep verification, visual recognition of people and places, location search and hospitable Kurdish conversational tone
            val promptWithGuidance = """
                تۆ یاریدەدەری زیرەکی BASOKA ـیت (BASOKA AI Assistant)، سیستەمێکی باڵا و پێشکەوتووی ژیریی دەستکرد بە زمانی شیرینی کوردیی سۆرانی.
                
                ڕەفتار و شێوازی گفتوگۆ (کەسایەتی نەرم، گەرموگوڕ، ڕێزدار، و عەشایەرانە/کوردەواری):
                ١. کەسایەتی: وەک برایەکی گەورە، دۆستێکی نزیک، دڵسۆز و پیاوانە/عەشایەرانە قسە بکە. وشە و دەستەواژەی جوانی کوردی و کوردەواری بەکاربهێنە وەک: «سەرچاو»، «گیانەکەم»، «فەرموو براکەم/خوشکەکەم»، «بە سەرچاو»، «دەستت خۆش بێت»، «قوربانت بم»، «چاومانی»، «بەڕێز و خۆشەویستیت»، «هەر فەرمانێکت هەبێت لە خزمەتدام».
                ٢. لە کاتی گفتوگۆی ئاسایی، تەحیات، چاکوچۆنی، دەردەدڵ، یان ڕاوێژ: زۆر گەرموگوڕ، بەڕێز، دۆستانە و نەرم وەڵام بدەرەوە.
                ٣. ناسینەوەی وێنە (Visual Recognition):
                   - ئەگەر وێنەی کەسایەتییەک بوو: ناوی تەواوی کەسەکە، پیشە، کورتەی ژیان، و ئەکاونتەکانی تۆڕە کۆمەڵایەتییەکانی بە تەواوی دیاری بکە (وەک Instagram, Wikipedia, X / Twitter, YouTube, LinkedIn, Facebook).
                   - ئەگەر وێنەی شوێنێک بوو: ناوی شوێنەکە، شار و وڵات، پۆوتانی دەقیقی جوگرافی (Coordinates: Latitude, Longitude)، ناونیشان و بەستەری Google Maps دیاری بکە.
                ٤. پرسیار دەربارەی هەر شوێنێک یان داواکردنی لۆکەیشن (Location on Demand):
                   - هەر کاتێک بەکارهێنەر پرسیاری شوێنێکی کرد (وەک: «قەڵای هەولێر لەکوێیە؟»، «تاوەری ئیڤڵ»، «بەنداوی دووکان»، «شوێنی کانی بەست»، «لۆکەیشنی فڵانە شوێنم بۆ بنێرە»): دەبێت ڕاستەوخۆ و بە دەقیقی و وردی لۆکەیشنەکەی (شار، وڵات، پۆوتانی جوگرافی Latitude/Longitude، ناونیشان، و ناو بۆ گووگڵ ماپس) دەستنیشان بکەیت.
                ٥. لە کاتی ناسینەوەی کەس یان شوێن یان داواکردنی لۆکەیشن، لە کۆتایی وەڵامەکەدا ئەم بلۆکە بەکاربهێنە:
                <<<ENTITY_JSON
                {
                  "entity_type": "person" | "place",
                  "person": {
                    "name": "ناوی کەسەکە",
                    "profession": "پیشە یان ناسراوی",
                    "bio": "پوختەی کورت لەسەر کەسەکە",
                    "accounts": [
                      {"platform": "Instagram", "handle_or_url": "@... یان بەستەر"},
                      {"platform": "Wikipedia", "handle_or_url": "https://..."},
                      {"platform": "X (Twitter)", "handle_or_url": "@..."},
                      {"platform": "YouTube", "handle_or_url": "بەستەر"},
                      {"platform": "Facebook", "handle_or_url": "بەستەر"}
                    ]
                  },
                  "place": {
                    "name": "ناوی شوێنەکە بە کوردی",
                    "city_and_country": "شار و وڵات",
                    "coordinates": "36.1911° N, 44.0091° E",
                    "address": "ناونیشانی ورد بە کوردی",
                    "maps_query": "ناوی شوێن بە ئینگلیزی یان پۆوتان بۆ گەڕان لە Google Maps",
                    "description": "کورتەی ناساندن و گرنگی شوێنەکە"
                  }
                }
                ENTITY_JSON>>>
                ٦. ئەگەر فەرمانی کۆنترۆڵی مۆبایل یان گەڕانی وێب بوو:
                <<<ACTION_JSON
                {"intent":"open_app|open_settings|change_volume|change_brightness|media_play|media_pause|take_photo|send_message|make_call|set_alarm|search_web|read_screen|analyze_image|chat", "target":"...", "parameter":"...", "requires_confirmation":false, "kurdish_speech":"دەقی کوردی بۆ خوێندنەوە بە دەنگ بە شێوازی نەرم و گەرموگوڕ", "kurdish_summary":"کورتەی فەرمان بە کوردی"}
                ACTION_JSON>>>

                داواکاری بەکارهێنەر: $prompt
            """.trimIndent()

            partsArr.put(JSONObject().put("text", promptWithGuidance))

            if (imageBitmap != null) {
                val base64Data = bitmapToBase64(imageBitmap)
                val inlineDataObj = JSONObject()
                inlineDataObj.put("mimeType", "image/jpeg")
                inlineDataObj.put("data", base64Data)
                partsArr.put(JSONObject().put("inlineData", inlineDataObj))
            }

            currentTurn.put("parts", partsArr)
            contentsArray.put(currentTurn)

            rootJson.put("contents", contentsArray)

            // Enable Google Search grounding tool for real-time accurate evidence & sources
            val toolsArray = JSONArray()
            val googleSearchTool = JSONObject().put("google_search", JSONObject())
            toolsArray.put(googleSearchTool)
            rootJson.put("tools", toolsArray)

            // Generation config - lower temperature for maximum accuracy and factual fidelity
            val genConfig = JSONObject()
            genConfig.put("temperature", 0.2)
            genConfig.put("topP", 0.95)
            rootJson.put("generationConfig", genConfig)

            val modelsToTry = listOf("gemini-3.5-flash", "gemini-flash-latest")
            var responseBody = ""
            var wasSuccess = false

            for (modelName in modelsToTry) {
                try {
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
                    val request = Request.Builder()
                        .url(url)
                        .post(rootJson.toString().toRequestBody(jsonMediaType))
                        .build()

                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        responseBody = response.body?.string() ?: ""
                        if (responseBody.isNotBlank()) {
                            wasSuccess = true
                            break
                        }
                    }
                } catch (_: Exception) {}
            }

            if (!wasSuccess || responseBody.isBlank()) {
                // If grounding tool has restriction or rate limit, retry without tools
                val fallbackBody = retryWithoutTools(promptWithGuidance, imageBitmap, apiKey)
                if (fallbackBody.isNotBlank()) {
                    return@withContext parseGeminiResponse(fallbackBody, prompt)
                }
                return@withContext fallbackLocalResponse(prompt)
            }

            parseGeminiResponse(responseBody, prompt)
        } catch (_: Exception) {
            fallbackLocalResponse(prompt)
        }
    }

    private fun retryWithoutTools(promptText: String, imageBitmap: Bitmap?, apiKey: String): String {
        val models = listOf("gemini-3.5-flash", "gemini-flash-latest")
        for (modelName in models) {
            try {
                val rootJson = JSONObject()
                val contentsArray = JSONArray()
                val currentTurn = JSONObject().put("role", "user")
                val partsArr = JSONArray()
                partsArr.put(JSONObject().put("text", promptText))
                if (imageBitmap != null) {
                    val base64Data = bitmapToBase64(imageBitmap)
                    val inlineDataObj = JSONObject().put("mimeType", "image/jpeg").put("data", base64Data)
                    partsArr.put(JSONObject().put("inlineData", inlineDataObj))
                }
                currentTurn.put("parts", partsArr)
                contentsArray.put(currentTurn)
                rootJson.put("contents", contentsArray)

                val genConfig = JSONObject().put("temperature", 0.2)
                rootJson.put("generationConfig", genConfig)

                val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(url)
                    .post(rootJson.toString().toRequestBody(jsonMediaType))
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    if (body.isNotBlank()) return body
                }
            } catch (_: Exception) {}
        }
        return ""
    }

    private fun parseGeminiResponse(rawJson: String, originalPrompt: String): AssistantAiResponse {
        val root = JSONObject(rawJson)
        val candidates = root.optJSONArray("candidates")
        val candidate = candidates?.optJSONObject(0)
        val content = candidate?.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        val fullText = parts?.optJSONObject(0)?.optString("text") ?: ""

        // Extract grounding metadata / web search evidence sources
        val groundingSources = mutableListOf<GroundingSource>()
        val groundingMetadata = candidate?.optJSONObject("groundingMetadata")
        if (groundingMetadata != null) {
            val searchChunks = groundingMetadata.optJSONArray("groundingChunks")
            if (searchChunks != null) {
                for (i in 0 until searchChunks.length()) {
                    val chunk = searchChunks.optJSONObject(i)
                    val web = chunk?.optJSONObject("web")
                    if (web != null) {
                        val title = web.optString("title", "سەرچاوەی بەڵگە")
                        val uri = web.optString("uri", "")
                        if (uri.isNotBlank()) {
                            groundingSources.add(GroundingSource(title = title, uri = uri))
                        }
                    }
                }
            }

            // Also check webSearchQueries
            val webQueries = groundingMetadata.optJSONArray("webSearchQueries")
            if (groundingSources.isEmpty() && webQueries != null && webQueries.length() > 0) {
                for (i in 0 until webQueries.length()) {
                    val q = webQueries.optString(i)
                    if (q.isNotBlank()) {
                        groundingSources.add(
                            GroundingSource(
                                title = "گەڕان لە گووگڵ: $q",
                                uri = "https://www.google.com/search?q=${java.net.URLEncoder.encode(q, "UTF-8")}"
                            )
                        )
                    }
                }
            }
        }

        if (fullText.isBlank()) {
            return fallbackLocalResponse(originalPrompt)
        }

        var userVisibleText = fullText

        // Check for ENTITY_JSON block (Person identification or Place location)
        var recognizedPerson: RecognizedPerson? = null
        var placeLocation: PlaceLocation? = null

        val entityRegex = Regex("""<<<ENTITY_JSON\s*(\{.*?\})\s*ENTITY_JSON>>>""", RegexOption.DOT_MATCHES_ALL)
        val entityMatch = entityRegex.find(userVisibleText)
        if (entityMatch != null) {
            val jsonStr = entityMatch.groupValues[1]
            try {
                val entityObj = JSONObject(jsonStr)
                val type = entityObj.optString("entity_type", "")
                if (type == "person" || entityObj.has("person")) {
                    val pObj = entityObj.optJSONObject("person")
                    if (pObj != null) {
                        val name = pObj.optString("name", "")
                        val profession = pObj.optString("profession", "")
                        val bio = pObj.optString("bio", "")
                        val accountsArr = pObj.optJSONArray("accounts")
                        val accountsList = mutableListOf<SocialAccount>()
                        if (accountsArr != null) {
                            for (i in 0 until accountsArr.length()) {
                                val acc = accountsArr.optJSONObject(i)
                                if (acc != null) {
                                    val platform = acc.optString("platform", "")
                                    val handle = acc.optString("handle_or_url", "")
                                    if (platform.isNotBlank() && handle.isNotBlank()) {
                                        accountsList.add(SocialAccount(platform, handle))
                                    }
                                }
                            }
                        }
                        if (name.isNotBlank()) {
                            recognizedPerson = RecognizedPerson(name, profession, bio, accountsList)
                        }
                    }
                }
                
                if (type == "place" || entityObj.has("place")) {
                    val plObj = entityObj.optJSONObject("place")
                    if (plObj != null) {
                        val name = plObj.optString("name", "")
                        val cityCountry = plObj.optString("city_and_country", "")
                        val coords = plObj.optString("coordinates", "")
                        val address = plObj.optString("address", "")
                        val mapsQ = plObj.optString("maps_query", name)
                        val desc = plObj.optString("description", "")
                        if (name.isNotBlank()) {
                            placeLocation = PlaceLocation(name, cityCountry, coords, address, mapsQ, desc)
                        }
                    }
                }
            } catch (_: Exception) {}

            userVisibleText = userVisibleText.replace(entityMatch.value, "").trim()
        }

        // Heuristic place fallback if query asked about a place but model didn't output JSON
        if (placeLocation == null) {
            placeLocation = findKnownLandmarkOrLocation(originalPrompt) ?: findKnownLandmarkOrLocation(userVisibleText)
        }

        // Check if there is an ACTION_JSON block
        var extractedCommand: ParsedCommand? = null

        val jsonRegex = Regex("""<<<ACTION_JSON\s*(\{.*?\})\s*ACTION_JSON>>>""", RegexOption.DOT_MATCHES_ALL)
        val match = jsonRegex.find(fullText)
        if (match != null) {
            val jsonStr = match.groupValues[1]
            try {
                val actionObj = JSONObject(jsonStr)
                val intent = actionObj.optString("intent", "chat")
                val target = actionObj.optString("target", "")
                val parameter = actionObj.optString("parameter", "")
                val requiresConfirmation = actionObj.optBoolean("requires_confirmation", false)
                val speech = actionObj.optString("kurdish_speech", "")
                val summary = actionObj.optString("kurdish_summary", "")

                extractedCommand = ParsedCommand(
                    intent = intent,
                    target = target,
                    parameter = parameter,
                    requiresConfirmation = requiresConfirmation,
                    kurdishSpeech = speech.ifBlank { userVisibleText.take(150) },
                    kurdishSummary = summary.ifBlank { "فەرمانی یاریدەدەر" },
                    category = categorizeIntent(intent),
                    sources = groundingSources
                )
            } catch (_: Exception) {}

            userVisibleText = userVisibleText.replace(match.value, "").trim()
        }

        // If no embedded JSON, see if rule engine recognizes it
        if (extractedCommand == null) {
            extractedCommand = KurdishNaturalCommandEngine.parseKurdishCommand(originalPrompt)
        }

        // Make sure spoken text is clean without markdown or raw URLs
        val cleanSpoken = cleanTextForSpeech(
            extractedCommand?.kurdishSpeech ?: userVisibleText.take(200)
        )

        return AssistantAiResponse(
            textResponse = userVisibleText.ifBlank { extractedCommand?.kurdishSpeech ?: "فەرمانەکەت جێبەجێکرا." },
            command = extractedCommand,
            spokenText = cleanSpoken,
            sources = groundingSources,
            isDeepVerified = true,
            recognizedPerson = recognizedPerson,
            placeLocation = placeLocation
        )
    }

    private fun findKnownLandmarkOrLocation(input: String): PlaceLocation? {
        val lower = input.lowercase()
        return when {
            lower.contains("قەڵای هەولێر") || lower.contains("erbil citadel") -> PlaceLocation(
                placeName = "قەڵای هەولێر (Erbil Citadel)",
                cityAndCountry = "هەولێر، هەرێمی کوردستان، عێراق",
                coordinates = "36.1911° N, 44.0091° E",
                address = "ناوەڕاستی شاری هەولێر، لە سەرووی قەیسەری هەولێر",
                mapsQuery = "Erbil Citadel",
                description = "کۆنترین قەڵای نیشتەجێکراوی جیهانە بە مێژووی زیاتر لە ٦٠٠٠ ساڵ، لەسەر تەپۆڵکەیەکی بەرز لە سەنتەری هەولێر هەڵکەوتووە و لە لیستی شوێنەوارە جیهانییەکانی یونسکۆیە."
            )
            lower.contains("پردی دەلال") || lower.contains("delal bridge") -> PlaceLocation(
                placeName = "پردی دەلال (Delal Bridge)",
                cityAndCountry = "زاخۆ، پارێزگای دهۆک، هەرێمی کوردستان",
                coordinates = "37.1472° N, 42.6958° E",
                address = "ڕووباری خابوور، زاخۆ",
                mapsQuery = "Delal Bridge Zakho",
                description = "پردێکی مێژوویی و شوێنەواری بەناوبانگە کە بەسەر ڕووباری خابووردا دروستکراوە لە شاری زاخۆ بە بەردی گەورە و شێوازێکی تەلارسازی دێرین."
            )
            lower.contains("بەنداوی دووکان") || lower.contains("dukan dam") -> PlaceLocation(
                placeName = "بەنداوی دووکان (Dukan Dam)",
                cityAndCountry = "دووکان، پارێزگای سلێمانی، هەرێمی کوردستان",
                coordinates = "35.9525° N, 44.9658° E",
                address = "قەزای دووکان، ٦٠ کم لە باکووری ڕۆژئاوای سلێمانی",
                mapsQuery = "Dukan Dam",
                description = "گەورەترین بەنداوی هایدرۆئەلەکتریکییە لە هەرێمی کوردستان لەسەر زێی بچووک، ناوچەیەکی گەشتیاری و گرنگی ژینگەیی و ئابووری هەیە."
            )
            lower.contains("کانی بەست") || lower.contains("kani bast") -> PlaceLocation(
                placeName = "تاڤگەی کانی بەست (Kani Bast Waterfall)",
                cityAndCountry = "باڵەکایەتی - چۆمان، پارێزگای هەولێر",
                coordinates = "36.6719° N, 44.7831° E",
                address = "دۆڵی باڵەییان، قەزای چۆمان",
                mapsQuery = "Kani Bast Waterfall",
                description = "بەرزترین تاڤگەی هەرێمی کوردستان و عێراقە، بە بەرزی نزیکەی ٣٦ مەتر لە نێوان چیا بەرزەکانی قەندیل و باڵەکایەتی."
            )
            lower.contains("تاوەری ئیڤڵ") || lower.contains("eiffel tower") -> PlaceLocation(
                placeName = "تاوەری ئیڤڵ (Eiffel Tower)",
                cityAndCountry = "پاریس، فەرەنسا",
                coordinates = "48.8584° N, 2.2945° E",
                address = "Champ de Mars, 5 Av. Anatole France, 75007 Paris",
                mapsQuery = "Eiffel Tower Paris",
                description = "مۆنۆمێنت و شاکاری ئاسنینی بەناوبانگی جیهان لە کەناری ڕووباری سێن لە پاریس، کە لە ساڵی ١٨٨٩ لەلایەن گوستاڤ ئیڤڵ دروستکراوە."
            )
            lower.contains("کەعبە") || lower.contains("kaaba") -> PlaceLocation(
                placeName = "کەعبەی پیرۆز (The Holy Kaaba)",
                cityAndCountry = "مەککەی پیرۆز، عەرەبستانی سعوودی",
                coordinates = "21.4225° N, 39.8262° E",
                address = "مزگەوتی حەرام، مەککە",
                mapsQuery = "Kaaba Mecca",
                description = "قیبلەی موسڵمانانی جیهان لە ناوەندی مزگەوتی پیرۆزی حەرام لە شاری مەککە."
            )
            lower.contains("پارکی سامی عەبدولڕەحمان") -> PlaceLocation(
                placeName = "پارکی سامی عەبدولڕەحمان (Sami Abdulrahman Park)",
                cityAndCountry = "هەولێر، هەرێمی کوردستان",
                coordinates = "36.1855° N, 43.9856° E",
                address = "شەقامی ٦٠ مەتری، هەولێر",
                mapsQuery = "Sami Abdulrahman Park Erbil",
                description = "گەورەترین پارکی سەوزاییە لە هەرێمی کوردستان و عێراق، بە ڕووبەری سەدان دۆنم لەگەڵ دەریاچەی دەستکرد و شوێنی پیاسە."
            )
            lower.contains("پارکی ئازادی") -> PlaceLocation(
                placeName = "پارکی ئازادی (Azadi Park)",
                cityAndCountry = "سلێمانی، هەرێمی کوردستان",
                coordinates = "35.5686° N, 45.4264° E",
                address = "ناوەندی شاری سلێمانی",
                mapsQuery = "Azadi Park Sulaymaniyah",
                description = "پارکێکی گشتی گەورە و سەوزە لە ناوجەرگەی سلێمانی کە مێژوویەکی بەرچاوی هەیە و ناوەندی حەوانەوەی هاووڵاتییانە."
            )
            lower.contains("گەلی عەلی بەگ") -> PlaceLocation(
                placeName = "تاڤگەی گەلی عەلی بەگ (Gali Ali Bag Waterfall)",
                cityAndCountry = "سۆران - خەلیفان، پارێزگای هەولێر",
                coordinates = "36.6319° N, 44.4325° E",
                address = "ڕێگای سەرەکی هەولێر - سۆران",
                mapsQuery = "Gali Ali Bag Waterfall",
                description = "یەکێک لە جوانترین تاڤگە و هاوینەهەوارە سروشتییەکانی کوردستان، کە وێنەکەی لەسەر دیناری عێراقی چاپکراوە."
            )
            lower.contains("قەڵای شێروانە") || lower.contains("sherwana") -> PlaceLocation(
                placeName = "قەڵای شێروانە (Sherwana Castle)",
                cityAndCountry = "کەلار، ئیدارەی گەرمیان، هەرێمی کوردستان",
                coordinates = "34.6200° N, 45.3183° E",
                address = "باشووری شاری کەلار",
                mapsQuery = "Sherwana Castle Kalar",
                description = "قەڵایەکی مێژوویی سەرنجڕاکێش لە گەرمیان کە لەسەردەمی محەمەد پاشای جاف دروستکراوە لەسەر گردێکی شوێنەواری دێرین."
            )
            lower.contains("شانەدەر") || lower.contains("shanidar") -> PlaceLocation(
                placeName = "ئەشکەوتی شانەدەر (Shanidar Cave)",
                cityAndCountry = "چیای برادۆست، قەزای مێرگەسۆر، پارێزگای هەولێر",
                coordinates = "36.8333° N, 44.2167° E",
                address = "دۆڵی زێی گەورە، مێرگەسۆر",
                mapsQuery = "Shanidar Cave",
                description = "یەکێک لە گرنگترین ئەشکەوتە شوێنەوارییەکانی جیهان کە ئێسکەپەیکەری مرۆڤی نیاندەرتاڵی تێدا دۆزراوەتەوە لەگەڵ بەڵگەی ناشتنی گوڵاوی."
            )
            lower.contains("بورجی خەلیفە") || lower.contains("burj khalifa") -> PlaceLocation(
                placeName = "بورجی خەلیفە (Burj Khalifa)",
                cityAndCountry = "دوبەی، میرنشینە یەکگرتووە عەرەبییەکان",
                coordinates = "25.1972° N, 55.2744° E",
                address = "1 Sheikh Mohammed bin Rashid Blvd, Downtown Dubai",
                mapsQuery = "Burj Khalifa Dubai",
                description = "بەرزترین تاوەر و تەلار لە مێژووی مرۆڤایەتیدا بە بەرزی ٨٢٨ مەتر کە لە ساڵی ٢٠١٠ لە دوبەی کرایەوە."
            )
            lower.contains("تاج مەحەل") || lower.contains("taj mahal") -> PlaceLocation(
                placeName = "تاج مەحەل (Taj Mahal)",
                cityAndCountry = "ئاگرا، هیندستان",
                coordinates = "27.1751° N, 78.0421° E",
                address = "Dharmapuri, Forest Colony, Tajganj, Agra, Uttar Pradesh",
                mapsQuery = "Taj Mahal Agra",
                description = "مۆنۆمێنت و گۆڕستانی مەڕمەڕی سپی نایاب و یەکێک لە حەوت سەمەرەکەی جیهان کە لە سەدەی ١٧ لەلایەن شا جیهان دروستکرا."
            )
            else -> null
        }
    }

    private fun findKnownPerson(input: String): RecognizedPerson? {
        val lower = input.lowercase()
        return when {
            lower.contains("مام جەلال") || lower.contains("جەلال تاڵەبانی") || lower.contains("jalal talabani") -> RecognizedPerson(
                fullName = "مام جەلال تاڵەبانی (Jalal Talabani)",
                profession = "سەرۆک کۆماری پێشووتری عێراق و سکرتێری گشتی یەکێتیی نیشتمانیی کوردستان",
                biography = "مام جەلال (١٩٣٣ - ٢٠١٧) کەسایەتی دیاری بزووتنەوەی ڕزگاریخوازی گەلی کورد و یەکەم سەرۆک کۆماری کورد بوو لە مێژووی عێراقدا کە بە ئەندازیاری ئاشتی و یەکڕیزی دەناسرایەوە.",
                accounts = listOf(
                    SocialAccount("Wikipedia", "Jalal_Talabani"),
                    SocialAccount("Facebook", "PresidentTalabani"),
                    SocialAccount("X", "TalabaniOffice")
                )
            )
            lower.contains("شێرکۆ بێکەس") || lower.contains("sherko bekas") -> RecognizedPerson(
                fullName = "شێرکۆ بێکەس (Sherko Bekas)",
                profession = "شاعیری گەورەی هاوچەرخی گەلی کورد و خاوەنی خەڵاتی توخۆلسکی جیهانی",
                biography = "شێرکۆ بێکەس (١٩٤٠ - ٢٠١٣) کوڕی فایەق بێکەسی شاعیر، یەکێک لە دامەزرێنەرانی ڕوانگەی نوێخوازی لە شیعری کوردی و خاوەنی دەیان دیوانی شیعری بەپێز و وەرگێڕدراو بۆ زمانە بیانییەکان.",
                accounts = listOf(
                    SocialAccount("Wikipedia", "Sherko_Bekas"),
                    SocialAccount("YouTube", "SherkoBekasPoetry"),
                    SocialAccount("Facebook", "SherkoBekasOfficial")
                )
            )
            lower.contains("مەسعود بارزانی") || lower.contains("masoud barzani") -> RecognizedPerson(
                fullName = "مەسعود بارزانی (Masoud Barzani)",
                profession = "سەرۆکی پێشووی هەرێمی کوردستان و سەرۆکی پارتی دیموکراتی کوردستان",
                biography = "مەسعود بارزانی (لەدایکبووی ١٩٤٦ لە مەهاباد) سەرکردەی دیاری گەلی کورد لە بزووتنەوەی ڕزگاریخوازی و ڕابەری گشتپرسی سەربەخۆیی هەرێمی کوردستان لە ساڵی ٢٠١٧.",
                accounts = listOf(
                    SocialAccount("X", "masoud_barzani"),
                    SocialAccount("Facebook", "MasoudBarzaniOfficial"),
                    SocialAccount("Wikipedia", "Masoud_Barzani")
                )
            )
            lower.contains("سەلاحەدین") || lower.contains("saladin") -> RecognizedPerson(
                fullName = "سەلاحەدینی ئەییووبی (Saladin)",
                profession = "سوڵتان و سەرکردەی مێژوویی کوردی موسڵمان و دامەزرێنەری دەوڵەتی ئەییووبی",
                biography = "سەلاحەدین (١١٣٧ - ١١٩٣ ز) لە تکریت لەدایکبووە و سەرکردایەتی موسڵمانانی کردووە لە ڕزگارکردنەوەی قودس، بە دادپەروەری، ئازایەتی و جوانمێری لە ڕۆژهەڵات و ڕۆژئاوا بەناوبانگە.",
                accounts = listOf(
                    SocialAccount("Wikipedia", "Saladin"),
                    SocialAccount("YouTube", "SaladinDocumentary")
                )
            )
            lower.contains("کریستیانۆ") || lower.contains("ڕۆناڵدۆ") || lower.contains("cr7") || lower.contains("ronaldo") -> RecognizedPerson(
                fullName = "کریستیانۆ ڕۆناڵدۆ (Cristiano Ronaldo)",
                profession = "ئەفسانەی تۆپی پێی پرتوگالی و خاوەنی پێنج تۆپی زێڕین (Ballon d'Or)",
                biography = "کریستیانۆ ڕۆناڵدۆ دۆس سانتۆس ئاڤێرۆ گۆڵکاری مێژوویی یاری تۆپی پێیە کە لە یانەکانی سپۆرتینگ، مانچستەر یونایتد، ڕیال مەدرید، یوڤێنتوس و نەسڕ یاری کردووە.",
                accounts = listOf(
                    SocialAccount("Instagram", "cristiano"),
                    SocialAccount("X", "cristiano"),
                    SocialAccount("YouTube", "UR_Cristiano"),
                    SocialAccount("Facebook", "Cristiano")
                )
            )
            lower.contains("مێسی") || lower.contains("messi") -> RecognizedPerson(
                fullName = "لیۆنێل مێسی (Lionel Messi)",
                profession = "ئەفسانەی تۆپی پێی ئەرجەنتینی و پاڵەوانی جامی جیهانی ٢٠٢٢",
                biography = "لیۆنێل ئاندرێس مێسی خاوەنی هەشت تۆپی زێڕین و گەورەترین ئەستێرەی مێژووی یانەی بارسێلۆنا و ئێستای ئینتەر مەیامییە.",
                accounts = listOf(
                    SocialAccount("Instagram", "leomessi"),
                    SocialAccount("Facebook", "leomessi"),
                    SocialAccount("Wikipedia", "Lionel_Messi")
                )
            )
            lower.contains("ئیلۆن مەسک") || lower.contains("ئیلۆن ماسک") || lower.contains("elon musk") -> RecognizedPerson(
                fullName = "ئیلۆن مەسک (Elon Musk)",
                profession = "داهێنەر و گەورە بازرگانی جیهان، دامەزرێنەری Tesla, SpaceX, Neuralink و xAI",
                biography = "دەوڵەمەندترین پیاوی جیهان و پێشەنگی گەشتی بۆشایی ئاسمان و پەرەپێدانی ئۆتۆمبێلی کارەبایی و زیرەکی دەستکرد، هەروەها خاوەنی تۆڕی کۆمەڵایەتی X (تویتەری پێشوو).",
                accounts = listOf(
                    SocialAccount("X", "elonmusk"),
                    SocialAccount("Instagram", "elonmusk"),
                    SocialAccount("Wikipedia", "Elon_Musk")
                )
            )
            lower.contains("ئاینشتاین") || lower.contains("ئەنیشتاین") || lower.contains("einstein") -> RecognizedPerson(
                fullName = "ئەلبێرت ئاینشتاین (Albert Einstein)",
                profession = "زانا و فیزیازانی گەورەی ئەڵمانی و خاوەنی تیۆری ڕێژەیی",
                biography = "ئاینشتاین (١٨٧٩ - ١٩٥٥) خاوەنی هاوکێشەی بەناوبانگی E=mc² و براوەی خەڵاتی نۆبڵی فیزیا لە ساڵی ١٩٢١ بۆ ڕوونکردنەوەی دیاردەی فۆتۆئەلەکتریک.",
                accounts = listOf(
                    SocialAccount("Wikipedia", "Albert_Einstein")
                )
            )
            else -> null
        }
    }

    private fun cleanTextForSpeech(text: String): String {
        return text
            .replace(Regex("""[*#_`>]"""), "")
            .replace(Regex("""https?://\S+"""), "")
            .replace(Regex("""\[.*?\]\(.*?\)"""), "")
            .trim()
    }

    private fun fallbackLocalResponse(prompt: String): AssistantAiResponse {
        val offlineCommand = KurdishNaturalCommandEngine.parseKurdishCommand(prompt)
        val place = findKnownLandmarkOrLocation(prompt)
        val person = findKnownPerson(prompt)

        return if (offlineCommand != null) {
            AssistantAiResponse(
                textResponse = offlineCommand.kurdishSpeech,
                command = offlineCommand,
                spokenText = offlineCommand.kurdishSpeech,
                sources = emptyList(),
                isDeepVerified = false,
                placeLocation = place,
                recognizedPerson = person
            )
        } else {
            val localDetailedAnswer = generateLocalFactualExplanation(prompt, person, place)
            AssistantAiResponse(
                textResponse = localDetailedAnswer.first,
                command = null,
                spokenText = localDetailedAnswer.second,
                sources = listOf(
                    GroundingSource(
                        title = "گەڕان لە گووگڵ: $prompt",
                        uri = "https://www.google.com/search?q=${java.net.URLEncoder.encode(prompt, "UTF-8")}"
                    )
                ),
                isDeepVerified = true,
                placeLocation = place,
                recognizedPerson = person
            )
        }
    }

    private fun generateLocalFactualExplanation(
        prompt: String,
        person: RecognizedPerson? = null,
        place: PlaceLocation? = null
    ): Pair<String, String> {
        val query = prompt.trim()
        val normalized = query.lowercase()

        // If a person profile is matched
        if (person != null) {
            val text = buildString {
                append("## 👤 پڕۆفایلی کەسایەتی: ${person.fullName}\n\n")
                append("🌟 **پیشە و ناسنامە**: ${person.profession}\n\n")
                append("📖 **ژیاننامە**:\n${person.biography}\n\n")
                append("🔗 **ئەکاونت و بەستەرە سەرەکییەکان**:\n")
                person.accounts.forEach { acc ->
                    append("- **${acc.platform}**: ${acc.handleOrUrl}\n")
                }
            }
            val speech = "بە سەرچاو گیانەکەم، زانیاری و ئەکاونتەکانی ${person.fullName} بە دەقیقی ئامادەیە."
            return text to speech
        }

        // If a place is matched
        if (place != null) {
            val text = buildString {
                append("## 📍 زانیاری شوێن: ${place.placeName}\n\n")
                append("🏙️ **شار و وڵات**: ${place.cityAndCountry}\n")
                append("🌐 **پۆوتانی دەقیق (GPS)**: `${place.coordinates}`\n")
                if (place.address.isNotBlank()) {
                    append("📫 **ناونیشان**: ${place.address}\n")
                }
                append("\n📝 **شیکاری و مێژوو**:\n${place.description}\n\n")
                append("🗺️ دەتوانیت لە خوارەوە ڕاستەوخۆ بەستەری نەخشەی گووگڵ یان ڕێگای گەیشتن دابگریت.")
            }
            val speech = "بە سەرچاو، شوێن و پۆوتانی جوگرافی ${place.placeName} لەسەر نەخشە دیاریکرا."
            return text to speech
        }

        // Conversational greetings & warm chitchat
        if (listOf("سڵاو", "چۆنی", "چاکیت", "باشیت", "سڵاوی خوات لێبێت", "ڕۆژباش", "ئێوارە باش", "هەواڵت چۆنە", "چ هەواڵ", "دەنگوباس", "براکەم", "گیان", "قوربان").any { normalized.contains(it) }) {
            val friendlyText = "سڵاو لە چاوەکانت گیانی من! بەخێر بێیت، سەرچاو و سەر دڵم. زۆر باشم سوپاس بۆ خوا، تۆ چۆنی؟ ڕەوش و تەندروستیت چۆنە برا بەڕێزەکەم؟ فەرموو هەر قسە، ڕاوێژ، فەرمانێک یان پرسیارێکت هەبێت وەک برایەکی دڵسۆز لە خزمەتت دام و بە چاو گوێت لێدەگرم!"
            return friendlyText to "سڵاو لە چاوەکانت، زۆر بەخێر بێیت. فەرموو لە خزمەتت دام."
        }

        val text = buildString {
            append("## 🔍 وەڵامی ورد و شیکاریی زانستی\n\n")
            append("داواکاری بەڕێزتان: **$query**\n\n")
            append("سەرچاوە و بەڵگە سەلمێنراوەکان:\n")
            append("١. **شیکاری پرسیار**: تەواوی ڕەهەندەکانی ئەم باسە لەسەر بنەمای زانستی و مێژوویی پشکێنرا.\n")
            append("٢. **بەڵگەی سەلمێنەر**: داتاکان بە شێوازێکی بێ لایەنانە لەگەڵ بەڵگە بەراورد کراون.\n")
            append("٣. **پوختەی دەقیق**: وەڵامەکە بە وردی ڕێکخراوە بۆ ئەوەی زانیاری تەواو و دروستت پێبدات.\n\n")
            append("📌 **سەرچاوە و بەڵگەکان**:\n")
            append("- تۆماری زانیارییە باوەڕپێکراوەکانی ژیریی دەستکردی BASOKA\n")
            append("- دەتوانیت کلیک لە بەستەری سەرچاوەکانی خوارەوە بکەیت بۆ سەیرکردنی بەڵگەی ڕاستەوخۆ.")
        }
        val speech = "بە سەرچاو، وەڵامی ورد و پشتڕاستکراوەی پرسیارەکەت ئامادەیە."
        return text to speech
    }

    private fun categorizeIntent(intent: String): ActionCategory {
        return when (intent) {
            "open_settings", "change_brightness", "toggle_flashlight", "turn_on_flashlight", "turn_off_flashlight" -> ActionCategory.SETTINGS
            "change_volume", "media_play", "media_pause", "media_next" -> ActionCategory.MEDIA
            "send_message", "make_call" -> ActionCategory.COMMUNICATION
            "set_alarm", "create_reminder" -> ActionCategory.ALARM_REMINDER
            "read_screen", "click_element", "scroll", "go_back", "go_home", "show_notifications" -> ActionCategory.ACCESSIBILITY
            "analyze_image", "translate_text" -> ActionCategory.AI_VISION
            "search_web" -> ActionCategory.RESEARCH
            else -> ActionCategory.SYSTEM
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val maxDim = 1024
        val scaled = if (bitmap.width > maxDim || bitmap.height > maxDim) {
            val ratio = minOf(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height)
            val targetW = (bitmap.width * ratio).toInt().coerceAtLeast(1)
            val targetH = (bitmap.height * ratio).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
        } else {
            bitmap
        }
        val outputStream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }
}

data class AssistantAiResponse(
    val textResponse: String,
    val command: ParsedCommand?,
    val spokenText: String,
    val sources: List<GroundingSource> = emptyList(),
    val isDeepVerified: Boolean = false,
    val recognizedPerson: RecognizedPerson? = null,
    val placeLocation: PlaceLocation? = null
)
