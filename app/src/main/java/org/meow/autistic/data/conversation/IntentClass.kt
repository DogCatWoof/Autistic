package org.meow.autistic.data.conversation

/** Coarse classification of the speaker's communicative intent. */
enum class IntentClass { Question, Command, Social, Sarcasm, Unknown }

/** Result of classifying a spoken utterance. */
data class ClassificationResult(val intentClass: IntentClass, val confidence: Float)
