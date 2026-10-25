# Play Console setup for Quickdot Pro

The `play` flavor (`applicationId` `com.nico2511.quickdot`) sells one in-app product. The `foss` flavor does not call Play Billing and includes every Pro feature. Create the product before a license tester can complete a purchase. The price is configured only in Play Console; the app shows the formatted price from Play when available, otherwise the string `One-time purchase` / `Achat unique`.

## What is free, Pro, and FOSS

| | Play (free) | Play + Quickdot Pro | FOSS build |
| --- | --- | --- | --- |
| Nothing-inspired UI (typography, layout, icons) | Yes | Yes | Yes |
| Accent colour | Signal red everywhere | Custom accent everywhere | Custom accent everywhere |
| Accent palette | Signal only | 12 preset colours + `#RRGGBB` hex with contrast check | Same as Pro |
| Per-conversation accent | No | Yes (bubbles, Send, rules, avatars, notifications) | Yes |
| Reply with quote | No | Yes | Yes |
| SMS send/receive | Unrestricted | Unrestricted | Unrestricted |

Quickdot Pro is a **one-time** purchase, not a subscription and not a consumable.

## 1. App

1. Open [Play Console](https://play.google.com/console).
2. Create the app, or open the existing one, with package name `com.nico2511.quickdot`.
3. Upload the signed `play` AAB (`Quickdot-v*-play-release.aab`) to an internal testing track. The product query returns nothing until the app build that asks for `quickdot_pro` is on a track the tester can install.

## 2. One-time product `quickdot_pro`

1. Monetize with Play → Products → One-time products.
2. Create a product with product ID exactly `quickdot_pro`.
3. Name: **Quickdot Pro**.
4. Description (suggested): *Unlock custom accent colours, per-conversation accents, and reply quotes. Sending and receiving messages stays free.*
5. Set the price in Play Console (localized per country).
6. Activate the product. Inactive products are not returned by `queryProductDetailsAsync`.

## 3. License testers

1. Play Console → Settings → License testing.
2. Add the Google account emails that should purchase without a real charge. License testers see the test card and are not billed.
3. On the internal testing track, add the same accounts as testers and open the opt-in link on the device.
4. Install the `play` build from that track while signed in with the tester account. A sideloaded debug build can open the purchase sheet only when the account is a license tester and the package name matches the Console app.

## 4. What the app does with the purchase

- **Recap (Settings).** The Quickdot Pro card shows the Play price when known, a short summary, **Buy**, **Restore**, and **Unlocked** when owned.
- **Purchase.** Settings → Buy calls `launchBillingFlow` for `quickdot_pro`. Locked accent swatches and the hex field route to the same purchase flow.
- **Pending.** A pending payment does not unlock Pro. The next time the app is in the foreground it queries purchases again and unlocks only after the state is `PURCHASED`.
- **Acknowledgement.** A purchased and unacknowledged item is acknowledged in the app. Pro is granted even if acknowledgement has to be retried on the next query.
- **Restore.** Settings → Restore, and every cold start, calls `queryPurchasesAsync`. A reinstall with an empty cache finds the Play purchase and unlocks again.
- **Offline cache.** `quickdotPro` in shared preferences is the saved unlock. If Play cannot be reached, that value is left as-is. A successful query with no `quickdot_pro` purchase clears it (refund, or a restore with nothing to restore).
- **Accent painting.** On the free Play build, the UI paints Signal red until Pro is unlocked; preferences still store the chosen colour. Per-conversation overrides and non-Signal presets are Pro-gated in the theme picker.
- **Reply quotes.** Quoting a message when replying is Pro-gated on Play; FOSS is unrestricted.
- **SMS.** Nothing in the send, receive, or notification delivery path is paywalled. Only accent painting and reply quotes read the entitlement.
