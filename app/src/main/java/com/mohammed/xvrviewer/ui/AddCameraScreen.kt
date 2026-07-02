package com.mohammed.xvrviewer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mohammed.xvrviewer.data.Camera

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCameraScreen(
    onBack: () -> Unit,
    onSave: (Camera) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var ip by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("554") }
    var channel by remember { mutableStateOf("1") }
    var username by remember { mutableStateOf("admin") }
    var password by remember { mutableStateOf("") }
    var quotaMb by remember { mutableStateOf("5120") }
    var recordingEnabled by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إضافة كاميرا جديدة") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("اسم الكاميرا") }, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = ip, onValueChange = { ip = it },
                label = { Text("عنوان IP لجهاز الـ XVR") },
                placeholder = { Text("192.168.1.100") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = port, onValueChange = { port = it },
                    label = { Text("المنفذ (Port)") }, modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = channel, onValueChange = { channel = it },
                    label = { Text("رقم القناة") }, modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = username, onValueChange = { username = it },
                label = { Text("اسم المستخدم") }, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("كلمة المرور") },
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = quotaMb, onValueChange = { quotaMb = it },
                label = { Text("الحد الأقصى للتخزين (ميجابايت)") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("تسجيل تلقائي مستمر (بدون تدخل)")
                Switch(checked = recordingEnabled, onCheckedChange = { recordingEnabled = it })
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    if (name.isNotBlank() && ip.isNotBlank()) {
                        onSave(
                            Camera(
                                name = name,
                                ipAddress = ip,
                                port = port.toIntOrNull() ?: 554,
                                channel = channel.toIntOrNull() ?: 1,
                                username = username,
                                password = password,
                                isRecordingEnabled = recordingEnabled,
                                storageQuotaMb = quotaMb.toLongOrNull() ?: 5120
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("حفظ الكاميرا")
            }

            Text(
                "ملاحظة: يتم بناء رابط RTSP تلقائياً بصيغة أغلب أجهزة XVR الشائعة " +
                    "(rtsp://ip:port/cam/realmonitor?channel=X&subtype=0). " +
                    "إذا كان جهازك يستخدم صيغة مختلفة راجع دليل الجهاز لمسار البث الصحيح.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
