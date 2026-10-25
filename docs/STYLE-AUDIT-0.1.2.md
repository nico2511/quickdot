# Quickdot 0.1.2 style audit

Master `26e88443dd4c2930b89de91d4a48cb6a81485b9f` (versionName 0.1.2, versionCode 3). The first pass was read-only. Statuses below the fold are that baseline. The leftovers pass on `cursor/style-leftovers-d235` is the current status.

## After the leftovers pass

Shared leftovers from the grouped list are in the app. `assembleDebug` passes.

| Mark | Surfaces now |
| --- | --- |
| Styled | Inbox, search, drawer, archived, thread, attachment sheet, conversation details, settings and its pickers, notification prefs, swipe actions, about, backup, scheduled, blocking and its sub-screens, message management, quick reply composer, `QkDialog` and stock alerts (light and dark), default-SMS hint, gallery chrome, both widgets (square plate, square compose, 4dp speak-unseen card). Action-mode menus use the pixel set. |
| Partially | Message notification. The small icon is still the pixel Q and the action glyphs are the pixel set. The shade itself is platform MessagingStyle. |
| Legacy | Archive snackbar (`Snackbar.make`, not in this pass). Shortcut mipmaps. Splash is still a black window. |

What changed:

- Compose control is a 4dp square, `qd_add`, signal red. Confirmed on the light inbox frame (corner inset ~4dp, not a circle).
- Thread composer and the audio chips are 4dp. Quick reply send is a square accent key.
- Material UI icons in layouts, menus, swipe, and notification actions are the square-pixel set. Blocking-manager brand marks and `ic_notification` stay.
- `QkDialog` radios are square. `QuickdotAlertDialog` is the alert theme (4dp window, Doto title, Space Mono buttons, not all-caps). Light and dark night-mode dialogs were captured open.
- Default-SMS hint: sprocket, dotted rule, display title, square “Change” label. Light frame shows the strings “Love texting again” / “Change”.
- Gallery: dark sprocket, display title, pixel back, missing-glyph error page. `GalleryActivity` is not exported and needs a part id, so there is no device frame.
- Conversation widget drops `circle_mask` for a 4dp plate and `qd_add`. Speak-unseen sits on a 4dp card. Dark widget-picker frames show the Quickdot row.

Frames: `/opt/cursor/artifacts/style-audit/before/shots/` (the 0.1.2 audit), `/opt/cursor/artifacts/style-audit/after/` (this pass), `/opt/cursor/artifacts/style-audit/contact-sheet.png` (after), `/opt/cursor/artifacts/style-audit/contact-sheet-before.png` (original 72-frame sheet).

Still legacy on purpose: the archive snackbar and the shortcut mipmaps. System role sheet, permission dialogs, and toasts stay platform UI.

## How the frames were taken

The API 34 `aosp_atd` image never produced a framebuffer. `screencap`, `screenrecord`, and `-gpu swiftshader_indirect` / `-gpu software` (the emulator remapped that to lavapipe + swangle) all returned a solid black 1080×2400 frame, including while System UI and Quickdot were focused.

Frames below are from a Pixel 6 AVD on **API 33 `default` x86_64**, GPU `swiftshader_indirect`, software CPU (`-accel off`). Light and dark follow the system night mode. Quickdot’s night setting defaults to System, so `cmd uimode night` restyles the app.

A few surfaces could not be opened from `adb` because the activity is not exported, or because they need an MMS part, a placed widget, or a system picker. Those are classified from the layout and code and called out in the table.

## Rubric

- **Styled** — the screen is carried by Quickdot marks: sprocket rail, matrix band or dotted rule, Doto / Space Mono, square 4dp stamps, speaker-ink avatars. Signal red `#E10600` only where it is the accent or a destructive action.
- **Partially** — those marks are present, and Material leftovers are still part of the surface (circle control, 22dp pill, Material 24dp icons, system dialog chrome).
- **Legacy** — old QKSMS / Material chrome, with no Quickdot marks that define the surface.

Effort is the size of the change, not calendar time.

- **Small** — one layout, menu, or icon swap. No new behavior.
- **Medium** — one screen plus its dialogs, or one shared component used in many places.
- **Large** — remote views, notification shade structure, or a flow that crosses activities and system UI.

## Capture method note

