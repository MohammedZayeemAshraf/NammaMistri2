# NammaMistri2 — Interview Questions & Answers
### For Selection Panel Presentation

---

## SECTION A: Project Overview & Motivation

**Q1. What is NammaMistri2 and what problem does it solve?**

NammaMistri2 is an offline-first Android application designed for construction site foremen (Mistris) in India. It solves the problem of paper-based record-keeping on construction sites — tracking daily worker attendance, wages, payments, site progress photos, and material costs. Construction supervisors in India typically use physical registers and manual calculations, which are error-prone and get lost. This app digitises all of that in a single mobile tool that works without internet.

---

**Q2. Why did you choose to build this as a mobile app rather than a web application?**

Construction sites have unreliable or no internet connectivity. A web app would be unusable in such environments. A native Android app with a local SQLite database through Room works fully offline. Additionally, construction foremen carry Android smartphones and are more comfortable with a mobile interface than a browser. Native Android also gives access to device hardware like GPS and camera, which are core features of the app.

---

**Q3. Who is your target user?**

The primary user is a Mistri — a construction foreman or site supervisor in India. They typically manage 5–30 daily wage workers, track attendance manually, and calculate wages at the end of each week or month. Secondary users include small civil contractors who oversee multiple sites.

---

**Q4. Why is the app called NammaMistri2?**

"Namma" means "our" in Kannada, one of the major South Indian languages. "Mistri" is a widely used term across India for a skilled construction worker or foreman. The name reflects the app's regional identity and target user base. The "2" denotes the second, modernised version of the application.

---

**Q5. What are the key features of the application?**

The six core features are:
1. **Labor Diary** — daily attendance marking (Present/Half Day/Absent), wage calculation, and payment recording
2. **Site Photos** — camera-based photo documentation of construction progress
3. **Construction Calculator** — brick, slab, and column quantity estimation
4. **Standard Rates** — editable material price list (cement, sand, brick, etc.)
5. **Add Site** — create construction sites with GPS-resolved address
6. **My Profile** — user identity with selfie and personal details

---

## SECTION B: Architecture & Design

**Q6. What software architecture pattern did you use?**

MVVM — Model-View-ViewModel. The UI layer (Jetpack Compose screens) observes data from ViewModels via StateFlow and Flow. ViewModels contain business logic and call the Repository. The Repository is the single source of truth and delegates to Room DAOs. This clean separation ensures the UI is purely reactive and ViewModels survive configuration changes like screen rotation.

---

**Q7. Why MVVM over MVP or MVC?**

MVVM integrates most naturally with Jetpack Compose and Kotlin Flows. In Compose, UI recomposition is triggered by state changes — MVVM's StateFlow/LiveData is a direct match. MVC would couple business logic to the Activity/Fragment. MVP requires explicit View interfaces which add boilerplate. MVVM also has the strongest official support from Google's Android Jetpack libraries.

---

**Q8. What is the Repository pattern and why did you use it?**

The Repository is a single class (`NammaMistriRepository`) that abstracts all data sources behind one interface. ViewModels never directly access DAOs or SharedPreferences — they always go through the Repository. This means if we later add a remote API or change the database structure, only the Repository needs to change. The ViewModels and UI are completely unaffected. It also makes unit testing easier since you can mock the Repository.

---

**Q9. How does data flow from the database to the UI?**

Room DAOs return `Flow<T>` — Kotlin reactive streams. The Repository exposes these flows. The ViewModel subscribes and transforms them using operators like `combine()` and `flatMapLatest()`, then exposes a `StateFlow` to the UI. In Compose, the UI collects this with `collectAsState()`. When any database row changes, the flow emits a new value, the StateFlow updates, and Compose automatically recomposes only the affected parts of the UI. No manual refresh is needed.

---

**Q10. How does your app handle screen rotation or process death?**

Screen rotation is handled automatically because ViewModels survive configuration changes — the ViewModel instance is retained by the Android lifecycle. For primitive UI state like selected tab, text field content, or selected date, we use `rememberSaveable` in Compose, which persists values to the saved instance state bundle and restores them after rotation or process death.

---

**Q11. What is the single responsibility of each layer in your architecture?**

- **UI Layer (Composables)**: Display data and send user events. No business logic.
- **ViewModel**: Hold UI state, process user actions, coordinate with Repository. No Android context dependencies.
- **Repository**: Coordinate data sources, apply business rules on data access. No UI dependencies.
- **DAOs**: Pure database queries and inserts. No business logic.
- **Entities**: Pure data classes representing database tables. No behaviour.

---

## SECTION C: Jetpack Compose & UI

**Q12. Why did you choose Jetpack Compose over XML layouts?**

