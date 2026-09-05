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

Debug builds are signed with a throwaway keystore. It is gitignored, so generate one first:

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

- **Sunset is not accounted for.** The Hebrew day starts at nightfall, so someone born after sunset
  has a Hebrew birthday one day later than the conversion gives. There is no "after sunset" toggle
  yet; the KosherJava dependency already ships the zmanim needed to add one.
- **No reminders.** Events are written without alarms, and the `.ics` has no `VALARM`.
- **No editing.** An event can be created and deleted, but not changed.
- **UI strings live in Kotlin**, in `AppStrings`, rather than `res/values/`. This works but gives up
  per-device locale, plurals and the standard translation tooling.
