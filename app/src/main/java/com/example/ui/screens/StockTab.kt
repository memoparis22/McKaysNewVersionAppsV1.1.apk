package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.InventoryItem
import com.example.data.Pricing
import com.example.data.User
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.RoseDanger

@Composable
fun StockTab(
    currentUser: User,
    branchInventory: Map<String, List<InventoryItem>>,
    masterPrices: Map<String, Pricing>,
    onUpdateMasterPrice: (String, Double, Double) -> Unit,
    onDeleteInventoryItem: (String, String) -> Unit,
    onSendStockOrder: (String, String, Double, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isAdmin = currentUser.role == "admin"
    val activeBranch = currentUser.branch
    val rawItems = branchInventory[activeBranch] ?: emptyList()

    var searchTerm by remember { mutableStateOf("") }
    var isOrderModalOpen by remember { mutableStateOf(false) }

    val filteredItems = remember(rawItems, searchTerm) {
        rawItems
            .filter { it.name.contains(searchTerm, ignoreCase = true) }
            .sortedBy { it.name }
    }

    // Delete confirmation dialog
    var itemToDelete by remember { mutableStateOf<InventoryItem?>(null) }
    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Hapus Item", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text("Hapus item produk '${itemToDelete?.name}' ini dari stok?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        itemToDelete?.let { onDeleteInventoryItem(activeBranch, it.id) }
                        itemToDelete = null
                    }
                ) {
                    Text("Hapus", color = RoseDanger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Batal")
                }
            }
        )
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Katalog & Stok ($activeBranch)",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Urut Abjad A-Z",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!isAdmin) {
                    Button(
                        onClick = { isOrderModalOpen = true },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        modifier = Modifier.testTag("open_order_stock_modal_button")
                    ) {
                        Text(
                            text = "Order Stok",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Admin Master Pricing Box
        if (isAdmin) {
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
                            text = "KELOLA HARGA MASTER (PER ML & BOTOL)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = BluePrimary,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Perubahan harga otomatis tersinkron ke semua akun staff cabang.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            masterPrices.forEach { (itemName, pricing) ->
                                MasterPriceItemRow(
                                    itemName = itemName,
                                    pricing = pricing,
                                    onSave = { mlPrice, botolPrice ->
                                        onUpdateMasterPrice(itemName, mlPrice, botolPrice)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchTerm,
                onValueChange = { searchTerm = it },
                placeholder = { Text("Cari produk...", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_stock_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BluePrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }

        // Inventory Items List
        if (filteredItems.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        text = "Belum ada stok.",
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
            items(filteredItems, key = { it.id }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = item.name,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "Bibit: ${item.stock_ml.toInt()}ml",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BluePrimary
                                )
                                Text(
                                    text = "Botol: ${item.stock_botol}pcs",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PurpleAccent
                                )
                            }
                        }

                        if (isAdmin) {
                            IconButton(
                                onClick = { itemToDelete = item },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(RoseDanger.copy(alpha = 0.1f))
                                    .testTag("delete_stock_item_${item.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Hapus",
                                    tint = RoseDanger,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Order Stock Modal for Staff
    if (isOrderModalOpen) {
        OrderStockDialog(
            activeBranch = activeBranch,
            onDismiss = { isOrderModalOpen = false },
            onSubmit = { itemName, ml, botol ->
                onSendStockOrder(activeBranch, itemName, ml, botol)
                isOrderModalOpen = false
            }
        )
    }
}

@Composable
private fun MasterPriceItemRow(
    itemName: String,
    pricing: Pricing,
    onSave: (Double, Double) -> Unit
) {
    var mlText by remember(pricing.price_ml) { mutableStateOf(pricing.price_ml.toLong().toString()) }
    var botolText by remember(pricing.price_botol) { mutableStateOf(pricing.price_botol.toLong().toString()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = itemName,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = mlText,
                onValueChange = {
                    mlText = it
                    val ml = it.toDoubleOrNull() ?: pricing.price_ml
                    val botol = botolText.toDoubleOrNull() ?: pricing.price_botol
                    onSave(ml, botol)
                },
                placeholder = { Text("Harga / ml", fontSize = 11.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold)
            )

            OutlinedTextField(
                value = botolText,
                onValueChange = {
                    botolText = it
                    val ml = mlText.toDoubleOrNull() ?: pricing.price_ml
                    val botol = it.toDoubleOrNull() ?: pricing.price_botol
                    onSave(ml, botol)
                },
                placeholder = { Text("Harga Botol", fontSize = 11.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
private fun OrderStockDialog(
    activeBranch: String,
    onDismiss: () -> Unit,
    onSubmit: (itemName: String, ml: Double, botol: Int) -> Unit
) {
    var itemName by remember { mutableStateOf("") }
    var mlText by remember { mutableStateOf("") }
    var botolText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Form Order Barang ke Admin",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                if (error != null) {
                    Text(
                        text = error ?: "",
                        color = RoseDanger,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column {
                    Text("Cari / Ketik Nama Item", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = itemName,
                        onValueChange = { itemName = it },
                        placeholder = { Text("cth: VC Scandal", fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Jumlah (ml)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = mlText,
                            onValueChange = { mlText = it },
                            placeholder = { Text("0", fontSize = 12.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Jumlah Botol (pcs)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = botolText,
                            onValueChange = { botolText = it },
                            placeholder = { Text("0", fontSize = 12.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Button(
                    onClick = {
                        if (itemName.isBlank()) {
                            error = "Nama item tidak boleh kosong!"
                            return@Button
                        }
                        val ml = mlText.toDoubleOrNull() ?: 0.0
                        val botol = botolText.toIntOrNull() ?: 0
                        onSubmit(itemName.trim(), ml, botol)
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Kirim Order ke Admin", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                }
            }
        }
    }
}
