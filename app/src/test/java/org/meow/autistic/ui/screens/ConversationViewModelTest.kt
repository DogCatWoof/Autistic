package org.meow.autistic.ui.screens

import android.app.Application
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.meow.autistic.data.conversation.IntentClass
import org.meow.autistic.data.conversation.ResponseTemplate
import org.meow.autistic.data.conversation.ResponseTemplateRepository

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var templateRepository: ResponseTemplateRepository
    private lateinit var application: Application
    private lateinit var viewModel: ConversationViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        mockkConstructor(TextToSpeech::class)
        mockkStatic(SpeechRecognizer::class)
        every { anyConstructed<TextToSpeech>().shutdown() } returns Unit
        every { SpeechRecognizer.isRecognitionAvailable(any()) } returns false
        templateRepository = mockk()
        application = mockk(relaxed = true)
        every { application.applicationContext } returns application
        viewModel = ConversationViewModel(templateRepository, application)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `initial state is empty`() = runTest {
        assertEquals("", viewModel.transcribedText.first())
        assertTrue(viewModel.chips.first().isEmpty())
        assertFalse(viewModel.isListening.first())
        assertFalse(viewModel.ttsEnabled.first())
        assertFalse(viewModel.isSpeakerTurn.first())
    }

    @Test
    fun `processTranscription updates text and chips for Social input`() = runTest {
        val templates = listOf(
            ResponseTemplate("That's interesting.", "neutral"),
            ResponseTemplate("Tell me more.", "neutral"),
        )
        every { templateRepository.getTemplates(IntentClass.Social, any()) } returns templates

        viewModel.processTranscriptionForTest("Hello there")

        assertEquals("Hello there", viewModel.transcribedText.first())
        assertEquals(templates, viewModel.chips.first())
        assertTrue(viewModel.isSpeakerTurn.first())
    }

    @Test
    fun `blank transcription does not update chips or turn`() = runTest {
        viewModel.processTranscriptionForTest("   ")

        assertEquals("   ", viewModel.transcribedText.first())
        assertTrue(viewModel.chips.first().isEmpty())
        assertFalse(viewModel.isSpeakerTurn.first())
    }

    @Test
    fun `toggleTts flips ttsEnabled`() = runTest {
        assertFalse(viewModel.ttsEnabled.first())
        viewModel.toggleTts()
        assertTrue(viewModel.ttsEnabled.first())
        viewModel.toggleTts()
        assertFalse(viewModel.ttsEnabled.first())
    }

    @Test
    fun `selectChip clears speaker turn`() = runTest {
        every { templateRepository.getTemplates(any(), any()) } returns listOf(
            ResponseTemplate("Hi!", "neutral")
        )
        viewModel.processTranscriptionForTest("Hello")
        viewModel.selectChip(ResponseTemplate("Hi!", "neutral"))
        assertFalse(viewModel.isSpeakerTurn.first())
    }

    @Test
    fun `chips populated with Question templates for question input`() = runTest {
        val templates = listOf(
            ResponseTemplate("That's a good question.", "neutral"),
            ResponseTemplate("I'm not sure.", "neutral"),
            ResponseTemplate("Let me think.", "neutral"),
        )
        every { templateRepository.getTemplates(IntentClass.Question, any()) } returns templates

        viewModel.processTranscriptionForTest("What time is it")

        assertEquals(templates, viewModel.chips.first())
    }
}

private fun ConversationViewModel.processTranscriptionForTest(text: String) {
    val method = ConversationViewModel::class.java.getDeclaredMethod("processTranscription", String::class.java)
    method.isAccessible = true
    method.invoke(this, text)
}
