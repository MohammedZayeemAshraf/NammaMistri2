# NammaMistri2 — Technical Report

---

## 1. Design Objectives

NammaMistri2 was designed with the following core objectives:

1. **Offline-first operation** — All data is stored locally on the device using a Room (SQLite) database, so the application works without an internet connection. This is essential for construction site environments where connectivity may be unreliable.

2. **Simplicity for field workers** — The user interface is built for low-tech users who may be more comfortable with regional languages. The UI uses large tap targets, icon-driven navigation, and a colour palette familiar to Indian construction workers (warm orange/cream theme).

3. **Multi-language support** — The application supports English, Kannada (ಕನ್ನಡ), and Hindi (हिन्दी) for all primary labels, navigation items, and section headers, enabling adoption across language groups.

4. **Comprehensive site management** — A single app replaces paper-based records for attendance, wage calculations, payment tracking, site photo documentation, and material cost reference.

5. **Data integrity** — The MVVM (Model–View–ViewModel) architecture with a single repository layer ensures that all UI components read from the same reactive data source, preventing inconsistencies.

6. **Light and dark mode** — A full dual-theme implementation ensures usability in both outdoor (bright) and indoor (low-light) environments.

7. **Privacy by design** — No user data leaves the device. Profile information is saved in SharedPreferences; photos are stored in device-scoped external storage accessed through a FileProvider. No network permissions are requested.

---

## 2. Major System Modules

The application is divided into six functional modules, each corresponding to a dedicated screen and supporting code.

### 2.1 Dashboard Module
**Screen:** `HomeScreen`

The entry point after the splash screen. Displays a project banner image, a Quick Access grid (4 cards linking to all major features), and an Active Sites list showing each site with its construction progress bar. Supports language switching via a top-bar dropdown.

### 2.2 Labor Diary Module
**Screen:** `LaborScreen`  
**ViewModel:** `LaborViewModel`

Manages the complete lifecycle of daily wage labor:
- **Attendance tab** — Mark each worker as Present (1.0), Half Day (0.5), or Absent (0.0) for any selected date. A date picker chip allows historical date selection.
- **Payments tab** — Record cash or bank advances against a worker's earned balance.
- **Summary tab** — Displays team-wide totals (Workers, Total Earned, Total Paid, Balance).
- Workers can be sorted by name, balance, or earnings. Full attendance history is expandable per worker.

### 2.3 Site Photos & Progress Module
**Screen:** `PhotoScreen`  
**ViewModel:** `PhotoViewModel`

Tracks visual documentation of each construction site:
- Select or switch between sites.
- Capture photos directly from the camera (with runtime `CAMERA` permission handling via Accompanist).
- View all photos in a scrollable gallery with delete support.
- A progress slider (0–100%) allows updating site completion percentage, persisted to the database.
- Summary cards show Total Photos and current Progress with a live mini progress bar.

### 2.4 Construction Calculator Module
**Screen:** `CalculatorScreen`  
**ViewModel:** `CalculatorViewModel`

Provides three engineering estimation tools:
- **Brick Calculator** — Supports Red Brick, Fly Ash Brick, Concrete Block, and AAC Block types. Calculates quantity, mortar volume, cement bags, and sand load from wall dimensions (length × height × thickness) in Feet, Meters, Inches, cm, or mm.
- **Slab Calculator** — Computes concrete volume, cement bags, sand, and aggregate for a flat slab.
- **Column Calculator** — Computes concrete volume and material quantities for circular or rectangular columns.

All calculations use real construction industry ratios (e.g., 1:2:4 mix, mortar fraction per brick type).

### 2.5 Standard Rates Module
**Screen:** `RatesScreen`  
**ViewModel:** `RatesViewModel`

A fully editable price list of construction materials:
- Default data (Brick ₹10/piece, Cement ₹400/bag, Sand ₹1500/m³) is seeded on first launch.
- Users can add, edit (via dialog), and delete any material rate.
- Each entry stores material name, unit of measure, and rate (₹).

### 2.6 Profile Module
**Screen:** `ProfileScreen`

