package com.mohammed.xvrviewer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohammed.xvrviewer.data.Camera

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraListScreen(
    cameras: List<Camera>,
    onAddCamera: () -> Unit,
    onOpenCamera: (Camera) -> Unit,
    onOpenRecordings: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("كاميرات المراقبة", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onOpenRecordings) {
                        Icon(Icons.Default.Videocam, contentDescription = "التسجيلات")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddCamera) {
                Icon(Icons.Default.Add, contentDescription = "إضافة كاميرا")
            }
        }
    ) { padding ->
        if (cameras.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("لا توجد كاميرات مضافة بعد\nاضغط + لإضافة كاميرا من جهاز الـ XVR", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(padding)
            ) {
                items(cameras) { camera ->
                    CameraCard(camera = camera, onClick = { onOpenCamera(camera) })
                }
            }
        }
    }
}

@Composable
private fun CameraCard(camera: Camera, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp)),
        onClick = onClick
    ) {
        Box(Modifier.fillMaxSize().background(Color(0xFF1B1B1F))) {
            Icon(
                Icons.Default.Videocam,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.align(Alignment.Center).size(48.dp)
            )
            Row(
                Modifier.align(Alignment.TopStart).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (camera.isRecordingEnabled) {
                    Icon(
                        Icons.Default.FiberManualRecord,
                        contentDescription = "يسجّل",
                        tint = Color.Red,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                }
            }
            Text(
                text = camera.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
            )
        }
    }
}
