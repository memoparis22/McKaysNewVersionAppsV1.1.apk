package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppTab
import com.example.ui.theme.BluePrimary

@Composable
fun BottomNavBar(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(16.dp, shape = RoundedCornerShape(28.dp), spotColor = Color.Black.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavBarItem(
                tab = AppTab.HOME,
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                label = "Home",
                isActive = currentTab == AppTab.HOME,
                onClick = { onTabSelected(AppTab.HOME) }
            )
            NavBarItem(
                tab = AppTab.STOCK,
                icon = Icons.Default.Inventory2,
                label = "Stok",
                isActive = currentTab == AppTab.STOCK,
                onClick = { onTabSelected(AppTab.STOCK) }
            )
            NavBarItem(
                tab = AppTab.HISTORY,
                icon = Icons.Default.ReceiptLong,
                label = "Riwayat",
                isActive = currentTab == AppTab.HISTORY,
                onClick = { onTabSelected(AppTab.HISTORY) }
            )
            NavBarItem(
                tab = AppTab.MEMBER,
                icon = Icons.Default.People,
                label = "Member",
                isActive = currentTab == AppTab.MEMBER,
                onClick = { onTabSelected(AppTab.MEMBER) }
            )
            NavBarItem(
                tab = AppTab.PROFILE,
                icon = Icons.Default.Settings,
                label = "Akun",
                isActive = currentTab == AppTab.PROFILE,
                onClick = { onTabSelected(AppTab.PROFILE) }
            )
        }
    }
}

@Composable
private fun NavBarItem(
    tab: AppTab,
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isActive) BluePrimary else Color.Transparent,
        label = "nav_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "nav_content"
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("nav_item_${tab.name.lowercase()}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = contentColor
        )
    }
}
