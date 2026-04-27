package org.meow.autistic.data.conversation

import org.junit.Assert.assertEquals
import org.junit.Test

class IntentClassifierTest {

    @Test
    fun `blank input returns Unknown`() {
        assertEquals(IntentClass.Unknown, IntentClassifier.classify("").intentClass)
        assertEquals(IntentClass.Unknown, IntentClassifier.classify("   ").intentClass)
    }

    @Test
    fun `greeting classified as Social`() {
        assertEquals(IntentClass.Social, IntentClassifier.classify("Hello there").intentClass)
        assertEquals(IntentClass.Social, IntentClassifier.classify("Hey how's it going").intentClass)
        assertEquals(IntentClass.Social, IntentClassifier.classify("Good morning!").intentClass)
        assertEquals(IntentClass.Social, IntentClassifier.classify("Thanks a lot").intentClass)
    }

    @Test
    fun `sarcasm classified as Sarcasm`() {
        assertEquals(IntentClass.Sarcasm, IntentClassifier.classify("Yeah right, I believe you").intentClass)
        assertEquals(IntentClass.Sarcasm, IntentClassifier.classify("Oh sure, that will work").intentClass)
        assertEquals(IntentClass.Sarcasm, IntentClassifier.classify("Oh great, another problem").intentClass)
    }

    @Test
    fun `question mark classified as Question`() {
        assertEquals(IntentClass.Question, IntentClassifier.classify("Are you ready?").intentClass)
        assertEquals(IntentClass.Question, IntentClassifier.classify("Is this correct?").intentClass)
    }

    @Test
    fun `question starter word classified as Question`() {
        assertEquals(IntentClass.Question, IntentClassifier.classify("What time is it").intentClass)
        assertEquals(IntentClass.Question, IntentClassifier.classify("Where should we go").intentClass)
        assertEquals(IntentClass.Question, IntentClassifier.classify("How does this work").intentClass)
        assertEquals(IntentClass.Question, IntentClassifier.classify("Why did that happen").intentClass)
    }

    @Test
    fun `command phrase classified as Command`() {
        assertEquals(IntentClass.Command, IntentClassifier.classify("Please help me with this").intentClass)
        assertEquals(IntentClass.Command, IntentClassifier.classify("Help me understand this").intentClass)
        assertEquals(IntentClass.Command, IntentClassifier.classify("Make sure this is correct").intentClass)
        assertEquals(IntentClass.Command, IntentClassifier.classify("Give me a summary").intentClass)
    }

    @Test
    fun `unmatched text classified as Unknown`() {
        assertEquals(IntentClass.Unknown, IntentClassifier.classify("I went to the store today").intentClass)
        assertEquals(IntentClass.Unknown, IntentClassifier.classify("The weather is nice").intentClass)
    }

    @Test
    fun `Social has priority over Sarcasm`() {
        // "hi" prefix with a sarcasm phrase — Social wins (higher priority)
        assertEquals(IntentClass.Social, IntentClassifier.classify("hi yeah right").intentClass)
    }

    @Test
    fun `Sarcasm has priority over Question`() {
        assertEquals(IntentClass.Sarcasm, IntentClassifier.classify("Oh sure, what do you want?").intentClass)
    }

    @Test
    fun `Question has priority over Command`() {
        // question mark + command word — Question wins
        assertEquals(IntentClass.Question, IntentClassifier.classify("Can you do this?").intentClass)
    }

    @Test
    fun `confidence values match expected`() {
        assertEquals(0.9f, IntentClassifier.classify("Hello world").confidence, 0.001f)
        assertEquals(0.8f, IntentClassifier.classify("Yeah right mate").confidence, 0.001f)
        assertEquals(0.9f, IntentClassifier.classify("What is this?").confidence, 0.001f)
        assertEquals(0.8f, IntentClassifier.classify("Please do this").confidence, 0.001f)
        assertEquals(0.5f, IntentClassifier.classify("Just a statement").confidence, 0.001f)
    }

    @Test
    fun `classification is case insensitive`() {
        assertEquals(IntentClass.Social, IntentClassifier.classify("HELLO THERE").intentClass)
        assertEquals(IntentClass.Question, IntentClassifier.classify("WHAT IS THIS").intentClass)
    }
}
