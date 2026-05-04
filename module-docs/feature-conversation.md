# :feature:conversation

## Purpose
AI conversation assistant: classifies user intents, selects response templates, and adapts tone based on user preferences.

## Functional Scope
- Conversational UI for interacting with the AI assistant
- Intent classification from user messages
- Response template selection and rendering
- Tone preference configuration (stored in DataStore)

## Key Files
- `IntentClassifier` — classifies incoming user messages into intent categories
- `ResponseTemplateRepository` — loads and selects response templates
- `TonePreferencesStore` — DataStore-backed user preference for response tone
- `ConversationViewModel` — conversation state and message handling
- `ConversationScreen` — chat UI composable

## Dependencies
- `:core:ui`

## Not In This Module
- Any database persistence beyond DataStore preferences
- Task, note, mood, or other feature data