Compose is the modern declarative UI toolkit for Android. Compared to XML, it eliminates the View binding boilerplate, allows UI to be defined as pure Kotlin functions, and makes state-driven UI recomposition automatic. It integrates natively with Kotlin coroutines and flows. Google has officially recommended Compose as the preferred way to build Android UIs since 2021.

---

**Q13. What is recomposition in Compose and how does it affect performance?**

Recomposition is when Compose re-executes composable functions whose input state has changed. Compose is smart — it only recomposes the composable functions that read the changed state, not the entire screen. For example, when a worker's attendance changes, only that `WorkerCard` recomposes, not the whole `LaborScreen`. We ensure performance by using `remember` and `rememberSaveable` to avoid unnecessary recomputations and by structuring state at the appropriate level.

---

**Q14. How did you implement theming (light/dark mode)?**

A custom `NAMMAMISTRITheme` wraps the app. It defines two complete `ColorScheme` objects — `LightColorScheme` with warm cream and orange tones, and `DarkColorScheme` with a near-black background and vibrant orange/amber/green accents. The user toggles between them using a sun/moon icon in the TopAppBar. The selection is stored in a `rememberSaveable` boolean at the top level and passed to `NAMMAMISTRITheme(darkTheme = isDarkTheme)`.

---

**Q15. What is `ModernComponents.kt` and why is it structured that way?**

It is a centralised library of all reusable Compose UI components: stats cards, quick access cards, bottom nav bar, drawer header, empty state views, filter chips, etc. Centralising them ensures visual consistency across all screens, makes changes apply everywhere at once, and keeps individual screen files clean and focused on logic rather than layout details.

---

**Q16. How did you implement multi-language support?**

Rather than using Android's resource system (which requires an Activity restart to switch languages), we implemented a pure Kotlin translation map. Two maps (`kannadaStrings` and `hindiStrings`) store key-value pairs for all UI strings. A `t(key, lang)` function looks up the translation. The selected language is stored in a `rememberSaveable` state variable in `MainScreen`. Any language switch causes instant recomposition with translated strings — no restart needed.

---

**Q17. How does the navigation system work in your app?**

Navigation uses Compose's state-driven approach. A `currentScreen` string is held in `rememberSaveable`. A `when(currentScreen)` block in `MainScreen` renders the appropriate composable. The `ModalNavigationDrawer` (hamburger menu) and `NavigationBar` (bottom tabs) both call `onScreenSelected()` to update `currentScreen`. This is simpler than Jetpack Navigation Component for a single-Activity app of this size, and avoids the back stack complexity.

---

## SECTION D: Room Database

**Q18. What is Room and how does it differ from raw SQLite?**

Room is Android Jetpack's ORM (Object-Relational Mapping) library that wraps SQLite. Unlike raw SQLite, Room provides: compile-time SQL query verification (a wrong column name causes a build error, not a runtime crash), type-safe entity mapping via annotations, `Flow`-based reactive queries, and eliminates most boilerplate (cursor management, ContentValues, etc.). It integrates natively with Kotlin coroutines.

---

**Q19. How are your database entities defined?**

Each entity is a Kotlin `data class` annotated with `@Entity(tableName = "...")`. Each field becomes a column. The primary key is annotated with `@PrimaryKey(autoGenerate = true)`. Foreign key relationships (Worker → Site, LaborEntry → Worker, Photo → Site) are maintained through matching `Long` ID fields.

---

**Q20. What does `OnConflictStrategy.REPLACE` mean in `LaborEntryDao`?**

When we insert a `LaborEntry` with `INSERT OR REPLACE`, if a row with the same primary key already exists, Room deletes the old row and inserts the new one. We use this for attendance marking — when a user changes a worker from Present to Half Day for the same date, we want the latest value to replace the previous entry rather than throwing a conflict error or ignoring the update.

---

**Q21. How do you handle database migrations?**

Currently the app uses `fallbackToDestructiveMigration()`. This means if the schema version increments and no migration path is defined, Room drops and recreates all tables. This is acceptable during active development. For a production release, we would define proper `Migration` objects that run `ALTER TABLE` SQL statements to preserve user data across version upgrades.

---

**Q22. Why do DAO queries return `Flow` instead of a `List` directly?**

A plain `List` query is a one-time read — it returns data once and never updates. A `Flow<List<T>>` is a reactive stream that emits a new list every time the underlying table changes. This means the UI automatically reflects database changes without any manual refresh polling. It also plays well with Kotlin coroutines — collecting the flow is a suspension that continuously receives updates.

---

**Q23. What is `@Query` and can you give an example from your app?**

