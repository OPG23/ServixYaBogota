package com.servixyabogota.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AdminBottomNavigation(
    selectedTab: String, // "Inicio" o "Usuarios"
    onInicioClick: () -> Unit,
    onUsuariosClick: () -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp,
        modifier = Modifier.height(65.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pestaña Inicio
            NavigationBarItem(
                selected = selectedTab == "Inicio",
                onClick = onInicioClick,
                icon = { Icon(Icons.Default.GridView, contentDescription = "Inicio") },
                label = { Text("Inicio", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF1976D2),
                    selectedTextColor = Color(0xFF1976D2),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color(0xFFE3F2FD)
                )
            )

            // Pestaña Usuarios
            NavigationBarItem(
                selected = selectedTab == "Usuarios",
                onClick = onUsuariosClick,
                icon = { Icon(Icons.Default.Group, contentDescription = "Usuarios") },
                label = { Text("Usuarios", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF1976D2),
                    selectedTextColor = Color(0xFF1976D2),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color(0xFFE3F2FD)
                )
            )
        }
    }
}