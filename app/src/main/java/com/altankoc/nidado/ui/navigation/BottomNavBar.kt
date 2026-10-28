package com.altankoc.nidado.ui.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.altankoc.nidado.ui.theme.*

@Composable
fun BottomNavBar(navController: NavController) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Calendar,
        null, // Orta buton (CreateList) için boşluk
        BottomNavItem.Courses,
        BottomNavItem.Profile
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .height(85.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Ana Navbar Gövdesi (Glassmorphism)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(32.dp),
                    ambientColor = if (isDark) Color.Black else DeepOcean.copy(alpha = 0.3f),
                    spotColor = if (isDark) Color.Black else DeepOcean.copy(alpha = 0.4f)
                ),
            shape = RoundedCornerShape(32.dp),
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (isDark) {
                                listOf(SurfaceVariantDark.copy(alpha = 0.65f), SurfaceDark.copy(alpha = 0.80f))
                            } else {
                                listOf(DeepOcean.copy(alpha = 0.70f), DeepOceanDark.copy(alpha = 0.85f))
                            }
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                White.copy(alpha = 0.35f),
                                Color.Transparent,
                                White.copy(alpha = 0.1f)
                            )
                        ),
                        shape = RoundedCornerShape(32.dp)
                    ),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (item == null) {
                            Spacer(modifier = Modifier.size(48.dp))
                        } else {
                            val isSelected = currentRoute == item.route
                            NavBarItem(
                                item = item,
                                isSelected = isSelected,
                                isDark = isDark,
                                onClick = {
                                    if (!isSelected) {
                                        navController.navigate(item.route) {
                                            popUpTo(Screen.Home.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Sabitlenmiş Bağımsız Orta Buton
        val isCreateSelected = currentRoute?.startsWith("create_list") == true
        FloatingCreateButton(
            isSelected = isCreateSelected,
            onClick = {
                navController.navigate(Screen.CreateList.createRoute()) {
                    popUpTo(Screen.Home.route) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
    }
}

@Composable
private fun NavBarItem(
    item: BottomNavItem,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    // Bubble Animasyonu
    val bubbleScale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bubble_scale"
    )

    val bubbleAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "bubble_alpha"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.2f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "icon_scale"
    )

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Baloncuk Arka Planı (Tam Daire)
        Box(
            modifier = Modifier
                .size(42.dp)
                .graphicsLayer {
                    scaleX = bubbleScale
                    scaleY = bubbleScale
                    alpha = bubbleAlpha
                }
                .background(
                    color = if (isDark) VibrantTeal.copy(alpha = 0.3f) else White.copy(alpha = 0.25f),
                    shape = CircleShape
                )
        )

        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = if (isSelected) {
                if (isDark) SoftTealDark else White
            } else White.copy(alpha = 0.5f),
            modifier = Modifier
                .size(24.dp)
                .scale(iconScale)
        )
    }
}

@Composable
private fun BoxScope.FloatingCreateButton(
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "fab_scale"
    )

    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .offset(y = (-20).dp)
            .size(60.dp)
            .scale(scale)
            .shadow(
                elevation = 16.dp,
                shape = CircleShape,
                ambientColor = VibrantTeal.copy(alpha = 0.5f),
                spotColor = VibrantTeal.copy(alpha = 0.7f)
            )
            .clip(CircleShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(SoftTeal, VibrantTeal, DeepOcean)
                )
            )
            .border(
                width = 2.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(White.copy(alpha = 0.7f), Color.Transparent)
                ),
                shape = CircleShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = "Oluştur",
            tint = White,
            modifier = Modifier.size(34.dp)
        )
    }
}
