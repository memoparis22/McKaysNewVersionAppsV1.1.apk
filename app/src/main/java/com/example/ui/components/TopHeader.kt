package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.User
import androidx.compose.material.icons.filled.CloudSync
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseDanger

import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.runtime.*

@Composable
fun TopHeader(
    currentUser: User,
    darkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    appLogo: String?,
    priceNotification: String?,
    onDismissPriceNotification: () -> Unit,
    isDriveSyncing: Boolean = false,
    isOnline: Boolean = true,
    onOpenDriveSyncDialog: () -> Unit,
    onLogout: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Logo & Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(BluePrimary),
                    contentAlignment = Alignment.Center
                ) {
                    if (!appLogo.isNullOrEmpty()) {
                        AsyncImage(
                            model = appLogo,
                            contentDescription = "App Logo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Fallback generated drawable logo
                        AsyncImage(
                            model = R.drawable.ic_launcher_foreground,
                            contentDescription = "McKays Logo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Column {
                    Text(
                        text = "McKays",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (currentUser.role == "admin") "Admin Pusat" else "Cabang: ${currentUser.branch}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Dark Mode Toggle & User Avatar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Google Drive Cloud Sync Button
                IconButton(
                    onClick = onOpenDriveSyncDialog,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .testTag("open_drive_sync_button")
                ) {
                    if (isDriveSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = BluePrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        BadgedBox(
                            badge = {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (isOnline) EmeraldSuccess else RoseDanger)
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "Google Drive Sync (budayaxwel@gmail.com)",
                                tint = BluePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onToggleDarkMode,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .testTag("toggle_dark_mode_button")
                ) {
                    Icon(
                        imageVector = if (darkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle Theme",
                        tint = if (darkMode) Color(0xFFFBBF24) else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, BluePrimary.copy(alpha = 0.5f), CircleShape)
                        .background(BluePrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!currentUser.profilePic.isNullOrEmpty()) {
                        AsyncImage(
                            model = currentUser.profilePic,
                            contentDescription = "Profile",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        val initials = if (currentUser.username.length >= 2) {
                            currentUser.username.substring(0, 2).uppercase()
                        } else {
                            currentUser.username.uppercase()
                        }
                        Text(
                            text = initials,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BluePrimary
                        )
                    }
                }

                if (onLogout != null) {
                    IconButton(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(RoseDanger.copy(alpha = 0.1f))
                            .testTag("top_header_logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Keluar / Logout",
                            tint = RoseDanger,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        if (showLogoutDialog && onLogout != null) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = {
                    Text(
                        text = "Keluar dari Akun?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                },
                text = {
                    Text(
                        text = "Data omset penjualan dan sisa stok tersimpan dengan aman di perangkat & cloud. Anda dapat masuk kembali kapan saja.",
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showLogoutDialog = false
                            onLogout()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoseDanger),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Keluar", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Batal", fontWeight = FontWeight.SemiBold)
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }

        // Price Update Notification for Staff
        if (priceNotification != null && currentUser.role != "admin") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BluePrimary)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = priceNotification,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = Color.White,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onDismissPriceNotification() }
                        .testTag("dismiss_price_notification")
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), thickness = 1.dp)
    }
}