| Path | What it is |
| --- | --- |
| `/opt/cursor/artifacts/style-audit/contact-sheet.png` | Contact sheet, 72 frames |
| `/opt/cursor/artifacts/style-audit/shots/` | Light and dark PNGs |
| `/opt/cursor/artifacts/style-audit/icons/` | Launcher vector, launcher PNG, notification icon, legacy shortcut PNG |

Black frames were dropped (API 33 still returns a black buffer during the black splash). `light-schedule-compose` was the same file as `light-scheduled` (the schedule button did not leave the empty state). Contacts was not opened as its own activity.

## Summary

The redesigned spine is in place: inbox rows, thread, drawer structure, settings, accent picker, and the drawer destinations (backup, scheduled, blocking, message management, about) all read as Quickdot. What is left is a consistent set of leftovers, not a second visual language on those screens.

Almost nothing user-facing is fully **Styled** once you count the control in the corner. The accent picker is the cleanest screen. Everything else that was restyled in 0.1.2 is **Partially** because Material icons, a circle compose button, a 22dp composer, or a stock dialog is still on it.

The genuine **Legacy** surfaces are the shared `QkDialog` (radios), alert dialogs, the archive snackbar, the default-SMS hint, quick reply’s pill composer, the gallery, both widgets, shortcut mipmaps, and the main/scheduled/blocked selection menus.

| Mark | Surfaces |
| --- | --- |
| Styled | Baseline: accent picker, launcher foreground, notification small icon, empty-state mark. After the leftovers pass, see the table at the top. |
| Partially | Baseline list below. After the leftovers pass, only the message notification shade is still partial. |
| Legacy | Baseline list below. After the leftovers pass: archive snackbar and shortcut mipmaps. |

## Every surface

