package com.example.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class KurdishSpeechManager(
    private val context: Context,
    private val onSpeechResult: (String) -> Unit,
    private val onSpeechPartial: (String) -> Unit = {},
    private val onError: (String) -> Unit = {}
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _audioRms = MutableStateFlow(0f)
    val audioRms: StateFlow<Float> = _audioRms.asStateFlow()

    init {
        initTts()
    }

    private fun initTts() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Try Kurdish (ckb) first, fallback to Arabic (ar_IQ) for shared phonetics, or device default
                val kurdishLocale = Locale("ckb", "IQ")
                val kurdishAvailable = textToSpeech?.isLanguageAvailable(kurdishLocale)
                if (kurdishAvailable == TextToSpeech.LANG_AVAILABLE || kurdishAvailable == TextToSpeech.LANG_COUNTRY_AVAILABLE) {
                    textToSpeech?.language = kurdishLocale
                } else {
                    val arabicLocale = Locale("ar", "IQ")
                    val arabicAvailable = textToSpeech?.isLanguageAvailable(arabicLocale)
                    if (arabicAvailable == TextToSpeech.LANG_AVAILABLE || arabicAvailable == TextToSpeech.LANG_COUNTRY_AVAILABLE) {
                        textToSpeech?.language = arabicLocale
                    } else {
                        textToSpeech?.language = Locale.getDefault()
                    }
                }
                textToSpeech?.setSpeechRate(0.95f)
                textToSpeech?.setPitch(1.0f)
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }
                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                })
                isTtsReady = true
            }
        }
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("خزمەتگوزاری ناسینەوەی دەنگ لەسەر ئەم ئامێرە بەردەست نییە.")
            return
        }

        stopListening()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                    _audioRms.value = 0f
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {
                    val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                    _audioRms.value = normalized
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _isListening.value = false
                    _audioRms.value = 0f
                }

                override fun onError(error: Int) {
                    _isListening.value = false
                    _audioRms.value = 0f
                    val kurdishError = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "دەنگت ڕوون نەبوو، تکایە دووبارەی بکەرەوە."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "هیچ دەنگێک نەبیسترا."
                        SpeechRecognizer.ERROR_AUDIO -> "هەڵە لە تۆمارکردنی دەنگ ڕوویدا."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "مۆڵەتی مایکڕۆفۆن پێویستە."
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "هەڵە لە پەیوەندی ئینتەرنێت."
                        else -> "هەڵەیەک لە ناسینەوەی دەنگ ڕوویدا."
                    }
                    onError(kurdishError)
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    _audioRms.value = 0f
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()?.trim()
                    if (!text.isNullOrBlank()) {
                        onSpeechResult(text)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()?.trim()
                    if (!text.isNullOrBlank()) {
                        onSpeechPartial(text)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ckb-IQ")
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("ckb", "ku", "ar-IQ", "en-US"))
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "فەرمانەکەت بڵێ بە کوردی سۆرانی...")
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _isListening.value = false
            onError("دەستپێکردنی دەنگ سەرکەوتوو نەبوو: ${e.message}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        _isListening.value = false
        _audioRms.value = 0f
    }

    fun speak(kurdishText: String, onComplete: () -> Unit = {}) {
        if (!isTtsReady || textToSpeech == null) {
            onComplete()
            return
        }
        stopSpeaking()
        _isSpeaking.value = true
        val utteranceId = "utterance_${System.currentTimeMillis()}"
        textToSpeech?.speak(kurdishText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stopSpeaking() {
        try {
            textToSpeech?.stop()
        } catch (_: Exception) {}
        _isSpeaking.value = false
    }

    fun destroy() {
        stopListening()
        stopSpeaking()
        try {
            textToSpeech?.shutdown()
        } catch (_: Exception) {}
        textToSpeech = null
    }
}
