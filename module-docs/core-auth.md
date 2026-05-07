# :core:auth

## Purpose
Google Sign-In via Firebase Auth and OAuth access token management for Google APIs (Tasks, Calendar).

## Functional Scope
- Google Sign-In via Credential Manager + Firebase Auth
- Firebase Auth session management (sign-in, sign-out, uid provider)
- OAuth access token refresh and caching for Google APIs
- Encrypted token storage via `TokenStore`

## Key Files
- `GoogleAuthManager` — sign-in flow, token management, Firebase Auth bridging
- `TokenStore` — encrypted SharedPreferences for OAuth tokens and account email

## Dependencies
- `:core:common`
- Firebase Auth SDK
- Credential Manager (AndroidX)
- Google Sign-In SDK (for `GoogleIdTokenCredential`)
- Google Auth Util (for `GoogleAuthUtil.getToken()`)

## Not In This Module
- Firestore operations (`:data:firestore`)
- UI for sign-in screens (`:app`)
- Any feature-domain logic
- Google API HTTP clients (`:data:sync`)