| Surface | Mark | Light | Dark | What’s left | Effort |
| --- | --- | --- | --- | --- | --- |
| Splash (`AppLaunchTheme`) | Partially | Black window only; not a separate frame | same | `windowBackground` is `@color/black`. No pixel-Q | Small |
| Launcher icon | Styled | `icons/ic_launcher.png`, foreground vector | monochrome asset exists | White pixel Q, one `#E10600` pixel | — |
| Shortcut icons | Legacy | `icons/ic_shortcut_compose.png` | n/a | Raster Material compose/person marks in `mipmap-xxxhdpi` | Small |
| Inbox | Partially | `light-inbox.png` | `dark-inbox.png` | Rows, speaker avatar, dotted rule, Doto title, and the search field are Quickdot. Compose control is still `circle_ripple` + `ic_add`. Sprocket is hidden on Inbox | Small (the circle) |
| Search | Partially | `light-search.png` | `dark-search.png` | Results row uses display/label type. Empty copy is plain | Small |
| Drawer | Partially | `light-drawer.png` | `dark-drawer.png` | Matrix section labels and square row marks. Every row icon is Material (`ic_inbox`, `ic_archive`, `ic_settings`, …) except message management | Small |
| Archived | Partially | `light-archived.png` | `dark-archived.png` | Sprocket and the empty mark. Same circle compose button as Inbox | Small |
| Thread | Partially | `light-thread.png` | `dark-thread.png` | Sprocket, dotted time rule, square avatar, `qd_` toolbar icons. Composer is `rounded_rectangle_22dp`. Mic is a borderless Material glyph | Medium |
| New message | Partially | `light-compose-new.png` | `dark-compose-new.png` | Recent row is the restyled contact row. Picker chrome is still a plain field | Small |
| Attachment sheet | Partially | `light-compose-attach.png` | `dark-compose-attach.png` | Square keys and 4dp labels. Glyphs are Material (`ic_person`, `ic_event`, `ic_mic`, camera, gallery) | Small |
| Conversation details | Partially | `light-conversation-info.png` | `dark-conversation-info.png` | Sprocket, matrix sections, square avatar. Row icons are Material. Name/delete dialogs are stock alerts | Small icons, small dialogs |
| Settings | Partially | `light-settings.png`, `light-settings-scrolled.png` | `dark-settings.png`, `dark-settings-scrolled.png` | Sprocket, matrix categories, square switches, Pro card (UI only). Every row icon is Material | Small |
| Accent picker | Styled | `light-theme-picker.png` | `dark-theme-picker.png` | Square swatches (12 presets + Signal), `#RRGGBB` hex with contrast warning, per-conversation reset, live preview, sprocket, matrix. Play locks non-Signal presets and hex until Quickdot Pro | — |
| Night / font / delay dialogs | Legacy | `light-dialog-night.png`, `light-dialog-font.png`, `light-dialog-delay.png` | matching `dark-dialog-*` | `QkDialog`: bold title, `radio_button` rows, no matrix. Same chrome for MMS size, link handling, notification preview, swipe-action choice | Medium (one component) |
| Signature dialog | Legacy | not separately framed (row opens `TextInputDialog`) | | Bold field inside an alert | Small |
| Swipe actions | Partially | `light-swipe-actions.png` | not re-shot after the dark pass died on a disk error | Sprocket and a square avatar in the preview. Action icons are Material. Title has no matrix band | Small |
| Notification prefs | Partially | `light-notification-prefs.png`, scrolled | `dark-notification-prefs.png`, scrolled | Matrix categories, square switches. Icons are Material. Preview/action pickers are `QkDialog` | Small icons |
| About | Partially | `light-about.png`, scrolled | `dark-about.png`, scrolled | Sprocket, matrix, Doto version, Quik/QKSMS credits. Icons are mixed (`qd_info`, `qd_share`, and Material) | Small |
| Backup | Partially | `light-backup.png`, scrolled | `dark-backup.png`, scrolled | Sprocket, matrix, square button. Icons and the progress glyph are Material. Restore list dialog is closer to Quickdot (`backup_list_dialog` uses display/label type) but was not opened (no backup files) | Small |
| Scheduled | Partially | `light-scheduled.png` | `dark-scheduled.png` | Sprocket and a styled empty sample. Square button still uses `ic_schedule_send`. Overflow menu is Material. Delete / send-now / edit are stock alerts. The schedule button did not open compose in this session | Small button + menu; small dialogs |
| Blocking | Partially | `light-blocking.png` | `dark-blocking.png` | Sprocket, matrix, square switches, signal red on Drop. Icons are mixed (`qd_call`, `qd_delete`, and Material) | Small |
| Blocking manager | Partially | `light-blocking-manager.png` | `dark-blocking-manager.png` | Sprocket. Third-party marks are intentional. Trailing chevron/check is Material | Small |
| Blocked numbers | Partially | `light-blocked-numbers.png` | `dark-blocked-numbers.png` | Empty mark is Styled. Add dialog (`light-dialog-block-number.png`) is a stock alert. Dark dialog frame was black and was dropped | Small dialog |
| Content filters | Partially | `light-content-filters.png`, `light-dialog-filter.png` | dark pair | Same split: styled empty, legacy add dialog | Small dialog |
| Blocked messages | Partially | `light-blocked-messages.png` | `dark-blocked-messages.png` | Styled empty. Selection menu (`menu/blocked_messages.xml`) is Material | Small |
| Message management | Partially | `light-message-utils.png`, scrolled | dark pair | Sprocket, matrix, red on destructive rows. Icons are a mix. Dedup confirm and auto-delete warning are stock alerts. Auto-delete picker is `AutoDeleteDialog` | Medium |
| Contacts picker | Partially | not captured (activity is not exported; no in-app path was completed) | | Layout has sprocket, dotted rules, styled empty and permission states | Small if a Material icon remains |
| Gallery / media viewer | Legacy | not captured (`GalleryActivity` needs a part id and is not exported) | | Translucent toolbar, no sprocket or matrix. Invalid page is a plain string. Save toast is a system toast | Medium |
| Quick reply | Partially | `light-qkreply.png` | not shot | 4dp card and a display title. Composer is the 22dp pill. Send and SIM icons are Material. Menu (`qkreply.xml`) is Material. On API 24+ the notification Reply action is the system inline reply, not this activity (`qkreply` defaults off) | Medium |
| Message notification | Partially | `light-notification-shade.png` | not shot | Small icon is the pixel Q. Colour follows the effective accent (Signal on free Play, chosen or per-conversation with Pro). Shade layout is MessagingStyle. Actions (Mark read, Reply, Call) use Material icons. Reply is `RemoteInput` | Medium for icons. Large to replace the shade, and not worth it |
| Failed-send / MMS download notifications | Partially | not triggered | | Same pattern: accent color, Material small icon (`ic_notification_failed`, `ic_file_download`) | Small icons |
| Conversation widget | Legacy | `light-widget-picker.png`, `light-widget-quickdot.png` (picker preview, not placed on the home screen) | widget code follows system night | `rounded_rectangle_4dp` card, `circle_mask` avatar, Material add icon, framework `TextView`s. RemoteViews cannot use Doto without prerendered bitmaps | Medium. Large only if custom type is required |
| Speak-unseen widget | Legacy | not placed | | Full-bleed image, no chrome | Small |
| Share / SEND target | Partially | compose is the target; system chooser was not shot | | `ComposeActivity` receives `SEND` / `SENDTO`. Chooser row uses the styled launcher icon. Direct-share shortcut art is the legacy mipmap | Small (the shortcut) |
| Default SMS | Legacy hint + system sheet | `light-default-sms-hint.png` (“Love texting again” / CHANGE) | not shot | `main_permission_hint.xml` is the old bold, all-caps snackbar. Tapping CHANGE opens the platform role sheet (`RoleManager.ROLE_SMS`), which is not app chrome | Small for the hint |
| Runtime permission dialogs | System | not shot | | Platform dialogs. Contacts empty/permission layouts in the app are already on the Quickdot empty mark | — |
| Archive snackbar | Legacy | not triggered | | `Snackbar.make` with the platform snackbar. Undo is Material | Small |
| Toasts | System | not shot | | `makeToast` (copy address, backup error, scheduled, gallery saved, …) | — |
| Inbox selection menu | Legacy | not shot (needs a long-press) | | `menu/main.xml` uses Material `ic_*`, unlike `menu/compose.xml` which is already `qd_*` | Small |
| Scheduled / blocked selection menus | Legacy | not shot | | Material icons in `scheduled_messages.xml` and `blocked_messages.xml` | Small |
| MMS part menu | Legacy | not shot | | Plain text context menu | Small |
| Changelog dialog | Legacy | not shown (`changelogVersion` is already 3) | | Bold title, all-caps buttons, no matrix | Small |
| Time / ringtone pickers | System | not shot | | `TimePickerDialog` and the ringtone picker for night hours and notification sound | — |

