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
- Edit an existing event; its calendar entries are rewritten to match
- Sync to any writable device calendar, or create a dedicated local one
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

### A note on installing CI builds

CI signs the APK with the `DEBUG_KEYSTORE_BASE64` repository secret if it is set, and with a
freshly generated throwaway key otherwise. A new key every run means every APK has a *different*
signature, and Android will not install one over another — you get "App not installed" and have to
uninstall first, losing the local database. To make builds upgrade in place, set the secret once:

```bash
base64 -i debug.keystore | pbcopy   # paste into Settings > Secrets > Actions
```

## How it works

```
ui/            Compose screens, the AppStrings bilingual table, theme
ui/viewmodel/  HebrewCalendarViewModel — the single source of UI state
domain/        HebrewCalendarEngine (dates), CalendarSyncManager (provider), IcsExporter
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

**All-day events are stored at midnight UTC** with `EVENT_TIMEZONE = "UTC"`. Anything else shifts
the day for users away from GMT.

## Known gaps

- **Sunset is a manual toggle, not a computed time.** Checking "after sunset" moves the Hebrew date
  forward a day; it does not look up the actual sunset for a location. KosherJava ships the zmanim
  to do that properly, but it needs a location and therefore a permission.
- **Reminders are limited to the day or week before, at 09:00.** An all-day event starts at
  midnight and the calendar provider counts reminders backwards from the start, so "09:00 on the day
  itself" cannot be expressed.
- **UI strings live in Kotlin**, in `AppStrings`, rather than `res/values/`. This works but gives up
  per-device locale, plurals and the standard translation tooling. Migrating is mechanical but
  touches every screen, so it belongs in its own change.
- **No instrumented tests.** Unit and Robolectric coverage only.
