# MyRAM Android

MyRAM is a native Android notes app for quickly capturing, editing, and returning to personal notes with as little friction as possible.

This repository contains the Android implementation. The matching iOS app lives in the MyRAM iOS repository.

---

# Current Direction

MyRAM focuses on:
- fast note creation
- low-friction editing
- auto-save while typing
- reopening the last active note
- creating a new note without leaving the editor
- keeping editor focus stable while the keyboard is visible

The app is designed around quick capture and continued editing rather than folder-heavy organization.

---

# Design Philosophy

The user should be able to:
- open the app and continue where they left off
- type without thinking about saving
- create a new note as soon as a new thought appears
- delete notes without leaving the current workflow
- keep the cursor and active text visible while editing

There are currently no:
- folder systems
- manual save flows
- sync requirements
- account requirements

Editing should feel immediate, local, and predictable.

---

# Technical Direction

MyRAM Android is built using:
- Kotlin
- Jetpack Compose
- Material 3
- Room
- Kotlin coroutines and Flow
- Gradle Kotlin DSL

The Android and iOS apps are native implementations of the same product direction. They are intentionally not a shared multiplatform codebase.

---

# Current App Goals

Current development focuses on:
- reliable editor behavior
- stable note switching
- predictable auto-save
- native platform editing controls
- simple local persistence

The current milestone is making note capture and editing feel dependable before expanding into encrypted export, recently deleted notes, desktop support, or cross-device workflows.

---

# Planned Structure

```text
app/
    src/main/java/com/apexcoretechs/myram/
        data/
        ui/
        ui/screens/

app/src/main/res/
```

---

# Build

From the repository root:

```sh
./gradlew assembleDebug
```
