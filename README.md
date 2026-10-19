# Movies Test

Android home-assignment app that searches the OMDb catalog, shows paginated results and details, stores favorites locally, and integrates native and interstitial test ads through an isolated SDK module.

## Features

- Debounced OMDb search with automatic paging.
- Loading, empty, append-error, and retry states.
- Details loaded separately by IMDb ID.
- Room-backed favorites sorted by newest first.
- Search and Favorites tabs with Details on top of the navigation stack.
- Native test ads at zero-based UI indexes 1 and 3.
  - One movie: the first ad is placed at index 1.
  - Two or more movies: ads are placed at indexes 1 and 3.
- Interstitial test ad shown once per installation on the second process foreground session.
- AdMob types and lifecycle contained inside the `:sdk` module.

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

The module dependency direction is `:app -> :sdk`. The SDK never depends on app models or UI, and Google Mobile Ads types do not cross its public API.

## Setup

1. Install Android SDK 36.1 and a compatible JDK.
2. Add the OMDb key to the ignored root `local.properties` file:

   ```properties
   OMDB_API_KEY=your_key_here
   ```

   Alternatively, set an `OMDB_API_KEY` environment variable before the Gradle build.

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

Do not publish a production build with these IDs. Replace them with project-owned identifiers and complete the required consent/privacy flow before release.

The SDK observes `ProcessLifecycleOwner`, so navigation and configuration changes do not count as app returns. Its foreground counter and one-time shown flag are stored in a dedicated SharedPreferences file. That file is excluded from cloud backup and device transfer.

## Testing

- JVM tests cover native-ad placement, OMDb paging/error mapping, Retrofit request/response integration, and the second-foreground interstitial policy.
- Instrumentation tests cover Room ordering and the core movie-card Compose UI.
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
- Search remains usable if an ad fails to load; a failed ad slot is removed from rendering.
