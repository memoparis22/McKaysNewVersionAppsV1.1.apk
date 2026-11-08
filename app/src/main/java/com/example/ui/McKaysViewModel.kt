package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.util.UUID

class McKaysViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = McKaysRepository(application.applicationContext)
    val driveSyncService = GoogleDriveSyncService(application.applicationContext)
    val firestoreRepository = FirestoreSyncRepository(application.applicationContext)

    val firestoreSyncStatus = firestoreRepository.firestoreSyncStatus
    val isDriveSyncing = driveSyncService.isSyncing
    val driveLastSyncTime = driveSyncService.lastSyncTime
    val driveAutoSync = driveSyncService.autoSyncEnabled
    val driveSyncLogs = driveSyncService.syncLogs
    val driveWebhookUrl = driveSyncService.webhookUrl

    fun isOnline(): Boolean = driveSyncService.isOnline()

    private val _currentUser = MutableStateFlow<User?>(repository.loadCurrentUser())
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _currentTab = MutableStateFlow(AppTab.HOME)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _darkMode = MutableStateFlow(repository.loadDarkMode())
    val darkMode: StateFlow<Boolean> = _darkMode.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _priceNotification = MutableStateFlow<String?>(null)
    val priceNotification: StateFlow<String?> = _priceNotification.asStateFlow()

    private val _masterPrices = MutableStateFlow(repository.loadMasterPrices())
    val masterPrices: StateFlow<Map<String, Pricing>> = _masterPrices.asStateFlow()

    private val _staffAccounts = MutableStateFlow(repository.loadStaffAccounts())
    val staffAccounts: StateFlow<List<StaffAccount>> = _staffAccounts.asStateFlow()

    private val _branchInventory = MutableStateFlow(repository.loadBranchInventory())
    val branchInventory: StateFlow<Map<String, List<InventoryItem>>> = _branchInventory.asStateFlow()

    private val _stockOrders = MutableStateFlow(repository.loadStockOrders())
    val stockOrders: StateFlow<List<StockOrder>> = _stockOrders.asStateFlow()

    private val _transactions = MutableStateFlow(repository.loadTransactions())
    val transactions: StateFlow<List<TransactionItem>> = _transactions.asStateFlow()

    private val _members = MutableStateFlow(repository.loadMembers())
    val members: StateFlow<List<Member>> = _members.asStateFlow()

    private val _appLogo = MutableStateFlow(repository.loadAppLogo())
    val appLogo: StateFlow<String?> = _appLogo.asStateFlow()

    fun showToast(msg: String) {
        viewModelScope.launch {
            _toastMessage.value = msg
            delay(3000)
            if (_toastMessage.value == msg) {
                _toastMessage.value = null
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun dismissPriceNotification() {
        _priceNotification.value = null
    }

    fun setDarkMode(dark: Boolean) {
        _darkMode.value = dark
        repository.saveDarkMode(dark)
    }

    fun setCurrentTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun login(username: String, password: String): Boolean {
        val trimmedUser = username.trim()
        val trimmedPass = password.trim()

        if (trimmedUser == "mckays" && trimmedPass == "bismillah22") {
            val user = User(username = "mckays", role = "admin", branch = "Pusat")
            _currentUser.value = user
            repository.saveCurrentUser(user)
            showToast("Login berhasil sebagai Admin Pusat!")
            return true
        }

        val foundStaff = _staffAccounts.value.find { it.username == trimmedUser && it.password == trimmedPass }
        if (foundStaff != null) {
            val user = User(username = foundStaff.username, role = "staff", branch = foundStaff.branch)
            _currentUser.value = user
            repository.saveCurrentUser(user)
            showToast("Login berhasil sebagai Staff (${foundStaff.branch})!")
            return true
        }

        return false
    }

    fun loginWithGoogle(displayName: String?, email: String?, photoUrl: String?): Boolean {
        val name = displayName?.takeIf { it.isNotBlank() }
            ?: email?.substringBefore("@")?.takeIf { it.isNotBlank() }
            ?: "Google User"
        val user = User(
            username = name,
            role = "admin",
            branch = "Pusat",
            profilePic = photoUrl
        )
        _currentUser.value = user
        repository.saveCurrentUser(user)
        showToast("Login Google berhasil: Selamat datang, $name!")
        return true
    }

    fun logout() {
        _currentUser.value = null
        repository.saveCurrentUser(null)
        try {
            com.google.firebase.Firebase.auth.signOut()
        } catch (e: Exception) {
            // Ignore if Firebase auth is not configured or offline
        }
        showToast("Anda telah keluar dari aplikasi.")
    }

    fun updateProfilePic(uri: String?) {
        val current = _currentUser.value ?: return
        val updated = current.copy(profilePic = uri)
        _currentUser.value = updated
        repository.saveCurrentUser(updated)
        showToast("Foto profil diperbarui!")
    }

    fun updateAppLogo(uri: String?) {
        _appLogo.value = uri
        repository.saveAppLogo(uri)
        showToast("Logo aplikasi berhasil diperbarui & disinkronkan!")
    }

    fun updateMasterPrice(itemName: String, priceMl: Double, priceBotol: Double) {
        val updated = _masterPrices.value.toMutableMap()
        updated[itemName] = Pricing(price_ml = priceMl, price_botol = priceBotol)
        _masterPrices.value = updated
        repository.saveMasterPrices(updated)
        _priceNotification.value = "Harga $itemName telah diperbarui oleh Admin!"
        showToast("Harga Master berhasil diperbarui & disinkronkan!")
    }

    fun deleteInventoryItem(branch: String, itemId: String) {
        val currentMap = _branchInventory.value.toMutableMap()
        val currentList = currentMap[branch]?.toMutableList() ?: mutableListOf()
        currentList.removeAll { it.id == itemId }
        currentMap[branch] = currentList
        _branchInventory.value = currentMap
        repository.saveBranchInventory(currentMap)
        showToast("Item berhasil dihapus.")
    }

    fun sendStockOrder(branch: String, itemName: String, ml: Double, botol: Int) {
        val newOrder = StockOrder(
            id = "ORD-${(100..999).random()}",
            branch = branch,
            item = itemName.trim(),
            ml = ml,
            botol = botol,
            status = "Menunggu Persetujuan"
        )
        val updated = _stockOrders.value + newOrder
        _stockOrders.value = updated
        repository.saveStockOrders(updated)
        showToast("Order berhasil dikirim ke Admin Pusat!")
    }

    fun approveStockOrder(orderId: String, approve: Boolean) {
        val targetBranch = _stockOrders.value.find { it.id == orderId }?.branch
        val currentOrders = _stockOrders.value.map { ord ->
            if (ord.id == orderId) {
                if (approve) {
                    val branch = ord.branch
                    val currentMap = _branchInventory.value.toMutableMap()
                    val branchList = (currentMap[branch] ?: emptyList()).toMutableList()
                    val existingIndex = branchList.indexOfFirst { it.name.equals(ord.item, ignoreCase = true) }

                    if (existingIndex >= 0) {
                        val existing = branchList[existingIndex]
                        branchList[existingIndex] = existing.copy(
                            stock_ml = existing.stock_ml + ord.ml,
                            stock_botol = existing.stock_botol + ord.botol
                        )
                    } else {
                        branchList.add(
                            InventoryItem(
                                id = UUID.randomUUID().toString(),
                                name = ord.item,
                                stock_ml = ord.ml,
                                stock_botol = ord.botol
                            )
                        )
                    }
                    currentMap[branch] = branchList
                    _branchInventory.value = currentMap
                    repository.saveBranchInventory(currentMap)
                }
                ord.copy(status = if (approve) "Disetujui" else "Ditolak")
            } else {
                ord
            }
        }
        _stockOrders.value = currentOrders
        repository.saveStockOrders(currentOrders)
        showToast(if (approve) "Order disetujui & stok masuk cabang!" else "Order ditolak.")

        if (approve && driveSyncService.autoSyncEnabled.value) {
            driveSyncService.syncDataRealTime(
                reason = "Persetujuan Stok Cabang",
                transactions = _transactions.value,
                inventory = _branchInventory.value
            )
        }
        if (approve && targetBranch != null) {
            viewModelScope.launch {
                _branchInventory.value[targetBranch]?.let { items ->
                    firestoreRepository.syncBranchStock(targetBranch, items)
                }
            }
        }
    }

    fun bulkUploadProducts(branch: String, text: String) {
        val lines = text.lines()
        var addedCount = 0
        val currentMap = _branchInventory.value.toMutableMap()
        val currentList = (currentMap[branch] ?: emptyList()).toMutableList()

        for (line in lines) {
            val parts = line.split(",").map { it.trim() }
            if (parts.isNotEmpty() && parts[0].isNotEmpty()) {
                val name = parts[0]
                val ml = parts.getOrNull(1)?.toDoubleOrNull() ?: 50.0
                val botol = parts.getOrNull(2)?.toIntOrNull() ?: 20

                val existingIndex = currentList.indexOfFirst { it.name.equals(name, ignoreCase = true) }
                if (existingIndex >= 0) {
                    val existing = currentList[existingIndex]
                    currentList[existingIndex] = existing.copy(
                        stock_ml = existing.stock_ml + ml,
                        stock_botol = existing.stock_botol + botol
                    )
                } else {
                    currentList.add(
                        InventoryItem(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            stock_ml = ml,
                            stock_botol = botol
                        )
                    )
                }
                addedCount++
            }
        }

        currentMap[branch] = currentList
        _branchInventory.value = currentMap
        repository.saveBranchInventory(currentMap)
        showToast("Berhasil upload $addedCount produk ke $branch!")

        if (driveSyncService.autoSyncEnabled.value) {
            driveSyncService.syncDataRealTime(
                reason = "Upload Massal Stok ke $branch",
                transactions = _transactions.value,
                inventory = currentMap
            )
        }
        viewModelScope.launch {
            firestoreRepository.syncBranchStock(branch, currentList)
        }
    }

    fun addStaff(username: String, pass: String, branch: String) {
        val newStaff = StaffAccount(
            id = UUID.randomUUID().toString(),
            username = username.trim(),
            password = pass.trim(),
            branch = branch.trim()
        )
        val updatedAccounts = _staffAccounts.value + newStaff
        _staffAccounts.value = updatedAccounts
        repository.saveStaffAccounts(updatedAccounts)

        val currentMap = _branchInventory.value.toMutableMap()
        if (!currentMap.containsKey(branch.trim())) {
            currentMap[branch.trim()] = emptyList()
            _branchInventory.value = currentMap
            repository.saveBranchInventory(currentMap)
        }
        showToast("Cabang & Staff baru berhasil ditambahkan!")
    }

    fun updateStaffPassword(staffId: String, newPass: String) {
        val updated = _staffAccounts.value.map {
            if (it.id == staffId) it.copy(password = newPass.trim()) else it
        }
        _staffAccounts.value = updated
        repository.saveStaffAccounts(updated)
        showToast("Password staff berhasil diperbarui!")
    }

    fun addMember(name: String, phone: String, initialPoints: Int) {
        val newMem = Member(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            phone = if (phone.isBlank()) "-" else phone.trim(),
            points = initialPoints
        )
        val updated = _members.value + newMem
        _members.value = updated
        repository.saveMembers(updated)
        showToast("Member baru ditambahkan dengan poin!")
    }

    fun deleteMember(memberId: String) {
        val updated = _members.value.filter { it.id != memberId }
        _members.value = updated
        repository.saveMembers(updated)
        showToast("Member berhasil dihapus.")
    }

    fun resetMemberPoints() {
        val updated = _members.value.map { it.copy(points = 0) }
        _members.value = updated
        repository.saveMembers(updated)
        showToast("Semua poin member dinolkan.")
    }

    fun processSale(
        branch: String,
        saleType: String,
        selectedItem: InventoryItem?,
        takaranMl: Double,
        jumlahBotol: Int,
        price: Double,
        memberName: String
    ): Boolean {
        if (selectedItem != null) {
            if (selectedItem.stock_ml < takaranMl) {
                showToast("Stok bibit tidak mencukupi!")
                return false
            }
            if (selectedItem.stock_botol < jumlahBotol) {
                showToast("Stok botol tidak mencukupi!")
                return false
            }

            // Deduct stock
            val currentMap = _branchInventory.value.toMutableMap()
            val list = (currentMap[branch] ?: emptyList()).map { item ->
                if (item.id == selectedItem.id) {
                    item.copy(
                        stock_ml = item.stock_ml - takaranMl,
                        stock_botol = item.stock_botol - jumlahBotol
                    )
                } else {
                    item
                }
            }
            currentMap[branch] = list
            _branchInventory.value = currentMap
            repository.saveBranchInventory(currentMap)
        }

        val pointsEarned = (price / 10000.0).toInt()
        val newTx = TransactionItem(
            id = "TRX-${(1000..9999).random()}",
            branch = branch,
            type = saleType,
            item = if (selectedItem != null) "${selectedItem.name} (${takaranMl.toInt()}ml)" else "Penjualan",
            detail = "$saleType | ${takaranMl.toInt()}ml | ${jumlahBotol}pcs",
            price = price,
            member = memberName,
            pointsEarned = pointsEarned,
            time = McKaysRepository.getCurrentTimeString(),
            date = McKaysRepository.getTodayDateString()
        )

        val updatedTxs = listOf(newTx) + _transactions.value
        _transactions.value = updatedTxs
        repository.saveTransactions(updatedTxs)

        // If member != "Umum", update member points
        if (memberName != "Umum" && pointsEarned > 0) {
            val updatedMembers = _members.value.map { m ->
                if (m.name.equals(memberName, ignoreCase = true)) {
                    m.copy(points = m.points + pointsEarned)
                } else {
                    m
                }
            }
            _members.value = updatedMembers
            repository.saveMembers(updatedMembers)
        }

        showToast("Transaksi berhasil disimpan!")

        // Real-time Cloud Sync to Google Drive (budayaxwel@gmail.com)
        if (driveSyncService.autoSyncEnabled.value) {
            driveSyncService.syncDataRealTime(
                reason = "Input Penjualan ${newTx.item} di $branch",
                transactions = updatedTxs,
                inventory = _branchInventory.value
            )
        }

        // Real-time Cloud Sync to Firebase Firestore
        viewModelScope.launch {
            firestoreRepository.syncSale(newTx)
            _branchInventory.value[branch]?.let { items ->
                firestoreRepository.syncBranchStock(branch, items)
            }
        }
        return true
    }

    fun syncAllToFirestore(onDone: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            val result = firestoreRepository.syncAllData(_transactions.value, _branchInventory.value)
            val success = result.isSuccess
            val msg = if (success) "Seluruh data penjualan & stok tersinkron ke Firestore!" else "Gagal sinkron Firestore: ${result.exceptionOrNull()?.message}"
            showToast(msg)
            onDone?.invoke(success, msg)
        }
    }

    fun syncGoogleDriveNow(onDone: ((Boolean, String) -> Unit)? = null) {
        driveSyncService.syncDataRealTime(
            reason = "Sinkronisasi Manual Pengguna",
            transactions = _transactions.value,
            inventory = _branchInventory.value,
            onComplete = { success, msg ->
                showToast(msg)
                onDone?.invoke(success, msg)
            }
        )
    }

    fun openGoogleDriveExport() {
        driveSyncService.openGoogleDriveExport(_transactions.value, _branchInventory.value)
    }

    fun setDriveAutoSync(enabled: Boolean) {
        driveSyncService.setAutoSync(enabled)
        showToast(if (enabled) "Sinkronisasi otomatis Google Drive diaktifkan" else "Sinkronisasi otomatis dinonaktifkan")
    }

    fun setDriveWebhookUrl(url: String) {
        driveSyncService.setWebhookUrl(url)
        showToast("Pengaturan Webhook Drive disimpan")
    }
}
