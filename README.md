# ExpiryWatch

An Android app that keeps track of when your groceries, medicine and anything else expires, and
reminds you before things go off.

Built with Kotlin, Jetpack Compose and Material 3, following Google's recommended app architecture.

## Features

- **Item list** sorted by expiry, grouped into *Expired*, *Expiring soon* and *Fresh*, with
  search (ignores case and accents), category, location and group filters, and several sort
  orders.
- **Swipe to finish**: swipe right when an item is used up, left when it's thrown away, with undo.
  Deleting an item in the editor can be undone from the list too.
- **Your own groups**: put items in groups you name, such as "Camping kit" or "Baby", whatever
  their category. The list folds each group into one row with its emoji, how many items it holds
  and which expires next; tap it to see them, in the list's sort order. Search finds items by their
  group's name too.
- **Select several items**: long-press an item, or a product's or group's row for all of its
  items, then tap others to add them. Snooze their reminders, put them on the shopping list or
  move them to a group together.
- **Several of the same product**: give an item more expiry dates in the editor, for example one per
  carton of a multipack. Each date is kept as an item of its own, and the list folds items with
  the same name into one row showing the next to expire; tap it to see, edit or finish each one.
- **Barcode scanning** with the camera. The details you enter are remembered per barcode, and
  unknown products are looked up in [Open Food Facts](https://world.openfoodfacts.org) to fill in
  the name, brand and photo.
- **Opened items**: record when a package was opened and how long it keeps afterwards (e.g. "use
  within 3 days"); the earlier of the two dates counts.
- **Per item**: how many days before it expires an item counts as expiring soon, overriding the
  setting for all items, and what it cost.
- **Daily reminders** at a time you choose, summarizing expired and soon-to-expire items. Android
  may deliver them late to save battery; turn on **Exact time** to get them on time with an alarm.
  Since Android 12 this needs permission to set alarms; the app opens that system setting when you
  turn it on.
  - Act on the reminder from the notification: snooze everything in it for a week (or, as an
    advanced option, only what has expired), or mark each of the first few expired items used up
    or thrown away, or snooze it.
  - **Reminder health**: Settings, and a banner above the list, point out what keeps reminders from
    arriving (notifications or alarms not allowed, restricted battery use such as One UI's sleeping
    apps) and reminders that stopped coming, with a way to fix it.
- **Snooze reminders** about items you know about, such as honey past its date: for a week, a
  month, until a date or until you turn them back on. Snoozed items stay in the list, marked with
  when their reminders start again, but the daily reminder leaves them out.
- **Shopping lists**: keep several, such as groceries and pharmacy, and add to them by typing
  ("2 l milk", "eggs x12", "butter (unsalted)"), from suggestions of what you buy often, or by
  scanning a barcode. Products you've bought before go to the same category with their usual name.
  - The list is grouped by aisle (the item's category), in the order of the aisles in your shop.
    Tap an item to put it in the cart, swipe it away to remove it, and long-press it to change its
    name, amount, category or note.
  - **Buy again**: what expired, ran out or was thrown away in the last two weeks is suggested for
    the list until you add or dismiss it. Put expired items on the list from the item list, with
    one tap for all of them, or from the editor.
  - **Put away** what you bought: the items in the cart become tracked items, with expiry dates
    guessed from how long the product kept last time.
  - Share a list as text, add items from text pasted or shared from another app, keep the screen on
    while shopping, and open the list from a launcher shortcut.
- **Insights**: how much you used up versus threw away, a monthly trend and the most wasted
  categories. With prices entered, also the value used up in time and the money wasted.
- **Home-screen widget** listing what expires next, with buttons to mark an item used up or thrown
  away. Tap the filter under the title to switch between all items, those needing attention and
  each of your groups; each widget keeps its own choice.
- **Backup and restore** to a JSON file through the system file picker, and **automatic backups**
  every week to a folder you choose, keeping the last five.
- **Diagnostics**: an opt-in log of what the app does, to export when reporting an issue.
- **Advanced options** in Settings: what counts as the same product in the list (same name, same
  name and brand, or no grouping) and whether a product's items stay in one group across
  statuses; whether daily reminders include expired items, and whether their Snooze all button
  also snoozes items about to expire; and whether unknown barcodes are looked up online, so they
  can stay on the device.
- Customizable **categories, storage locations and groups**, made in Settings or right from the
  editor. Picking one, the two used most often come first, then the three used most recently, then
  the rest by name.
- Light/dark theme, dynamic color, launcher shortcuts and an adaptive layout (navigation rail on
  tablets and foldables).

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
  status, filtering and sorting, validation, insights, shopping lists and the backup format. It
  defines repository interfaces but doesn't depend on Android.
- **Data layer** (`core:data`): implements the repository interfaces with Room, DataStore, the Open
  Food Facts API and the Storage Access Framework.

| Module | Contents |
|--------|----------|
| `app` | Application, activity, navigation shell, flavor wiring |
| `feature:items` | The item list |
| `feature:editor` | Add/edit screen and barcode lookup |
| `feature:shopping` | Shopping lists, putting away what was bought, adding items from text |
| `feature:insights` | Usage and waste statistics |
| `feature:settings` | Settings, categories, locations and groups, backup |
| `feature:widget` | Glance home-screen widget |
| `core:model` | Domain models (pure Kotlin) |
| `core:domain` | Use cases and repository interfaces (pure Kotlin) |
| `core:common` | Coroutine dispatchers, clock, deep links (pure Kotlin) |
| `core:network` | Open Food Facts client (pure Kotlin) |
| `core:data` | Repository implementations and weekly automatic backups |
| `core:database` | Room database |
| `core:datastore` | User settings |
| `core:notifications` | Daily reminder work, exact alarms, notifications and their buttons, reminder health |
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
build. Signing release builds and publishing releases are described in [RELEASING.md](RELEASING.md),
and what changed in each release in [CHANGELOG.md](CHANGELOG.md).

## Testing and checks

```bash
./gradlew spotlessApply                      # format the code
./gradlew :core:domain:test testDebugUnitTest testPlayDebugUnitTest
./gradlew :app:lintPlayDebug
```

Domain, network and navigation logic has plain JVM unit tests. Room DAOs and repositories are
tested against an in-memory database with Robolectric, and ViewModels are tested with fakes from
`core:testing`. Key screens, such as the item list and the editor, also have Compose UI tests
that run on Robolectric. The [CI workflow](.github/workflows/ci.yml) runs formatting checks, all unit
tests, lint and both flavors' release builds on every push that changes more than documentation.

## Backup file format

Backups are versioned JSON (`"format": "expirywatch-backup"`, `"version": 2`) with categories,
locations, groups, items, remembered products and the shopping lists: their items, the products added to
them before and the order of the aisles. The format has its own DTOs, separate from the domain
models, so internal refactors can't silently change it. Importing validates the whole file before
replacing any data, and drops values the editor would reject, such as a negative quantity. Version 1
files, from before shopping lists, still import; they keep the shopping lists on the device, moved
to the restored categories and locations of the same name. Fields added since, such as groups,
reminder snoozes, prices and per-item reminder lead times, are optional, so older files read as not
having them. Automatic backups use the same format, in files named
`expirywatch-auto-backup-YYYY-MM-DD.json`.

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

It only contains technical details. Items are identified by number, never by name, scanned codes
other than product barcodes (such as QR codes) are recorded by length only, and the settings are
reported without the automatic backup folder or the products whose suggestions were dismissed. The log is capped
at about 512 KB, isn't included in backups, and never leaves the device unless you export it.

## Credits

- Product data comes from [Open Food Facts](https://world.openfoodfacts.org), available under the
  [Open Database License](https://opendatacommons.org/licenses/odbl/1-0/).
- Icons are [Material Icons](https://fonts.google.com/icons) (Apache License 2.0).
- Some feature ideas were inspired by [ExpiryWatcher](https://sourceforge.net/projects/expirywatcher/).
  ExpiryWatch shares no code with it.
