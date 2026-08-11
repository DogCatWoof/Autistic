# :feature:note

## Purpose
Note-taking: create, edit, view, soft-delete freeform text notes with topic organization, voice capture, and search.

## Functional Scope
- Create and edit notes with title, body, and topic association
- View note list filtered by topic; topic selector in the list header
- Full-text search across title and content, optionally restricted to a topic
- Voice capture via platform speech recognizer: push-to-talk, real-time partial transcription, punctuation conversion, error retry with backoff
- Soft-delete notes with restore and hard-delete (propagated to Firestore via `:data:firestore`)
- Sync status indicator (synced / pending) per note
- Keep screen on during voice capture

## Key Files
- `NoteRepository` — CRUD for `NoteEntity`; sets `pendingFirestoreSync` on every write
- `TopicRepository` — CRUD for `TopicEntity`
- `TopicPreferencesStore` — DataStore for persisting last-selected topic ID
- `NotesViewModel` — note list, topic, search, and voice capture state
- `NotesScreen` — note list, editor, topic picker, search bar, voice UI

## Dependencies
- `:core:database`
- `:core:ui`
- Android SpeechRecognizer (platform SDK)

## Not In This Module
- Firestore sync (handled by `:data:firestore`)
- Any other feature domain
