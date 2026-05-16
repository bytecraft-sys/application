# Application Brain & Technical Architecture

This document serves as the technical "brain" of the ByteCraft Chat application, detailing the core logic, state management, and architectural decisions.

## 🧠 Core Logic: Message Processing

The application uses a specialized **GeminiChatMessageProcessor** and a State Machine pattern to handle the flow of messages between the user and the Gemini AI.

### Message Flow
1. **Input**: User sends a message via `HomeScreen`.
2. **Session Check**: `ChatViewModel` checks for an active `sessionId`. If null, a new session is created with a title auto-generated from the first message.
3. **Processing**: The message is passed to the `GeminiChatMessageProcessor`.
4. **AI Stream**: Gemini returns a stream of tokens which are aggregated and persisted in real-time.
5. **Aura Sync**: The `AuraCircle` state (Breathing, Thinking, Responding) is synced with the processing state.

## 💾 Data Architecture

### Room Schema (v2)
- **ChatSession**: Stores metadata for conversation groupings.
    - `id`: UUID Primary Key.
    - `title`: Truncated first message.
    - `updatedAt`: Used for categorization (Today, Yesterday, etc.).
- **ChatMessage**: Linked to sessions via Foreign Key (`sessionId`) with `ON DELETE CASCADE`.

### Migrations
- **Migration 1 -> 2**: A complex manual migration that handles table re-creation to support Foreign Key constraints and optimized indices for search.

## 🔍 Search Engine
In-session search is implemented using SQLite `LIKE` queries, optimized with a `sessionId` index.
- **ViewModel Logic**: Uses `combine` to filter results and maintain a `currentSearchIndex` for directional navigation.
- **UI Logic**: `buildAnnotatedString` highlights matching tokens using the `BrandColor`.

## 🎨 UI Design System

### The Aura
The `AuraCircle` is the visual centerpiece. It uses `graphicsLayer` animations and `InfiniteTransition` to provide tactile feedback on the AI's current "thinking" status.

### Theme
- **Primary Color**: `#1D9E75` (Brand Green)
- **Aesthetic**: Premium Dark Mode with Glassmorphic overlays and high-contrast typography (Material 3).

## 🚀 Future Scalability
- **Streaming UI**: Potential for incremental UI updates as tokens arrive.
- **Offline Mode**: Proto DataStore integration for caching session drafts.
- **Voice-to-Text**: Integration with Android Speech recognizer for hands-free interaction.

---
*Generated and maintained by the Antigravity AI agent.*
