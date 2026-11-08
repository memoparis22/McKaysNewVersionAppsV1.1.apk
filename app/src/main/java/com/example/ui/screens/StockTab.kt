package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.RoseDanger

enum class StockCatalogFilter {
    ALL,
    BIBIT,
    BOTOL
}

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
    val staffBranch = currentUser.branch

    // For Admin: allow branch switching; For Staff: locked strictly to staffBranch
    var selectedBranch by remember(staffBranch, isAdmin) {
        mutableStateOf(if (isAdmin) "Pusat" else staffBranch)
    }

    // Active items for the displayed branch
    val branchItems = branchInventory[selectedBranch] ?: emptyList()

    var catalogFilter by remember { mutableStateOf(StockCatalogFilter.ALL) }
    var searchTerm by remember { mutableStateOf("") }
    var isOrderModalOpen by remember { mutableStateOf(false) }

    // Filter items by search and catalog type
    val displayedItems = remember(branchItems, searchTerm, catalogFilter) {
        branchItems
            .filter { item ->
                val matchesSearch = item.name.contains(searchTerm, ignoreCase = true)
                val matchesCatalog = when (catalogFilter) {
                    StockCatalogFilter.ALL -> true
                    StockCatalogFilter.BIBIT -> item.stock_ml > 0 || !item.name.contains("botol", ignoreCase = true)
                    StockCatalogFilter.BOTOL -> item.stock_botol > 0 || item.name.contains("botol", ignoreCase = true)
                }
                matchesSearch && matchesCatalog
            }
            .sortedBy { it.name }
    }

    // Calculate total remaining stock for the branch
    val totalBibitMl = remember(branchItems) { branchItems.sumOf { it.stock_ml } }
    val totalBotolPcs = remember(branchItems) { branchItems.sumOf { it.stock_botol } }

    // Delete confirmation dialog
    var itemToDelete by remember { mutableStateOf<InventoryItem?>(null) }
    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Hapus Item", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text("Hapus item produk '${itemToDelete?.name}' dari stok cabang $selectedBranch?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        itemToDelete?.let { onDeleteInventoryItem(selectedBranch, it.id) }
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
        // Header with branch title & order button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Katalog & Sisa Stok",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(BluePrimary.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = selectedBranch,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BluePrimary
                            )
                        }
                    }
                    Text(
                        text = if (isAdmin) "Tampilan Stok Multi-Cabang (Admin)" else "Stok khusus akun Staff Cabang $staffBranch",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!isAdmin) {
                    Button(
                        onClick = { isOrderModalOpen = true },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        modifier = Modifier.testTag("open_order_stock_modal_button")
                    ) {
                        Icon(imageVector = Icons.Default.AddShoppingCart, contentDescription = "Order", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
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

        // Admin Branch Selector Chips (if Admin)
        if (isAdmin) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "PILIH CABANG YANG DILIHAT:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )
                    val availableBranches = listOf("Pusat", "Bandung", "Surabaya")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(availableBranches) { branch ->
                            val isSelected = selectedBranch == branch
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedBranch = branch },
                                label = {
                                    Text(
                                        text = "Cabang $branch",
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                },
                                leadingIcon = if (isSelected) {
                                    { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null,
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BluePrimary.copy(alpha = 0.15f),
                                    selectedLabelColor = BluePrimary
                                )
                            )
                        }
                    }
                }
            }
        }

        // Summary Metric Cards: Total Sisa Bibit (ml) & Total Sisa Botol (pcs)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Bibit Summary Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = BluePrimary.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, BluePrimary.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = "Bibit",
                                tint = BluePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Sisa Stok Bibit",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BluePrimary
                            )
                        }
                        Text(
                            text = "${totalBibitMl.toInt()} ml",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tersedia di $selectedBranch",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Botol Summary Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = PurpleAccent.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = "Botol",
                                tint = PurpleAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Sisa Stok Botol",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PurpleAccent
                            )
                        }
                        Text(
                            text = "$totalBotolPcs pcs",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tersedia di $selectedBranch",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Catalog Category Tabs: Semua | Bibit (ml) | Botol (pcs)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CatalogTabButton(
                        text = "Semua Stok",
                        icon = Icons.Default.Category,
                        selected = catalogFilter == StockCatalogFilter.ALL,
                        onClick = { catalogFilter = StockCatalogFilter.ALL },
                        modifier = Modifier.weight(1f)
                    )
                    CatalogTabButton(
                        text = "Katalog Bibit (ml)",
                        icon = Icons.Default.Science,
                        selected = catalogFilter == StockCatalogFilter.BIBIT,
                        onClick = { catalogFilter = StockCatalogFilter.BIBIT },
                        modifier = Modifier.weight(1.15f)
                    )
                    CatalogTabButton(
                        text = "Katalog Botol (pcs)",
                        icon = Icons.Default.Inventory2,
                        selected = catalogFilter == StockCatalogFilter.BOTOL,
                        onClick = { catalogFilter = StockCatalogFilter.BOTOL },
                        modifier = Modifier.weight(1.15f)
                    )
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchTerm,
                onValueChange = { searchTerm = it },
                placeholder = {
                    Text(
                        text = when (catalogFilter) {
                            StockCatalogFilter.ALL -> "Cari bibit atau botol..."
                            StockCatalogFilter.BIBIT -> "Cari aroma bibit parfum..."
                            StockCatalogFilter.BOTOL -> "Cari stok botol packaging..."
                        },
                        fontSize = 12.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchTerm.isNotEmpty()) {
                        IconButton(onClick = { searchTerm = "" }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_stock_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BluePrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }

        // Admin Master Pricing Box (if Admin)
        if (isAdmin) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
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

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

        // Stock Items List
        if (displayedItems.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory,
                            contentDescription = "Empty",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Belum ada item untuk kategori ini di cabang $selectedBranch.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(displayedItems, key = { it.id }) { item ->
                StockItemCard(
                    item = item,
                    catalogFilter = catalogFilter,
                    isAdmin = isAdmin,
                    onDelete = { itemToDelete = item }
                )
            }
        }
    }

    // Order Stock Modal for Staff
    if (isOrderModalOpen) {
        OrderStockDialog(
            activeBranch = staffBranch,
            onDismiss = { isOrderModalOpen = false },
            onSubmit = { itemName, ml, botol ->
                onSendStockOrder(staffBranch, itemName, ml, botol)
                isOrderModalOpen = false
            }
        )
    }
}

