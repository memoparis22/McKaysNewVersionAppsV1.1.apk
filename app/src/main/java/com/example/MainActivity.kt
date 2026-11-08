package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppTab
import com.example.ui.McKaysViewModel
import com.example.ui.components.BottomNavBar
import com.example.ui.components.GoogleDriveSyncDialog
import com.example.ui.components.ToastNotification
import com.example.ui.components.TopHeader
import com.example.ui.screens.*
import com.example.ui.theme.McKaysTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: McKaysViewModel = viewModel()
            val darkMode by viewModel.darkMode.collectAsStateWithLifecycle()
            val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
            val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
            val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
            val priceNotification by viewModel.priceNotification.collectAsStateWithLifecycle()
            val masterPrices by viewModel.masterPrices.collectAsStateWithLifecycle()
            val staffAccounts by viewModel.staffAccounts.collectAsStateWithLifecycle()
            val branchInventory by viewModel.branchInventory.collectAsStateWithLifecycle()
            val stockOrders by viewModel.stockOrders.collectAsStateWithLifecycle()
            val transactions by viewModel.transactions.collectAsStateWithLifecycle()
            val members by viewModel.members.collectAsStateWithLifecycle()
            val appLogo by viewModel.appLogo.collectAsStateWithLifecycle()

            // Google Drive Real-time Sync states
            val isDriveSyncing by viewModel.isDriveSyncing.collectAsStateWithLifecycle()
            val driveLastSyncTime by viewModel.driveLastSyncTime.collectAsStateWithLifecycle()
            val driveAutoSync by viewModel.driveAutoSync.collectAsStateWithLifecycle()
            val driveSyncLogs by viewModel.driveSyncLogs.collectAsStateWithLifecycle()
            val driveWebhookUrl by viewModel.driveWebhookUrl.collectAsStateWithLifecycle()
            val firestoreStatus by viewModel.firestoreSyncStatus.collectAsStateWithLifecycle()

            var isSalesModalOpen by remember { mutableStateOf(false) }
            var isDriveSyncDialogOpen by remember { mutableStateOf(false) }

            // Back handler to return to HOME tab if on a sub tab
            if (currentUser != null && currentTab != AppTab.HOME) {
                BackHandler {
                    viewModel.setCurrentTab(AppTab.HOME)
                }
            }

            McKaysTheme(darkTheme = darkMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    val user = currentUser
                    if (user == null) {
                        LoginScreen(
                            appLogo = appLogo,
                            onLogin = { username, password ->
                                viewModel.login(username, password)
                            },
                            onGoogleLogin = { name, email, photo ->
                                viewModel.loginWithGoogle(name, email, photo)
                            }
                        )
                    } else {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            topBar = {
                                TopHeader(
                                    currentUser = user,
                                    darkMode = darkMode,
                                    onToggleDarkMode = { viewModel.setDarkMode(!darkMode) },
                                    appLogo = appLogo,
                                    priceNotification = priceNotification,
                                    onDismissPriceNotification = { viewModel.dismissPriceNotification() },
                                    isDriveSyncing = isDriveSyncing,
                                    isOnline = viewModel.isOnline(),
                                    onOpenDriveSyncDialog = { isDriveSyncDialogOpen = true },
                                    onLogout = { viewModel.logout() }
                                )
                            },
                            bottomBar = {
                                BottomNavBar(
                                    currentTab = currentTab,
                                    onTabSelected = { viewModel.setCurrentTab(it) }
                                )
                            },
                            contentWindowInsets = WindowInsets.safeDrawing
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                when (currentTab) {
                                    AppTab.HOME -> {
                                        HomeTab(
                                            currentUser = user,
                                            branchInventory = branchInventory,
                                            transactions = transactions,
                                            masterPrices = masterPrices,
                                            members = members,
                                            onOpenSalesModal = { isSalesModalOpen = true }
                                        )
                                    }
                                    AppTab.STOCK -> {
                                        StockTab(
                                            currentUser = user,
                                            branchInventory = branchInventory,
                                            masterPrices = masterPrices,
                                            onUpdateMasterPrice = { name, ml, botol ->
                                                viewModel.updateMasterPrice(name, ml, botol)
                                            },
                                            onDeleteInventoryItem = { branch, id ->
                                                viewModel.deleteInventoryItem(branch, id)
                                            },
                                            onSendStockOrder = { branch, item, ml, botol ->
                                                viewModel.sendStockOrder(branch, item, ml, botol)
                                            }
                                        )
                                    }
                                    AppTab.HISTORY -> {
                                        HistoryTab(
                                            currentUser = user,
                                            transactions = transactions,
                                            onShowToast = { viewModel.showToast(it) }
                                        )
                                    }
                                    AppTab.MEMBER -> {
                                        MemberTab(
                                            currentUser = user,
                                            members = members,
                                            onAddMember = { name, phone, points ->
                                                viewModel.addMember(name, phone, points)
                                            },
                                            onDeleteMember = { viewModel.deleteMember(it) }
                                        )
                                    }
                                    AppTab.PROFILE -> {
                                        ProfileTab(
                                            currentUser = user,
                                            darkMode = darkMode,
                                            onToggleDarkMode = { viewModel.setDarkMode(!darkMode) },
                                            appLogo = appLogo,
                                            staffAccounts = staffAccounts,
                                            branchInventory = branchInventory,
                                            stockOrders = stockOrders,
                                            transactions = transactions,
                                            onUpdateProfilePic = { viewModel.updateProfilePic(it) },
                                            onUpdateAppLogo = { viewModel.updateAppLogo(it) },
                                            onBulkUpload = { branch, text ->
                                                viewModel.bulkUploadProducts(branch, text)
                                            },
                                            onApproveStockOrder = { id, approve ->
                                                viewModel.approveStockOrder(id, approve)
                                            },
                                            onAddStaff = { u, p, b -> viewModel.addStaff(u, p, b) },
                                            onUpdateStaffPassword = { id, p -> viewModel.updateStaffPassword(id, p) },
                                            onResetMemberPoints = { viewModel.resetMemberPoints() },
                                            onLogout = { viewModel.logout() },
                                            onShowToast = { viewModel.showToast(it) },
                                            onOpenDriveSyncDialog = { isDriveSyncDialogOpen = true }
                                        )
                                    }
                                }

                                // Sales Modal / Kasir Dialog
                                if (isSalesModalOpen) {
                                    val activeBranchInventory = branchInventory[user.branch] ?: emptyList()
                                    SalesModal(
                                        currentUser = user,
                                        branchInventory = activeBranchInventory,
                                        masterPrices = masterPrices,
                                        members = members,
                                        onDismiss = { isSalesModalOpen = false },
                                        onSubmitSale = { saleType, selectedItem, takaranMl, jumlahBotol, price, memberName ->
                                            viewModel.processSale(
                                                branch = user.branch,
                                                saleType = saleType,
                                                selectedItem = selectedItem,
                                                takaranMl = takaranMl,
                                                jumlahBotol = jumlahBotol,
                                                price = price,
                                                memberName = memberName
                                            )
                                        }
                                    )
                                }

                                // Google Drive Real-time Sync Dialog
                                if (isDriveSyncDialogOpen) {
                                    GoogleDriveSyncDialog(
                                        isOnline = viewModel.isOnline(),
                                        isSyncing = isDriveSyncing,
                                        lastSyncTime = driveLastSyncTime,
                                        autoSyncEnabled = driveAutoSync,
                                        syncLogs = driveSyncLogs,
                                        webhookUrl = driveWebhookUrl,
                                        onToggleAutoSync = { viewModel.setDriveAutoSync(it) },
                                        onSaveWebhookUrl = { viewModel.setDriveWebhookUrl(it) },
                                        onSyncNow = { viewModel.syncGoogleDriveNow() },
                                        onOpenDriveExport = { viewModel.openGoogleDriveExport() },
                                        firestoreStatus = firestoreStatus,
                                        onSyncFirestore = { viewModel.syncAllToFirestore() },
                                        onDismiss = { isDriveSyncDialogOpen = false }
                                    )
                                }
                            }
                        }
                    }

                    // Floating Toast Notification
                    ToastNotification(
                        message = toastMessage,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 85.dp)
                    )
                }
            }
        }
    }
}
