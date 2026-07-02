package com.mohammed.xvrviewer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mohammed.xvrviewer.data.Recording
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingsScreen(
    recordings: List<Recording>,
    onBack: () -> Unit,
    onPlay: (Recording) -> Unit,
    onDelete: (Recording) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale("ar")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("التسجيلات المحفوظة") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        if (recordings.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("لا توجد تسجيلات محفوظة بعد")
            }
        } else {
            LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(8.dp)) {
                items(recordings, key = { it.id }) { rec ->
                    ListItem(
                        headlineContent = { Text(rec.cameraName) },
                        supportingContent = {
                            val sizeMb = rec.fileSizeBytes / (1024 * 1024)
                            Text("${dateFormat.format(Date(rec.startTime))} • ${sizeMb} MB")
                        },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { onPlay(rec) }) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "تشغيل")
                                }
                                IconButton(onClick = { onDelete(rec) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف")
                                }
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
