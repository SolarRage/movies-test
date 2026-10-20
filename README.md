# Movies Test

Android test assignment that searches the OMDb catalog, shows paginated results and movie details, stores favorites locally, and integrates native and interstitial test ads through a separate SDK module.

## Features

- Debounced OMDb search with automatic paging.
- Loading, empty, error, and retry states.
- Details loaded separately by IMDb ID.
- Room-backed favorites sorted by newest first.
- Search and Favorites tabs with Details on top of the navigation stack.
- Native test ads at zero-based UI indexes 1 and 3 when at least three movies are available.
- Movie results are displayed immediately; fixed-size placeholders reserve native-ad positions while ads load.
- Interstitial test ad shown once per installation on the second process foreground session.
- AdMob types and lifecycle contained inside the `:sdk` module.
- MVVM with unidirectional data flow and repository abstractions.

## Project structure

```text
app
├── data
│   ├── local          Room database, DAO, entities
│   ├── remote         OMDb API, DTOs, paging source
│   └── repository     Repository implementations
├── domain
│   ├── model
│   └── repository
├── di                  Koin application graph
└── ui
    ├── components
    ├── details
    ├── favorites
    ├── navigation
    └── search

sdk
├── public AdsManager API
└── internal
    ├── provider abstraction
    ├── AdMob provider
    ├── native-ad lifecycle
    └── interstitial policy and persistence
```

The module dependency direction is `:app -> :sdk`. The SDK does not depend on app models or UI, and Google Mobile Ads types do not cross its public API.

## Setup

1. Install Android SDK 36.1 and a compatible JDK.
2. The public OMDb key from the assignment is used by default. To override it, add a key to the ignored root `local.properties` file:

   ```properties
   OMDB_API_KEY=your_key_here
   ```

   Alternatively, set an `OMDB_API_KEY` environment variable before the Gradle build. Values from `local.properties` and the environment take precedence over the assignment key.

3. Build the project:

   ```shell
   ./gradlew test assembleDebug
   ```

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Advertising behavior

The project uses Google's official demo identifiers only:

- Native: `ca-app-pub-3940256099942544/2247696110`
- Interstitial: `ca-app-pub-3940256099942544/1033173712`
- Sample application ID: `ca-app-pub-3940256099942544~3347511713`

The SDK observes `ProcessLifecycleOwner`, so navigation and configuration changes do not count as app returns. The foreground counter and one-time shown flag are stored in `SharedPreferences`.

> **Return counting:** the initial launch is foreground session 1, so the first return to the app is session 2 and triggers the one-time interstitial.

Ad unit IDs are supplied through `AdsConfig`. `adsSdkModule` also accepts an `AdProvider` factory, so another provider can be added without changing `AdsManager` or the app UI. The provider-neutral API does not expose AdMob types.

## Testing

- JVM tests cover native-ad placement and lifecycle, the public ads API, OMDb paging/error mapping, Retrofit integration, ViewModels, concurrent favorite toggles, and the interstitial policy.
- Instrumentation tests cover Room ordering, ads manager behavior, and the core movie-card Compose UI.
- Real network calls and real ads are not used by automated tests.

Compile all test APKs with:

```shell
./gradlew test assembleDebugAndroidTest
```

Run instrumentation tests on an emulator or device with:

```shell
./gradlew connectedDebugAndroidTest
```

## Implementation notes

- Search starts 500 ms after the latest text change; blank queries are not sent.
- Paging uses OMDb's page parameter and its fixed page size of 10.
- Missing poster values (`N/A`) are normalized to no image.
- Favorites use IMDb ID as the primary key.
- Search remains usable while ads load; failed ad slots are removed without leaving empty rows.
