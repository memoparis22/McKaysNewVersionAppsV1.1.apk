package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.BlueDark
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.IndigoDark
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.RoseDanger
import java.text.NumberFormat
import java.util.*

@Composable
fun HomeTab(
    currentUser: User,
    branchInventory: Map<String, List<InventoryItem>>,
    transactions: List<TransactionItem>,
    masterPrices: Map<String, Pricing>,
    members: List<Member>,
    onOpenSalesModal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isAdmin = currentUser.role == "admin"
    val activeBranch = currentUser.branch

    var selectedDate by remember { mutableStateOf(McKaysRepository.getTodayDateString()) }

    // Filter transactions for omset
    val dateTx = transactions.filter { tx ->
        tx.date == selectedDate && (isAdmin || tx.branch == activeBranch)
    }
    val totalOmzet = dateTx.sumOf { it.price }

    val rawItems = branchInventory[if (isAdmin) "Pusat" else activeBranch] ?: emptyList()
    val lowStockItems = rawItems.filter { it.stock_ml < 10.0 || it.stock_botol < 10 }.take(4)

    val bestSellerNames = listOf("VC Scandal", "PL Romantichwish", "Pink Shiffon", "ES Cherry", "Air ZR Nude")
    val activeBestSellers = rawItems.filter { item ->
        bestSellerNames.any { bs -> item.name.contains(bs, ignoreCase = true) }
    }

    val currencyFormatter = remember {
        NumberFormat.getNumberInstance(Locale("id", "ID"))
    }

    // DatePicker Dialog helper
    val calendar = Calendar.getInstance()
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val mStr = if (month + 1 < 10) "0${month + 1}" else "${month + 1}"
                val dStr = if (dayOfMonth < 10) "0$dayOfMonth" else "$dayOfMonth"
                selectedDate = "$year-$mStr-$dStr"
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
    ) {
        // 1. Omzet Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, shape = RoundedCornerShape(24.dp), spotColor = BluePrimary.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(BluePrimary, Color(0xFF1E40AF), IndigoDark)
                            )
                        )
                        .padding(22.dp)
                ) {
                    // Decorative icon in background
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.12f),
                        modifier = Modifier
                            .size(110.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = 16.dp, y = 16.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column {
                            Text(
                                text = "TOTAL OMSET (${if (isAdmin) "Pusat" else activeBranch})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFBFDBFE),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Rp ${currencyFormatter.format(totalOmzet)}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }

                        // Date Selector Pill
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.18f))
                                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                                .clickable { datePickerDialog.show() }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                                .testTag("select_omzet_date_button"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Calendar",
                                tint = Color(0xFFBFDBFE),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = selectedDate,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 2. Action Button: Buka Kasir Penjualan
        item {
            Button(
                onClick = onOpenSalesModal,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .shadow(12.dp, shape = RoundedCornerShape(18.dp), spotColor = BluePrimary.copy(alpha = 0.4f))
                    .testTag("open_sales_cashier_button"),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "Kasir",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Buka Kasir Penjualan",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }
        }

        // 3. Best Seller Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "BEST SELLER",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )

                if (activeBestSellers.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Text(
                            text = "Belum ada item best seller di stok toko.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        activeBestSellers.take(2).forEach { item ->
                            BestSellerCard(item = item, modifier = Modifier.weight(1f))
                        }
                    }
                    if (activeBestSellers.size > 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            activeBestSellers.drop(2).take(2).forEach { item ->
                                BestSellerCard(item = item, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // 4. Low Stock Warning Section (< 10)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = "Warning",
                        tint = RoseDanger,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "STOK MENIPIS (< 10)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = RoseDanger,
                        letterSpacing = 0.5.sp
                    )
                }

                if (lowStockItems.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Text(
                            text = "Semua stok aman.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    lowStockItems.forEach { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (item.stock_ml < 10.0) "${item.stock_ml.toInt()}ml" else "${item.stock_botol}pcs",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = RoseDanger
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BestSellerCard(
    item: InventoryItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = item.name,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${item.stock_ml.toInt()}ml",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary
                )
                Text(
                    text = "${item.stock_botol}pcs",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurpleAccent
                )
            }
        }
    }
}