Allows the user (the Mistri/foreman) to set up a personal identity for the app:
- Upload a selfie (camera) or choose from gallery.
- Enter full name, phone number, and trade/role.
- Data is persisted in `SharedPreferences` and displayed as a circular photo + name in the navigation drawer header.

---

## 3. Working Flow of System

### 3.1 Application Launch Flow

```
App Launch
    │
    ▼
SplashScreen (minimum 5 seconds)
    │  ← Room database initialises in background (IO Dispatcher)
    │  ← Default material rates seeded if empty
    │
    ▼
MainScreen (Scaffold + ModalNavigationDrawer + BottomNav)
    │
    ▼
HomeScreen (Dashboard)
```

### 3.2 Attendance Recording Flow

```
User opens Labor Diary
    │
    ▼
Date selection (defaults to today, changeable via date picker chip)
    │
    ▼
Worker list loads from Room DB via StateFlow
    │
    ▼
User taps chip → Present / Half Day / Absent
    │
    ▼
LaborViewModel.markAttendance(workerId, value)
    │
    ▼
LaborEntry inserted to Room DB (INSERT OR REPLACE)
    │
    ▼
StateFlow recomposition → UI updates worker card instantly
    │
    ▼
TeamSummary (Earned, Paid, Balance) recalculated reactively
```

### 3.3 Site Photo Flow

```
User selects a site in Sites tab
    │
    ▼
Tap "Add Photos" FAB
    │
    ▼
Runtime CAMERA permission check (Accompanist)
    │  ← Granted? → FileProvider creates temp URI in external scoped storage
    │
    ▼
Camera opens with the temp URI
    │
    ▼
Photo captured → URI saved to Room DB (photos table)
    │
    ▼
Photo gallery recomposes via Flow<List<Photo>>
```

### 3.4 Navigation Flow

The app uses a `ModalNavigationDrawer` (hamburger menu) combined with a 5-item `NavigationBar` at the bottom:

| Trigger | Destination |
|---|---|
| Bottom Nav: Dashboard | HomeScreen |
| Bottom Nav: Sites | PhotoScreen |
| Bottom Nav: Calculate | CalculatorScreen |
| Bottom Nav: Labor | LaborScreen |
| Bottom Nav: Rates | RatesScreen |
| Quick Access card | Respective screen |
| Drawer → My Profile | ProfileScreen |
| Drawer → Add Site | AddSiteScreen |
| PhotoScreen (no sites) | AddSiteScreen |

---

## 4. Implementation

### 4.1 Architecture Pattern

The application follows **MVVM (Model–View–ViewModel)** architecture:

```
View (Compose UI)
    ↕  observes StateFlow / collectAsState
ViewModel (business logic)
    ↕  calls suspend functions / Flow
Repository (single source of truth)
    ↕  delegates to DAOs
Room DAOs (SQL abstraction)
    ↕
SQLite Database
```

This separation ensures:
- The UI is purely reactive — no direct database access from composables.
- ViewModels survive configuration changes (screen rotation).
- The Repository abstracts all data sources (Room, SharedPreferences, FileProvider) behind a single interface.

### 4.2 Reactive Data with Kotlin Flows

All database queries expose `Flow<T>` streams. The UI collects them using Compose's `collectAsState()`:

```kotlin
// In LaborViewModel
val workerStates: Flow<List<WorkerState>> = combine(
    workers, _selectedDate
) { workerList, date -> workerList to date }
.flatMapLatest { (workerList, date) ->
    combine(
        repository.getEntriesByWorker(worker.id),
        repository.getTotalDaysWorkedFlow(worker.id),
        repository.getTotalAdvanceFlow(worker.id)
    ) { entries, days, paid ->
        WorkerState(...)
    }
}
```

This ensures every UI element auto-refreshes when any underlying data changes, with zero manual refresh calls.

### 4.3 Coroutines for Async Operations

All database write operations run on Kotlin Coroutines within `viewModelScope`:

