# AllCalc

A native Android app that bundles a **scientific calculator**, a **scan-and-solve OCR tool**, a **unit & currency converter**, and **44 specialized calculators** into one app with a shared history database and offline-first defaults.

- Kotlin + Jetpack Compose + Material 3
- AGP 9.4.1 · Kotlin 2.2.10 · Gradle 9.6.0 · KSP 2.3.6
- minSdk 26 (Android 8.0) · targetSdk / compileSdk 35
- Offline-first: the calculator, unit conversions, specialized calculators and OCR history all work without a network. Only currency rates are fetched online.

---

## 1. Features

### Scientific calculator
- Full expression input with a live result preview and copy-to-clipboard.
- Supports `+ − × ÷`, powers (`^`), roots (`√`), factorials (`!`), percent (`%`), absolute-value bars (`|`), π, e, trig/log functions, and equation solving (`2x+5=15 → x = 5`).
- **Editable display:** tap anywhere in the expression to place a blinking caret; insert or backspace at that exact spot.
- **Long-press a number** to select the whole token, then type to replace it or press ⌫ to delete it.
- Long display lines are never cut off: the expression auto-shrinks (34sp → 15sp) and can use up to 4 lines; the result line wraps to 2.
- One scrollable scientific strip:

  `%  √  x²  ±  1/x  ^  !  |  π  e  ln  log  sin  cos  tan  asin  acos  atan  abs`

  - `%` — percent (`50% = 0.5`; `25% of 800 = 200`; calculator-style `200 + 10% = 220`, `200 − 10% = 180`, `200 × 10% = 20`)
  - `√` — square root (`√9 = 3`, `√(9+16) = 5`)
  - `x²` — squares the number at the caret
  - `±` — flips the sign of the number at the caret
  - `1/x` — reciprocal of the number at the caret
- DEG/RAD toggle, configurable decimal precision, history saving, and a long-press on ⌫ to clear everything.

### Scan & Solve (OCR)
1. **Capture** with CameraX or pick an image from the gallery.
2. **Crop** the equation with a custom Compose overlay for better accuracy.
3. **Recognise** with ML Kit Text Recognition v1 — the Latin model is bundled, so OCR works fully offline.
4. **Normalise** OCR confusions (`O→0`, `I/l→1`, `B→8`, `S→5`, `x→×`, word math, superscripts, thousands separators) in `MathNormalizer`.
5. **Verify** the editable detected text before solving; low confidence is surfaced as a warning.
6. **Solve** through the same expression engine used by the calculator.

### Unit converter
- 12 categories: Length, Weight, Area, Volume, Temperature, Time, Speed, Pressure, Energy, Power, Data, Angle.
- Exact factors for linear units and dedicated converters for temperature, data (decimal vs IEC) and time.
- Results can be copied and saved to history.

### Currency converter
- 30+ currencies with flags.
- Live rates from multiple free providers with automatic fallback (see section 6).
- Cached in DataStore, works offline with clearly-marked sample rates until the first successful update.
- Quick amounts, swap, copy, save to history, and a "last updated" indicator.

### Specialized calculators (44)
- Categories: **Math** (17), **Statistics** (9), **Geometry** (9), **Financial** (4), **Fitness & Health** (3), **Other** (2).
- Searchable hub (`SpecializedHubScreen`) by title and keywords.
- Examples: fractions, percentage, quadratic formula, roots, logs, ratios, LCM/GCF, factors, rounding, matrices, scientific notation, big numbers, binary/hex, half-life, standard deviation, permutation/combination, probability, z-score, confidence interval, triangles, circles, area/volume/surface area, slope, distance, loans/EMI, interest, tips, ROI, BMI, BMR/calories, body fat, age, GPA.
- Each runner (`SpecializedRunnerScreen`) shows result details/steps, supports copy, and saves to history.

### History
- Room database shared by every tool (`CALC`, `SCAN`, `CONVERSION` entries).
- Open a saved calculation back in the calculator or converter.
- Clear all history from Settings.

### Settings
- Theme (System / Light / Dark), decimal precision, DEG/RAD angle mode.
- Haptic feedback and key sounds.
- Auto-update currency rates.
- Clear all history.

