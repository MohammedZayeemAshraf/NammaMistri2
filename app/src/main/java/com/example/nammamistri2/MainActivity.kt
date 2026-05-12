package com.example.nammamistri2

import android.Manifest
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import java.io.File
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.example.nammamistri2.data.AppDatabase
import com.example.nammamistri2.data.MaterialRate
import com.example.nammamistri2.data.Site
import com.example.nammamistri2.repository.NammaMistriRepository
import com.example.nammamistri2.ui.*
import com.example.nammamistri2.ui.theme.NAMMAMISTRITheme
import com.example.nammamistri2.viewmodel.*
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var database: AppDatabase
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Log.d(TAG, "onCreate: Starting MainActivity initialization")

        setContent {
            var repository by remember { mutableStateOf<NammaMistriRepository?>(null) }
            var initError by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(Unit) {
                val splashStart = System.currentTimeMillis()
                try {
                    Log.d(TAG, "LaunchedEffect: Starting database initialization")
                    database = withContext(Dispatchers.IO) {
                        Room.databaseBuilder(
                            applicationContext,
                            AppDatabase::class.java,
                            "namma_mistri_v2_db"
                        ).fallbackToDestructiveMigration().build()
                    }
                    Log.d(TAG, "LaunchedEffect: Database built successfully")

                    val repo = NammaMistriRepository(
                        database.siteDao(),
                        database.workerDao(),
                        database.laborEntryDao(),
                        database.materialRateDao(),
                        database.photoDao()
                    )
                    Log.d(TAG, "LaunchedEffect: Repository created successfully")

                    withContext(Dispatchers.IO) {
                        try {
                            Log.d(TAG, "LaunchedEffect: Checking and seeding default data")
                            val rates = repo.getAllMaterialRates().first()
                            if (rates.isEmpty()) {
                                Log.d(TAG, "LaunchedEffect: Inserting default rates")
                                repo.insertMaterialRate(MaterialRate(materialName = "Brick", unit = "piece", rate = 10.0))
                                repo.insertMaterialRate(MaterialRate(materialName = "Cement", unit = "bag", rate = 400.0))
                                repo.insertMaterialRate(MaterialRate(materialName = "Sand", unit = "cubic meter", rate = 1500.0))
                            }
                            Log.d(TAG, "LaunchedEffect: Data seeding completed successfully")
                        } catch (e: Exception) {
                            Log.e(TAG, "LaunchedEffect: Error seeding data", e)
                        }
                    }

                    // Enforce minimum 5-second splash screen
                    val elapsed = System.currentTimeMillis() - splashStart
                    if (elapsed < 5000L) delay(5000L - elapsed)

                    repository = repo
                } catch (e: Throwable) {
                    Log.e(TAG, "LaunchedEffect: Error during database initialization", e)
                    e.printStackTrace()
                    val elapsed = System.currentTimeMillis() - splashStart
                    if (elapsed < 5000L) delay(5000L - elapsed)
                    initError = e.message ?: "Unknown initialization error: ${e::class.java.simpleName}"
                }
            }

            var isDarkTheme by rememberSaveable { mutableStateOf(false) }
            NAMMAMISTRITheme(darkTheme = isDarkTheme) {
                when {
                    initError != null -> ErrorScreen(initError!!)
                    repository == null -> SplashScreen()
                    else -> {
                        val drawerState = rememberDrawerState(DrawerValue.Closed)
                        val scope = rememberCoroutineScope()
                        var currentScreen by rememberSaveable { mutableStateOf("Home") }
                        var selectedSiteId by rememberSaveable { mutableStateOf<Long?>(null) }
                        val ctx = LocalContext.current
                        var profileData by remember { mutableStateOf(loadProfile(ctx)) }

                        ModalNavigationDrawer(
                            drawerState = drawerState,
                            drawerContent = {
                                ModalDrawerSheet(
                                    modifier = Modifier.fillMaxWidth(0.85f)
                                ) {
                                    ModernDrawerHeader(
                                        userName = if (profileData.name.isNotBlank()) profileData.name else "Mistri",
                                        photoUri = profileData.photoUri
                                    )
                                    
                                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                                    
                                    val drawerItems = listOf(
                                        DrawerItem("Home", Icons.Default.List, "Home"),
                                        DrawerItem("Add Site", Icons.Default.Add, "Add Site"),
                                        DrawerItem("Calculator", Icons.Default.Calculate, "Calculator"),
                                        DrawerItem("Labor Diary", Icons.Default.Person, "Labor"),
                                        DrawerItem("Site Photos", Icons.Default.PhotoCamera, "Photos"),
                                        DrawerItem("Standard Rates", Icons.Default.Assessment, "Rates"),
                                        DrawerItem("My Profile", Icons.Default.AccountCircle, "Profile")
                                    )
                                    
                                    Column(
                                        modifier = Modifier
                                            .padding(horizontal = 12.dp)
                                            .padding(top = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        drawerItems.forEach { item ->
                                            ModernDrawerItem(
                                                label = item.title,
                                                icon = item.icon,
                                                isSelected = currentScreen == item.route,
                                                onClick = {
                                                    currentScreen = item.route
                                                    scope.launch { drawerState.close() }
                                                }
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.weight(1f))
                                    
                                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                                    
                                    Column(
                                        modifier = Modifier
                                            .padding(16.dp)
                                            .fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            "v1.0.0",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Build Better Together",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        ) {
                            // Intercept system back button: go Home instead of closing the app
                            if (currentScreen != "Home") {
                                BackHandler { currentScreen = "Home" }
                            }

                            MainScreen(
                                repository = repository!!,
                                currentScreen = currentScreen,
                                onScreenSelected = { currentScreen = it },
                                openDrawer = { scope.launch { drawerState.open() } },
                                selectedSiteId = selectedSiteId,
                                onSelectSite = { selectedSiteId = it },
                                onBack = { currentScreen = "Home" },
                                isDarkTheme = isDarkTheme,
                                onToggleTheme = { isDarkTheme = !isDarkTheme },
                                profileData = profileData,
                                onProfileUpdated = { profileData = it }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SplashScreen() {
    var visible by remember { mutableStateOf(false) }
    val imageAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "splashAlpha"
    )

    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.openpage),
            contentDescription = "Opening page",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = imageAlpha }
        )
    }
}

@Composable
fun ErrorScreen(errorMessage: String) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Error initializing app: $errorMessage",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    repository: NammaMistriRepository,
    currentScreen: String,
    onScreenSelected: (String) -> Unit,
    openDrawer: () -> Unit,
    selectedSiteId: Long?,
    onSelectSite: (Long?) -> Unit,
    onBack: () -> Unit,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    profileData: ProfileData = ProfileData(),
    onProfileUpdated: (ProfileData) -> Unit = {}
) {
    val sites by repository.getAllSites().collectAsState(initial = emptyList())
    var selectedBottomNavItem by remember { mutableIntStateOf(0) }
    var selectedLanguage by rememberSaveable { mutableStateOf("English") }
    var showLanguageMenu by remember { mutableStateOf(false) }
    val languages = listOf("English", "ಕನ್ನಡ", "हिन्दी")

    val bottomNavItems = listOf(
        BottomNavItem(t("Dashboard", selectedLanguage), Icons.Default.Home, "Home"),
        BottomNavItem(t("Sites", selectedLanguage), Icons.Default.LocationOn, "Photos"),
        BottomNavItem(t("Calculate", selectedLanguage), Icons.Default.Calculate, "Calculator"),
        BottomNavItem(t("Labor", selectedLanguage), Icons.Default.Person, "Labor"),
        BottomNavItem(t("Rates", selectedLanguage), Icons.Default.Assessment, "Rates")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = openDrawer) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open navigation drawer"
                        )
                    }
                },
                title = {
                    Text(
                        when (currentScreen) {
                            "Home" -> t("Dashboard", selectedLanguage)
                            "Photos" -> t("Sites", selectedLanguage)
                            "Calculator" -> t("Calculator", selectedLanguage)
                            "Labor" -> t("Labor Diary", selectedLanguage)
                            "Rates" -> t("Standard Rates", selectedLanguage)
                            "Add Site" -> t("Add New Site", selectedLanguage)
                            else -> currentScreen
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                actions = {
                    // Theme toggle
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDarkTheme) "Switch to Light Mode" else "Switch to Dark Mode"
                        )
                    }
                    // Language picker
                    Box {
                        IconButton(onClick = { showLanguageMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Language"
                            )
                        }
                        DropdownMenu(
                            expanded = showLanguageMenu,
                            onDismissRequest = { showLanguageMenu = false }
                        ) {
                            Text(
                                "Select Language",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                            languages.forEach { lang ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (selectedLanguage == lang) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                            } else {
                                                Spacer(Modifier.width(24.dp))
                                            }
                                            Text(lang)
                                        }
                                    },
                                    onClick = {
                                        selectedLanguage = lang
                                        showLanguageMenu = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
            )
        },
        bottomBar = {
            ModernBottomNavigationBar(
                items = bottomNavItems,
                selectedItem = selectedBottomNavItem,
                onItemSelected = { index ->
                    selectedBottomNavItem = index
                    val newScreen = when (index) {
                        0 -> "Home"
                        1 -> "Photos"
                        2 -> "Calculator"
                        3 -> "Labor"
                        4 -> "Rates"
                        else -> "Home"
                    }
                    onScreenSelected(newScreen)
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentScreen) {
                "Home" -> HomeScreen(
                    sites = sites,
                    onQuickAccessSelected = { screen ->
                        onScreenSelected(screen)
                        // Update bottom nav selection
                        selectedBottomNavItem = when (screen) {
                            "Photos" -> 1
                            "Calculator" -> 2
                            "Labor" -> 3
                            "Rates" -> 4
                            else -> 0
                        }
                    },
                    onSiteSelected = { siteId ->
                        onSelectSite(siteId)
                        onScreenSelected("Photos")
                        selectedBottomNavItem = 1
                    },
                    selectedLanguage = selectedLanguage
                )
                "Add Site" -> AddSiteScreen(
                    repository = repository,
                    onSaved = {
                        onScreenSelected("Photos")
                        selectedBottomNavItem = 1
                    }
                )
                "Calculator" -> CalculatorScreen(viewModel(factory = CalculatorViewModelFactory(repository)))
                "Labor" -> LaborScreen(viewModel = viewModel(factory = LaborViewModelFactory(repository)), onBack = {
                    onScreenSelected("Home")
                    selectedBottomNavItem = 0
                })
                "Photos" -> PhotoScreen(
                    repository = repository,
                    selectedSiteId = selectedSiteId,
                    onSelectSite = onSelectSite,
                    onNavigateToAddSite = {
                        onScreenSelected("Add Site")
                    }
                )
                "Rates" -> RatesScreen(viewModel(factory = RatesViewModelFactory(repository)))
                "Profile" -> ProfileScreen(
                    profileData = profileData,
                    onSaved = { updated -> onProfileUpdated(updated) }
                )
                else -> HomeScreen(
                    sites = sites,
                    onQuickAccessSelected = { screen ->
                        onScreenSelected(screen)
                        selectedBottomNavItem = when (screen) {
                            "Photos" -> 1
                            "Calculator" -> 2
                            "Labor" -> 3
                            "Rates" -> 4
                            else -> 0
                        }
                    },
                    onSiteSelected = { siteId ->
                        onSelectSite(siteId)
                        onScreenSelected("Photos")
                        selectedBottomNavItem = 1
                    },
                    selectedLanguage = selectedLanguage
                )
            }
        }
    }
}

@Composable
fun HomeScreen(
    sites: List<Site>,
    onQuickAccessSelected: (String) -> Unit,
    onSiteSelected: (Long) -> Unit,
    selectedLanguage: String = "English"
) {
    val homeActions = listOf(
        QuickAccessItem(t("Calculator", selectedLanguage), Icons.Default.Calculate, "Calculator"),
        QuickAccessItem(t("Labor Diary", selectedLanguage), Icons.Default.Person, "Labor"),
        QuickAccessItem(t("Site Photos", selectedLanguage), Icons.Default.PhotoCamera, "Photos"),
        QuickAccessItem(t("Standard Rates", selectedLanguage), Icons.Default.Assessment, "Rates")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp, horizontal = 16.dp)
    ) {
        item {
            Image(
                painter = painterResource(id = R.drawable.banner),
                contentDescription = "Construction banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(20.dp))
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        t("Quick Access", selectedLanguage),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    homeActions.chunked(2).forEach { rowActions ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowActions.forEach { action ->
                                ModernQuickAccessCard(
                                    title = action.title,
                                    icon = action.icon,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onQuickAccessSelected(action.destination) }
                                )
                            }
                            if (rowActions.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    t("Active Sites", selectedLanguage),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "View All",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { }
                )
            }
        }

        if (sites.isEmpty()) {
            item {
                ModernEmptyState(
                    icon = Icons.Default.Construction,
                    title = "No Active Sites",
                    subtitle = "Create your first site to start tracking construction progress",
                    actionLabel = "+ Add Site",
                    onAction = { onQuickAccessSelected("Add Site") }
                )
            }
        } else {
            items(sites) { site ->
                SiteProgressCard(site = site, onClick = { onSiteSelected(site.id) })
            }
        }
    }
}

data class QuickAccessItem(val title: String, val icon: ImageVector, val destination: String)

data class DrawerItem(val title: String, val icon: ImageVector, val route: String)

data class ProfileData(
    val name: String = "",
    val phone: String = "",
    val role: String = "",
    val photoUri: String? = null
)

private const val PREFS_PROFILE = "namma_mistri_profile"

fun loadProfile(context: Context): ProfileData {
    val p = context.getSharedPreferences(PREFS_PROFILE, Context.MODE_PRIVATE)
    return ProfileData(
        name = p.getString("name", "") ?: "",
        phone = p.getString("phone", "") ?: "",
        role = p.getString("role", "") ?: "",
        photoUri = p.getString("photo_uri", null)
    )
}

fun saveProfile(context: Context, profile: ProfileData) {
    val editor = context.getSharedPreferences(PREFS_PROFILE, Context.MODE_PRIVATE).edit()
    editor.putString("name", profile.name)
    editor.putString("phone", profile.phone)
    editor.putString("role", profile.role)
    if (profile.photoUri != null) editor.putString("photo_uri", profile.photoUri)
    else editor.remove("photo_uri")
    editor.apply()
}

// ── Translations ──────────────────────────────────────────────────
private val kannadaStrings = mapOf(
    "Dashboard" to "ಮುಖಪುಟ",
    "Sites" to "ತಾಣಗಳು",
    "Calculate" to "ಲೆಕ್ಕ",
    "Calculator" to "ಲೆಕ್ಕ",
    "Labor" to "ಕೆಲಸ",
    "Labor Diary" to "ಕಾರ್ಮಿಕ ದಾಖಲೆ",
    "Rates" to "ದರ",
    "Standard Rates" to "ದರ ಪಟ್ಟಿ",
    "Add New Site" to "ಹೊಸ ತಾಣ",
    "Quick Access" to "ತ್ವರಿತ ಪ್ರವೇಶ",
    "Site Photos" to "ತಾಣ ಫೋಟೋ",
    "Active Sites" to "ಸಕ್ರಿಯ ತಾಣಗಳು"
)

private val hindiStrings = mapOf(
    "Dashboard" to "डैशबोर्ड",
    "Sites" to "साइट्स",
    "Calculate" to "कैलकुलेटर",
    "Calculator" to "कैलकुलेटर",
    "Labor" to "मजदूरी",
    "Labor Diary" to "मजदूरी डायरी",
    "Rates" to "दरें",
    "Standard Rates" to "मानक दरें",
    "Add New Site" to "नई साइट",
    "Quick Access" to "त्वरित पहुँच",
    "Site Photos" to "साइट फ़ोटो",
    "Active Sites" to "सक्रिय साइट्स"
)

fun t(key: String, lang: String): String = when (lang) {
    "ಕನ್ನಡ" -> kannadaStrings[key] ?: key
    "हिन्दी" -> hindiStrings[key] ?: key
    else -> key
}

@Composable
fun AddSiteScreen(repository: NammaMistriRepository, onSaved: () -> Unit) {
    val context = LocalContext.current
    var siteName by rememberSaveable { mutableStateOf("") }
    var siteLocation by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var isLocating by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            isLocating = true
            scope.launch(Dispatchers.IO) {
                try {
                    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    @Suppress("MissingPermission")
                    val location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                        ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if (location != null) {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        val addressLine = addresses?.firstOrNull()?.getAddressLine(0)
                            ?: "%.5f, %.5f".format(location.latitude, location.longitude)
                        withContext(Dispatchers.Main) { siteLocation = addressLine }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Could not detect location. Please enter manually.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Location error. Please enter manually.", Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    withContext(Dispatchers.Main) { isLocating = false }
                }
            }
        } else {
            Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Add New Site", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)

        OutlinedTextField(
            value = siteName,
            onValueChange = { siteName = it },
            label = { Text("Site Name") },
            leadingIcon = { Icon(Icons.Default.Construction, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = siteLocation,
            onValueChange = { siteLocation = it },
            label = { Text("Location / Address") },
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
            trailingIcon = {
                if (isLocating) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            minLines = 2
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLocating
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    if (isLocating) "Detecting..." else "Use My Location",
                    fontSize = 12.sp
                )
            }
            OutlinedButton(
                onClick = {
                    val query = Uri.encode(siteLocation.ifBlank { "construction site" })
                    val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$query"))
                        .apply { setPackage("com.google.android.apps.maps") }
                    if (mapIntent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(mapIntent)
                    } else {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=$query"))
                        )
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("View on Map", fontSize = 12.sp)
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                if (siteName.isBlank()) {
                    Toast.makeText(context, "Please enter a site name", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (siteLocation.isBlank()) {
                    Toast.makeText(context, "Please enter a location", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                scope.launch {
                    repository.insertSite(Site(name = siteName.trim(), location = siteLocation.trim()))
                    Toast.makeText(context, "Site \"${siteName.trim()}\" added!", Toast.LENGTH_SHORT).show()
                    onSaved()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B00))
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Save Site", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun ProfileScreen(
    profileData: ProfileData,
    onSaved: (ProfileData) -> Unit
) {
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf(profileData.name) }
    var phone by rememberSaveable { mutableStateOf(profileData.phone) }
    var role by rememberSaveable { mutableStateOf(profileData.role) }
    var photoUri by rememberSaveable { mutableStateOf(profileData.photoUri) }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) pendingPhotoUri?.let { photoUri = it.toString() }
        pendingPhotoUri = null
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { photoUri = it.toString() } }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val file = File(
                context.getExternalFilesDir("Pictures") ?: context.filesDir,
                "profile_${System.currentTimeMillis()}.jpg"
            )
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            pendingPhotoUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Camera permission required", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Avatar section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (photoUri != null) {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = "Profile photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(54.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Take Selfie", fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Gallery", fontSize = 13.sp)
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
        }

        item {
            OutlinedTextField(
                value = role,
                onValueChange = { role = it },
                label = { Text("Trade / Role (e.g. Mason, Carpenter)") },
                leadingIcon = { Icon(Icons.Default.Work, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }

        item {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val updated = ProfileData(
                        name = name.trim(),
                        phone = phone.trim(),
                        role = role.trim(),
                        photoUri = photoUri
                    )
                    saveProfile(context, updated)
                    onSaved(updated)
                    Toast.makeText(context, "Profile saved!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B00))
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Save Profile", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun QuickAccessCard(action: QuickAccessItem, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = action.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun SiteProgressCard(site: Site, onClick: () -> Unit) {
    val progress = site.progress.coerceIn(0, 100)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(site.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(site.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = progress / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Progress: $progress%", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap to open site photos and update progress",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ViewModel Factories
class CalculatorViewModelFactory(private val repository: NammaMistriRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CalculatorViewModel(repository) as T
    }
}

class LaborViewModelFactory(private val repository: NammaMistriRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return LaborViewModel(repository) as T
    }
}

class PhotoViewModelFactory(private val repository: NammaMistriRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PhotoViewModel(repository) as T
    }
}

class RatesViewModelFactory(private val repository: NammaMistriRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return RatesViewModel(repository) as T
    }
}