```kotlin
fun addPayment(workerId: Long, amount: Double, mode: String, date: Long) {
    viewModelScope.launch {
        repository.insertLaborEntry(LaborEntry(
            workerId = workerId,
            advance = amount,
            paymentMode = mode,
            date = date
        ))
    }
}
```

The database itself is initialised on `Dispatchers.IO` during the splash screen to avoid blocking the main thread.

### 4.4 GPS Location (Add Site)

When adding a site, the user can tap "Use My Location":

```kotlin
val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
val location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
    ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
val addresses = Geocoder(context, Locale.getDefault())
    .getFromLocation(latitude, longitude, 1)
siteLocation = addresses?.firstOrNull()?.getAddressLine(0)
```

The resolved address is pre-filled into the location field, which the user can edit before saving.

---

## 5. User Interface Implementation

### 5.1 Technology

The entire UI is built with **Jetpack Compose** (declarative UI toolkit), using **Material Design 3** components and tokens from the `androidx.compose.material3` library.

### 5.2 Theme System

The app defines a custom `NAMMAMISTRITheme` with two complete colour schemes:

| Token | Light Mode | Dark Mode |
|---|---|---|
| Primary | Orange `#E65100` | Orange `#FF7D2A` |
| Background | Cream `#FFF8F0` | Near-black `#0F1117` |
| Surface | White `#FFF8F0` | Dark panel `#1A1D27` |
| SurfaceVariant | — | `#252836` |
| Secondary | Brown `#5D4037` | Amber `#FFB300` |
| Tertiary | Amber `#FF8F00` | Green `#66BB6A` |

The user toggles themes via the sun/moon icon in the TopAppBar. The selection persists via `rememberSaveable`.

### 5.3 Reusable Component Library (`ModernComponents.kt`)

All shared UI elements are centralised in a single file:

| Component | Purpose |
|---|---|
| `ModernQuickAccessCard` | Orange 88dp card with icon + label, used on Dashboard |
| `ModernStatsCard` | Compact info card with label, value, icon chip, optional progress bar |
| `ModernHeaderBanner` | Gradient card banner; supports `compact = true` for 64dp slim mode |
| `ModernBottomNavigationBar` | 5-item bottom nav bar with orange selected state |
| `ModernDrawerHeader` | Gradient header showing profile photo (or person icon) + name |
| `ModernDrawerItem` | Animated drawer menu row with selection highlight |
| `ModernEmptyState` | Centred icon + title + subtitle + optional action button |
| `ModernFilterChip` | Animated pill chip for filtering lists |
| `ModernFormCard` | Elevated card wrapper for form sections |
| `ModernPillToggle` | Animated segmented button row |

### 5.4 Navigation Structure

```
MainActivity
 └── NAMMAMISTRITheme
      └── ModalNavigationDrawer
           ├── ModalDrawerSheet (7 items + profile header)
           └── MainScreen (Scaffold)
                ├── TopAppBar (title + theme toggle + language picker)
                ├── BottomNavigationBar (5 tabs)
                └── Content Area
                     ├── HomeScreen
                     ├── LaborScreen
                     ├── PhotoScreen
                     ├── CalculatorScreen
                     ├── RatesScreen
                     ├── AddSiteScreen
                     └── ProfileScreen
```

### 5.5 Multi-language Implementation

Language switching is implemented via a pure Kotlin translation map (no Android resource system), allowing instant in-session switching without an Activity restart:

```kotlin
private val kannadaStrings = mapOf(
    "Dashboard" to "ಮುಖಪುಟ",
    "Labor Diary" to "ಕಾರ್ಮಿಕ ದಾಖಲೆ",
    "Standard Rates" to "ದರ ಪಟ್ಟಿ",
    ...
)

fun t(key: String, lang: String): String = when (lang) {
    "ಕನ್ನಡ" -> kannadaStrings[key] ?: key
    "हिन्दी" -> hindiStrings[key] ?: key
    else -> key
}
```

---

## 6. Database Implementation (Room Database)

### 6.1 Overview

The application uses **Room Persistence Library v2.8.4**, which provides a type-safe SQLite abstraction layer for Android. The database is named `namma_mistri_v2_db` and is at schema version 8, configured with `fallbackToDestructiveMigration` for development.