`@Query` is a Room annotation that maps a SQL query to a DAO function. Example from `LaborEntryDao`:
```sql
@Query("SELECT COALESCE(SUM(attendance), 0.0) FROM labor_entries WHERE workerId = :workerId")
fun getTotalDaysWorkedFlow(workerId: Long): Flow<Double>
```
This sums all attendance values for a worker across all dates. `COALESCE` ensures it returns `0.0` instead of `null` when there are no entries. The `:workerId` is a safe parameterised binding — Room prevents SQL injection.

---

**Q24. How do you calculate a worker's balance?**

```
Balance = Total Earned − Total Paid
Total Earned = Total Days Worked × Daily Wage
Total Days Worked = SUM(attendance) across all LaborEntry rows for that worker
Total Paid = SUM(advance) across all LaborEntry rows where paymentMode IS NOT NULL
```
This is computed reactively in `LaborViewModel` by combining three Flow streams. Every time any of these change, the `WorkerState` object updates and the card recomposes.

---

## SECTION E: Specific Features

**Q25. How does the GPS location feature work in Add Site?**

When the user taps "Use My Location", the app first checks for `ACCESS_FINE_LOCATION` or `ACCESS_COARSE_LOCATION` permission using Accompanist's permission API. If granted, it calls `LocationManager.getLastKnownLocation()` on `NETWORK_PROVIDER` (faster, lower accuracy) with `GPS_PROVIDER` as fallback. The latitude/longitude is passed to Android's `Geocoder` which converts it to a human-readable address string. This runs on `Dispatchers.IO` to avoid blocking the UI thread.

---

**Q26. How are photos stored and retrieved securely?**

Photos are captured using `ActivityResultContracts.TakePicture`. Before launching the camera, we create a `File` in the app's scoped external storage (`getExternalFilesDir("Pictures")`). A secure URI is generated using `FileProvider.getUriForFile()` — this prevents other apps from directly accessing the file path, complying with Android's security model (no `file://` URIs after Android 7). Only the content URI (`content://`) is stored in the Room database.

---

**Q27. How does the Construction Calculator work? What formulas does it use?**

The Brick Calculator takes wall dimensions (length × height × thickness) in any unit, converts to meters, then:
- Volume of brickwork (m³) = L × H × T
- Number of bricks = Volume × bricks-per-m³ (varies by brick size)
- Mortar volume = Volume × mortar fraction (e.g., 0.25 for Red Brick)
- Cement bags = Mortar volume × cement ratio ÷ bag size
- Sand = Mortar volume × sand ratio

The Slab and Column calculators similarly compute concrete volume using standard 1:1.5:3 or 1:2:4 mix ratios. All unit conversions (feet/inches/cm/mm to meters) use standard SI conversion factors.

---

**Q28. How does attendance tracking handle half days?**

Attendance is stored as a `Double` in `labor_entries`: `1.0` = Full Day, `0.5` = Half Day, `0.0` = Absent. This allows the same `SUM(attendance)` query to correctly compute total days worked across all three states. If a worker works 10 full days and 4 half days, `SUM(attendance) = 12.0` days, and earnings = `12.0 × daily wage`. The UI chip cycles through these three states on each tap.

---

**Q29. How does the payment system distinguish between attendance and payments?**

Both use the same `labor_entries` table but the `paymentMode` field acts as a discriminator. An attendance record has `paymentMode = null`. A payment record has `paymentMode = "Cash"` or `"Bank"`. The `getTotalAdvanceFlow` query uses `SUM(advance)` across all rows. The `getTotalDaysWorkedFlow` query uses `SUM(attendance)` which is `0.0` on payment rows. This dual-use design avoids needing a separate payments table.

---

**Q30. How does the Profile feature persist data?**

`SharedPreferences` with the name `"namma_mistri_profile"` stores four key-value pairs: `name`, `phone`, `role`, and `photo_uri`. The `loadProfile(context)` function reads them at app startup. When the user saves, `saveProfile(context, profile)` writes to SharedPreferences and `onProfileUpdated(updated)` propagates the new state upward, causing the drawer header to recompose with the updated name and photo.

---

## SECTION F: Technical Decisions

**Q31. Why did you use KSP instead of KAPT for Room?**

KSP (Kotlin Symbol Processing) is the modern replacement for KAPT (Kotlin Annotation Processing Tool). KSP is approximately 2× faster because it works directly with Kotlin's compiler API rather than generating Java stubs first. Since Room 2.6+, KSP is the recommended annotation processor. For a project using Kotlin 2.x and AGP 9.x, KSP is the correct choice.

---

**Q32. Why did you choose Coil over Glide or Picasso for image loading?**

Coil (Coroutine Image Loader) is written entirely in Kotlin and built on Kotlin Coroutines and OkHttp. Its Compose extension provides the `AsyncImage` composable which integrates naturally with the Compose lifecycle. Glide and Picasso are older Java-based libraries that require more boilerplate when used with Compose. Coil's `AsyncImage` is a single line to load, cache, and display an image.

