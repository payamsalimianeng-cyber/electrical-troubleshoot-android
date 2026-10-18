package com.electrical.troubleshoot.ui.screens.pdf

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electrical.troubleshoot.data.remote.*
import com.electrical.troubleshoot.ui.theme.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadPdfScreen(
    onBack: () -> Unit,
    onCreated: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember { NetworkModule.provideApiService(context) }

    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var isCreating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var analyzeResult by remember { mutableStateOf<EplanAnalyzeResponse?>(null) }

    // Editable fields after analysis
    var roomName by remember { mutableStateOf("") }
    var roomCode by remember { mutableStateOf("") }
    var mechanismName by remember { mutableStateOf("") }
    var mechanismCode by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }

    var showSuccess by remember { mutableStateOf(false) }

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            selectedFileName = getFileName(context, uri) ?: "document.pdf"
            analyzeResult = null
            error = null
            showSuccess = false
        }
    }

    fun analyzePdf() {
        val uri = selectedUri ?: return
        scope.launch {
            isAnalyzing = true
            error = null
            analyzeResult = null
            try {
                val part = withContext(Dispatchers.IO) {
                    uriToMultipart(context, uri, selectedFileName ?: "map.pdf")
                }
                val result = api.analyzePdf(part)
                analyzeResult = result

                if (result.success && result.extracted != null) {
                    val e = result.extracted
                    roomName = e.suggestedRoomName ?: e.installationLocation ?: ""
                    roomCode = e.suggestedRoomCode ?: e.drawingNumber ?: ""
                    mechanismName = e.suggestedMechanismName ?: e.cabinet ?: e.pageDescription ?: ""
                    mechanismCode = ""
                    location = e.installationLocation ?: ""
                } else {
                    error = result.message ?: "استخراج ناموفق بود"
                }
            } catch (e: Exception) {
                error = "خطا در ارسال فایل: ${e.message}"
            } finally {
                isAnalyzing = false
            }
        }
    }

    fun createFromExtract() {
        if (roomName.isBlank() || mechanismName.isBlank()) {
            error = "نام اتاق و نام مکانیزم الزامی است"
            return
        }
        scope.launch {
            isCreating = true
            error = null
            try {
                val body = CreateFromExtractRequest(
                    tempFile = analyzeResult?.tempFile,
                    roomName = roomName.trim(),
                    roomCode = roomCode.trim().ifBlank { null },
                    mechanismName = mechanismName.trim(),
                    mechanismCode = mechanismCode.trim().ifBlank { null },
                    location = location.trim().ifBlank { null }
                )
                val result = api.createFromExtract(body)
                if (result.success) {
                    showSuccess = true
                } else {
                    error = result.message ?: "خطا در ایجاد"
                }
            } catch (e: Exception) {
                error = "خطا: ${e.message}"
            } finally {
                isCreating = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("آپلود نقشه EPLAN", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Step 1: Select file
            Text(
                "۱. انتخاب فایل PDF",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(12.dp))

                    if (selectedFileName != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(selectedFileName!!, fontWeight = FontWeight.Medium)
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    OutlinedButton(
                        onClick = { pdfPicker.launch("application/pdf") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (selectedFileName == null) "انتخاب فایل PDF" else "تغییر فایل")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Analyze button
            Button(
                onClick = { analyzePdf() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = selectedUri != null && !isAnalyzing
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("در حال استخراج...")
                } else {
                    Text("استخراج نام از نقشه EPLAN", fontSize = 15.sp)
                }
            }

            // Error
            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(error!!, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }

            // Success after create
            if (showSuccess) {
                Spacer(Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFFE8F5E9))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            null,
                            tint = AppColors.Success,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "اتاق و مکانیزم با موفقیت ایجاد شد",
                            fontWeight = FontWeight.Bold,
                            color = AppColors.Success
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = onCreated) {
                            Text("بازگشت به لیست اتاق‌ها")
                        }
                    }
                }
            }

            // Step 2: Show extracted + editable fields
            if (analyzeResult?.success == true && analyzeResult?.extracted != null && !showSuccess) {
                Spacer(Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                Text(
                    "۲. بررسی و تأیید اطلاعات استخراج‌شده",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))

                val conf = analyzeResult!!.extracted!!.confidence
                Text(
                    "اطمینان استخراج: $conf٪",
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        conf >= 60 -> AppColors.Success
                        conf >= 30 -> AppColors.Warning
                        else -> AppColors.Error
                    }
                )

                Spacer(Modifier.height(16.dp))

                // Show raw extracted fields as info
                val e = analyzeResult!!.extracted!!
                if (!e.installationLocation.isNullOrBlank() || !e.cabinet.isNullOrBlank() ||
                    !e.pageDescription.isNullOrBlank() || !e.projectName.isNullOrBlank()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("مقادیر خام استخراج‌شده:", fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(6.dp))
                            e.installationLocation?.let { Text("• محل نصب: $it", fontSize = 13.sp) }
                            e.cabinet?.let { Text("• تابلو: $it", fontSize = 13.sp) }
                            e.pageDescription?.let { Text("• شرح صفحه: $it", fontSize = 13.sp) }
                            e.projectName?.let { Text("• پروژه: $it", fontSize = 13.sp) }
                            e.drawingNumber?.let { Text("• شماره نقشه: $it", fontSize = 13.sp) }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                OutlinedTextField(
                    value = roomName,
                    onValueChange = { roomName = it },
                    label = { Text("نام اتاق برق *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = roomCode,
                    onValueChange = { roomCode = it },
                    label = { Text("کد اتاق (اختیاری)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("محل / توضیحات") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = mechanismName,
                    onValueChange = { mechanismName = it },
                    label = { Text("نام مکانیزم / تابلو *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = mechanismCode,
                    onValueChange = { mechanismCode = it },
                    label = { Text("کد مکانیزم (اختیاری)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = { createFromExtract() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !isCreating && roomName.isNotBlank() && mechanismName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Success)
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("در حال ایجاد...")
                    } else {
                        Icon(Icons.Default.CheckCircle, null)
                        Spacer(Modifier.width(8.dp))
                        Text("تأیید و ایجاد اتاق + مکانیزم", fontSize = 15.sp)
                    }
                }
            }

            // No text found warning
            if (analyzeResult != null && analyzeResult?.hasText == false) {
                Spacer(Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "متن قابل استخراج یافت نشد",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "احتمالاً نقشه اسکن‌شده (تصویری) است. در این حالت باید نام اتاق و تابلو را دستی وارد کنید.",
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// --- Helpers ---

private fun getFileName(context: Context, uri: Uri): String? {
    var name: String? = null
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && idx >= 0) {
            name = cursor.getString(idx)
        }
    }
    return name
}

private fun uriToMultipart(context: Context, uri: Uri, fileName: String): MultipartBody.Part {
    val inputStream = context.contentResolver.openInputStream(uri)
        ?: throw Exception("نمی‌توان فایل را خواند")
    val tempFile = File(context.cacheDir, fileName)
    FileOutputStream(tempFile).use { output ->
        inputStream.copyTo(output)
    }
    inputStream.close()

    val requestBody = tempFile.asRequestBody("application/pdf".toMediaTypeOrNull())
    return MultipartBody.Part.createFormData("pdf", fileName, requestBody)
}

