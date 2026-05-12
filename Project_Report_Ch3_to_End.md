# NammaMistri Project Report

## CHAPTER 3: TASKS PERFORMED

### 3.1 Work Schedule

The project was executed in the following high-level phases:

- Requirement gathering and planning
- System design and architecture definition
- UI/UX design and component creation
- Database design and data model implementation
- Screen-level feature implementation
- Integration of local persistence and repository layer
- Testing and bug fixing
- Final review and polishing

Typical daily work items included:

- Defining functional requirements for site management, labor tracking, calculator, photo gallery, and material rate administration
- Designing the app navigation flow, drawer menu, and bottom navigation bar
- Building reusable Compose UI components for modern interface elements
- Implementing Room database entities, DAOs, repository methods, and data seeding
- Developing screen-specific logic for dashboard, calculator, photos, labor, and rates
- Verifying UI behavior on Android API 24+ devices and testing database persistence
- Refining the app theme, colors, spacing, and user interactions

### 3.2 Introduction

NammaMistri is an Android application designed to support construction workers and site supervisors by organizing project data on mobile devices. The app brings together key construction management tools in one place:

- site tracking and progress monitoring
- labor diary management
- material rate administration
- on-site photo documentation
- construction calculator utilities

The application is built using modern Android technologies, with Jetpack Compose for UI, Room for local data storage, and an MVVM-inspired architecture to keep presentation and data logic separated.

### 3.3 System Requirements

#### Hardware Requirements

- Android smartphone or tablet
- Minimum Android 7.0 (API level 24)
- Recommended screen size: phone or small tablet
- Local storage for app installation and photo data

#### Software Requirements

- Android Studio
- Kotlin/JVM toolchain 17
- Gradle build system
- Android SDK 36
- Jetpack Compose and Material 3
- Room persistence library
- Coil image loading library
- Accompanist permissions library

#### Functional Requirements

- Create and manage construction sites
- Track labor workers and attendance entries
- Maintain material rate information
- Capture and display site photos with metadata
- Provide an interactive calculator interface
- Support navigation via drawer and bottom navigation bar
- Persist data locally using Room database

#### Non-Functional Requirements

- Responsive and modern mobile UI
- Offline support via local database
- Smooth animations and gesture-friendly controls
- Simple and readable data entry interfaces
- Maintainable modular architecture

### 3.4 System Design

#### Architecture

The app follows a modular architecture with the following layers:

- **Presentation Layer**: Jetpack Compose screens in `app/src/main/java/com/example/nammamistri2/ui`
- **ViewModel Layer**: screen-specific view models in `app/src/main/java/com/example/nammamistri2/viewmodel`
- **Repository Layer**: `NammaMistriRepository` manages data operations and abstracts the database
- **Data Layer**: Room database with entities, DAOs, and database builder in `app/src/main/java/com/example/nammamistri2/data`

#### Core Components

- `MainActivity.kt`: app launcher and entry point that initializes the Room database, configures splash and error screens, and hosts the navigation drawer and main UI.
- `ModernComponents.kt`: reusable Compose UI components such as pill toggles, quick access cards, stats cards, header banners, drawer items, and empty states.
- `RatesScreen.kt`: material rate management screen with add/edit/delete dialogs.
- `CalculatorScreen.kt`, `PhotoScreen.kt`, `LaborScreen.kt`: feature screens that display and manage domain-specific tasks.

#### Database Design

The local data model includes the following entities:

- `Site`: tracks site name, location, progress, and creation date
- `Worker`: stores worker name, role, daily wage, and assigned site
- `LaborEntry`: records attendance, advance payment, and payment mode for workers
- `MaterialRate`: stores material name, unit, and rate
- `Photo`: stores photo URI, description, site association, and capture date

The Room database is defined in `AppDatabase.kt` and upgraded to version 8.

#### Data Flow

- The app initializes the Room database inside `MainActivity` and builds `NammaMistriRepository`.
- Screens observe flows from the repository and update UI state reactively.
- User interactions trigger repository methods for insert, update, delete, and query operations.
- Default material rate records are seeded on first launch.

#### Navigation Design

- `ModalNavigationDrawer` for side navigation menu
- Bottom navigation bar for primary screen selection
- Drawer items for Home, Add Site, Calculator, Labor Diary, Site Photos, Standard Rates, and Profile

### 3.5 Implementation

#### MainActivity Initialization

- Enables edge-to-edge display
- Builds a Room database named `namma_mistri_v2_db`
- Configures `NammaMistriRepository` with DAOs for sites, workers, labor entries, material rates, and photos
- Seeds default material rate entries when the database is empty
- Enforces a minimum 5-second splash screen for consistent startup experience
- Handles initialization errors with an error screen fallback

#### User Interface

- `MainActivity` hosts a dashboard with greeting, location, notification icon, profile avatar, and quick access cards
- Modern navigation drawer includes a branded header and list of menu items
- `ModernComponents.kt` provides custom UI primitives used throughout the app
- Bottom navigation exposes key sections: Dashboard, Sites, Calculate, Labor, Rates

#### Feature Screens

- **Dashboard/Home**: shows active sites, quick actions, modern cards, and navigation shortcuts
- **Calculator**: implements modern pill toggles, tabbed calculation sections, and interactive input cards
- **Photo Screen**: displays site photo summaries, filter chips, gallery cards, and upload actions
- **Labor Screen**: tracks worker attendance, payments, summary stats, and labor diary entries
- **Rates Screen**: manages material rates using Compose dialogs, add/edit/delete operations, and reactive lists

#### Data Persistence

- CRUD operations for site, worker, labor entry, material rate, and photo entities
- Flow-based data retrieval for live UI updates
- Room migrations handled with `fallbackToDestructiveMigration()` during development

#### UI/UX Improvements

- Custom Material 3 theming with modern color scheme and typography
- Enhanced card design with rounded corners, shadows, and gradient banners
- Responsive layout spacing and accessible text hierarchy
- Smooth animated state transitions in custom components

## CHAPTER 4: RESULTS AND LEARNINGS

### Results

- Delivered a functional Android application for construction site management
- Implemented a modern dashboard and navigation experience
- Built a local Room database to persist sites, workers, labor entries, rates, and photos
- Created reusable Compose UI components for consistent styling
- Enabled offline-first operation with reliable local persistence
- Provided a clean material rate manager with add/edit/delete functionality
- Delivered a professional UI with modern cards, banners, chips, and dialogs

### Learnings

- Learned how to structure an Android app using Jetpack Compose and Room
- Gained experience with the MVVM/repository architecture pattern
- Improved skills in designing reusable Compose components
- Learned to manage UI state using `rememberSaveable` and flow-backed state
- Understood how to initialize and seed Room databases safely in a Compose app
- Practiced building responsive, modern mobile screens with Material 3 design principles
- Learned how to integrate and manage multiple feature screens within a unified navigation experience

## BIBLIOGRAPHY

- Android Developers Documentation: Jetpack Compose
- Android Developers Documentation: Room Persistence Library
- Android Developers Documentation: Material 3 for Android
- Kotlin Language Documentation
- Coil Image Loading Library Documentation
- Accompanist Permissions Library Documentation
- Android Studio and Gradle Build System Documentation
