package org.meow.autistic.ui.screens

import android.app.Application
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.meow.autistic.data.conversation.IntentClassifier
import org.meow.autistic.data.conversation.ResponseTemplate
import org.meow.autistic.data.conversation.ResponseTemplateRepository
import org.meow.autistic.data.conversation.TONE_NEUTRAL
import org.meow.autistic.data.conversation.TonePreferencesStore
import java.util.Locale

/**
 * Drives the conversation scaffolding screen.
 * Manages push-to-talk via [SpeechRecognizer], classifies speech, and surfaces response chips.
 */
class ConversationViewModel(
    private val templateRepository: ResponseTemplateRepository,
    application: Application,
) : AndroidViewModel(application) {

    private val _transcribedText = MutableStateFlow("")
    val transcribedText: StateFlow<String> = _transcribedText.asStateFlow()

    private val _chips = MutableStateFlow<List<ResponseTemplate>>(emptyList())
    val chips: StateFlow<List<ResponseTemplate>> = _chips.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _ttsEnabled = MutableStateFlow(false)
    val ttsEnabled: StateFlow<Boolean> = _ttsEnabled.asStateFlow()

    private val _isSpeakerTurn = MutableStateFlow(false)
    val isSpeakerTurn: StateFlow<Boolean> = _isSpeakerTurn.asStateFlow()

    val tone: StateFlow<String> = TonePreferencesStore.getToneFlow(application)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TONE_NEUTRAL)

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null

    init {
        initTts()
    }

    fun startListening() {
        if (_isListening.value) return
        val app = getApplication<Application>()
        if (!SpeechRecognizer.isRecognitionAvailable(app)) return
        _isSpeakerTurn.value = false
        _isListening.value = true
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(app).also { sr ->
            sr.setRecognitionListener(buildListener())
            sr.startListening(
                android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                }
            )
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _isListening.value = false
    }

    fun selectChip(template: ResponseTemplate) {
        if (_ttsEnabled.value) {
            tts?.speak(template.text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
        _isSpeakerTurn.value = false
    }

    fun toggleTts() {
        _ttsEnabled.value = !_ttsEnabled.value
    }

    fun setTone(newTone: String) = viewModelScope.launch {
        TonePreferencesStore.setTone(getApplication(), newTone)
    }

    override fun onCleared() {
        speechRecognizer?.destroy()
        tts?.shutdown()
    }

    private fun buildListener() = object : RecognitionListener {
        override fun onResults(results: Bundle) {
            val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull() ?: ""
            _isListening.value = false
            processTranscription(text)
        }
        override fun onError(error: Int) { _isListening.value = false }
        override fun onPartialResults(partial: Bundle) {}
        override fun onReadyForSpeech(params: Bundle) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray) {}
        override fun onEndOfSpeech() {}
        override fun onEvent(eventType: Int, params: Bundle) {}
    }

    private fun processTranscription(text: String) {
        _transcribedText.value = text
        if (text.isBlank()) return
        val result = IntentClassifier.classify(text)
        _chips.value = templateRepository.getTemplates(result.intentClass, tone.value)
        _isSpeakerTurn.value = true
    }

    private fun initTts() {
        tts = TextToSpeech(getApplication()) { status ->
            if (status != TextToSpeech.SUCCESS) tts = null
        }
    }
}
