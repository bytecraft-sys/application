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
- Voice input still uses Android `SpeechRecognizer` for transcription.
- Real-time glow amplitude is read separately from `AudioRecord`.
- `HomeScreen` switches `AuraCircle` between `Breathing` and `Listening` from `voiceState.isSpeaking`.
- `HomeScreen` passes `voiceState.amplitude` into `AuraCircle`.
- Mic button toggles listening through `onToggleVoice`.
- A keyboard icon opens the text input with a custom animated slide from the bottom.
- Upward scroll updates `scrollOffset` and fades the Aura circle via alpha.
- Upward scroll also applies parallax via `graphicsLayer.translationY`.
- Chat history uses `LazyPagingItems<ChatMessage>` from Room through `ChatMessageDao.pagingSourceForSession`.
- `PagingConfig` sets `pageSize = 20` and `initialLoadSize = 20`.
- Chat row UI shows sender, timestamp, and message content.

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
- A new message during `Responding` cancels the active job and restarts the pipeline.
- `HomeScreen` renders a visibly different `ChatStateBanner` for each chat state.
- Error UI includes a retry action through `ChatViewModel.retryLastMessage`.
- Unit tests use `StandardTestDispatcher`.
- Tests cover cancellation during processing and timeout-to-error.
- Happy-path test asserts the full sequence `Typing -> Validating -> Processing -> Responding -> Idle`.

## Verification

Last known successful checks after Part 1 fixes:

```powershell
.\gradlew.bat :app:compileDebugKotlin
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
```