---

**Q33. Why Accompanist for permissions rather than handling it manually?**

Manual runtime permission handling in Compose requires writing `ActivityResultLauncher`, handling the callback, and managing state across recompositions. Accompanist's `rememberPermissionState` encapsulates all of this into a reactive state object. It reduces permission handling to a few lines and integrates cleanly with Compose's state model.

---

**Q34. Why is there no network permission in the AndroidManifest?**

The app is designed to be fully offline. No data is sent to or received from a remote server. Avoiding internet permission is both a deliberate privacy design choice and a trust signal — users can verify the app cannot send their worker salary data or site information anywhere. The only external interaction is opening Google Maps via an implicit Intent — no network call from NammaMistri2 itself.

---

**Q35. What is `fallbackToDestructiveMigration` and what are its risks?**

It tells Room: "if you encounter a schema version that doesn't have a migration path defined, drop all tables and recreate from scratch." The risk is **data loss** — all user data is deleted when the app upgrades. We use it in development because schema changes are frequent during prototyping. Before public release, every schema change from version N to N+1 would need a `Migration(N, N+1)` object with appropriate `ALTER TABLE` SQL.

---

**Q36. How does the app ensure the splash screen shows for at least 5 seconds?**

```kotlin
val splashStart = System.currentTimeMillis()
// ... database initialisation ...
val elapsed = System.currentTimeMillis() - splashStart
if (elapsed < 5000L) delay(5000L - elapsed)
repository = repo
```
If the DB initialises in 200ms, we delay for 4800ms more. If it takes 6 seconds (slow device), we navigate immediately. This ensures both minimum display time and no unnecessary waiting on slow devices.

---

## SECTION G: Challenges & Problem Solving

**Q37. What was the most difficult bug you encountered and how did you fix it?**

The most challenging bug was related to AGP 9.x and the Kotlin plugin. In earlier Gradle, you had to explicitly apply `kotlin("android")` plugin. In AGP 9.x, the Kotlin Android extension is automatically registered internally. Manually applying it caused a crash: *"Extension already registered: kotlin"*. The fix was to remove `kotlin.android` from both `build.gradle.kts` files and only keep `kotlin.compose` and `ksp` plugins.

---

**Q38. The splash screen image was not filling the full screen. How did you fix it?**

The original code used `ContentScale.Fit` which scales the image to fit within bounds while preserving aspect ratio — leaving blank bars on the sides or top. The fix was changing to `ContentScale.Crop` which scales the image so it fills the entire Box, cropping any overflow. The background was also changed to `Color.Black` so any minimal edge bleed is invisible.

---

**Q39. How did you handle the problem of Quick Access cards showing a purple background?**

The cards used `MaterialTheme.colorScheme.primaryContainer` for their background. Because our custom `LightColorScheme` didn't explicitly define `primaryContainer`, Material3's colour system auto-generated it — which produced an unexpected purple. The fix was to use a hardcoded `Color(0xFFFFF3E8)` — a warm orange cream — that always matches the app's brand regardless of how the theme generates derived colours.

---

**Q40. How did you resolve the "graphicsLayer alpha" issue with the splash animation?**

Inside the `graphicsLayer` lambda, `alpha` resolved to `GraphicsLayerScope.alpha` (the receiver property) rather than the local `imageAlpha` variable. This meant the animation had no visible effect. The fix was to remove the animation entirely and show the image directly, since a 5-second timer was already in place for the splash duration.

---

**Q41. The Labor page had a back button not present on other pages. Why did that happen and how did you fix it?**

`LaborScreen` had its own `Scaffold { topBar = { TopAppBar { navigationIcon = { ArrowBack } } } }`. This created a second app bar stacked below `MainScreen`'s app bar — causing the back button and a visual gap. The fix was to remove `LaborScreen`'s entire `topBar` parameter. The sort and date controls were moved into a compact toolbar row inside the content area instead.

---

## SECTION H: Testing & Quality

**Q42. How did you test the application?**

Testing was done through:
1. **Android Emulator** — tested on API 26 through 35 emulators in Android Studio
2. **Physical device testing** — installed via USB debug on an Android smartphone to verify camera, GPS, and performance on real hardware
3. **Manual functional testing** — each feature was tested individually: adding workers, marking attendance across multiple dates, verifying wage calculations, capturing photos, adding sites with GPS, language switching
4. **Edge case testing** — empty states (no sites, no workers), negative balances, long worker names, special characters in site names

---

**Q43. What automated tests exist in the project?**

The project scaffolding includes `ExampleUnitTest.kt` (JUnit4 unit tests) and `ExampleInstrumentedTest.kt` (Espresso UI tests). For a production release, the priority automated tests would be: Repository unit tests (mocking DAOs), ViewModel unit tests (testing Flow emissions), and DAO integration tests using Room's `in-memory database` test support (`Room.inMemoryDatabaseBuilder`).

