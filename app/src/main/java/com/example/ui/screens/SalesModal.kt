package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import com.example.data.Member
import com.example.data.Pricing
import com.example.data.User
import com.example.ui.theme.BluePrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesModal(
    currentUser: User,
    branchInventory: List<InventoryItem>,
    masterPrices: Map<String, Pricing>,
    members: List<Member>,
    onDismiss: () -> Unit,
    onSubmitSale: (
        saleType: String,
        selectedItem: InventoryItem?,
        takaranMl: Double,
        jumlahBotol: Int,
        price: Double,
        memberName: String
    ) -> Boolean
) {
    val saleTypes = listOf("Kombinasi (Bibit + Botol)", "Bibit Saja", "Botol Saja")
    var selectedSaleType by remember { mutableStateOf(saleTypes[0]) }
    var selectedItem by remember { mutableStateOf(branchInventory.firstOrNull()) }
    var takaranMlText by remember { mutableStateOf("30") }
    var jumlahBotolText by remember { mutableStateOf("1") }
    var priceText by remember { mutableStateOf("") }
    var selectedMember by remember { mutableStateOf("Umum") }

    var expandedSaleType by remember { mutableStateOf(false) }
    var expandedItem by remember { mutableStateOf(false) }
    var expandedMember by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    // Auto-calculate price when item, takaran, botol, or sale type changes
    LaunchedEffect(selectedItem, takaranMlText, jumlahBotolText, selectedSaleType, masterPrices) {
        val item = selectedItem ?: return@LaunchedEffect
        val ml = takaranMlText.toDoubleOrNull() ?: 0.0
        val botol = jumlahBotolText.toIntOrNull() ?: 0
        val pricing = masterPrices[item.name] ?: Pricing(4000.0, 6000.0)

        val calc = when {
            selectedSaleType.contains("Kombinasi") -> (pricing.price_ml * ml) + (pricing.price_botol * botol)
            selectedSaleType.contains("Bibit") -> pricing.price_ml * ml
            selectedSaleType.contains("Botol") -> pricing.price_botol * botol.toDouble()
            else -> 0.0
        }
        priceText = calc.toLong().toString()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
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
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Kasir Terpadu (${currentUser.branch})",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("close_sales_modal")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                if (validationError != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = validationError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // 1. Tipe Penjualan Dropdown
                Column {
                    Text(
                        text = "Tipe Penjualan",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    ExposedDropdownMenuBox(
                        expanded = expandedSaleType,
                        onExpandedChange = { expandedSaleType = !expandedSaleType }
                    ) {
                        OutlinedTextField(
                            value = selectedSaleType,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSaleType) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(14.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedSaleType,
                            onDismissRequest = { expandedSaleType = false }
                        ) {
                            saleTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        selectedSaleType = type
                                        expandedSaleType = false
                                    }
                                )
                            }
                        }
                    }
                }

                // 2. Pilih Bibit / Parfum Dropdown
                Column {
                    Text(
                        text = "Pilih Bibit / Parfum",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    ExposedDropdownMenuBox(
                        expanded = expandedItem,
                        onExpandedChange = { expandedItem = !expandedItem }
                    ) {
                        val displayText = selectedItem?.let {
                            "${it.name} (Bibit: ${it.stock_ml.toInt()}ml | Botol: ${it.stock_botol}pcs)"
                        } ?: "Tidak ada stok di cabang ini"

                        OutlinedTextField(
                            value = displayText,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedItem) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(14.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedItem,
                            onDismissRequest = { expandedItem = false }
                        ) {
                            branchInventory.forEach { item ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(item.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text(
                                                "Bibit: ${item.stock_ml.toInt()}ml | Botol: ${item.stock_botol}pcs",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedItem = item
                                        expandedItem = false
                                        validationError = null
                                    }
                                )
                            }
                        }
                    }
                }

                // 3. Takaran (ml) & Jumlah Botol (pcs)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Takaran (ml)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = takaranMlText,
                            onValueChange = {
                                takaranMlText = it
                                validationError = null
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(14.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                            modifier = Modifier.fillMaxWidth().testTag("sales_takaran_ml_input")
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Jumlah Botol (pcs)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = jumlahBotolText,
                            onValueChange = {
                                jumlahBotolText = it
                                validationError = null
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(14.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                            modifier = Modifier.fillMaxWidth().testTag("sales_jumlah_botol_input")
                        )
                    }
                }

                // 4. Nominal Harga
                Column {
                    Text(
                        text = "Nominal Harga (Dapat Diubah Manual)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = {
                            priceText = it
                            validationError = null
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = BluePrimary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("sales_nominal_price_input")
                    )
                }

                // 5. Pelanggan Member Dropdown
                Column {
                    Text(
                        text = "Pelanggan Member",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    ExposedDropdownMenuBox(
                        expanded = expandedMember,
                        onExpandedChange = { expandedMember = !expandedMember }
                    ) {
                        OutlinedTextField(
                            value = if (selectedMember == "Umum") "Pelanggan Umum (Tanpa Poin)" else selectedMember,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMember) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(14.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedMember,
                            onDismissRequest = { expandedMember = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Pelanggan Umum (Tanpa Poin)", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    selectedMember = "Umum"
                                    expandedMember = false
                                }
                            )
                            members.forEach { m ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(m.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text("(${m.points} Poin)", fontSize = 11.sp, color = BluePrimary)
                                        }
                                    },
                                    onClick = {
                                        selectedMember = m.name
                                        expandedMember = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Submit Button
                Button(
                    onClick = {
                        val ml = takaranMlText.toDoubleOrNull() ?: 0.0
                        val botol = jumlahBotolText.toIntOrNull() ?: 0
                        val prc = priceText.toDoubleOrNull() ?: 0.0

                        if (selectedItem == null) {
                            validationError = "Pilih item produk terlebih dahulu!"
                            return@Button
                        }
                        if (selectedItem!!.stock_ml < ml) {
                            validationError = "Stok bibit tidak mencukupi! (Tersedia: ${selectedItem!!.stock_ml}ml)"
                            return@Button
                        }
                        if (selectedItem!!.stock_botol < botol) {
                            validationError = "Stok botol tidak mencukupi! (Tersedia: ${selectedItem!!.stock_botol}pcs)"
                            return@Button
                        }

                        val ok = onSubmitSale(
                            selectedSaleType,
                            selectedItem,
                            ml,
                            botol,
                            prc,
                            selectedMember
                        )
                        if (ok) {
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_sale_transaction_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    Text(
                        text = "Proses & Simpan Transaksi",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}
