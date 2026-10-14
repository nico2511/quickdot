# Play Console setup for Quickdot Pro

The `play` flavor (`applicationId` `com.nico2511.quickdot`) sells one product. The `foss` flavor does not call Play Billing. Create the product before a license tester can complete a purchase. The price is configured in Play Console, not in the app. The app shows `1 EUR · one-time` until Play returns a formatted price.

## 1. App

1. Open [Play Console](https://play.google.com/console).
2. Create the app, or open the existing one, with package name `com.nico2511.quickdot`.
3. Upload the signed `play` AAB (`Quickdot-v*-play-release.aab`) to an internal testing track. The product query returns nothing until the app build that asks for `quickdot_pro` is on a track the tester can install.

## 2. One-time product `quickdot_pro`

1. Monetize with Play → Products → One-time products.
2. Create a product with product ID exactly `quickdot_pro`.
3. Name: Quickdot Pro.
4. Description: Unlocks the chosen accent on every conversation. Sending messages stays free.
5. Set the price to **1 EUR** (Play localizes it for other countries).
6. Activate the product. Inactive products are not returned by `queryProductDetailsAsync`.

This is a one-time product, not a subscription and not a consumable. The app acknowledges it and does not consume it.

## 3. License testers

1. Play Console → Settings → License testing.
2. Add the Google account emails that should purchase without a real charge. License testers see the test card and are not billed.
3. On the internal testing track, add the same accounts as testers and open the opt-in link on the device.
4. Install the `play` build from that track while signed in with the tester account. A sideloaded debug build can open the purchase sheet only when the account is a license tester and the package name matches the Console app.

## 4. What the app does with the purchase

- **Purchase.** Settings → Purchase calls `launchBillingFlow` for `quickdot_pro`.
- **Pending.** A pending payment does not unlock style. The next time the app is in the foreground it queries purchases again and unlocks only after the state is `PURCHASED`.
- **Acknowledgement.** A purchased and unacknowledged item is acknowledged in the app. Style is granted even if acknowledgement has to be retried on the next query.
- **Restore.** Settings → Restore, and every cold start, calls `queryPurchasesAsync`. A reinstall with an empty cache finds the Play purchase and unlocks again.
- **Offline cache.** `quickdotPro` in shared preferences is the saved unlock. If Play cannot be reached, that value is left as-is. A successful query with no `quickdot_pro` purchase clears it (refund, or a restore with nothing to restore).
- **SMS.** Nothing in the send, receive, or notification path reads this entitlement. Only the conversation accent is limited on the free Play build.
