package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.accessibility.BasitAccessibilityService
import com.example.ai.GeminiAssistantService
import com.example.ai.KurdishNaturalCommandEngine
import com.example.data.ApiKeyPreferenceManager
import com.example.data.local.BasitDatabase
import com.example.data.local.ConversationEntity
import com.example.executor.PhoneActionExecutor
import com.example.model.ActionCategory
import com.example.model.AssistantState
import com.example.model.ChatMessage
import com.example.model.ExecutionStatus
import com.example.model.ParsedCommand
import com.example.model.PermissionItem
import com.example.model.ScreenHierarchyData
import com.example.permission.PermissionManager
import com.example.speech.KurdishSpeechManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = BasitDatabase.getDatabase(application)
    private val conversationDao = db.conversationDao()
    private val executor = PhoneActionExecutor(application)
    private val permissionManager = PermissionManager(application)
    val apiKeyManager = ApiKeyPreferenceManager(application)
    private val geminiService = GeminiAssistantService(apiKeyManager)

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _assistantState = MutableStateFlow(AssistantState.IDLE)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _statusText = MutableStateFlow("ئامادەیە بۆ گوێگرتن")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _recognizedKurdishSpeech = MutableStateFlow("")
    val recognizedKurdishSpeech: StateFlow<String> = _recognizedKurdishSpeech.asStateFlow()

    private val _isHandsFreeMode = MutableStateFlow(false)
    val isHandsFreeMode: StateFlow<Boolean> = _isHandsFreeMode.asStateFlow()

    private val _permissions = MutableStateFlow<List<PermissionItem>>(emptyList())
    val permissions: StateFlow<List<PermissionItem>> = _permissions.asStateFlow()

    private val _screenData = MutableStateFlow<ScreenHierarchyData?>(null)
    val screenData: StateFlow<ScreenHierarchyData?> = _screenData.asStateFlow()

    private val _showPermissionSheet = MutableStateFlow(false)
    val showPermissionSheet: StateFlow<Boolean> = _showPermissionSheet.asStateFlow()

    private val _showScreenReaderSheet = MutableStateFlow(false)
    val showScreenReaderSheet: StateFlow<Boolean> = _showScreenReaderSheet.asStateFlow()

    private val _showApiKeyDialog = MutableStateFlow(false)
    val showApiKeyDialog: StateFlow<Boolean> = _showApiKeyDialog.asStateFlow()

    private val _customApiKey = MutableStateFlow(apiKeyManager.getCustomApiKey())
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _hasAutomaticKey = MutableStateFlow(apiKeyManager.hasAutomaticKey())
    val hasAutomaticKey: StateFlow<Boolean> = _hasAutomaticKey.asStateFlow()

    private val _selectedImageBitmap = MutableStateFlow<Bitmap?>(null)
    val selectedImageBitmap: StateFlow<Bitmap?> = _selectedImageBitmap.asStateFlow()

    private var speechManager: KurdishSpeechManager? = null

    val audioRms: StateFlow<Float>
        get() = speechManager?.audioRms ?: MutableStateFlow(0f)

    init {
        initSpeechManager()
        loadConversationHistory()
        refreshPermissions()
    }

    fun toggleApiKeyDialog(show: Boolean) {
        _showApiKeyDialog.value = show
        if (show) {
            _customApiKey.value = apiKeyManager.getCustomApiKey()
            _hasAutomaticKey.value = apiKeyManager.hasAutomaticKey()
        }
    }

    fun saveCustomApiKey(key: String) {
        apiKeyManager.setCustomApiKey(key)
        _customApiKey.value = apiKeyManager.getCustomApiKey()
        _showApiKeyDialog.value = false
        val msg = if (key.isNotBlank()) "کلیلی تایبەت بە سەرکەوتوویی پاشەکەوت کرا." else "سیستەم دەگەڕێتەوە سەر کلیلی ئۆتۆماتیکی."
        _statusText.value = msg
    }

    fun clearCustomApiKey() {
        apiKeyManager.clearCustomApiKey()
        _customApiKey.value = ""
        _showApiKeyDialog.value = false
        _statusText.value = "کلیلی تایبەت سڕایەوە، سیستەم کلیلی ئۆتۆماتیکی بەکاردێنێت."
    }

    private fun initSpeechManager() {
        speechManager = KurdishSpeechManager(
            context = getApplication(),
            onSpeechResult = { recognizedText ->
                _recognizedKurdishSpeech.value = recognizedText
                processUserInput(recognizedText)
            },
            onSpeechPartial = { partial ->
                _recognizedKurdishSpeech.value = partial
                _statusText.value = "دەبیستێت: $partial"
            },
            onError = { errorMsg ->
                _assistantState.value = AssistantState.IDLE
                _statusText.value = errorMsg
            }
        )
    }

    private fun loadConversationHistory() {
        viewModelScope.launch {
            conversationDao.getAllMessages().collect { entities ->
                if (entities.isEmpty()) {
                    // Seed initial welcome message in Sorani Kurdish with hospitable warmth
                    val welcomeMsg = ChatMessage(
                        text = "سڵاو لە چاوەکانت گیانی من! بەخێر بێیت بۆ لای BASOKA. من یاریدەدەر و برای دڵسۆزی تۆم؛ لە گفتوگۆی نەرم و کوردەواری، وەڵامی وردی زانستی بە سەرچاوەوە، تا کۆنترۆڵکردنی مۆبایلەکەت، فەرموو لەسەر چاوم لە خزمەتت دام.",
                        isUser = false,
                        command = null,
                        status = ExecutionStatus.SUCCESS
                    )
                    _messages.value = listOf(welcomeMsg)
                } else {
                    _messages.value = entities.map { entity ->
                        ChatMessage(
                            id = entity.id,
                            text = entity.text,
                            isUser = entity.isUser,
                            timestamp = entity.timestamp,
                            command = if (entity.intentName != null) {
                                ParsedCommand(
                                    intent = entity.intentName,
                                    target = entity.intentTarget ?: "",
                                    parameter = entity.intentParam ?: ""
                                )
                            } else null,
                            status = try {
                                ExecutionStatus.valueOf(entity.status)
                            } catch (_: Exception) {
                                ExecutionStatus.IDLE
                            },
                            resultMessage = entity.resultMessage,
                            attachmentUri = entity.attachmentUri
                        )
                    }
                }
            }
        }
    }

    fun refreshPermissions() {
        _permissions.value = permissionManager.getPermissionsList()
    }

    fun togglePermissionSheet(show: Boolean) {
        _showPermissionSheet.value = show
        if (show) refreshPermissions()
    }

    fun toggleScreenReaderSheet(show: Boolean) {
        _showScreenReaderSheet.value = show
        if (show) {
            inspectScreen()
        }
    }

    fun toggleHandsFree() {
        _isHandsFreeMode.value = !_isHandsFreeMode.value
        if (_isHandsFreeMode.value) {
            startVoiceInput()
        } else {
            stopVoiceInput()
        }
    }

    fun setImageForAnalysis(bitmap: Bitmap?) {
        _selectedImageBitmap.value = bitmap
        if (bitmap != null) {
            _statusText.value = "وێنە دیاریکرا بۆ شیکارکردن"
        }
    }

    fun startVoiceInput() {
        speechManager?.stopSpeaking()
        _assistantState.value = AssistantState.LISTENING
        _statusText.value = "گوێ دەگرێت لە فەرمانی کوردی..."
        speechManager?.startListening()
    }

    fun stopVoiceInput() {
        speechManager?.stopListening()
        _assistantState.value = AssistantState.IDLE
        _statusText.value = "ئامادەیە"
    }

    fun cancelCurrentAction() {
        speechManager?.stopListening()
        speechManager?.stopSpeaking()
        _assistantState.value = AssistantState.IDLE
        _statusText.value = "فەرمانەکە هەڵوەشێنرایەوە"
    }

    fun processUserInput(inputText: String) {
        val trimmed = inputText.trim()
        val currentImage = _selectedImageBitmap.value
        if (trimmed.isBlank() && currentImage == null) return

        viewModelScope.launch {
            val userMsg = ChatMessage(
                text = trimmed.ifBlank { "شیکارکردنی ئەم وێنەیە (ناسینەوەی کەس یان شوێن)" },
                isUser = true,
                status = ExecutionStatus.SUCCESS,
                imageBitmap = currentImage
            )

            val currentList = _messages.value.toMutableList()
            currentList.add(userMsg)
            _messages.value = currentList

            // Persist user turn
            conversationDao.insertMessage(
                ConversationEntity(
                    text = userMsg.text,
                    isUser = true,
                    status = "SUCCESS"
                )
            )

            _assistantState.value = AssistantState.THINKING
            _statusText.value = if (currentImage != null) "شیکاری وێنە و ناسینەوەی کەس یان شوێن..." else "گەڕان لە گووگڵ و ئامادەکردنی وەڵام..."

            // Check if input is a conversational dialogue or knowledge query
            val isConversationOrQuery = isConversationOrKnowledgeQuery(trimmed) || currentImage != null
            val localCommand = if (!isConversationOrQuery) KurdishNaturalCommandEngine.parseKurdishCommand(trimmed) else null

            if (localCommand != null && currentImage == null) {
                handleParsedCommand(localCommand, trimmed)
            } else {
                // Call Gemini AI Engine with deep grounding, accuracy, and source citation
                val historyContext = _messages.value.takeLast(6).map { it.text to it.isUser }
                val aiResponse = geminiService.processQuery(
                    prompt = trimmed,
                    imageBitmap = currentImage,
                    conversationContext = historyContext,
                    forceDeepResearch = true
                )

                _selectedImageBitmap.value = null

                if (aiResponse.command != null && aiResponse.command.intent != "chat" && !isConversationOrQuery) {
                    handleParsedCommand(aiResponse.command, trimmed, aiResponse.textResponse)
                } else {
                    val assistantMsg = ChatMessage(
                        text = aiResponse.textResponse,
                        isUser = false,
                        command = aiResponse.command,
                        status = ExecutionStatus.SUCCESS,
                        sources = aiResponse.sources,
                        isDeepVerified = aiResponse.isDeepVerified,
                        recognizedPerson = aiResponse.recognizedPerson,
                        placeLocation = aiResponse.placeLocation
                    )

                    addAssistantMessage(assistantMsg, aiResponse.spokenText)
                }
            }
        }
    }

    private fun isConversationOrKnowledgeQuery(input: String): Boolean {
        val conversationalKeywords = listOf(
            "سڵاو", "چۆنی", "چاکیت", "باشیت", "هەواڵت", "براکەم", "دەردەدڵ", "قسە", "ڕاوێژ",
            "گیان", "قوربان", "ئێوارە باش", "بەیانیت باش", "ڕۆژباش", "تۆ کێیت", "خۆت بناسێنە",
            "کەیفی", "چ هەواڵ", "دەنگوباس", "چی دەکەیت", "حاڵت", "سوپاس", "دەست خۆش",
            "چییە", "چیە", "چۆن", "بۆچی", "کەی", "کێیە", "لەکوێ", "ئایا", "ڕوونبکەرەوە",
            "بۆم باس بکە", "پێم بڵێ", "چارەسەری", "چارەسەر", "هۆکاری", "مێژووی", "زانستی",
            "پرسیار", "سەرچاوە", "بەڵگە", "یاسای", "چۆنیەتی", "فێرم بکە", "کێشەی", "دروستکردنی",
            "بیرکاری", "پێناسەی", "فەلسەفە", "تەندروستی", "?", "؟",
            "لەکوێیە", "لۆکەیشن", "شوێن", "ناونیشان", "نەخشە", "ماپس", "maps", "کێیە ئەمە",
            "کێیە", "ئەکاونت", "ئینستاگرام", "گەڕان لە گووگڵ", "گووگڵ", "گۆگڵ"
        )
        val normalized = input.lowercase()
        return conversationalKeywords.any { normalized.contains(it) } || input.length > 40
    }

    private suspend fun handleParsedCommand(
        command: ParsedCommand,
        originalPrompt: String,
        customAiResponse: String? = null
    ) {
        if (command.requiresConfirmation) {
            // Must ask confirmation for sensitive tasks (SMS, Calls, Deletion)
            val confirmSpeech = command.kurdishSpeech.ifBlank { "ئایا دڵنیایت دەتەوێت ئەم کردارە ئەنجام بدەیت؟" }
            val pendingMsg = ChatMessage(
                text = customAiResponse ?: confirmSpeech,
                isUser = false,
                command = command,
                status = ExecutionStatus.PENDING_CONFIRMATION
            )
            _assistantState.value = AssistantState.AWAITING_CONFIRMATION
            _statusText.value = "چاوەڕوانی ڕەزامەندی بەکارهێنەر..."

            addAssistantMessage(pendingMsg, confirmSpeech)
        } else {
            // Execute legitimate action immediately
            _statusText.value = "ئەنجامدانی: ${command.kurdishSummary}"
            val result = executor.execute(command)

            val feedbackText = buildString {
                if (!customAiResponse.isNullOrBlank()) {
                    append(customAiResponse)
                    append("\n\n")
                }
                append(result.kurdishFeedback)
                if (!result.extraData.isNullOrBlank()) {
                    append("\n\n")
                    append(result.extraData)
                }
            }

            val executionStatus = if (result.success) ExecutionStatus.SUCCESS else ExecutionStatus.ERROR

            val assistantMsg = ChatMessage(
                text = feedbackText,
                isUser = false,
                command = command,
                status = executionStatus,
                resultMessage = result.extraData,
                sources = command.sources,
                isDeepVerified = command.sources.isNotEmpty()
            )

            val spokenText = if (result.success) command.kurdishSpeech.ifBlank { result.kurdishFeedback } else result.kurdishFeedback

            addAssistantMessage(assistantMsg, spokenText)

            if (_isHandsFreeMode.value) {
                // Auto resume listening after short delay
                viewModelScope.launch {
                    kotlinx.coroutines.delay(3500)
                    if (_isHandsFreeMode.value) {
                        startVoiceInput()
                    }
                }
            }
        }
    }

    fun confirmPendingAction(message: ChatMessage) {
        val command = message.command ?: return
        viewModelScope.launch {
            _assistantState.value = AssistantState.THINKING
            _statusText.value = "جێبەجێکردنی فەرمان..."

            val result = executor.execute(command)
            val updatedStatus = if (result.success) ExecutionStatus.SUCCESS else ExecutionStatus.ERROR

            val updatedList = _messages.value.map {
                if (it.id == message.id) {
                    it.copy(
                        status = updatedStatus,
                        text = "${it.text}\n\n${result.kurdishFeedback}"
                    )
                } else it
            }
            _messages.value = updatedList

            speakText(result.kurdishFeedback)
            _assistantState.value = AssistantState.IDLE
            _statusText.value = "فەرمانەکە بە سەرکەوتوویی جێبەجێکرا"
        }
    }

    fun cancelPendingAction(message: ChatMessage) {
        val updatedList = _messages.value.map {
            if (it.id == message.id) {
                it.copy(
                    status = ExecutionStatus.CANCELLED,
                    text = "${it.text}\n\n[ئەم کردارە لەلایەن بەکارهێنەرەوە هەڵوەشێنرایەوە]"
                )
            } else it
        }
        _messages.value = updatedList
        _assistantState.value = AssistantState.IDLE
        _statusText.value = "فەرمانەکە هەڵوەشێنرایەوە"
        speakText("فەرمانەکە هەڵوەشێنرایەوە لەسەر داوای تۆ.")
    }

    private suspend fun addAssistantMessage(message: ChatMessage, spokenText: String) {
        val currentList = _messages.value.toMutableList()
        currentList.add(message)
        _messages.value = currentList

        conversationDao.insertMessage(
            ConversationEntity(
                text = message.text,
                isUser = false,
                intentName = message.command?.intent,
                intentTarget = message.command?.target,
                intentParam = message.command?.parameter,
                status = message.status.name,
                resultMessage = message.resultMessage
            )
        )

        speakText(spokenText)
    }

    fun speakText(text: String) {
        _assistantState.value = AssistantState.SPEAKING
        speechManager?.speak(text) {
            _assistantState.value = AssistantState.IDLE
        }
    }

    fun inspectScreen() {
        val service = BasitAccessibilityService.get()
        if (service != null) {
            val hierarchy = service.readCurrentScreen()
            _screenData.value = hierarchy
        } else {
            _screenData.value = null
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            conversationDao.clearHistory()
            _messages.value = emptyList()
            _statusText.value = "مێژووی گفتوگۆ پاککرایەوە"
        }
    }

    fun getSettingsIntent(item: PermissionItem) = permissionManager.getSettingsIntentFor(item)

    override fun onCleared() {
        super.onCleared()
        speechManager?.destroy()
    }
}
