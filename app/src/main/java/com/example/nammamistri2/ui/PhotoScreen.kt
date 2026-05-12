package com.example.nammamistri2.ui

import android.Manifest
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.nammamistri2.repository.NammaMistriRepository
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoScreen(
    repository: NammaMistriRepository,
    selectedSiteId: Long?,
    onSelectSite: (Long?) -> Unit,
    onNavigateToAddSite: () -> Unit = {}
) {
    val sites by repository.getAllSites().collectAsState(initial = emptyList())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentSiteId by rememberSaveable { mutableStateOf(selectedSiteId) }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedProgress by rememberSaveable { mutableStateOf(0f) }
    var selectedFilter by rememberSaveable { mutableStateOf("All") }

    LaunchedEffect(selectedSiteId) {
        if (selectedSiteId != null) {
            currentSiteId = selectedSiteId
        }
    }

    val currentSite = sites.firstOrNull { it.id == currentSiteId }
    val photosState = if (currentSiteId != null) {
        repository.getPhotosBySite(currentSiteId!!).collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList<com.example.nammamistri2.data.Photo>()) }
    }
    val photos by photosState

    LaunchedEffect(currentSite) {
        currentSite?.let { selectedProgress = it.progress.toFloat() }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentSiteId != null) {
            pendingPhotoUri?.let { uri ->
                scope.launch {
                    repository.insertPhoto(
                        com.example.nammamistri2.data.Photo(
                            siteId = currentSiteId!!,
                            uri = uri.toString(),
                            description = "Work progress"
                        )
                    )
                }
            }
        }
        pendingPhotoUri = null
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createImageUri(context)
            if (uri != null) {
                pendingPhotoUri = uri
                cameraLauncher.launch(uri)
            } else {
                Toast.makeText(context, "Unable to access external storage", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "Camera permission is required to take photos", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        floatingActionButton = {
            if (currentSite != null) {
                ExtendedFloatingActionButton(
                    onClick = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    icon = { Icon(Icons.Default.PhotoCamera, contentDescription = "Add Photo") },
                    text = { Text("Add Photos") }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp, horizontal = 16.dp)
        ) {
            item {
                ModernHeaderBanner(
                    title = "Track Site Progress",
                    subtitle = "Capture and organize your construction updates",
                    backgroundColor = MaterialTheme.colorScheme.primary
                )
            }

            if (currentSite == null) {
                if (sites.isEmpty()) {
                    item {
                        ModernEmptyState(
                            icon = Icons.Default.LocationOn,
                            title = "No Sites Available",
                            subtitle = "Create a site first to track photos",
                            actionLabel = "Add Site",
                            onAction = onNavigateToAddSite
                        )
                    }
                } else {
                    item {
                        Text(
                            "Select a Site",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    items(sites) { site ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentSiteId = site.id
                                    onSelectSite(site.id)
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        site.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        site.location,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Select",
                                    modifier = Modifier
                                        .size(20.dp)
                                        .graphicsLayer(rotationZ = 180f),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                return@LazyColumn
            }

            // Site Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    currentSite.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    currentSite.location,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = {
                                    currentSiteId = null
                                    onSelectSite(null)
                                },
                                modifier = Modifier.height(36.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Change", fontSize = 12.sp)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            "Progress: ${selectedProgress.toInt()}%",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Slider(
                            value = selectedProgress,
                            onValueChange = { selectedProgress = it },
                            valueRange = 0f..100f,
                            steps = 4,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Button(
                            onClick = {
                                scope.launch {
                                    repository.updateSite(currentSite.copy(progress = selectedProgress.toInt()))
                                    Toast.makeText(context, "Progress updated", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Update Progress")
                        }
                    }
                }
            }

            // Stats Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ModernStatsCard(
                        label = "Total Photos",
                        value = photos.size.toString(),
                        icon = Icons.Default.PhotoCamera,
                        backgroundColor = Color(0xFFFFF3E8),
                        iconTint = Color(0xFFFF6B00),
                        modifier = Modifier.weight(1f)
                    )
                    ModernStatsCard(
                        label = "Progress",
                        value = "${selectedProgress.toInt()}%",
                        icon = Icons.Default.Assessment,
                        backgroundColor = Color(0xFFE8F5E9),
                        iconTint = Color(0xFF2E7D32),
                        progressFraction = selectedProgress / 100f,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Filter Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All", "Today", "This Week", "Completed", "Ongoing").forEach { filter ->
                        ModernFilterChip(
                            label = filter,
                            isSelected = selectedFilter == filter,
                            onClick = { selectedFilter = filter }
                        )
                    }
                }
            }

            // Photo Gallery
            if (photos.isEmpty()) {
                item {
                    ModernEmptyState(
                        icon = Icons.Default.PhotoCamera,
                        title = "No Site Photos Yet",
                        subtitle = "Capture construction progress to track your work visually",
                        actionLabel = "Take First Photo",
                        onAction = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                    )
                }
            } else {
                items(photos) { photo ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column {
                            AsyncImage(
                                model = photo.uri,
                                contentDescription = photo.description,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        photo.description,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "Captured on ${java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(java.util.Date())}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            repository.deletePhoto(photo.id)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun createImageUri(context: Context): Uri? {
    val picturesDir = context.getExternalFilesDir("Pictures") ?: return null
    val imageFile = File(
        picturesDir,
        "photo_${System.currentTimeMillis()}.jpg"
    )
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )
}