---

## 2. Project structure

```
AllCalcApp/
├─ build.gradle.kts                  # root: AGP 9.4.1, Kotlin 2.2.10, KSP 2.3.6
├─ settings.gradle.kts               # repositories + :app module
├─ gradle.properties                 # AndroidX, non-transitive R, config cache
├─ gradle/
│  ├─ wrapper/                       # Gradle 9.6.0 wrapper
│  └─ gradle-daemon-jvm.properties   # daemon toolchain (JBR 21 via Foojay)
├─ gradlew / gradlew.bat
└─ app/
   ├─ build.gradle.kts               # Compose BOM 2024.09.03, Room, DataStore, CameraX, ML Kit
   ├─ proguard-rules.pro
   └─ src/
      ├─ main/
      │  ├─ AndroidManifest.xml      # CAMERA + INTERNET, network security config
      │  ├─ res/
      │  │  └─ xml/network_security_config.xml  # HTTPS-only traffic
      │  └─ java/com/allcalc/app/
      │     ├─ MainActivity.kt                    # edge-to-edge host, theme preference
      │     ├─ AllCalcApplication.kt              # Application + AppContainer (manual DI)
      │     ├─ domain/
      │     │  ├─ calculator/ExpressionEngine.kt  # tokenizer, parser, evaluator, solver
      │     │  ├─ converter/ConversionEngine.kt   # 12 unit categories with exact factors
      │     │  ├─ scanner/MathNormalizer.kt       # OCR/word cleanup → expression
      │     │  └─ specialized/
      │     │     ├─ SpecializedCatalog.kt        # 44 calculator definitions + search
      │     │     ├─ MathCalculators.kt
      │     │     ├─ StatisticsCalculators.kt
      │     │     ├─ GeometryCalculators.kt
      │     │     └─ ExtraCalculators.kt          # financial, fitness, other
      │     ├─ data/
      │     │  ├─ local/HistoryRepository.kt      # Room entity/DAO/database
      │     │  ├─ preferences/PreferencesRepository.kt  # DataStore settings + rate cache
      │     │  └─ currency/CurrencyRepository.kt  # multi-provider rate fetching + cache
      │     ├─ utils/
      │     │  ├─ AppViewModelProvider.kt         # viewModelFactory wiring + ShortcutBus
      │     │  └─ BitmapUtils.kt                  # camera/gallery image helpers
      │     └─ ui/
      │        ├─ navigation/AllCalcApp.kt        # NavHost + bottom navigation
      │        ├─ common/Components.kt            # shared keys, banners, dropdowns
      │        ├─ theme/                          # light/dark schemes
      │        ├─ specialized/                    # hub + generic runner screen
      │        └─ home/ calculator/ scanner/ converter/ currency/ history/ settings/
      └─ test/java/com/allcalc/app/               # JVM unit tests (see section 9)
```

Architecture: unidirectional — each screen has one `ViewModel` that owns its state; the UI reads `StateFlow`s and forwards events. `AppContainer` (created once in `AllCalcApplication`) holds the database, preferences, engines and repositories; ViewModels are built through `AppViewModelProvider`. Cross-screen handoff (history → calculator/converter) goes through `ShortcutBus` flows.

---

## 3. Toolchain & dependencies

| Area | Version / dependency |
|---|---|
| Android Gradle Plugin | `9.4.1` |
| Kotlin / Compose compiler plugin | `2.2.10` |
| KSP | `2.3.6` |
| Gradle | `9.6.0` (wrapper) |
| Daemon JVM | JetBrains Runtime **21** (`gradle/gradle-daemon-jvm.properties`) |
| Compose | `androidx.compose:compose-bom:2024.09.03` (ui, foundation, material3, material-icons-extended) |
| Lifecycle | `lifecycle-runtime-compose`, `lifecycle-viewmodel-compose` `2.8.4` |
| Navigation | `androidx.navigation:navigation-compose` `2.7.7` |
| Storage | Room `2.7.0` (`room-runtime`, `room-ktx`, `room-compiler` via KSP) |
| Settings | `androidx.datastore:datastore-preferences` `1.1.1` |
| Camera | CameraX `1.3.4` (`camera-camera2`, `camera-lifecycle`, `camera-view`) |
| OCR | `com.google.mlkit:text-recognition` `16.0.0` (bundled Latin model) |
| Images | `androidx.exifinterface:exifinterface` `1.3.7` |
| Coroutines | `org.jetbrains.kotlinx:kotlinx-coroutines-android` `1.8.1` |
| Tests | `junit:junit:4.13.2` |