## Legacy and partial work, grouped

Do these as shared pieces. Fixing them one screen at a time will redo the same dialog five times.

1. **`QkDialog` + `menu_list_item` radios** — medium. One layout covers night mode, font size, send delay, MMS size, link handling, notification preview, notification actions, and the swipe-action chooser.
2. **Stock `AlertDialog` confirms** — small each, medium together. Delete, block, scheduled send/edit, dedup, auto-delete warning, blocking-manager copy. They can share one square, non-radio dialog.
3. **Material icons on otherwise restyled screens** — small. Drawer, settings, details, about, backup, blocking, notification prefs, swipe actions, attachment sheet. The `qd_` set already used on the compose toolbar is the replacement.
4. **Circle compose button** — small. `main_activity.xml` `circle_ripple`. Inbox and Archived.
5. **22dp composer** — medium. `compose_activity.xml` message field, `qkreply_activity.xml`, `mms_audio_preview_list_item.xml`, and the audio-message player.
6. **Action-mode menus** — small. `menu/main.xml`, `scheduled_messages.xml`, `blocked_messages.xml`, `qkreply.xml` still use Material icons. `menu/compose.xml` is already `qd_`.
7. **Default-SMS hint and archive snackbar** — small. Both are the old snackbar (bold title, all-caps action).
8. **Gallery** — medium. Toolbar, error page, and the save toast.
9. **Widgets** — medium for a square avatar, no circle mask, and a square compose mark. Large only if the widget must use Doto (RemoteViews needs prerendered text).
10. **Shortcut mipmaps** — small. Three legacy PNGs.
11. **Splash** — small. Put the pixel Q on the black window.

## Not a style bug

- Quickdot Pro on Settings is the purchase recap: price from Play when available, summary, Buy, Restore, and Unlocked. On the `play` flavor, `ProBillingHook.enforcementEnabled` is on: free users see the full Nothing-inspired chrome with Signal red `#E10600` as the painted accent; Pro unlocks custom accents, the 12-colour palette, hex input, per-conversation accents, and reply quotes. The `foss` flavor skips enforcement and includes everything.
- `HSVPickerView` remains in the tree for legacy callers; the accent screen uses the hex field and preset grid instead.
- System role sheet, runtime permission dialogs, ringtone picker, time picker, and toasts are platform UI.
