# PaperFolio

Paper trading for Indian stocks. Android, Kotlin, Jetpack Compose. No account, no server, no real money — everything runs on the device.

The catalog is bundled: 8,100 NSE/BSE listings (7,740 equities, 359 ETFs). Live prices and charts come from Yahoo Finance, fetched only when you open a stock or a portfolio — nothing is fetched in bulk at startup.

Current version: **1.0.4**.

## Install

Build the release APK and install it with adb:

```bash
./gradlew assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

Installing 1.0.4 over 1.0.3 keeps your portfolios. (Portfolios wiped by the 1.0.1 bug can't be recovered — they have to be re-created; see [Data safety](#data-safety).)

## What it does

- **Portfolios.** Create as many as you want, each with its own starting capital (₹1,000 minimum, no upper limit). Cash goes down on a buy, back up on a sell.
- **Buy and sell, delivery only.** No F&O, no leverage. Average buy price is tracked per holding; selling shows estimated realized P&L before you confirm.
- **Search.** The whole catalog is on-device, so search is instant. Filter by NSE / BSE / ETF.
- **Charts.** Interactive price chart with previous-close reference and touch crosshair, plus day and 52-week low/high range bars.
- **P&L statement.** Total net P&L split into realized (sold trades, for the selected period) and unrealized (open holdings, right now), with a breakdown per portfolio and a realized-trades history. Custom date ranges supported. Realized P&L and holding averages use FIFO cost basis — a sell matches the oldest still-open buy lots (fixed in 1.0.4; earlier versions averaged all buys ever, which overstated P&L after a sell-and-rebuy).
- **Daily snapshots.** A background worker records one portfolio snapshot a day so the portfolio chart has history.
- **New listings.** An IPO or stock that isn't in the bundled catalog can be added from Settings — it's validated against Yahoo Finance first, then stored locally.

## Design

The UI follows the VELTO fintech concept (New Minds, Awwwards nominee): off-white screens, white cards, one dark-green hero card per screen, a single lime accent for the primary action, and pill navigation, tabs, chips and buttons. Type is Inter — 34sp ExtraBold for money figures, Semibold sentence-case section titles. No all-caps headers.

Every text colour was checked against its background (WCAG AA, 4.5:1 for body text):

| Token | Hex | Contrast | Used for |
|---|---|---|---|
| Background | `#F4F6F1` | — | App background |
| Surface | `#FFFFFF` | — | Cards (1dp `#E3E6DF` border, 16dp corners) |
| Surface muted | `#E9ECE5` | — | Search bars, steppers, unselected pills |
| Text primary | `#12151A` | 16.8:1 | Headings, values |
| Text secondary | `#5B6675` | 5.4:1 | Labels, subtext |
| Text tertiary | `#66707E` | 4.6:1 | Captions, timestamps |
| Velto green | `#122D28` | 13.5:1 | Hero card, active nav pill, selected pills |
| Lime | `#DCF97B` | 12.5:1 (dark text) | Primary action fill — never with white text (1.2:1) |
| Hero text | `#F4F6F1` | 13.5:1 on hero | Value on the dark card |
| Hero secondary | `#A8B8B0` | 7.1:1 on hero | Labels on the dark card |
| Hero profit / loss | `#8FE3B0` / `#FF9A8A` | 9.6:1 / 7.2:1 on hero | P&L on the dark card |
| Profit green | `#007A5E` | 4.9:1 | Gains, P&L text on light |
| Loss red | `#C62828` | 5.2:1 | Losses, SELL, errors (also the SELL fill) |
| Chart green | `#00B386` | — | Chart line and gradient |

## Data safety

**The bug (v1.0.1):** create portfolios, buy stocks, close the app from Recents, reopen it — everything was gone.

**Root cause:** the database was built with `.createFromAsset("stock_master.db")` + `.fallbackToDestructiveMigration()`. The bundled asset shipped with `PRAGMA user_version = 0` while Room expected version 1, so on every cold open Room saw a version mismatch with no migration path and deleted the live database and re-copied the asset — user tables included. Two more wipe paths existed: an app-level catch that deleted the database on any open error, and a recovery screen whose "Reset & Retry" deleted it on one tap.

**The fix (v1.0.2):**

- No `createFromAsset`, no destructive fallback. The catalog is seeded by opening the asset read-only and copying rows with plain INSERTs (`CatalogSeedCallback`) — it only ever INSERTs into `stock_master` / `stock_master_fts` and can't touch user tables.
- If the catalog is ever found empty on open, it re-seeds itself, again without touching user data.
- Buy, sell and portfolio creation run inside single Room transactions, so a crash mid-order can't leave a half-written state.
- Erasing data from the recovery screen is now a deliberate two-tap action.

**Regression test:** `app/src/test/java/com/paperfolio/PersistenceTest.kt` (Robolectric) replays the exact user flow — create, insert, close, reopen, twice — and asserts portfolios, trades and the catalog all survive, plus that an emptied catalog self-heals without touching user data. Run it with `./gradlew testDebugUnitTest`.

## Known limitations

Stated plainly, because they're real:

- **Yahoo Finance is the only price source.** If a quote fetch fails, the app silently falls back to the average buy price as "current", which flattens P&L instead of showing an error.
- **No backup or export.** Portfolios live only in the on-device database.
- **The release keystore is tracked in git** (`app/release-key.jks`, passwords in `app/build.gradle.kts`). Anyone with the repo can sign updates as the app. Key rotation is needed.

## Build from source

Prerequisites: JDK 17, Android SDK with build-tools 34.0.0, Android Studio Hedgehog or newer.

```bash
git clone https://github.com/MetriSomesh/PaperFolio.git
cd PaperFolio
```

Create `local.properties` in the project root with your SDK path:

```properties
sdk.dir=/path/to/Android/Sdk
```

Then:

```bash
./gradlew assembleRelease          # signed release APK
./gradlew testDebugUnitTest        # unit + Robolectric persistence tests
./gradlew assembleDebug            # debug APK
```

## Tech

Kotlin 1.9.24 · Jetpack Compose (BOM 2024.05.00) + Material 3 · Navigation Compose 2.7.7 · Room 2.6.1 (SQLite) · Retrofit 2.11.0 (Yahoo Finance v8 chart API) · WorkManager 2.9.0 · Kotlinx Serialization.

Structure is deliberately boring: a single-activity Compose `NavHost`, one ViewModel per screen, repositories over Room. No DI framework — the `Application` class holds the lazy singletons. `tools/build_stock_db.py` builds the bundled catalog from NSE/BSE data.

## License

MIT — see [LICENSE](LICENSE).