### 6.2 Entity Definitions

#### `sites` table
| Column | Type | Constraint | Description |
|---|---|---|---|
| `id` | INTEGER | PK, auto-increment | Unique site identifier |
| `name` | TEXT | NOT NULL | Site display name |
| `location` | TEXT | NOT NULL | Address or GPS-resolved location |
| `progress` | INTEGER | DEFAULT 0 | Completion percentage (0–100) |
| `createdDate` | INTEGER | DEFAULT now | Unix timestamp of creation |

#### `workers` table
| Column | Type | Constraint | Description |
|---|---|---|---|
| `id` | INTEGER | PK, auto-increment | Unique worker identifier |
| `name` | TEXT | NOT NULL | Worker full name |
| `role` | TEXT | DEFAULT 'Labor' | Trade/role label |
| `dailyWage` | REAL | NOT NULL | Daily wage in ₹ |
| `siteId` | INTEGER | FK → sites.id | Site the worker is assigned to |

#### `labor_entries` table
| Column | Type | Constraint | Description |
|---|---|---|---|
| `id` | INTEGER | PK, auto-increment | Entry identifier |
| `workerId` | INTEGER | FK → workers.id | Worker reference |
| `date` | INTEGER | NOT NULL | Unix timestamp of the work date |
| `attendance` | REAL | DEFAULT 1.0 | 1.0 = Full Day, 0.5 = Half Day, 0.0 = Absent |
| `advance` | REAL | DEFAULT 0.0 | Payment/advance amount in ₹ |
| `paymentMode` | TEXT | NULLABLE | NULL = attendance entry; "Cash"/"Bank" = payment |

#### `material_rates` table
| Column | Type | Constraint | Description |
|---|---|---|---|
| `id` | INTEGER | PK, auto-increment | Rate identifier |
| `materialName` | TEXT | NOT NULL | Material display name |
| `unit` | TEXT | NOT NULL | Unit of measure (bag, piece, m³, etc.) |
| `rate` | REAL | NOT NULL | Price per unit in ₹ |

#### `photos` table
| Column | Type | Constraint | Description |
|---|---|---|---|
| `id` | INTEGER | PK, auto-increment | Photo identifier |
| `siteId` | INTEGER | FK → sites.id | Site the photo belongs to |
| `uri` | TEXT | NOT NULL | Android content URI or file path |
| `description` | TEXT | NOT NULL | Caption/label |
| `date` | INTEGER | DEFAULT now | Unix timestamp of capture |

### 6.3 Data Access Objects (DAOs)

Each entity has a dedicated DAO interface:

| DAO | Key Operations |
|---|---|
| `SiteDao` | `insert`, `update`, `getAllSites(): Flow`, `getSiteById` |
| `WorkerDao` | `insert`, `deleteById`, `getWorkersBySite(): Flow`, `getWorkerById` |
| `LaborEntryDao` | `insert (REPLACE)`, `getEntriesByWorker(): Flow`, `getTotalAdvanceFlow(): Flow<Double>`, `getTotalDaysWorkedFlow(): Flow<Double>` |
| `MaterialRateDao` | `insert`, `update`, `delete`, `getAllRates(): Flow`, `getRateById` |
| `PhotoDao` | `insert`, `deleteById`, `getPhotosBySite(): Flow` |

All read operations return `Flow<T>` for reactive updates. All write operations are `suspend` functions called from coroutines.

### 6.4 AppDatabase

```kotlin
@Database(
    entities = [Site::class, Worker::class, LaborEntry::class,
                MaterialRate::class, Photo::class],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun siteDao(): SiteDao
    abstract fun workerDao(): WorkerDao
    abstract fun laborEntryDao(): LaborEntryDao
    abstract fun materialRateDao(): MaterialRateDao
    abstract fun photoDao(): PhotoDao
}
```

The database instance is built once in `MainActivity.onCreate()` on `Dispatchers.IO`, then injected into a single `NammaMistriRepository` instance that is passed down to all ViewModels.

---

## 7. Programming Languages

