package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.McKaysRepository
import com.example.data.TransactionItem
import com.example.data.User
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.EmeraldSuccess
import java.text.NumberFormat
import java.util.*

@Composable
fun HistoryTab(
    currentUser: User,
    transactions: List<TransactionItem>,
    onShowToast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isAdmin = currentUser.role == "admin"
    val activeBranch = currentUser.branch

    var filterType by remember { mutableStateOf("daily") } // "daily", "monthly", "yearly"
    var selectedDate by remember { mutableStateOf(McKaysRepository.getTodayDateString()) }
    var selectedMonth by remember { mutableStateOf(McKaysRepository.getTodayDateString().substring(0, 7)) }
    var selectedYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR).toString()) }

    val filteredTx = transactions.filter { tx ->
        if (!isAdmin && tx.branch != activeBranch) return@filter false

        when (filterType) {
            "daily" -> tx.date == selectedDate
            "monthly" -> tx.date.startsWith(selectedMonth)
            "yearly" -> tx.date.startsWith(selectedYear)
            else -> true
        }
    }

    val totalOmzetFiltered = filteredTx.sumOf { it.price }

    val currencyFormatter = remember {
        NumberFormat.getNumberInstance(Locale("id", "ID"))
    }

    val calendar = Calendar.getInstance()
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val mStr = if (month + 1 < 10) "0${month + 1}" else "${month + 1}"
                val dStr = if (dayOfMonth < 10) "0$dayOfMonth" else "$dayOfMonth"
                selectedDate = "$year-$mStr-$dStr"
                selectedMonth = "$year-$mStr"
                selectedYear = year.toString()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun exportCSV() {
        val sb = StringBuilder()
        sb.append("No,Tanggal,ID Transaksi,Cabang,Tipe,Produk,Harga,Member\n")
        filteredTx.forEachIndexed { idx, tx ->
            sb.append("${idx + 1},${tx.date},${tx.id},${tx.branch},${tx.type},\"${tx.item}\",${tx.price.toLong()},\"${tx.member}\"\n")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "Laporan Omset McKays ($filterType)")
            putExtra(Intent.EXTRA_TEXT, sb.toString())
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan Laporan CSV"))
        onShowToast("Laporan CSV siap dibagikan!")
    }

    fun exportPDF() {
        val sb = StringBuilder()
        sb.append("LAPORAN TRANSAKSI MCKAYS\n")
        sb.append("Periode: $filterType ($selectedDate)\n")
        sb.append("Cabang: ${if (isAdmin) "Pusat (Semua Cabang)" else activeBranch}\n")
        sb.append("Total Omset: Rp ${currencyFormatter.format(totalOmzetFiltered)}\n")
        sb.append("Jumlah Transaksi: ${filteredTx.size}\n\n")
        filteredTx.forEachIndexed { idx, tx ->
            sb.append("${idx + 1}. [${tx.id}] ${tx.item} - Rp ${currencyFormatter.format(tx.price)} (${tx.date} ${tx.time} | ${tx.member})\n")
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Laporan Transaksi McKays")
            putExtra(Intent.EXTRA_TEXT, sb.toString())
        }
        context.startActivity(Intent.createChooser(intent, "Cetak / Bagikan Laporan"))
        onShowToast("Membuka dialog cetak laporan...")
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "Riwayat Transaksi",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Filter berdasarkan Kalender Masehi",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Filter Box
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Segmented Filter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "daily" to "Harian",
                            "monthly" to "Bulanan",
                            "yearly" to "Tahunan"
                        ).forEach { (type, label) ->
                            val isSelected = filterType == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) BluePrimary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { filterType = type }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Date / Month / Year Picker
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { datePickerDialog.show() }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = when (filterType) {
                                "daily" -> "Tanggal: $selectedDate"
                                "monthly" -> "Bulan: $selectedMonth"
                                "yearly" -> "Tahun: $selectedYear"
                                else -> selectedDate
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Pilih Tanggal",
                            tint = BluePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Total Omset Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Omset:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Rp ${currencyFormatter.format(totalOmzetFiltered)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = BluePrimary
                        )
                    }

                    // Export Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { exportCSV() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = "CSV", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("CSV", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = { exportPDF() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                        ) {
                            Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = "PDF", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PDF", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Transactions List
        if (filteredTx.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        text = "Tidak ada riwayat transaksi.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            items(filteredTx, key = { it.id }) { tx ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tx.item,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${tx.date} • ${tx.time} • ${tx.branch} • Member: ${tx.member}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "Rp ${currencyFormatter.format(tx.price)}",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = EmeraldSuccess
                        )
                    }
                }
            }
        }
    }
}
