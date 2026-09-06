# Hebrew Calendar Sync

An offline Android app for recurring **Hebrew-date** events — birthdays, anniversaries and
yahrzeits. It converts a Hebrew date to its Gregorian equivalent for each year ahead, then either
writes those dates straight into a device calendar or exports them as an `.ics` file.

The hard part is not the conversion, it's the rules around it: which Adar a date falls in during a
leap year, and what happens to a 30th that does not exist in a shorter month. Those rules live in
[`HebrewCalendarEngine`](app/src/main/java/com/example/domain/hebrew/HebrewCalendarEngine.kt) and are
covered by [unit tests](app/src/test/java/com/example/HebrewCalendarEngineTest.kt).

## Features

- Enter a date as either Gregorian or Hebrew, with live conversion between the two
- Yearly or monthly Hebrew recurrence, projected 10–100 years ahead
- Choice of halachic rule for leap years: Adar II (default), Adar I, or both
- **After-sunset toggle** — the Hebrew day begins at nightfall, so an event after sunset belongs to
  the following Hebrew day. Without this a yahrzeit is observed a day early.
- Optional reminders, written as calendar alarms and as `VALARM` in exported files
- A colour per event, from Google Calendar's own palette
- The weekday is shown beside every converted date, so a conversion can be sanity-checked
- Edit an existing event; its calendar entries are rewritten to match
- Sync to any writable device calendar, or create a dedicated local one
- Or create a calendar **inside your Google account** and sync there via the Calendar REST API,
  so the dates appear on every device signed into that account
- Export to `.ics` for any other calendar app
- Hebrew/English UI with full RTL support, switchable without a restart
- Light and dark themes, following the system setting

## Requirements

| | |
|---|---|
| JDK | 21 (Robolectric requires it for `compileSdk` 36) |
| Android SDK | platform 36, build-tools 36.0.0 |
| Min SDK | 24 (Android 7.0) |

## Building

```bash
./gradlew assembleDebug
```

Debug builds are signed with a local keystore. It is gitignored, so generate one first:

```bash
keytool -genkeypair -v -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
```

Run the tests:

```bash
./gradlew testDebugUnitTest
```

Release builds read their signing config from the environment, and are skipped entirely if no
keystore is present:

```bash
KEYSTORE_PATH=/path/to/upload.jks STORE_PASSWORD=... KEY_PASSWORD=... ./gradlew assembleRelease
```

The key alias is `upload`.

### Installing a CI build

Each run uploads two artifacts:

| artifact | size | notes |
|---|---|---|
| `app-release-apk` | ~1.4 MB | shrunk by R8 — install this one |
| `app-debug-apk` | ~17 MB | unshrunk, bundles the Compose tooling; for debugging |

Both are signed with the same key, so either can replace the other without an uninstall.

### A note on signing

CI signs the APK with the `DEBUG_KEYSTORE_BASE64` repository secret if it is set, and with a
freshly generated throwaway key otherwise. A new key every run means every APK has a *different*
signature, and Android will not install one over another — you get "App not installed" and have to
uninstall first, losing the local database. To make builds upgrade in place, set the secret once:

```bash
base64 -i debug.keystore | pbcopy   # paste into Settings > Secrets > Actions
```

## How it works

```
ui/            Compose screens, theme, the AppStrings resource facade
res/values/    English strings; Hebrew in res/values-iw/ (note: 'iw', not 'he')
ui/viewmodel/  HebrewCalendarViewModel — the single source of UI state
domain/        HebrewCalendarEngine (dates), CalendarSyncManager (device provider),
               GoogleCalendarCloudManager (Calendar REST API), IcsExporter
data/          Room database, entity, DAO, repository
```

Three things are worth knowing before changing anything:

**The engine is the only place that decides when an event occurs.** The calendar screen asks it
which events land on a day rather than comparing Hebrew day/month numbers itself. Two hand-rolled
copies of that rule used to exist, and both had drifted.

**Calendar rows are deleted by tag, never by title.** Every row written carries a unique `syncTag`,
as both an `ExtendedProperties` entry and a marker in the description. Deletion matches on that.
There is deliberately no title-only fallback: an earlier version had one, and it would delete
identically-named events the user had created themselves.

**Cloud events are tagged in `extendedProperties.private`.** That field is queryable through the
`privateExtendedProperty` parameter, and it is the only thing that makes a cloud event findable
again — and therefore deletable. A description marker is not enough. Bulk writes also retry
429/5xx and rate-limit 403s with backoff, because a century of occurrences is hundreds of requests.

**All-day events are stored at midnight UTC** with `EVENT_TIMEZONE = "UTC"`. Anything else shifts
the day for users away from GMT.

**Hebrew resources live in `res/values-iw/`, not `values-he/`.** `iw` is the legacy ISO code, and
it is the qualifier Android's resource system expects. Getting it wrong fails silently — the app
just falls back to English — so `AppStringsTest` asserts that Hebrew actually resolves.

## Google Calendar setup

Cloud sync needs an OAuth client of its own; the code alone is not enough. In a Google Cloud
project: enable the Calendar API, add the `.../auth/calendar` scope to the consent screen, and
create an **Android** OAuth client for the package `com.aistudio.hebrewcalendar.zrqk` with the
SHA-1 of the signing key.

OAuth binds to that certificate, which is why `DEBUG_KEYSTORE_BASE64` is required rather than
optional — a per-build key changes the SHA-1 and sign-in stops working. Get the fingerprint with:

```bash
keytool -list -v -keystore debug.keystore -storepass android -alias androiddebugkey | grep SHA1
```

Until the client exists, the sign-in button compiles and fails at runtime.

## Known gaps

- **Sunset is a manual toggle, not a computed time.** Checking "after sunset" moves the Hebrew date
  forward a day; it does not look up the actual sunset for a location. KosherJava ships the zmanim
  to do that properly, but it needs a location and therefore a permission.
- **Reminders are limited to the day or week before, at 09:00.** An all-day event starts at
  midnight and the calendar provider counts reminders backwards from the start, so "09:00 on the day
  itself" cannot be expressed.
- **The in-app language switch is not the system per-app language.** It builds a configuration
  context by hand so the toggle can apply without a restart. Android 13's per-app language setting
  would be more idiomatic but needs `appcompat` for anything below API 33.
- **Cloud deletion needs you signed in.** Removing an app event deletes its cloud entries only
  while a Google account is connected; otherwise the app says so rather than pretending it
  succeeded.
- **`GoogleAuthUtil` is the deprecated auth path.** Credential Manager is the current one.
- **Per-event colour on the device calendar is best effort.** The cloud path sets `colorId`, which
  is well defined. `Events.EVENT_COLOR` is honoured by some providers and silently ignored by
  others, which only accept `EVENT_COLOR_KEY` — and that needs a `Colors` row only a sync adapter
  can create.
- **No instrumented tests.** Unit and Robolectric coverage only.