@Composable
private fun CatalogTabButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) BluePrimary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StockItemCard(
    item: InventoryItem,
    catalogFilter: StockCatalogFilter,
    isAdmin: Boolean,
    onDelete: () -> Unit
) {
    val ml = item.stock_ml
    val botol = item.stock_botol

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Item Name
                Text(
                    text = item.name,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Detailed stock based on selected filter
                when (catalogFilter) {
                    StockCatalogFilter.BIBIT -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Sisa Stok Bibit: ${ml.toInt()} ml",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = BluePrimary
                            )
                            StockStatusBadge(
                                status = when {
                                    ml <= 0 -> "Habis"
                                    ml <= 20 -> "Menipis"
                                    else -> "Tersedia"
                                },
                                color = when {
                                    ml <= 0 -> RoseDanger
                                    ml <= 20 -> AmberWarning
                                    else -> EmeraldSuccess
                                }
                            )
                        }
                    }
                    StockCatalogFilter.BOTOL -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Sisa Stok Botol: $botol pcs",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = PurpleAccent
                            )
                            StockStatusBadge(
                                status = when {
                                    botol <= 0 -> "Habis"
                                    botol <= 10 -> "Menipis"
                                    else -> "Tersedia"
                                },
                                color = when {
                                    botol <= 0 -> RoseDanger
                                    botol <= 10 -> AmberWarning
                                    else -> EmeraldSuccess
                                }
                            )
                        }
                    }
                    StockCatalogFilter.ALL -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Bibit:",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${ml.toInt()} ml",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BluePrimary
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Botol:",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$botol pcs",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PurpleAccent
                                )
                            }
                        }
                    }
                }
            }

            if (isAdmin) {
                IconButton(
                    onClick = onDelete,
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

@Composable
private fun StockStatusBadge(status: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = status,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color
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
    var orderType by remember { mutableStateOf(StockCatalogFilter.ALL) }
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
                    Column {
                        Text(
                            text = "Form Order Barang ke Admin",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Cabang: $activeBranch",
                            fontSize = 11.sp,
                            color = BluePrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
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
                    Text("Nama Item / Varian Parfum", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text("Jumlah Bibit (ml)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
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
                        Text("Jumlah Botol (pcs)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PurpleAccent)
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
                        if (ml <= 0.0 && botol <= 0) {
                            error = "Harap masukkan jumlah bibit atau botol yang diorder!"
                            return@Button
                        }
                        onSubmit(itemName.trim(), ml, botol)
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_stock_order_button")
                ) {
                    Text("Kirim Order ke Admin", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                }
            }
        }
    }
}
