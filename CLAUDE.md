# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

AstroAlert is an Android application for astronomers and astrophotographers to monitor observing conditions at multiple locations and receive alerts when conditions are optimal. The app integrates with multiple APIs for weather data, astronomical targets, and AI-powered condition analysis.

## Build Commands

```bash
# Build the project
./gradlew build

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run Android instrumented tests
./gradlew connectedAndroidTest

# Clean build
./gradlew clean
```

## API Keys Setup

API keys must be configured in `local.properties` (not version controlled):
```properties
ASTROSPHERIC_API_KEY=your_key
TELESCOPIUS_API_KEY=your_key
GEMINI_API_KEY=your_key
```

These are loaded at build time into `BuildConfig`.

## Architecture

```
┌─────────────────────────────────────┐
│        UI Layer (MainActivity)       │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│        Domain Layer                  │
│  ConditionEvaluator, SunTimeHelper  │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│        Data Layer                    │
│  AstroRepository, ApiClient, Room   │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│    External APIs & Local Database    │
└─────────────────────────────────────┘
```

**Key packages:**
- `data/` - Repository, database (Room), API clients (Retrofit), models
- `data/api/` - Retrofit API interfaces for Astrospheric, Telescopius, Sunrise-Sunset
- `data/models/` - Data models for API responses and database entities
- `domain/` - Business logic including AI-powered condition evaluation
- `workers/` - WorkManager background tasks for scheduled checks

## Core Components

**MainActivity.kt** - Main UI orchestrating the 2-phase check workflow: location condition scoring followed by astronomical target calculation. Contains astronomical calculations (hour angle, altitude, transit time).

**AstroRepository.kt** - Single entry point for all API calls, wrapping results in `Result<T>`.

**ConditionEvaluator.kt** - Uses Google Gemini API to analyze nightly observing conditions. Falls back to basic averaging if API fails.

**ApiClient.kt** - Singleton managing three Retrofit instances with OkHttp logging.

**WorkScheduler.kt / WeatherCheckWorker.kt** - Schedules automatic condition checks at 6 AM, 11 AM, and 4 PM daily.

## API Integrations

| API | Purpose |
|-----|---------|
| Astrospheric | Weather forecast (cloud cover, seeing, transparency) and sky data (moon/sun position) |
| Telescopius | Seasonal astronomical target lists |
| Sunrise-Sunset | Sun rise/set times for a location |
| Google Gemini | AI-powered condition analysis |

## Key Dependencies

- **Retrofit 2.9.0** / **OkHttp 4.12.0** - Networking
- **Room 2.6.1** - Local database
- **WorkManager 2.9.0** - Background scheduling
- **Coroutines 1.7.3** - Async operations

## Development Requirements

- Android Studio with SDK API 26+ (target API 36)
- Java 11+
- Valid API keys in `local.properties`
