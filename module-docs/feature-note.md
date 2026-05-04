# :feature:note

## Purpose
Note-taking: create, edit, view, and soft-delete freeform text notes.

## Functional Scope
- Create and edit notes with title and body
- View note list and individual notes
- Soft-delete notes (propagated to Firestore via `:data:firestore`)

## Key Files
- `NoteRepository` — CRUD for `NoteEntity`; sets `pendingFirestoreSync` on every write
- `NotesViewModel` — note list and editor state
- `NotesScreen` — note list and detail/edit UI

## Dependencies
- `:core:database`
- `:core:ui`

## Not In This Module
- Firestore sync (handled by `:data:firestore`)
- Any other feature domain