| Language | Version | Role |
|---|---|---|
| **Kotlin** | 2.2.10 | Primary application language — all source code including UI (Compose), ViewModels, Repository, and DAOs |
| **XML** | — | AndroidManifest.xml, resource files (colors.xml, strings.xml, themes.xml, file_paths.xml, drawable XML) |
| **Kotlin DSL (Gradle)** | — | Build scripts (`build.gradle.kts`, `settings.gradle.kts`, `libs.versions.toml`) |

---

## 8. Libraries and Tools

### 8.1 Core Android Libraries

| Library | Version | Purpose |
|---|---|---|
| Android Gradle Plugin (AGP) | 9.0.1 | Android build system |
| `androidx.core:core-ktx` | 1.18.0 | Kotlin extensions for Android APIs |
| `androidx.activity:activity-compose` | 1.13.0 | Compose integration with ComponentActivity |
| `androidx.lifecycle:lifecycle-runtime-ktx` | 2.10.0 | Lifecycle-aware coroutine scopes |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | 2.10.0 | `viewModel()` factory helper for Compose |

### 8.2 Jetpack Compose

| Library | Version | Purpose |
|---|---|---|
| `androidx.compose:compose-bom` | 2024.09.00 | Bill of Materials — coordinates all Compose versions |
| `androidx.compose.ui:ui` | (BOM) | Core Compose UI runtime |
| `androidx.compose.material3:material3` | (BOM) | Material Design 3 components and theming |
| `androidx.compose.material:material-icons-extended` | 1.6.0 | Extended icon set (400+ icons) |
| `androidx.compose.ui:ui-tooling-preview` | (BOM) | Android Studio layout preview support |

### 8.3 Room Persistence Library

| Library | Version | Purpose |
|---|---|---|
| `androidx.room:room-runtime` | 2.8.4 | Room database runtime |
| `androidx.room:room-ktx` | 2.8.4 | Kotlin coroutines and Flow extensions for Room |
| `androidx.room:room-compiler` | 2.8.4 | KSP annotation processor — generates DAO implementations |

### 8.4 Asynchronous Programming

| Library | Version | Purpose |
|---|---|---|
| Kotlin Coroutines | (bundled with Kotlin 2.2.10) | `viewModelScope.launch`, `Dispatchers.IO`, `StateFlow`, `Flow`, `combine`, `flatMapLatest` |

### 8.5 Image Loading

| Library | Version | Purpose |
|---|---|---|
| `io.coil-kt:coil-compose` | 2.6.0 | Async image loading in Compose — used for displaying site photos and profile pictures via `AsyncImage` |

### 8.6 Permissions

| Library | Version | Purpose |
|---|---|---|
| `com.google.accompanist:accompanist-permissions` | 0.34.0 | Runtime permission handling in Compose (`rememberPermissionState`) — used for CAMERA and LOCATION permissions |

### 8.7 Build Tools

| Tool | Version | Purpose |
|---|---|---|
| KSP (Kotlin Symbol Processing) | 2.3.7 | Annotation processing for Room DAO code generation (replaces KAPT) |
| Gradle | (wrapper) | Build automation |
| `libs.versions.toml` | — | Version catalog — centralises all dependency versions |

### 8.8 Android System APIs Used

| API | Purpose |
|---|---|
| `android.location.LocationManager` | GPS/Network location for Add Site |
| `android.location.Geocoder` | Convert coordinates to human-readable address |
| `androidx.core.content.FileProvider` | Secure URI sharing for camera-captured photos |
| `android.content.SharedPreferences` | Persist profile data (name, phone, role, photo URI) |
| `android.app.DatePickerDialog` | Native date picker for attendance date selection |
| `androidx.activity.result.ActivityResultContracts` | `TakePicture`, `GetContent`, `RequestPermission` launchers |

### 8.9 Development Tools

| Tool | Purpose |
|---|---|
| Android Studio | IDE — development, emulator, Logcat |
| Git | Version control |
| Mermaid.js | Architecture diagram generation (diagrams in `/images/`) |

---

*Report generated for NammaMistri2 — Android construction site management application.*
