# Changelog

What changed in each release of ExpiryWatch, newest first. The APKs are on the
[releases page](https://github.com/abhi-k9/ExpiryWatch/releases).

## 1.9.0 (2026-10-28)

- Search opens from the lower add/scan controls, with its input, clear and close controls near
  the bottom of the screen and above the keyboard.
- Save controls sit at the bottom of the item editor. **Save & add another** keeps the category
  and storage location for the next item while clearing the previous product's details and dates.
- Recent-category shortcuts beside the category field make repeated selections a single tap.
- The editor's full category, storage location and group lists stay alphabetical. Shared pickers
  open in scrollable bottom sheets near the current selection, with a visible selection indicator
  and a pinned action for creating a new label.

## 1.8.1 (2026-10-22)

- The diagnostics report no longer contains the automatic backup folder's path or the names of
  products whose restock suggestions were dismissed; it says only whether automatic backups are on,
  how the last one went and how many suggestions were dismissed.
- The diagnostics report says whether the app's background use is restricted.
- Importing a backup drops prices above the editor's maximum, like other values the editor would
  reject.

## 1.8.0 (2026-10-22)

- **Reminder health**: Settings and a banner above the item list point out what keeps reminders
  from arriving (notifications or alarms not allowed, restricted battery use such as One UI's
  sleeping apps) and reminders that stopped coming, with a way to fix it.
- **Act on reminders from the notification**: snooze everything in the daily reminder for a week,
  and mark each of the first three expired items used up or thrown away, or snooze it, from a
  notification of its own. An advanced option leaves items that are only expiring soon out of
  "Snooze all".
- **Per item**: how many days before it expires an item counts as expiring soon, and its price.
- **Insights** show the value used up in time and the money wasted, once prices are entered.
- **Automatic backups** every week to a folder you choose, keeping the last five.
- **Widget**: buttons to mark items used up or thrown away, a filter for all items, those needing
  attention or one of your groups, and group emoji on items.
- Filter the item list by group; a group's items follow the list's sort order.
- Deleting an item in the editor can be undone from the list.
- The shopping list's add field and the editor's name field get the keyboard back after rotating
  the screen.

## 1.7.0 (2026-10-20)

- **Your own groups**, such as "Camping kit" or "Baby": the list folds each group into one row.
  Move selected items to a group, and manage groups in Settings.
- Pickers for categories, locations and groups list the two used most often first, then the three
  used most recently, then the rest by name.
- Make a new category, location or group right from the editor.

## 1.6.0 (2026-10-18)

- **Snooze reminders** about items for a week, a month, until a date or until you turn them back
  on. Snoozed items stay in the list but the daily reminder leaves them out.
- **Select several items** with a long press, to snooze their reminders or put them on the
  shopping list together.

## 1.5.0 (2026-10-11)

- **Shopping lists**: keep several, add to them by typing ("2 l milk"), from suggestions or by
  scanning a barcode, arrange the aisles in your shop's order, share a list as text and add items
  from shared text.
- **Buy again** suggestions for what expired, ran out or was thrown away recently.
- **Put away** what you bought as tracked items, with expiry dates guessed from last time.
- Put tracked items, or everything expired, on the shopping list.
- Backups include shopping lists.

## 1.4.0 (2026-10-06)

- **Advanced options** in Settings: what the list groups as the same product, whether a product's
  items stay in one group across statuses, whether reminders include expired items, and whether
  unknown barcodes are looked up online.

## 1.3.0 (2026-10-06)

- Add several expiry dates for the same product, such as a multipack, in the editor.
- The list folds items with the same name into one row showing the next to expire.

## 1.2.0 (2026-10-04)

- **Exact-time reminders** with an alarm, when the system allows the app to set alarms. Alarms are
  set again after a restart, an update or a clock change.

## 1.1.2 (2026-10-04)

- Reminders no longer drift to other times of day after a late run.
- The widget shows changes made while it's on screen.
- Backups with invalid ids are rejected instead of crashing the app, and storage errors during
  backups and diagnostics exports are reported instead of crashing.
- The reminders switch explains when notifications are blocked and opens the system setting.
- Failures in background work and unreadable camera frames no longer crash the app.

## 1.1.1 (2026-10-03)

- Fix the crash when opening the barcode scanner in release builds.
- Releases include R8 mapping files, and a Retrace workflow turns crash stack traces back into
  readable ones.

## 1.1.0 (2026-10-01)

- **Diagnostics**: an opt-in log of what the app does, to export from Settings when reporting an
  issue.

## 1.0.0 (2026-10-01)

- First release: the item list with search, filters and sorting, swipe to finish, barcode scanning
  with Open Food Facts lookups, opened-item tracking, daily reminders, insights, the home-screen
  widget, categories and locations, and backup and restore.
