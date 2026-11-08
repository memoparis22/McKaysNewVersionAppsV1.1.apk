package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.*
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseDanger
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTab(
    currentUser: User,
    darkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    appLogo: String?,
    staffAccounts: List<StaffAccount>,
    branchInventory: Map<String, List<InventoryItem>>,
    stockOrders: List<StockOrder>,
    transactions: List<TransactionItem>,
    onUpdateProfilePic: (String?) -> Unit,
    onUpdateAppLogo: (String?) -> Unit,
    onBulkUpload: (String, String) -> Unit,
    onApproveStockOrder: (String, Boolean) -> Unit,
    onAddStaff: (String, String, String) -> Unit,
    onUpdateStaffPassword: (String, String) -> Unit,
    onResetMemberPoints: () -> Unit,
    onLogout: () -> Unit,
    onShowToast: (String) -> Unit,
    onOpenDriveSyncDialog: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isAdmin = currentUser.role == "admin"

    // Photo pickers
    val profilePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onUpdateProfilePic(uri.toString())
        }
    }

    val logoPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onUpdateAppLogo(uri.toString())
        }
    }

    // Bulk upload states
    val branches = remember(branchInventory) {
        listOf("Pusat") + branchInventory.keys.filter { it != "Pusat" }
    }
    var selectedBulkBranch by remember { mutableStateOf("Pusat") }
    var expandedBulkBranch by remember { mutableStateOf(false) }
    var bulkText by remember { mutableStateOf("") }

    // Add staff states
    var newStaffUser by remember { mutableStateOf("") }
    var newStaffPass by remember { mutableStateOf("") }
    var newStaffBranch by remember { mutableStateOf("") }
    var editingStaffId by remember { mutableStateOf<String?>(null) }
    var editStaffPass by remember { mutableStateOf("") }

    // Reset points dialog
    var showResetDialog by remember { mutableStateOf(false) }
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Nol-kan Poin Member", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text("Yakin ingin me-reset (nol-kan) semua poin seluruh member terdaftar?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onResetMemberPoints()
                        showResetDialog = false
                    }
                ) {
                    Text("Nol-kan Poin", color = RoseDanger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    fun exportMonthlyReport(type: String) {
        val monthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val sb = StringBuilder()
        sb.append("No,Tanggal,Cabang,Produk,Harga,Member\n")
        transactions.filter { it.date.startsWith(monthStr) }.forEachIndexed { idx, tx ->
            sb.append("${idx + 1},${tx.date},${tx.branch},\"${tx.item}\",${tx.price.toLong()},\"${tx.member}\"\n")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            this.type = if (type == "excel") "text/csv" else "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Laporan Bulanan McKays ($monthStr)")
            putExtra(Intent.EXTRA_TEXT, sb.toString())
        }
        context.startActivity(Intent.createChooser(intent, "Export Laporan Bulanan"))
        onShowToast("Export ${type.uppercase()} Laporan Bulanan Berhasil!")
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
    ) {
        // Top Title & Dark Mode Toggle
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pengaturan Akun",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onToggleDarkMode,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = if (darkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle Dark Mode",
                        tint = if (darkMode) Color(0xFFFBBF24) else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Profile Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .border(2.dp, BluePrimary, CircleShape)
                            .background(BluePrimary.copy(alpha = 0.2f))
                            .clickable {
                                profilePhotoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!currentUser.profilePic.isNullOrEmpty()) {
                            AsyncImage(
                                model = currentUser.profilePic,
                                contentDescription = "Profile Pic",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = if (currentUser.username.length >= 2) currentUser.username.substring(0, 2).uppercase() else currentUser.username.uppercase(),
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                color = BluePrimary
                            )
                        }

                        // Camera icon overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Upload Foto",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = currentUser.username,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isAdmin) "Admin Pusat" else "Staff Cabang: ${currentUser.branch}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BluePrimary
                        )
                        Text(
                            text = "Ketuk foto profil untuk mengganti",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Google Drive Real-Time Sync Section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenDriveSyncDialog() },
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = BluePrimary.copy(alpha = 0.08f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, BluePrimary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(BluePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "Google Drive",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Sinkronisasi Google Drive",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = GoogleDriveSyncService.TARGET_DRIVE_ACCOUNT,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BluePrimary
                            )
                            Text(
                                text = "Data penjualan & stok tersimpan real-time",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Buka",
                        tint = BluePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Admin Control Hub
        if (isAdmin) {
            item {
                Text(
                    text = "KONTROL MASTER & MANAJEMEN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
            }

            // 1. Change App Logo
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Ubah Logo Aplikasi (Sinkron Semua Akun)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BluePrimary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(BluePrimary)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!appLogo.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = appLogo,
                                        contentDescription = "Logo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    AsyncImage(
                                        model = R.drawable.ic_launcher_foreground,
                                        contentDescription = "Logo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    logoPhotoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Upload Logo Baru", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            // 2. Bulk Upload Products
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Upload List Produk Massal",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BluePrimary
                        )

                        // Cabang selector
                        ExposedDropdownMenuBox(
                            expanded = expandedBulkBranch,
                            onExpandedChange = { expandedBulkBranch = !expandedBulkBranch }
                        ) {
                            OutlinedTextField(
                                value = selectedBulkBranch,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedBulkBranch) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                shape = RoundedCornerShape(12.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            )
                            ExposedDropdownMenu(
                                expanded = expandedBulkBranch,
                                onDismissRequest = { expandedBulkBranch = false }
                            ) {
                                branches.forEach { b ->
                                    DropdownMenuItem(
                                        text = { Text(b, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            selectedBulkBranch = b
                                            expandedBulkBranch = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = bulkText,
                            onValueChange = { bulkText = it },
                            placeholder = {
                                Text(
                                    "Format: NamaProduk, ml, botol\ncth: Vanilla Blue, 100, 30",
                                    fontSize = 11.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            },
                            minLines = 3,
                            maxLines = 5,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 12.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        )

                        Button(
                            onClick = {
                                if (bulkText.isNotBlank()) {
                                    onBulkUpload(selectedBulkBranch, bulkText)
                                    bulkText = ""
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                            modifier = Modifier.fillMaxWidth().height(46.dp)
                        ) {
                            Text("Upload Massal ke Cabang", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }

            // 3. Persetujuan Request Barang Cabang
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Persetujuan Request Barang Cabang",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BluePrimary
                        )

                        if (stockOrders.isEmpty()) {
                            Text("Belum ada order masuk.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            stockOrders.forEach { ord ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text("${ord.branch} - ${ord.item}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("Ml: ${ord.ml.toInt()} | Botol: ${ord.botol}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                ord.status,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when (ord.status) {
                                                    "Disetujui" -> EmeraldSuccess
                                                    "Ditolak" -> RoseDanger
                                                    else -> AmberWarning
                                                }
                                            )
                                        }

                                        if (ord.status == "Menunggu Persetujuan") {
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Button(
                                                    onClick = { onApproveStockOrder(ord.id, true) },
                                                    shape = RoundedCornerShape(10.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Text("Setuju", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                }
                                                Button(
                                                    onClick = { onApproveStockOrder(ord.id, false) },
                                                    shape = RoundedCornerShape(10.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = RoseDanger),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Text("Tolak", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Kelola Toko Cabang & Staff
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Kelola Toko Cabang & Staff",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BluePrimary
                        )

                        OutlinedTextField(
                            value = newStaffUser,
                            onValueChange = { newStaffUser = it },
                            placeholder = { Text("Username Staff", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = newStaffPass,
                            onValueChange = { newStaffPass = it },
                            placeholder = { Text("Password", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = newStaffBranch,
                            onValueChange = { newStaffBranch = it },
                            placeholder = { Text("Nama Cabang (cth: Medan)", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                if (newStaffUser.isNotBlank() && newStaffPass.isNotBlank() && newStaffBranch.isNotBlank()) {
                                    onAddStaff(newStaffUser, newStaffPass, newStaffBranch)
                                    newStaffUser = ""
                                    newStaffPass = ""
                                    newStaffBranch = ""
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                            modifier = Modifier.fillMaxWidth().height(46.dp)
                        ) {
                            Text("Tambah Cabang & Staff", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                        }

                        // List & Update staff passwords
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            text = "Daftar Akun Staff & Ganti Password:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        staffAccounts.forEach { staff ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(staff.username, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                            Text("(${staff.branch})", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        TextButton(
                                            onClick = {
                                                if (editingStaffId == staff.id) {
                                                    editingStaffId = null
                                                } else {
                                                    editingStaffId = staff.id
                                                    editStaffPass = staff.password
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                if (editingStaffId == staff.id) "Batal" else "Ubah Password",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BluePrimary
                                            )
                                        }
                                    }

                                    if (editingStaffId == staff.id) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = editStaffPass,
                                                onValueChange = { editStaffPass = it },
                                                placeholder = { Text("Password baru...", fontSize = 11.sp) },
                                                singleLine = true,
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.weight(1f),
                                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                                            )
                                            Button(
                                                onClick = {
                                                    if (editStaffPass.isNotBlank()) {
                                                        onUpdateStaffPassword(staff.id, editStaffPass)
                                                        editingStaffId = null
                                                        editStaffPass = ""
                                                    }
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                                            ) {
                                                Text("Simpan", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Export Monthly Reports
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Export Laporan Bulanan",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BluePrimary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { exportMonthlyReport("pdf") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                            ) {
                                Text("Export PDF", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Button(
                                onClick = { exportMonthlyReport("excel") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                            ) {
                                Text("Export Excel", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // 6. Reset Points Button
            item {
                Button(
                    onClick = { showResetDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .border(1.dp, RoseDanger.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoseDanger.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Nol-kan Poin Seluruh Member",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoseDanger
                        )
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Points",
                            tint = RoseDanger,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Logout Button
        item {
            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("logout_app_button"),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RoseDanger)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Logout",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "LOGOUT DARI APLIKASI",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
            }
        }

        // Footer
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "McKays",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