Dependencies resolve from `google()` / `mavenCentral()`; the Foojay toolchain resolver downloads JDK 21 on first sync if it is not installed.

---

## 4. Building and running

1. Install a recent **Android Studio** (one that supports AGP 9.4.1) and a JDK 17+ (the bundled JBR works).
2. **File → Open…** and select the folder containing `settings.gradle.kts`.
3. Let Gradle sync finish (internet needed for the first sync).
4. Accept the Android SDK 35 / build-tools prompts if shown.
5. Create an emulator (**Device Manager → Create Device → Pixel 7 → API 35**) or connect a device with USB debugging.
6. Press **Run ▶**.

Command line (from the project root):

```bash
./gradlew assembleDebug        # debug APK → app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest    # JVM unit tests
./gradlew installDebug         # install on a connected device/emulator
```

On Windows use `gradlew.bat` instead of `./gradlew`.

---

## 5. How rate fetching works

`CurrencyRepository.refresh()` tries these HTTPS sources in order and uses the first one that returns usable data:

1. `https://open.er-api.com/v6/latest/USD` — `{ "result": "success", "rates": {...}, "time_last_update_unix": ... }`
2. `https://api.exchangerate-api.com/v4/latest/USD` — `{ "rates": {...}, "time_last_updated": ... }`
3. `https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/usd.json` — nested `usd` object (CDN-hosted)
4. `https://api.frankfurter.app/latest?from=USD` — partial rate set, used as a last resort

Details:
- Plain HTTPS `GET`, 15 s connect/read timeouts, redirects followed, HTTP caching disabled (`useCaches = false`, `Cache-Control: no-cache`), JSON parsed with `org.json`.
- Codes are uppercased and normalised; non-currency entries are dropped. `USD = 1.0` is always present, and codes missing from a fallback provider are filled from the offline seed so a conversion never fails with "No rate available".
- The normalised `rates` map is persisted in **DataStore** with `updatedAt` and `isSample = false`.
- Auto-refresh runs when the cache is sample/missing or older than **12 hours** (if *Auto-update rates* is on). **Update rates** always forces a refresh and shows "Live rates updated ✓" on success; failures show the real error with **Retry** / **Dismiss**.
- If no live fetch has ever succeeded, clearly-marked **sample rates** are shown with a banner and are replaced on the first successful refresh.
- Conversion math: `amount / fromRate * toRate`, rounded to 2 decimals (4 for sub-unit amounts).
- `res/xml/network_security_config.xml` allows only HTTPS traffic to the rate providers.

---

## 6. Calculator display editing

The calculator display is editable even though input comes from the keypad:

- **Tap** the expression to place the caret at the nearest character; the caret blinks in the primary colour.
- **Type** digits/functions/operators to insert at the caret; **⌫** deletes the character before the caret.
- **Long-press** a number or function name to select that token (highlighted); the next digit/function replaces it and ⌫ deletes it.
- Operators collapse correctly when adjacent (e.g. pressing `+` then `×` replaces the `+`).
- A decimal point cannot be added twice to the same number token.
- After `=`, the result is shown in the display: tap to edit it, or press a digit to start a new calculation.

---

## 7. Adding a unit or category

**Add a unit to an existing category** — edit `ConversionCategory` in `domain/converter/ConversionEngine.kt`:

```kotlin
LENGTH(
    "Length", null,
    listOf(
        // ... existing units ...
        linearUnit("Link", "link", 0.201168)   // name, symbol, factor to base unit (m)
    )
)
```

