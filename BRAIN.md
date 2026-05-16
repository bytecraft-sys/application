# Aura Brain

This file is the technical memory for the Aura Android project. Keep it aligned with the real code, not intended behavior.

## Product Scope

Aura is an AI chat app with:

- A required three-step onboarding flow.
- A profile persisted in DataStore.
- Room-backed chat sessions and messages.
- A Compose home screen centered around an animated `AuraCircle`.
- A coroutine/StateFlow chat state machine.

## Part 1: Onboarding Flow

Implementation files:

- `ui/onboarding/OnboardingScreen.kt`
- `ui/onboarding/ValuePropsScreen.kt`
- `ui/onboarding/ProfileFormScreen.kt`
- `ui/onboarding/TraitSelectionScreen.kt`
- `ui/onboarding/OnboardingState.kt`
- `ui/onboarding/OnboardingViewModel.kt`
- `data/local/UserProfile.kt`
- `data/repository/UserProfileRepository.kt`
- `data/local/UserProfileSerializer.kt`

Current behavior:

- `OnboardingScreen` uses `HorizontalPager` for the three onboarding steps.
- Step 1 is `ValuePropsScreen`; it reveals value props one at a time using delayed `AnimatedVisibility`.
- Step 2 is `ProfileFormScreen`; it collects name, age, phone, and a four-digit OTP.
- `DEV_OTP` is hardcoded as `1234` in `OnboardingViewModel`.
- Step 3 is `TraitSelectionScreen`; it uses a `FlowRow` of trait chips.
- `REQUIRED_TRAIT_COUNT` is `3`; completion requires exactly 3 selected traits.
- `OnboardingState.isProfileStepValid` validates name, age, 10-digit phone, and verified OTP.
- `OnboardingState.isTraitsStepValid` validates exactly 3 selected traits.
- `UserProfile` stores id, name, age, phone, selected traits, timestamps, and OTP.
- `UserProfileRepository` persists the profile using `DataStore<UserProfile>`.
- `UserProfile` is not a Room entity.

## Part 2: Home Screen

Implementation files:

- `ui/home/AuraCircle.kt`
- `ui/home/AuraState.kt`
- `ui/home/HomeScreen.kt`
- `ui/home/HomeRoute.kt`
- `ui/chat/VoiceToTextParser.kt`
- `ui/chat/ChatViewModel.kt`
- `data/local/ChatMessage.kt`
- `data/local/ChatMessageDao.kt`

Current behavior:

- `AuraCircle` is a standalone reusable Composable.
- `AuraCircle` accepts `state: AuraState` and optional `amplitude`.
- Drawing is done with Compose `Canvas` and `drawCircle`.
- Idle/breathing animation uses `rememberInfiniteTransition`.
- Voice input currently uses Android `SpeechRecognizer`, not `AudioRecord`.
- `VoiceToTextParser.onRmsChanged` does not update amplitude.
- `HomeScreen` currently hardcodes `AuraCircle(state = AuraState.Breathing)`.
- Mic button toggles speech recognition through `onToggleVoice`.
- There is no keyboard icon that controls a custom slide-up text input.
- The text input offsets itself using `WindowInsets.ime`; this is default keyboard-inset behavior, not a custom keyboard-icon animation.
- Upward scroll updates `scrollOffset` and fades the Aura circle via alpha.
- Parallax is not implemented yet.
- Chat history uses `LazyPagingItems<ChatMessage>` from Room through `ChatMessageDao.pagingSourceForSession`.
- `PagingConfig` sets `pageSize = 20`, but does not set `initialLoadSize = 20`.
- Chat row UI shows message content and sometimes `AI`, but does not show timestamp and does not show sender for user messages.

Part 2 known requirement gaps:

- Implement real mic amplitude with `AudioRecord`.
- Drive `AuraCircle` state and amplitude from listening state.
- Add keyboard icon driven custom slide-up input.
- Add scroll parallax during the Aura-to-history transition.
- Render sender, message, and timestamp on every chat row.
- Configure paging to load exactly 20 items at a time.

## Part 3: Coroutine State Machine

Implementation files:

- `domain/statemachine/ChatState.kt`
- `domain/statemachine/ChatStateMachine.kt`
- `domain/statemachine/ChatMessageProcessor.kt`
- `domain/statemachine/GeminiChatMessageProcessor.kt`
- `ui/chat/ChatViewModel.kt`
- `ui/home/HomeScreen.kt`
- `src/test/java/com/yourapp/domain/statemachine/ChatStateMachineTest.kt`

Current behavior:

- `ChatState` is a sealed class with `Idle`, `Typing`, `Validating`, `Processing`, `Responding`, and `Error`.
- `ChatStateMachine` exposes `StateFlow<ChatState>`.
- Normal non-empty messages transition through `Typing -> Validating -> Processing -> Responding -> Idle`.
- Processing timeout is exactly `8_000L`.
- Timeout transitions to `ChatState.Error("Request timed out")`.
- A new message during `Processing` cancels the active processing job and restarts the pipeline.
- A new message during `Responding` is not explicitly cancelled.
- `HomeScreen` accepts `chatState`, but does not render visibly different UI per state.
- Error retry UI is missing.
- Unit tests use `StandardTestDispatcher`.
- Tests cover cancellation during processing and timeout-to-error.
- Happy-path test currently asserts only `Processing` then `Idle`, not the full exact state sequence.

Part 3 known requirement gaps:

- Render different UI for each `ChatState`.
- Cancel and restart when a new message arrives during `Responding`.
- Show retry UI for `Error`.
- Update happy-path test to assert the full sequence `Typing -> Validating -> Processing -> Responding -> Idle`.

## Verification

Last known successful checks after Part 1 fixes:

```powershell
.\gradlew.bat :app:compileDebugKotlin
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
```