---

**Q44. How would you add unit tests to the ViewModel?**

A `FakeNammaMistriRepository` implementing the same interface would be used instead of the real Room database, keeping tests fast and deterministic:
```kotlin
@Test
fun `worker balance equals earned minus paid`() = runTest {
    val fakeRepo = FakeNammaMistriRepository()
    val viewModel = LaborViewModel(fakeRepo)
    viewModel.workerStates.first().also { states ->
        assertEquals(expected, states[0].balance, 0.01)
    }
}
```

---

## SECTION I: Security & Privacy

**Q45. What security measures are implemented in the app?**

1. **FileProvider** — photos shared using `content://` URIs rather than `file://` URIs, preventing unauthorised file access
2. **No internet permission** — the app cannot send data off the device
3. **Scoped storage** — photos stored in the app's scoped external directory
4. **Parameterised queries** — all Room DAO queries use `:parameter` binding, compiled to `PreparedStatement` — SQL injection is impossible
5. **No hardcoded credentials** — no API keys or passwords anywhere in the codebase

---

**Q46. How does the app comply with OWASP Mobile Top 10?**

- **M1 (Improper Credential Usage)**: No credentials used — app is fully local
- **M2 (Inadequate Supply Chain Security)**: All libraries from official repositories with pinned versions in `libs.versions.toml`
- **M5 (Insecure Communication)**: No network communication
- **M6 (Inadequate Privacy Controls)**: No personal data leaves the device; photo URIs are scoped
- **M9 (Insecure Data Storage)**: SharedPreferences stores only non-sensitive profile data; no passwords or tokens

---

## SECTION J: Scalability & Future Improvements

**Q47. What are the limitations of the current version?**

1. Workers are scoped to `siteId = 1` by default in `LaborViewModel` — workers aren't yet fully multi-site
2. `fallbackToDestructiveMigration` means schema updates erase data — needs proper migration paths
3. No data backup or cloud sync — if the device is lost, all data is lost
4. The Construction Calculator results are not saved to the database
5. No export feature (PDF/Excel) for wage reports

---

**Q48. How would you add cloud backup in a future version?**

The cleanest approach would be to add Firebase Firestore as a secondary data source in the Repository layer. The ViewModel and UI would not change at all. The Repository would write to both Room (local) and Firestore (remote), with a sync strategy to handle offline writes via Firestore's built-in offline persistence.

---

**Q49. How would you add PDF export for wage reports?**

Android provides the `PdfDocument` API in `android.graphics.pdf`. We would add an `exportWageReport(siteId, month)` function that queries all workers and their entries, creates a `PdfDocument.Page`, draws worker names, attendance counts, earnings, paid amounts, and balances using `Canvas` draw calls, then saves the PDF to the Downloads folder via `MediaStore` API.

---

**Q50. How would you handle multi-user scenarios?**

Add a `userId` column to Sites and Workers tables, add a login screen (PIN-based), store the active user ID in SharedPreferences, and filter all Repository queries by `userId`. For full team collaboration, combine with Firebase Authentication (phone-number OTP) and Firestore sync.

---

**Q51. What would change in the database design for a proper multi-site labor diary?**

