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

Status: Partially implemented. The following requirements are still broken or missing:

- Listening glow does not use real mic amplitude from `AudioRecord`.
- Mic button starts speech recognition, but `AuraCircle` is still hardcoded to breathing state.
- Keyboard icon custom slide-up input is missing.
- Scroll fade exists, but the parallax effect is missing.
- Chat rows do not show timestamp, and user rows do not show sender.
- Paging uses `pageSize = 20`, but does not guarantee exactly 20 items per load.

### Part 3: Coroutine State Machine

Status: Partially implemented. The following requirements are still broken or missing:

- `HomeScreen` receives `chatState`, but does not render visibly different UI for each state.
- New messages during `Responding` do not cancel the current flow.
- Error state does not show a retry option in the UI.
- Happy-path unit test does not assert the full exact sequence `Typing -> Validating -> Processing -> Responding -> Idle`.

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
