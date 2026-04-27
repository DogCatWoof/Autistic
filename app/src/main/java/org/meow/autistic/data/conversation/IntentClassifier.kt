package org.meow.autistic.data.conversation

/**
 * Rule-based intent classifier.
 * Evaluates priority-ordered rules; first match wins.
 */
object IntentClassifier {

    private val SOCIAL_PREFIXES = setOf(
        "hi", "hello", "hey", "good morning", "good afternoon", "good evening",
        "good night", "bye", "goodbye", "see you", "take care", "thanks", "thank you",
        "nice to meet", "how are you", "how's it going", "what's up",
    )

    private val QUESTION_STARTERS = setOf(
        "what", "who", "where", "when", "why", "how", "which", "whose",
        "is", "are", "was", "were", "do", "does", "did", "can", "could",
        "would", "should", "will", "have", "has", "had", "may", "might",
    )

    private val COMMAND_INDICATORS = setOf(
        "please", "could you", "can you", "would you", "help me", "tell me",
        "show me", "give me", "do this", "let me know", "make sure", "make this",
        "fix this", "stop", "start", "open", "close",
    )

    private val SARCASM_PHRASES = setOf(
        "yeah right", "oh sure", "oh great", "how wonderful", "big surprise",
        "as if", "like that's", "oh obviously", "clearly", "wow thanks",
    )

    fun classify(text: String): ClassificationResult {
        val lower = text.trim().lowercase()
        if (lower.isBlank()) { return ClassificationResult(IntentClass.Unknown, 1.0f) }

        val isSocial = SOCIAL_PREFIXES.any { lower.startsWith(it) }
        if (isSocial) { return ClassificationResult(IntentClass.Social, 0.9f) }

        val hasSarcasm = SARCASM_PHRASES.any { lower.contains(it) }
        if (hasSarcasm) { return ClassificationResult(IntentClass.Sarcasm, 0.8f) }

        val isQuestion = lower.endsWith("?") ||
            QUESTION_STARTERS.any { lower.startsWith("$it ") }
        if (isQuestion) { return ClassificationResult(IntentClass.Question, 0.9f) }

        val isCommand = COMMAND_INDICATORS.any { lower.contains(it) }
        if (isCommand) { return ClassificationResult(IntentClass.Command, 0.8f) }

        return ClassificationResult(IntentClass.Unknown, 0.5f)
    }
}
