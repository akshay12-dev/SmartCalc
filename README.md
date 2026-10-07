# AllCalc

A native Android app combining a **scientific calculator**, **scan-and-solve OCR camera tool**, and **unit / currency converter**, with a shared history database and offline-first defaults.

- Kotlin + Jetpack Compose + Material 3
- minSdk 26 (Android 8.0), targetSdk 35
- Offline-first: calculator, unit conversion and OCR history all work without a network. Only currency rates are fetched online.

---

## 1. Project structure

```
AllCalcApp/
├─ build.gradle.kts                 # root: AGP 8.7.3, Kotlin 2.0.21, Compose plugin, KSP
├─ settings.gradle.kts              # repositories + module include
├─ gradle.properties                # AndroidX, non-transitive R, etc.
├─ gradle/wrapper/                  # Gradle 8.9 wrapper (jar + properties)
├─ gradlew / gradlew.bat
└─ app/
   ├─ build.gradle.kts              # deps: Compose BOM 2024.09.03, Room, DataStore, CameraX, ML Kit
   ├─ proguard-rules.pro
   └─ src/
      ├─ main/
      │  ├─ AndroidManifest.xml     # CAMERA + INTERNET permissions, launcher activity
      │  ├─ res/                    # themes, adaptive launcher icon, strings, colours
      │  └─ java/com/allcalc/app/
      │     ├─ MainActivity.kt              # edge-to-edge host, reads theme preference
      │     ├─ AllCalcApplication.kt        # Application + AppContainer (manual DI)
      │     ├─ domain/
      │     │  ├─ calculator/ExpressionEngine.kt   # tokenizer + parser + evaluator + equation solver
      │     │  ├─ converter/ConversionEngine.kt    # 12 categories of units with exact factors
      │     │  └─ scanner/MathNormalizer.kt        # OCR/word cleanup -> expression
      │     ├─ data/
      │     │  ├─ local/HistoryRepository.kt       # Room entity/DAO/database + repository
      │     │  ├─ preferences/PreferencesRepository.kt  # DataStore settings + currency cache
      │     │  └─ currency/CurrencyRepository.kt   # open.er-api.com fetch + cache
      │     ├─ utils/AppViewModelProvider.kt       # viewModelFactory wiring + ShortcutBus
      │     ├─ utils/BitmapUtils.kt                # camera/gallery image decode helpers
      │     └─ ui/
      │        ├─ navigation/AllCalcApp.kt         # NavHost + bottom navigation
      │        ├─ common/Components.kt             # shared buttons, banners, dropdowns
      │        ├─ theme/                           # light/dark schemes
      │        ├─ home/  calculator/  scanner/  converter/  currency/  history/  settings/
      └─ test/java/com/allcalc/app/                # unit tests (see section 9)
```

Architecture: unidirectional — each screen has one `ViewModel` that owns its state; UI only reads `StateFlow`s and forwards taps. `AppContainer` (created once in `AllCalcApplication`) holds the Room database, DataStore preferences, engines and repositories; ViewModels are built through `AppViewModelProvider`. Cross-screen handoff (e.g. history → calculator) goes through `ShortcutBus` flows.

## 2. Opening the project in Android Studio

1. Install **Android Studio** (Ladybug or newer recommended) with **JDK 17** bundled.
2. **File → Open…** and select the `AllCalcApp` folder (the one containing `settings.gradle.kts`).
3. Wait for Gradle sync. Android Studio will download Gradle 8.9 using the included wrapper and resolve dependencies (internet required for the first sync).
4. If prompted, accept the Android SDK (API 35) and build-tools installation.
5. Create an emulator (**Device Manager → Create Device → Pixel 7 → API 35**) or connect a physical device with USB debugging enabled.
6. Press **Run ▶**.

## 3. Dependencies used in `app/build.gradle.kts`

| Area | Dependency |
|---|---|
| Compose | `androidx.compose:compose-bom:2024.09.03` (ui, foundation, material3, material-icons-extended) |
| Compiler | `org.jetbrains.kotlin.plugin.compose` (Kotlin 2.0.21) |
| Lifecycle | `lifecycle-runtime-compose`, `lifecycle-viewmodel-compose` 2.8.4 |
| Navigation | `androidx.navigation:navigation-compose` 2.7.7 |
| Storage | `androidx.room:room-runtime/room-ktx` 2.6.1 + `room-compiler` via KSP |
| Settings | `androidx.datastore:datastore-preferences` 1.1.1 |
| Camera | `androidx.camera:camera-camera2`, `camera-lifecycle`, `camera-view` 1.3.4 |
| OCR | `com.google.mlkit:text-recognition` 16.0.0 (Latin script model, bundled in APK) |
| Images | `androidx.exifinterface:exifinterface` 1.3.7 (rotation from gallery picks) |
| Coroutines | `org.jetbrains.kotlinx:kotlinx-coroutines-android` 1.8.1 |
| Tests | `junit:junit:4.13.2` (local JVM unit tests) |

Everything except the ML Kit model is resolved from `maven.google.com` / `mavenCentral()`; the first sync needs network.

## 4. Running the app

