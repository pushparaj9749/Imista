# Permissions — Imista Forward

## Sender (`com.imista.forward.sender`)

| Permission | Purpose | Required? |
|------------|---------|-----------|
| `INTERNET` | Send to Telegram API | Yes |
| `ACCESS_NETWORK_STATE` | Check connectivity for queue | Yes |
| `POST_NOTIFICATIONS` | Foreground service & status | Android 13+ permission runtime |
| `FOREGROUND_SERVICE` + `dataSync` | WorkManager delivery | Yes |
| `RECEIVE_BOOT_COMPLETED` | Re-enqueue after reboot | Yes |
| `QUERY_ALL_PACKAGES` | List apps for filtering | Yes (filtered to 200 for display) |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Capture notifications | Yes — system grants via settings |
| `BIND_DEVICE_ADMIN` | Optional lock device | No — purely optional, explained |

**NotificationListenerService** only sees what Android shows in notifications. No hidden DB access. Filtering respects visibility.

**Device Admin**: Only `force-lock` / `watch-login`. Activation screen explains, shows status, user-deactivatable, no uninstall bypass.

## Receiver (`com.imista.forward.receiver`)

| Permission | Purpose |
|------------|---------|
| `INTERNET` | Verify bot |
| `ACCESS_NETWORK_STATE` | Check connectivity |
| `POST_NOTIFICATIONS` | Mirror notifications if enabled |
| `RECEIVE_BOOT_COMPLETED` | Reschedule polling |
| `QUERY_ALL_PACKAGES` | (optional) list apps for filter display |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Capture Telegram notifications to reconstruct |

Receiver **does not** request Notification Access unless user enables Telegram reconstruction feature. No Device Admin requested.

## Handling Revocation
- If Listener disabled → Dashboard shows `Disabled`, forwarding pauses, pending queues.
- If Device Admin disabled → shows `Inactive`, forwarding continues.
- App never requests permissions via overlay tricks; always via system settings intents.

## Battery Optimization
Android Doze + OEM killers may delay WorkManager. User advised to exclude Imista from battery optimization. No false promise of permanent alive.
