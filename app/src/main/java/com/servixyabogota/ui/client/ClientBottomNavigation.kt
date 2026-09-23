package com.servixyabogota.ui.client

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ClientBottomNavigation(
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        val navItems = listOf(
            Triple("Inicio", Icons.Outlined.Home, Icons.Filled.Home),
            Triple("Solicitudes", Icons.Outlined.Assignment, Icons.Filled.Assignment),
            Triple("Propuestas", Icons.Outlined.ChatBubbleOutline, Icons.Filled.ChatBubble), // o "Mensajes"
            Triple("Ajustes", Icons.Outlined.Settings, Icons.Filled.Settings)
        )

        navItems.forEach { (title, unselectedIcon, selectedIcon) ->
            val isSelected = selectedTab == title
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(title) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) selectedIcon else unselectedIcon,
                        contentDescription = title
                    )
                },
                label = {
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF2563EB),
                    selectedTextColor = Color(0xFF2563EB),
                    unselectedIconColor = Color(0xFF94A3B8),
                    unselectedTextColor = Color(0xFF94A3B8),
                    indicatorColor = Color(0xFFEFF6FF)
                )
            )
        }
    }
}