- **Emulator:** select a device and press **Run ▶**. Camera access works on emulators with a virtual scene camera (use **More → Virtual scene** as the camera source).
- **Physical device:** enable *Developer options → USB debugging*, pick the device, press **Run ▶**.
- Grant the **camera** permission when Scan & Solve requests it; everything else runs offline.

## 5. Building an APK from the IDE

**Build → Build App Bundle(s) / APK(s) → Build APK(s)** produces `app/build/outputs/apk/debug/app-debug.apk`.

Command line equivalent (from the project root, with Android Studio's SDK on `ANDROID_HOME`):

```bash
./gradlew assembleDebug      # debug APK
./gradlew assembleRelease    # release APK (needs signing config, see section 10)
```

## 6. How OCR scanning works (ML Kit)

1. **Capture.** CameraX `ImageCapture` grabs a JPEG, or the user picks an image with the system photo picker.
2. **Crop.** A custom Compose crop overlay (drag the corners/edges) extracts the region of interest before OCR — higher accuracy, less noise, private by default.
3. **Recognise.** The cropped bitmap is handed to **ML Kit Text Recognition v1** (`com.google.mlkit:text-recognition`). The Latin script model ships inside the APK, so recognition works **fully offline**.
4. **Normalise.** `MathNormalizer.prepare()` fixes OCR confusions (`O→0`, `I/l→1`, `B→8`, `S→5` between digits, `x→×` between digits), converts word math (`times`, `percent of`, `square root of`), strips thousands separators and sentence punctuation.
5. **Verify.** Average element confidence is surfaced as a warning when it is low or when no digits were found. The user can **edit the detected text** before solving — OCR never silently overrides the equation.
6. **Solve.** The cleaned text goes to `ExpressionEngine.evaluate()`, which returns a value, a solved variable (linear equations), or a boolean equality, with step lines shown as details.

## 7. How currency fetching works (open.er-api.com)

- Endpoint: `https://open.er-api.com/v6/latest/USD` (free, no API key, rates in USD base).
- Request: plain HTTPS GET with 10 s connect/read timeouts, `Accept: application/json`, parsed with `org.json`.
- Result: `rates` map is persisted in **DataStore** with `updatedAt` timestamp and an `isSample` flag.
- Freshness: rates older than **24 hours** are auto-refreshed on open (if *Auto-update rates* is on) and a stale banner is shown; **Update exchange rates** forces a refresh. The last-updated time is always displayed.
- Offline seed: if no live fetch has ever succeeded, clearly-marked **sample rates** are shown with a banner saying so, and are replaced by real data on the first successful refresh. Sample rates are never presented as live data.
- Conversion: `amount / fromRate * toRate`, then rounded to 2 decimals (4 for sub-unit amounts).

## 8. Adding a new unit or category

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

`linearUnit(name, symbol, factor)` builds `toBase = v * factor` and `fromBase = v / factor`. For offset units (like temperature) supply a `UnitDefinition` with explicit lambdas instead.

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

Nothing else is needed — the converter screen, dropdowns, history labels and persistence all iterate `ConversionCategory.entries`.

## 9. Testing

**Unit tests** (JVM, no device needed):

- `ExpressionEngineTest` — arithmetic, powers/roots, trig (deg & rad), %, implicit multiplication, factorials, constants, equalities, linear solving, div-by-zero errors, `formatSmart`.
- `ConversionEngineTest` — kg→g, ft→cm, mi→km, acre→m², gal→l, °C↔°F↔K, decimal GB vs IEC GiB, month/year (30.437 d / 365.2425 d), speed.
- `MathNormalizerTest` — word math, percentage phrasing, square roots in words, OCR letter-digit fixes, superscripts, thousands separators, punctuation stripping.

Run them from **Gradle tool window → Tasks → verification → test**, or:

```bash
./gradlew testDebugUnitTest
```

**Manual smoke checks:** calculator `25×12+50÷2 = 325`, `2x+5=15 → x = 5`, `sin(30) = 0.5` in DEG mode; converter `1 ft → 30.48 cm`, `1 acre → 4046.8564224 m²`; scan a printed equation and confirm the editable text before solving; toggle dark mode in Settings; airplane mode → calculator/converter/history still work.

## 10. Building a signed release APK

1. **Build → Generate Signed App Bundle / APK → APK → Next**.
2. Create a keystore if you do not have one (**+ New keystore**): choose a strong password, key alias (e.g. `allcalc`), validity (≥ 25 years) and fill in the certificate fields. **Back up the keystore and passwords** — losing them blocks updates.
3. Choose **release** and finish. Output: `app/build/outputs/apk/release/app-release.apk` (unsigned if you skip signing: `app-release-unsigned.apk`).
4. To sign automatically every release build, add a `signingConfigs` block in `app/build.gradle.kts` reading the keystore path/passwords from `local.properties` (never commit them), then reference it from `buildTypes.release.signingConfig`.

Command-line alternative with `apksigner` (from `build-tools`):

```bash
apksigner sign --ks my-release.keystore --out allcalc-release.apk app-release-unsigned.apk
apksigner verify allcalc-release.apk
```

R8 minification and resource shrinking are already enabled for the release build; add rules to `proguard-rules.pro` only if a reflection-based library warns at runtime.