`linearUnit(name, symbol, factor)` builds `toBase = v * factor` and `fromBase = v / factor`. For offset units (like temperature) supply a `UnitDefinition` with explicit lambdas.

**Add a new category:**

```kotlin
FORCE(
    "Force", "1 N = 1 kg⋅m/s²",          // note is optional (shown as an info card)
    listOf(
        linearUnit("Newton", "N", 1.0),
        linearUnit("Kilogram-force", "kgf", 9.80665)
    )
)
```

Nothing else needs changing — the converter screen, dropdowns, history labels and persistence iterate `ConversionCategory.entries`.

---

## 8. Adding a specialized calculator

1. Add a `SpecializedCalculatorDef` in `domain/specialized/SpecializedCatalog.kt`:

```kotlin
SpecializedCalculatorDef(
    id = "my_calculator",
    title = "My Calculator",
    subtitle = "One-line description",
    category = SpecializedCategory.MATH,
    keywords = listOf("keyword1", "keyword2"),
    iconName = "Calculate"
)
```

2. Implement the pure calculation in the appropriate `*Calculators.kt` file (return a `CalculationResult` with a primary result and optional detail lines/steps).
3. Add a branch for the new `id` in `SpecializedRunnerScreen` that renders inputs and calls your function — the hub search and navigation pick it up automatically.
4. Add JVM tests in `SpecializedCalculatorsTest`.

---

## 9. Testing

**Unit tests** (JVM, no device required):

- `ExpressionEngineTest` — arithmetic, powers/roots (`√`), trig (deg & rad), percent (postfix and binary), implicit multiplication, factorials, constants, equalities, linear solving, divide-by-zero, `formatSmart`.
- `ConversionEngineTest` — kg→g, ft→cm, mi→km, acre→m², gal→l, °C↔°F↔K, decimal GB vs IEC GiB, month/year (30.437 d / 365.2425 d), speed.
- `MathNormalizerTest` — word math, percentage phrasing, square roots in words, OCR letter-digit fixes, superscripts, thousands separators, punctuation stripping.
- `SpecializedCalculatorsTest` — fractions, percentages, percent error, quadratic formula, roots/logs, LCM/GCF, statistics, geometry, finance and fitness calculators.

Run them from **Gradle tool window → Tasks → verification → test**, or:

```bash
./gradlew testDebugUnitTest
```

**Manual smoke checks:** `25×12+50÷2 = 325`, `2x+5=15 → x = 5`, `sin(30) = 0.5` in DEG mode, `50% = 0.5`, `√9 = 3`; tap into a long sum and edit/delete a number; converter `1 ft → 30.48 cm`, `1 acre → 4046.8564224 m²`; scan a printed equation and confirm the editable text before solving; change currencies and press **Update rates**; toggle dark mode in Settings; airplane mode → calculator/converter/history still work.

---

## 10. Building a signed release APK

1. **Build → Generate Signed App Bundle / APK → APK → Next**.
2. Create a keystore if needed (**+ New keystore**): strong password, alias (e.g. `allcalc`), validity ≥ 25 years, certificate fields. **Back up the keystore and passwords** — losing them blocks updates.
3. Choose **release** and finish. Output: `app/build/outputs/apk/release/app-release.apk` (or `app-release-unsigned.apk` without signing).
4. To sign automatically on every release build, add a `signingConfigs` block in `app/build.gradle.kts` reading the keystore path/passwords from `local.properties` (never commit them), then reference it from `buildTypes.release.signingConfig`.

Command-line alternative with `apksigner`:

```bash
apksigner sign --ks my-release.keystore --out allcalc-release.apk app-release-unsigned.apk
apksigner verify allcalc-release.apk
```

R8 minification and resource shrinking are already enabled for the release build; add rules to `proguard-rules.pro` only if a reflection-based library warns at runtime.

---

## 11. Notes

- `local.properties` is intentionally untracked — it holds your local Android SDK path.
- Generated folders (`.gradle/`, `.kotlin/`, `build/`, `app/build/`) are rebuildable and ignored by Git.
- The app never sends data anywhere; the only network traffic is fetching public exchange rates.
