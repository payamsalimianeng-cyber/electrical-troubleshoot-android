package com.electrical.troubleshoot.ui.screens.troubleshooting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electrical.troubleshoot.data.model.*
import com.electrical.troubleshoot.data.remote.NetworkModule
import com.electrical.troubleshoot.ui.theme.AppColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TroubleshootingScreen(
    sessionId: String?,
    roomId: String?,
    mechanismId: String?,
    mechanismName: String?,
    onBack: () -> Unit,
    onSessionCreated: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember { NetworkModule.provideApiService(context) }

    var session by remember { mutableStateOf<Session?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var problemText by remember { mutableStateOf("") }
    var currentStepIndex by remember { mutableIntStateOf(0) }
    var noteText by remember { mutableStateOf("") }
    var showNoteDialog by remember { mutableStateOf(false) }

    // Create session if needed
    LaunchedEffect(sessionId, mechanismId) {
        if (sessionId != null) {
            // Load existing
            try {
                val res = api.getSession(sessionId)
                if (res.success && res.data != null) {
                    session = res.data
                    currentStepIndex = res.data.steps.indexOfFirst { it.status == "PENDING" }
                        .coerceAtLeast(0)
                }
            } catch (e: Exception) {
                error = e.message
            } finally {
                isLoading = false
            }
        } else {
            isLoading = false // wait for user to enter problem and start
        }
    }

    fun startNewSession() {
        if (roomId == null || mechanismId == null || problemText.isBlank()) return
        scope.launch {
            isLoading = true
            try {
                val res = api.startSession(
                    StartSessionRequest(roomId, mechanismId, problemText.trim())
                )
                if (res.success && res.data != null) {
                    onSessionCreated(res.data._id)
                } else {
                    error = res.message ?: "خطا در شروع جلسه"
                }
            } catch (e: Exception) {
                error = e.message
            } finally {
                isLoading = false
            }
        }
    }

    fun updateStep(status: String) {
        val s = session ?: return
        val step = s.steps.getOrNull(currentStepIndex) ?: return
        scope.launch {
            try {
                val res = api.updateStep(
                    s._id,
                    step._id,
                    UpdateStepRequest(status, noteText.ifBlank { null })
                )
                if (res.success && res.data != null) {
                    session = res.data
                    noteText = ""
                    if (status == "CHECKED") {
                        // resolved
                    } else {
                        // go next pending
                        val next = res.data.steps.indexOfFirst { it.status == "PENDING" }
                        currentStepIndex = if (next >= 0) next else res.data.steps.lastIndex
                    }
                }
            } catch (e: Exception) {
                error = e.message
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        mechanismName ?: session?.mechanism?.name ?: "عیب‌یابی",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        when {
            isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            session == null && sessionId == null -> {
                // Start form
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "توضیح مشکل را بنویسید",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "مثلاً: موتور نوار نقاله کار نمی‌کند / رله فلان قطع است",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(20.dp))
                    OutlinedTextField(
                        value = problemText,
                        onValueChange = { problemText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        placeholder = { Text("شرح مشکل...") }
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { startNewSession() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = problemText.isNotBlank()
                    ) {
                        Text("شروع چک‌لیست عیب‌یابی", fontSize = 16.sp)
                    }
                    if (error != null) {
                        Spacer(Modifier.height(12.dp))
                        Text(error!!, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            session != null -> {
                val s = session!!
                val steps = s.steps
                val current = steps.getOrNull(currentStepIndex)
                val progress = steps.count { it.status != "PENDING" }.toFloat() / steps.size.coerceAtLeast(1)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    // Progress
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                    )
                    Text(
                        text = "مرحله ${currentStepIndex + 1} از ${steps.size}",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall
                    )

                    if (s.status == "RESOLVED") {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Check,
                                    null,
                                    tint = AppColors.Success,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "مشکل برطرف شد",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.Success
                                )
                                Spacer(Modifier.height(24.dp))
                                Button(onClick = onBack) { Text("بازگشت به لیست") }
                            }
                        }
                    } else if (current != null) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        ) {
                            // Point type badge
                            Surface(
                                color = when (current.point.type) {
                                    "RELAY" -> Color(0xFFE3F2FD)
                                    "TERMINAL" -> Color(0xFFE8F5E9)
                                    "FUSE" -> Color(0xFFFFF3E0)
                                    else -> Color(0xFFF3E5F5)
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    current.point.type,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                current.point.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            if (!current.point.instruction.isNullOrBlank()) {
                                Text(
                                    current.point.instruction,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            } else {
                                Text(
                                    "این مورد را بررسی کنید و نتیجه را ثبت کنید.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "صفحه ${current.point.page} نقشه",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Action buttons - large for field use
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { updateStep("CHECKED") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Success)
                            ) {
                                Icon(Icons.Default.Check, null)
                                Spacer(Modifier.width(8.dp))
                                Text("پیدا شد / مشکل برطرف شد", fontSize = 16.sp)
                            }
                            OutlinedButton(
                                onClick = { updateStep("NOT_FOUND") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Error)
                            ) {
                                Icon(Icons.Default.Close, null)
                                Spacer(Modifier.width(8.dp))
                                Text("پیدا نشد → مرحله بعدی", fontSize = 16.sp)
                            }
                            TextButton(
                                onClick = { updateStep("SKIPPED") },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.SkipNext, null)
                                Spacer(Modifier.width(4.dp))
                                Text("رد کردن این مرحله")
                            }
                        }
                    }
                }
            }
            else -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(error ?: "خطا در بارگذاری")
                }
            }
        }
    }
}
