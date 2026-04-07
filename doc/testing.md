# Testing Guide

**Team GLITCH** | CS360 Software Engineering

This document records the project testing approach, the commands used to run it, and the main test areas in the repository.

## Test Layers

- Unit tests live under `app/src/test/java`.
- Instrumented Android tests live under `app/src/androidTest/java`.
- Model and repository contract tests cover the Firestore mapping and repository behavior.
- UI adapter and fragment tests cover list rendering, navigation, and screen-level behavior.

## Common Commands

Run these from the repository root.

```bash
./gradlew test
```
Runs the JVM unit test suite.

```bash
./gradlew connectedAndroidTest
```
Runs instrumented tests on a connected device or emulator.

```bash
./gradlew lint
```
Runs Android lint checks for code quality and resource issues.

```bash
./gradlew assembleDebug
```
Builds the debug APK and validates the project compiles.

## Test Focus Areas

- Model mapping: Firestore map-to-model conversion and fallback behavior.
- Repository contracts: listener callbacks, completion callbacks, and error handling.
- UI components: adapters, navigation routing, and fragment startup behavior.
- Session and auth flows: login validation, profile loading, and role routing.

## Representative Test Files

- `app/src/test/java/com/example/glitch/model/`
- `app/src/test/java/com/example/glitch/data/`
- `app/src/test/java/com/example/glitch/auth/`
- `app/src/androidTest/java/com/example/glitch/ui/DashboardFragmentTest.java`

## Review Notes

- Run unit tests before pushing any documentation or UI changes.
- Re-run Android instrumentation tests after touching fragments, adapters, or navigation logic.
- Keep new tests close to the feature they validate so the repository stays easy to maintain.