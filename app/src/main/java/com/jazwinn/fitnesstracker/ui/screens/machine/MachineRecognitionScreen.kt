package com.jazwinn.fitnesstracker.ui.screens.machine

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.jazwinn.fitnesstracker.ui.viewmodel.MachineRecognitionUiState
import com.jazwinn.fitnesstracker.ui.viewmodel.MachineRecognitionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Image
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MachineRecognitionScreen(
    onNavigateBack: () -> Unit,
    viewModel: MachineRecognitionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    
    LaunchedEffect(Unit) {
        cameraPermissionState.launchPermissionRequest()
    }

    if (cameraPermissionState.status.isGranted) {
        when (val state = uiState) {
            is MachineRecognitionUiState.Idle -> {
                CameraCaptureScreen(
                    onImageCaptured = { bitmap ->
                        viewModel.analyzeImage(bitmap)
                    },
                    onBack = onNavigateBack
                )
            }
            is MachineRecognitionUiState.Analyzing -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                    Text("Identifying Equipment...", modifier = Modifier.padding(top = 64.dp))
                }
            }
            is MachineRecognitionUiState.Recognized -> {
                RecognitionResultScreen(
                    bitmap = state.bitmap,
                    machineName = state.machineName,
                    onRetake = { viewModel.reset() },
                    onGetGuide = { viewModel.fetchGuide(state.machineName) }
                )
            }
            is MachineRecognitionUiState.LoadingGuide -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                    Text("Generating Guide for ${state.machineName}...", modifier = Modifier.padding(top = 64.dp))
                }
            }
            is MachineRecognitionUiState.GuideLoaded -> {
                GuideDisplayScreen(
                    machineName = state.machineName,
                    guideText = state.guideText,
                    onDone = { viewModel.reset() }
                )
            }
            is MachineRecognitionUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Error: ${state.message}", color = Color.Red)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.reset() }) {
                            Text("Try Again")
                        }
                    }
                }
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Camera permission required.")
            Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                Text("Grant Permission")
            }
        }
    }
}

private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        BitmapFactory.decodeStream(inputStream)
    } catch (e: Exception) {
        Log.e("MachineRecognition", "Failed to load image from gallery", e)
        null
    }
}

@Composable
fun CameraCaptureScreen(
    onImageCaptured: (Bitmap) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val preview = remember { Preview.Builder().build() }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = loadBitmapFromUri(context, it)
            if (bitmap != null) {
                // Ensure ARGB_8888 for TFLite compatibility
                val safeBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                onImageCaptured(safeBitmap)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { previewView ->
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()
                
                // Safe check for camera availability (reusing logic from ExerciseScreen fix)
                // However, for simplicity here, assuming if lensFacing is set, it exists or we catch error.
                
                try {
                    val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                    preview.setSurfaceProvider(previewView.surfaceProvider)
                } catch (e: Exception) {
                    Log.e("MachineRecognition", "Camera bind failed", e)
                }
            }
        )

        // Overlay UI
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                IconButton(onClick = {
                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) 
                        CameraSelector.LENS_FACING_FRONT 
                    else 
                        CameraSelector.LENS_FACING_BACK
                }) {
                    Icon(Icons.Default.Cameraswitch, contentDescription = "Switch Camera", tint = Color.White)
                }
            }

            // Bottom Bar (Capture & Gallery)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Gallery Button
                IconButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Image, contentDescription = "Gallery", tint = Color.White)
                }

                // Capture Button
                Button(
                    onClick = {
                        captureImage(imageCapture, context) { bitmap ->
                            onImageCaptured(bitmap)
                        }
                    },
                    shape = CircleShape,
                    modifier = Modifier.size(80.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Icon(Icons.Default.Camera, contentDescription = "Capture", tint = Color.Black)
                }
                
                // Placeholder to balance layout (empty 56dp)
                Spacer(modifier = Modifier.size(56.dp))
            }
        }
    }
}

@Composable
fun RecognitionResultScreen(
    bitmap: Bitmap,
    machineName: String,
    onRetake: () -> Unit,
    onGetGuide: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Identified Machine", color = Color.White, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))
        
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Captured Image",
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = machineName,
            color = Color.Green,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = onRetake,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retake")
            }
            
            Button(
                onClick = onGetGuide,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Get Guide")
            }
        }
    }
}

@Composable
fun GuideDisplayScreen(
    machineName: String,
    guideText: String,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = machineName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = onDone) {
                Icon(Icons.Default.Check, contentDescription = "Done")
            }
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        
        // Markdown Rendering
        dev.jeziellago.compose.markdowntext.MarkdownText(
            markdown = guideText,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyLarge
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Done")
        }
    }
}

private fun captureImage(
    imageCapture: ImageCapture,
    context: Context,
    onImageCaptured: (Bitmap) -> Unit
) {
    val mainExecutor = ContextCompat.getMainExecutor(context)
    
    imageCapture.takePicture(mainExecutor, object : ImageCapture.OnImageCapturedCallback() {
        override fun onCaptureSuccess(image: ImageProxy) {
            try {
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.capacity())
                buffer.get(bytes)
                var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                
                // Rotation adjustment
                val rotation = image.imageInfo.rotationDegrees
                if (rotation != 0) {
                    val matrix = Matrix()
                    matrix.postRotate(rotation.toFloat())
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                }
                
                onImageCaptured(bitmap)
            } finally {
                image.close()
            }
        }

        override fun onError(exception: ImageCaptureException) {
            Log.e("CameraCapture", "Photo capture failed: ${exception.message}", exception)
        }
    })
}
