# :core:auth

## Purpose
Google OAuth and Firebase Auth sign-in flow, token lifecycle management. Shared by all modules that need an authenticated identity.

## Functional Scope
- Google sign-in and sign-out
- Firebase Auth credential bridging (so Firestore security rules can use `request.auth.uid`)
- Access token refresh and caching

## Key Files
- `GoogleAuthManager` — orchestrates Google sign-in, Firebase credential, token refresh
- `TokenStore` — persists and retrieves OAuth tokens

## Dependencies
- `:core:common`
- Firebase Auth SDK
- Google Sign-In SDK

## Not In This Module
- Firestore operations (`:data:firestore`)
- UI for sign-in screens (`:app`)
- Any feature-domain logic
