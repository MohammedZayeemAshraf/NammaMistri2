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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
    onSelectSite: (Long?) -> Unit
) {
    val sites by repository.getAllSites().collectAsState(initial = emptyList())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentSiteId by rememberSaveable { mutableStateOf(selectedSiteId) }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedProgress by rememberSaveable { mutableStateOf(0f) }

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
                FloatingActionButton(onClick = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }) {
                    Text("+")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Site Photos", style = MaterialTheme.typography.headlineMedium)
            }

            if (currentSite == null) {
                if (sites.isEmpty()) {
                    item {
                        Text("No sites available. Please add a site first.", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    item {
                        Text("Select a site to view photos", style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    items(sites) { site ->
                        Button(
                            onClick = {
                                currentSiteId = site.id
                                onSelectSite(site.id)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(site.name)
                        }
                    }
                }
                return@LazyColumn
            }

            item {
                Text("Site: ${currentSite.name}", style = MaterialTheme.typography.headlineSmall)
                Text(currentSite.location, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Progress: ${selectedProgress.toInt()}%", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = selectedProgress,
                    onValueChange = { selectedProgress = it },
                    valueRange = 0f..100f,
                    steps = 4
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    scope.launch {
                        repository.updateSite(currentSite.copy(progress = selectedProgress.toInt()))
                        Toast.makeText(context, "Progress updated", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("Update progress")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Photos for ${currentSite.name}", style = MaterialTheme.typography.headlineSmall)
                    TextButton(onClick = {
                        currentSiteId = null
                        onSelectSite(null)
                    }) {
                        Text("Change site")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (photos.isEmpty()) {
                item {
                    Text("No photos yet for this site.", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                items(photos) { photo ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            AsyncImage(
                                model = photo.uri,
                                contentDescription = photo.description,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(photo.description)
                                    Text("Date: ${java.util.Date(photo.date)}")
                                }
                                TextButton(
                                    onClick = {
                                        scope.launch { repository.deletePhoto(photo.id) }
                                    },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("Delete")
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