package com.example.nammamistri2

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

                        ModalNavigationDrawer(
                            drawerState = drawerState,
                            drawerContent = {
                                ModalDrawerSheet(
                                    modifier = Modifier.fillMaxWidth(0.85f)
                                ) {
                                    ModernDrawerHeader(
                                        userName = "Mistri",
                                        location = "Bangalore, India"
                                    )
                                    
                                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                                    
                                    val drawerItems = listOf(
                                        DrawerItem("Home", Icons.Default.List, "Home"),
                                        DrawerItem("Add Site", Icons.Default.Add, "Add Site"),
                                        DrawerItem("Calculator", Icons.Default.Calculate, "Calculator"),
                                        DrawerItem("Labor Diary", Icons.Default.Person, "Labor"),
                                        DrawerItem("Site Photos", Icons.Default.PhotoCamera, "Photos"),
                                        DrawerItem("Standard Rates", Icons.Default.Assessment, "Rates")
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
                                onToggleTheme = { isDarkTheme = !isDarkTheme }
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
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.openpage),
            contentDescription = "Opening page",
            contentScale = ContentScale.Fit,
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
    onToggleTheme: () -> Unit = {}
) {
    val sites by repository.getAllSites().collectAsState(initial = emptyList())
    var selectedBottomNavItem by remember { mutableIntStateOf(0) }
    
    val bottomNavItems = listOf(
        BottomNavItem("Dashboard", Icons.Default.Home, "Home"),
        BottomNavItem("Sites", Icons.Default.LocationOn, "Photos"),
        BottomNavItem("Calculate", Icons.Default.Calculate, "Calculator"),
        BottomNavItem("Labor", Icons.Default.Person, "Labor"),
        BottomNavItem("Rates", Icons.Default.Assessment, "Rates")
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
                    if (currentScreen == "Home") {
                        Column {
                            Text("Dashboard", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Bangalore, India", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Text(currentScreen)
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications"
                        )
                    }
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDarkTheme) "Switch to Light Mode" else "Switch to Dark Mode"
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
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
                    }
                )
                "Add Site" -> AddSiteScreen(
                    repository = repository,
                    onSaved = { onScreenSelected("Home") }
                )
                "Calculator" -> CalculatorScreen(viewModel(factory = CalculatorViewModelFactory(repository)))
                "Labor" -> LaborScreen(viewModel = viewModel(factory = LaborViewModelFactory(repository)), onBack = {
                    onScreenSelected("Home")
                    selectedBottomNavItem = 0
                })
                "Photos" -> PhotoScreen(
                    repository = repository,
                    selectedSiteId = selectedSiteId,
                    onSelectSite = onSelectSite
                )
                "Rates" -> RatesScreen(viewModel(factory = RatesViewModelFactory(repository)))
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
                    }
                )
            }
        }
    }
}

@Composable
fun HomeScreen(
    sites: List<Site>,
    onQuickAccessSelected: (String) -> Unit,
    onSiteSelected: (Long) -> Unit
) {
    val homeActions = listOf(
        QuickAccessItem("Calculator", Icons.Default.Calculate, "Calculator"),
        QuickAccessItem("Labor Diary", Icons.Default.Person, "Labor"),
        QuickAccessItem("Site Photos", Icons.Default.PhotoCamera, "Photos"),
        QuickAccessItem("Standard Rates", Icons.Default.Assessment, "Rates")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp, horizontal = 16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Namaskara, Mistri!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "ನಿರ್ಮಾಣ ಕಾರ್ಯ ಕೆಲಸವನ್ನು ಸುಗಮವಾಗಿ ನಡೆಸಿ",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Bangalore, Karnataka",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
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
                        "Quick Access",
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
                    "Active Sites",
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

@Composable
fun AddSiteScreen(repository: NammaMistriRepository, onSaved: () -> Unit) {
    val context = LocalContext.current
    var siteName by rememberSaveable { mutableStateOf("") }
    var siteLocation by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Add Site", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = siteName,
            onValueChange = { siteName = it },
            label = { Text("Site name") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = siteLocation,
            onValueChange = { siteLocation = it },
            label = { Text("Location") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                if (siteName.isBlank() || siteLocation.isBlank()) {
                    Toast.makeText(context, "Enter name and location", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                scope.launch {
                    repository.insertSite(Site(name = siteName.trim(), location = siteLocation.trim()))
                    onSaved()
                    Toast.makeText(context, "Site added", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Save Site")
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