The `workers` table already has `siteId` FK. We would: add a site selector on the Labor screen (similar to PhotoScreen's site selector), pass the selected `siteId` into `LaborViewModel` as a parameter rather than hardcoding it, and change `getWorkersBySite(siteId)` to use the dynamic value. The database schema itself does not need to change.

---

**Q52. How would you optimise the app for very large datasets (e.g., 500 workers)?**

1. **Paging** — use Jetpack Paging 3 library with `PagingSource` to load workers in pages of 20
2. **Lazy loading** — LazyColumn already only composes visible items
3. **Database indices** — add `@Index` on `workers.siteId`, `labor_entries.workerId`, and `labor_entries.date` columns
4. **Flow debouncing** — add `debounce(300)` to flows that fire on every keystroke in search fields

---

**Q53. If you had to rebuild this project, what would you do differently?**

1. Use Jetpack Navigation Component from the start instead of manual `when(currentScreen)` routing
2. Use Hilt for dependency injection — injecting the Repository and ViewModelFactory manually becomes verbose
3. Define Room `@ForeignKey` constraints explicitly to enforce referential integrity
4. Use a `SiteId` wrapper type (value class) instead of raw `Long` to prevent passing a `workerId` where a `siteId` is expected

---

**Q54. Can the app be published on the Google Play Store as-is?**

With some changes: replace `fallbackToDestructiveMigration` with proper migrations, add a privacy policy (required by Play), test on a wider range of API levels and screen sizes, sign the APK with a release keystore, and review the target SDK for compliance with Play's current requirements (API 34+). The core feature set is complete and functional for publication.

---

**Q55. How would you implement data export for the foreman to share with their employer?**

Multiple approaches:
1. **WhatsApp/Email share** — format a text summary string and share via Android's `Intent.ACTION_SEND`
2. **CSV export** — write worker data to a `.csv` file in Downloads using `MediaStore`
3. **PDF report** — use `PdfDocument` API
4. **QR code** — encode a JSON summary of the month's wages as a QR code

For quick implementation, option 1 is simplest: a "Share Report" button that builds a formatted string of all workers' names, attendance, earned, paid, and balance, then fires a WhatsApp Intent.

---

## SECTION K: Kotlin Language & Coroutines

**Q56. What are Kotlin coroutines and why are they important in this app?**

Kotlin coroutines are a concurrency framework that allows asynchronous code to be written sequentially without callbacks. In NammaMistri2, all database operations (Room queries, inserts) are `suspend` functions that must run on a background thread. Instead of callbacks or RxJava, we use `viewModelScope.launch { }` which ties coroutine lifetime to the ViewModel. When the ViewModel is cleared (user leaves the screen), all in-progress coroutines are automatically cancelled — no memory leaks.

---

**Q57. What is `StateFlow` and how is it different from `LiveData`?**

`StateFlow` is a Kotlin Coroutines construct — a hot flow that always holds a current value and emits updates to all collectors. Unlike `LiveData`, it is not tied to the Android lifecycle and works in pure Kotlin (no Android dependency). It is type-safe, supports null-safety explicitly, and integrates with the structured concurrency model. In Compose, `collectAsState()` converts a `StateFlow` to a `State<T>` that triggers recomposition. `LiveData` requires an `Observer` and an `owner` lifecycle — unnecessary overhead in Compose.

---

**Q58. Explain the difference between `launch` and `async` in coroutines.**

- `launch` starts a coroutine and returns a `Job`. It is fire-and-forget — used when no result value is needed. Example: `viewModelScope.launch { repository.insertWorker(worker) }`
- `async` starts a coroutine and returns a `Deferred<T>`. It is used when a result is needed and you call `.await()` to get it. Example: `val count = async { repository.getWorkerCount() }.await()`

In NammaMistri2, `launch` is used for all insert/update/delete operations. `async` would be used if we needed two parallel database reads before combining their results.

---

**Q59. What is `Dispatchers.IO` and why must database operations use it?**

`Dispatchers.IO` is a coroutine dispatcher backed by a thread pool optimised for blocking I/O operations — file access, database queries, network calls. The Android main thread (UI thread) must never be blocked. Room enforces this — calling a DAO function on the main thread throws a `IllegalStateException` at runtime. Room's Kotlin extensions automatically switch to `Dispatchers.IO` for suspend functions, but explicit background work uses `withContext(Dispatchers.IO) { }`.

---

**Q60. What is a `data class` in Kotlin and why are Room entities defined as data classes?**

A `data class` automatically generates `equals()`, `hashCode()`, `toString()`, and `copy()` based on its constructor properties. Room entities are data classes because:
1. Room needs `equals()` to detect when a row has changed (for diff algorithms in `DiffUtil`)
2. `copy()` is used in ViewModels to update a single field without mutating the original: `worker.copy(dailyWage = newWage)`
3. Kotlin's immutability model means we create new instances rather than mutating existing ones, which prevents concurrency bugs

---

**Q61. What is a `companion object` in Kotlin? Give an example from this project.**

A `companion object` is a singleton scoped to a class — similar to Java's `static` members. In `AppDatabase.kt`, the `INSTANCE` variable and the `getDatabase()` factory function live in a companion object:
```kotlin
companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null
    fun getDatabase(context: Context): AppDatabase { ... }
}
```
This ensures only one database connection is ever created (singleton pattern), which is critical for Room to function correctly.

---

**Q62. What does `@Volatile` mean on the database INSTANCE variable?**

`@Volatile` marks the field as always read from and written to main memory, never from a thread-local CPU cache. Without it, two threads could simultaneously see `INSTANCE == null`, both try to create a database, and end up with two separate `AppDatabase` instances — breaking Room's assumption of a single connection. `@Volatile` combined with a `synchronized` block (double-checked locking) makes the singleton thread-safe.

---

## SECTION L: Android Platform & Lifecycle

**Q63. What is the Android Activity lifecycle and how does your app handle it?**

The key lifecycle callbacks are: `onCreate → onStart → onResume` (app visible/active), then `onPause → onStop → onDestroy` (app going away). NammaMistri2 uses a single `MainActivity` with Jetpack Compose. Since the entire UI is Compose, we only interact with `setContent { }` in `onCreate`. ViewModels survive `onStop` and configuration changes. `rememberSaveable` survives process death. Room's Flow-based queries automatically restart when the app resumes.

---

**Q64. What is an Intent and how is it used in NammaMistri2?**

An `Intent` is a message object used to request an action from another app component. In NammaMistri2:
- **Camera launch**: `ActivityResultContracts.TakePicture` internally fires an `Intent` to the device camera app with a `FileProvider` URI
- **Gallery pick**: `ActivityResultContracts.GetContent("image/*")` fires an Intent to the media picker
- **Google Maps**: An implicit Intent `Intent(Intent.ACTION_VIEW, geoUri)` opens the Maps app with the site coordinates
- **WhatsApp share** (planned): `Intent(Intent.ACTION_SEND)` with `type = "text/plain"` targets WhatsApp

---

**Q65. What is `rememberSaveable` and when would you use `remember` instead?**

- `remember { }` — stores a value in Compose's memory, survives recompositions but is lost on screen rotation or process death. Use for expensive computations or objects that can be recreated.
- `rememberSaveable { }` — additionally persists the value in the saved instance state bundle. Survives rotation, going to background, and process death. Use for user-entered text, selected tabs, filter choices.

In NammaMistri2: `selectedLanguage`, `isDarkTheme`, `currentScreen`, and all text field values use `rememberSaveable`.

---

**Q66. What is a `ViewModel` and why does it survive screen rotation?**

A `ViewModel` is an Android Architecture Component that lives longer than an Activity or Fragment. It is stored in a `ViewModelStore` which is retained through configuration changes (screen rotation). When the Activity is recreated after rotation, `ViewModelProvider` returns the same existing ViewModel instance rather than creating a new one. This means all in-progress coroutines, loaded data, and user state are preserved without re-fetching from the database.

---

**Q67. What is `FileProvider` and why is it required for camera capture?**

`FileProvider` is a special `ContentProvider` that generates secure `content://` URIs for files in the app's private directory. Since Android 7 (API 24), passing a `file://` URI to an external app (like the camera) throws a `FileUriExposedException`. `FileProvider` converts the file path to a `content://` URI that the camera app can write to, without exposing the actual file path. The URI is configured in `AndroidManifest.xml` with a `<provider>` declaration referencing `file_paths.xml`.

---

**Q68. What is the difference between `getExternalFilesDir()` and `getFilesDir()`?**

- `getFilesDir()` — app's internal private storage. Inaccessible to other apps or users. Deleted when app is uninstalled. Not accessible via USB file transfer.
- `getExternalFilesDir()` — app's scoped external storage (e.g., `/sdcard/Android/data/com.example.nammamistri2/`). Accessible via USB transfer. Deleted when app is uninstalled. Does not require `WRITE_EXTERNAL_STORAGE` permission on API 29+.

NammaMistri2 stores photos in `getExternalFilesDir("Pictures")` so they are accessible for backup while still being scoped to the app.

---

## SECTION M: UI/UX Design Decisions

**Q69. What is Material Design 3 and how did you apply it?**

Material Design 3 (Material You) is Google's latest design system for Android. Key elements used in NammaMistri2:
- **Dynamic colour tokens**: `primary`, `onPrimary`, `surface`, `surfaceVariant`, etc. from `ColorScheme`
- **Components**: `Scaffold`, `TopAppBar`, `NavigationBar`, `FloatingActionButton`, `Card`, `OutlinedTextField`, `ElevatedButton` — all Material3 variants
- **Typography scale**: `headlineMedium`, `titleLarge`, `bodyMedium` from `MaterialTheme.typography`
- **Shape system**: `RoundedCornerShape` consistently applied to cards and buttons

---

**Q70. What is a `Scaffold` in Jetpack Compose?**

`Scaffold` is a layout composable that implements the basic Material Design layout structure. It provides predefined slots for:
- `topBar` — `TopAppBar`
- `bottomBar` — `NavigationBar`
- `floatingActionButton` — `FloatingActionButton`
- `drawerContent` — `ModalNavigationDrawer`
- `content` — the main screen content with proper padding applied via `PaddingValues`

The `Scaffold` handles padding calculations automatically so content doesn't overlap with the status bar, navigation bar, or FAB.

---

**Q71. Why did you use `ModalNavigationDrawer` instead of a bottom sheet for the menu?**

A `ModalNavigationDrawer` (hamburger menu) is the correct Material Design pattern for apps with many top-level destinations that can't all fit in the bottom navigation bar. NammaMistri2 has 6+ destinations. The bottom navigation bar shows the 4 most frequent ones (Home, Labor, Photos, Rates), while less-frequent actions (Profile, Add Site, Settings) live in the drawer. This follows Material Design navigation guidelines.

---

**Q72. How did you make the app accessible?**

Key accessibility considerations:
1. All icons have `contentDescription` for screen readers
2. Text colours maintain sufficient contrast ratios (orange `#FF6B00` on white `#FFF8F0` exceeds 3:1 ratio)
3. Touch targets are at least 48×48dp (per Material guidelines)
4. Text sizes are SP units, not DP — respects the user's system font size preference
5. All interactive elements (chips, buttons) have clear visual pressed/focus states

---

**Q73. What is the purpose of `LazyColumn` over a regular `Column`?**

A regular `Column` composes and lays out all children at once, regardless of visibility. With 50 workers, a `Column` creates 50 `WorkerCard` composables simultaneously — wasting memory and CPU. `LazyColumn` only composes items that are currently visible on screen (plus a small buffer). It recycles composable slots as the user scrolls — the same slot recomposes with new data for a newly visible item. This is essential for long lists in a performance-sensitive app.

---

## SECTION N: Build System & Tooling

**Q74. What is Gradle and what is its role in Android development?**

Gradle is the build automation tool used by Android Studio. It handles: downloading dependencies from Maven repositories (Google, Maven Central), compiling Kotlin source files, running annotation processors (KSP for Room), packaging resources, signing the APK/AAB, and running tests. Android projects have two `build.gradle.kts` files — one at the project level (plugin versions) and one at the app module level (dependencies, SDK versions, build types).

---

**Q75. What is `libs.versions.toml` and why is it used?**

It is a Gradle Version Catalog — a centralised file that declares all dependency versions and aliases in one place. Instead of repeating version strings across multiple `build.gradle.kts` files, you reference `libs.room.runtime`, `libs.compose.bom`, etc. Benefits:
- Single place to update a version (change it once, applies everywhere)
- IDE auto-completion for dependency names
- Prevents version mismatches between modules
- Required format for modern multi-module Android projects

---

**Q76. What is the difference between `implementation`, `api`, and `ksp` in Gradle dependencies?**

- `implementation` — dependency is available to this module only; not exposed to consumers of this module. Used for all runtime dependencies.
- `api` — dependency is exposed to all modules that depend on this module. Rarely used; creates tight coupling.
- `ksp` — tells Gradle this dependency is a Kotlin Symbol Processor (annotation processor). It runs at compile time to generate code (Room generates DAO implementations, entity mappings). Not included in the final APK — compile-time only.

---

**Q77. What is ProGuard and what does `proguard-rules.pro` do?**

ProGuard is a code shrinker, obfuscator, and optimizer that runs when building a release APK. It:
- **Shrinks** the APK by removing unused classes and methods
- **Obfuscates** by renaming classes to short names (a, b, c) making reverse engineering harder
- **Optimises** bytecode for performance

`proguard-rules.pro` contains `-keep` rules that tell ProGuard what NOT to obfuscate. Room entity class names must be kept (`-keep class * extends androidx.room.RoomDatabase`) because Room references them by name via reflection.

---

## SECTION O: Soft Skills & Project Management

**Q78. How did you plan the development of this project?**

Development followed a feature-first vertical slice approach:
1. **Foundation**: Database schema, entities, DAOs, Repository
2. **Core screens**: Labor diary (most complex), then Sites, then Rates
3. **Hardware features**: Camera (Photos), GPS (Add Site)
4. **Polish**: Theme system, dark mode, language support
5. **Documentation**: Architecture diagrams, technical report

Each feature was built end-to-end (database → ViewModel → UI) before starting the next, which meant the app was always in a working state.

---

**Q79. If a senior engineer reviewed your code, what criticism do you think they would raise?**

Honest self-assessment:
1. `LaborViewModel` hardcodes `siteId = 1` — should be dynamic
2. No Hilt/Dagger — manual dependency injection is verbose and error-prone at scale
3. `MainActivity.kt` is too large — Profile, Add Site, and Main navigation should each be separate files
4. No proper error handling for GPS failures or camera errors — silent failures
5. No unit tests or integration tests yet
6. `fallbackToDestructiveMigration` is dangerous for real users

---

**Q80. What did you learn from building this project that you didn't know before?**

Key learnings:
1. **Compose state hoisting** — understanding which composable should own state and how to pass it down vs. hoist it up
2. **Room reactive queries** — how `Flow<List<T>>` from DAOs eliminates manual refresh logic entirely
3. **FileProvider security model** — the difference between `file://` and `content://` URIs and why it matters
4. **AGP version compatibility** — how major Android Gradle Plugin versions can break existing build configurations
5. **Material3 colour system** — how derived colours (like `primaryContainer`) are auto-generated and can surprise you with unexpected results

---

*Last updated: May 2026 | NammaMistri2 — Android Construction Site Management App*
