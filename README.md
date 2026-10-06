# ExpiryWatch

An Android app that keeps track of when your groceries, medicine and anything else expires, and
reminds you before things go off.

Built with Kotlin, Jetpack Compose and Material 3, following Google's recommended app architecture.

## Features

- **Item list** sorted by expiry, grouped into *Expired*, *Expiring soon* and *Fresh*, with
  search (ignores case and accents), category and location filters, and several sort orders.
- **Swipe to finish**: swipe right when an item is used up, left when it's thrown away, with undo.
- **Several of the same product**: give an item more expiry dates in the editor, for example one per
  carton of a multipack. Each date is kept as an item of its own, and the list folds items with
  the same name into one row showing the next to expire; tap it to see, edit or finish each one.
- **Barcode scanning** with the camera. The details you enter are remembered per barcode, and
  unknown products are looked up in [Open Food Facts](https://world.openfoodfacts.org) to fill in
  the name, brand and photo.
- **Opened items**: record when a package was opened and how long it keeps afterwards (e.g. "use
  within 3 days"); the earlier of the two dates counts.
- **Daily reminders** at a time you choose, summarizing expired and soon-to-expire items. Android
  may deliver them late to save battery; turn on **Exact time** to get them on time with an alarm.
  Since Android 12 this needs permission to set alarms; the app opens that system setting when you
  turn it on.
- **Insights**: how much you used up versus threw away, a monthly trend and the most wasted
  categories.
- **Home-screen widget** listing what expires next.
- **Backup and restore** to a JSON file through the system file picker.
- **Diagnostics**: an opt-in log of what the app does, to export when reporting an issue.
- Customizable **categories and storage locations**, light/dark theme, dynamic color, launcher
  shortcuts and an adaptive layout (navigation rail on tablets and foldables).

## Build flavors

| Flavor | Barcode scanner | Notes |
|--------|-----------------|-------|
| `play` | Google ML Kit (bundled on-device model) | For Google Play |
| `foss` | ZXing | No proprietary Google libraries; suitable for F-Droid |

Both flavors share one scanner UI behind a `BarcodeAnalyzerFactory` interface; each flavor
contributes its implementation through Hilt.

## Architecture

The app follows a layered, multi-module architecture with unidirectional data flow:

```
        app ──────────────► feature:* ──────────────► core:ui, core:designsystem, core:navigation
         │                     │
         │                     ▼
         └──────────────► core:domain  (use cases + repository interfaces, pure Kotlin)
                               ▲
                               │ implements
                          core:data ──► core:database (Room)
                                    ──► core:datastore (DataStore)
                                    ──► core:network (Retrofit, pure Kotlin)
```

- **UI layer**: Compose screens observe immutable UI state exposed by Hilt ViewModels.
- **Domain layer** (`core:domain`): pure Kotlin use cases holding the business rules: expiry
  status, filtering and sorting, validation, insights and the backup format. It defines repository
  interfaces but doesn't depend on Android.
- **Data layer** (`core:data`): implements the repository interfaces with Room, DataStore, the Open
  Food Facts API and the Storage Access Framework.

| Module | Contents |
|--------|----------|
| `app` | Application, activity, navigation shell, flavor wiring |
| `feature:items` | The item list |
| `feature:editor` | Add/edit screen and barcode lookup |
| `feature:insights` | Usage and waste statistics |
| `feature:settings` | Settings, categories and locations, backup |
| `feature:widget` | Glance home-screen widget |
| `core:model` | Domain models (pure Kotlin) |
| `core:domain` | Use cases and repository interfaces (pure Kotlin) |
| `core:common` | Coroutine dispatchers, clock, deep links (pure Kotlin) |
| `core:network` | Open Food Facts client (pure Kotlin) |
| `core:data` | Repository implementations |
| `core:database` | Room database |
| `core:datastore` | User settings |
| `core:notifications` | Daily reminder work, exact alarms and notifications |
| `core:diagnostics` | Diagnostics log and report |
| `core:scanner`, `core:scanner-mlkit`, `core:scanner-zxing` | CameraX scanner UI and the two analyzers |
| `core:designsystem` | Theme, colors and basic components |
| `core:ui` | Shared composables and formatting |
| `core:navigation` | Navigation keys and the navigator |
| `core:testing` | Fakes and test utilities |
| `build-logic` | Gradle convention plugins shared by all modules |

## Tech stack

- Kotlin 2.4, coroutines and Flow
- Jetpack Compose with Material 3 and adaptive navigation
- Navigation 3
- Hilt for dependency injection, KSP for annotation processing
- Room, DataStore, WorkManager, Glance, CameraX
- Retrofit, OkHttp and kotlinx.serialization; Coil for images
- Google ML Kit (`play`) and ZXing (`foss`) for barcodes
- Gradle version catalog, convention plugins, configuration cache
- Spotless and ktlint for formatting, Android Lint
- JUnit, kotlinx-coroutines-test, Turbine, Robolectric and MockWebServer for tests

## Building

Requirements: JDK 17 or newer, and the Android SDK (Android Studio sets this up for you).

```bash
./gradlew assemblePlayDebug      # or assembleFossDebug
./gradlew installPlayDebug       # install on a connected device
```

Debug builds use the application ID suffix `.debug`, so they can be installed next to a release
build.

### Release signing

Release builds are minified with R8. To sign them with your own key, create `keystore.properties`
in the project root (it's git-ignored):

```properties
storeFile=path/to/release.jks
storePassword=…
keyAlias=…
keyPassword=…
```

Without it, release builds are signed with the debug key.

To sign the release APKs built by CI and the release workflow, add these repository secrets
(Settings → Secrets and variables → Actions):

| Secret | Value |
|--------|-------|
| `RELEASE_KEYSTORE_BASE64` | The keystore file, base64-encoded on a single line (`base64 -w 0 release.jks`) |
| `RELEASE_KEYSTORE_PASSWORD` | The keystore password |
| `RELEASE_KEY_ALIAS` | The key's alias |
| `RELEASE_KEY_PASSWORD` | Optional: the key's password, if it differs from the keystore's |

### Releases

The release workflow publishes a [GitHub release](https://github.com/abhi-k9/ExpiryWatch/releases)
with signed APKs of both flavors and their SHA-256 checksums. Start it either way:

- In the Actions tab, open **Release**, choose **Run workflow** and enter the version, such as
  `1.2.0`. The workflow tags the branch's latest commit as `v1.2.0`.
- Or push a tag: `git tag v1.2.0 && git push origin v1.2.0`.

The version sets the app's version name (`1.2.0`) and version code (`10200`), so each part must be
between 0 and 99. Local builds are version `1.0.0` unless you pass
`-Pexpirywatch.versionName=1.2.0`. The workflow refuses to publish without the signing secrets.

## Testing and checks

```bash
./gradlew spotlessApply                      # format the code
./gradlew :core:domain:test testDebugUnitTest testPlayDebugUnitTest
./gradlew :app:lintPlayDebug
```

Domain, network and navigation logic has plain JVM unit tests. Room DAOs and repositories are
tested against an in-memory database with Robolectric, and ViewModels are tested with fakes from
`core:testing`. The [CI workflow](.github/workflows/ci.yml) runs formatting checks, all unit
tests, both flavors' builds and lint on every push.

## Backup file format

Backups are versioned JSON (`"format": "expirywatch-backup"`, `"version": 1`) with categories,
locations, items and remembered products. The format has its own DTOs, separate from the domain
models, so internal refactors can't silently change it. Importing validates the whole file before
replacing any data.

## Diagnostics

To investigate an issue, turn on **Record diagnostics** in Settings, reproduce the issue, then
**Export log** to a file and attach it to the report. Debug builds record by default.

The log records what the app does and why: reminder scheduling and checks, notifications shown or
blocked, barcode lookups, backups, item changes, the camera, process starts and crashes. The export
starts with a snapshot of the app and device: versions, notification, alarm and battery settings,
scheduled reminders, recent process exits and how much data is stored.

Release builds are obfuscated by R8, so crash stack traces in the log name classes like `pp2`. To
read one, run the **Retrace** workflow from the Actions tab with the app version and flavor shown at
the top of the log, and paste the stack trace. Releases also include each flavor's R8 mapping file
for use with the Android SDK's `retrace` tool.

It only contains technical details. Items are identified by number, never by name, and scanned
codes other than product barcodes (such as QR codes) are recorded by length only. The log is capped
at about 512 KB, isn't included in backups, and never leaves the device unless you export it.

## Credits

- Product data comes from [Open Food Facts](https://world.openfoodfacts.org), available under the
  [Open Database License](https://opendatacommons.org/licenses/odbl/1-0/).
- Icons are [Material Icons](https://fonts.google.com/icons) (Apache License 2.0).
- Some feature ideas were inspired by [ExpiryWatcher](https://sourceforge.net/projects/expirywatcher/).
  ExpiryWatch shares no code with it.
