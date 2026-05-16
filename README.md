# ByteCraft Gemini AI Chat

A premium, modern AI chat application built with Jetpack Compose, featuring persistent conversation history, in-session search, and deep Gemini AI integration.

## ✨ Features

- **Persistent Chat History**: Categorized conversation history (Pinned, Today, Yesterday, etc.) with session-based persistence using Room.
- **In-Session Search**: High-performance search within specific conversations with real-time highlighting and directional navigation.
- **Gemini AI Integration**: Seamless real-time chat powered by the Google Gemini SDK.
- **Premium UI/UX**:
    - **AuraCircle**: Dynamic breathing animation that responds to AI states.
    - **Material 3**: Full implementation of modern Material Design 3 components.
    - **Swipe-to-Delete**: Intuitive session management.
    - **Glassmorphism**: Elegant card designs with subtle transparencies and gradients.
- **Clean Architecture**: Robust codebase using Hilt for DI, StateFlow for reactive UI, and Repository pattern for data management.

## 🛠 Tech Stack

- **UI**: Jetpack Compose, Material 3
- **Dependency Injection**: Hilt
- **Database**: Room (with multi-version migrations)
- **Networking**: Gemini AI SDK
- **Architecture**: MVVM / Clean Architecture
- **State Management**: Kotlin Coroutines & Flow

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug or newer
- JDK 17+
- Gemini API Key from [Google AI Studio](https://aistudio.google.com/)

### Configuration
1. Create a `local.properties` file in the root directory if it doesn't exist.
2. Add your API key:
   ```properties
   GEMINI_API_KEY=your_api_key_here
   ```

### Running the App
1. Sync project with Gradle files.
2. Run the `app` module on an emulator or physical device (Min SDK: 24).

## 📂 Project Structure

- `com.yourapp.data`: Local database entities, DAOs, and repository implementations.
- `com.yourapp.domain`: Core logic, state machines, and message processors.
- `com.yourapp.ui`: Jetpack Compose screens, ViewModels, and UI state definitions.
- `com.yourapp.ui.theme`: Custom color palettes and typography.

---
Built with ❤️ using Antigravity AI.
