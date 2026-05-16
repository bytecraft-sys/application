# Aura

Aura is a Jetpack Compose Android chat app with a three-step onboarding flow, persisted user profile data, Room-backed chat history, and a coroutine-based chat state machine.

## Current Assignment Status

### Part 1: Onboarding Flow

Status: Complete against the reviewed requirements.

- Three-step onboarding implemented with `HorizontalPager`.
- Step 1 lives in `ValuePropsScreen` and reveals value props one at a time with animation.
- Step 2 lives in `ProfileFormScreen` and collects name, age, phone, and OTP.
- Mock OTP is hardcoded as `1234`.
- Step 3 lives in `TraitSelectionScreen` and presents a trait tag grid.
- The user must select exactly 3 traits before completion.
- Phone validation requires exactly 10 digits.
- Collected onboarding data is stored in a single `UserProfile` data class.
- `UserProfile` is persisted through DataStore.
- Back navigation keeps ViewModel state, so previous step data is restored without loss.

### Part 2: Home Screen

Status: Complete against the previously reviewed fail/missing requirements.

- `AuraCircle` is pure Canvas, standalone, and accepts `AuraState` plus amplitude.
- Idle state uses a slow breathing pulse animation.
- Listening state uses real mic amplitude from `AudioRecord`.
- The mic button drives listening state and feeds amplitude into `AuraCircle`.
- The keyboard icon opens the text input with a custom slide-up animation.
- Upward scroll fades the Aura circle and applies a parallax translation into the chat list.
- Chat history is loaded from Room through Paging.
- Chat rows show sender, message, and timestamp.
- Paging is configured with `pageSize = 20` and `initialLoadSize = 20`.

### Part 3: Coroutine State Machine

Status: Complete against the previously reviewed fail/missing requirements.

- The state machine uses `StateFlow` and sealed `ChatState`.
- Normal messages follow `Typing -> Validating -> Processing -> Responding -> Idle`.
- Processing timeout is exactly 8 seconds.
- Timeout moves to `Error`.
- New messages during `Processing` or `Responding` cancel the active job and restart the pipeline.
- `HomeScreen` renders visibly different UI per chat state.
- Error UI includes a retry action.
- Unit tests cover the full happy-path sequence, cancellation, and timeout using `StandardTestDispatcher`.

## Tech Stack

- UI: Jetpack Compose, Material 3
- Navigation: Navigation Compose
- Dependency injection: Hilt
- Local chat storage: Room
- User profile storage: DataStore
- Async/state: Kotlin Coroutines, Flow, StateFlow
- Paging: AndroidX Paging
- AI client: Google Gemini SDK

## Configuration

Add a Gemini API key either as an environment variable or in `local.properties`:

```properties
GEMINI_API_KEY=your_api_key_here
```

The app builds without a key, but Gemini responses will return a missing-key message until one is configured.

## Useful Commands

```powershell
.\gradlew.bat :app:compileDebugKotlin
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
```

## Project Structure

- `app/src/main/java/com/yourapp/data`: Room entities, DAOs, DataStore serializer, and repositories.
- `app/src/main/java/com/yourapp/domain/statemachine`: Chat pipeline state machine and message processors.
- `app/src/main/java/com/yourapp/ui/onboarding`: Part 1 onboarding screens and ViewModel.
- `app/src/main/java/com/yourapp/ui/home`: Home screen, Aura circle, chat list, and message input.
- `app/src/main/java/com/yourapp/ui/chat`: Chat ViewModel and voice-to-text wrapper.
- `app/src/test/java/com/yourapp/domain/statemachine`: State machine unit tests.
