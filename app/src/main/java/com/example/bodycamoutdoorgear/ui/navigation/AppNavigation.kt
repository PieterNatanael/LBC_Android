package com.example.bodycamoutdoorgear.ui.navigation

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.bodycamoutdoorgear.manager.SubscriptionManager
import com.example.bodycamoutdoorgear.ui.camera.CameraScreen
import com.example.bodycamoutdoorgear.ui.camera.PhotoScreen
import com.example.bodycamoutdoorgear.ui.gallery.GalleryScreen
import com.example.bodycamoutdoorgear.ui.theme.CompassGold
import com.example.bodycamoutdoorgear.ui.theme.SurfaceDark
import com.example.bodycamoutdoorgear.ui.theme.TextSecondary
import com.example.bodycamoutdoorgear.viewmodel.CameraViewModel
import com.example.bodycamoutdoorgear.viewmodel.PhotoViewModel

private sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Video   : Screen("video",   "VIDEO",   Icons.Default.Videocam)
    object Photo   : Screen("photo",   "PHOTO",   Icons.Default.CameraAlt)
    object Gallery : Screen("gallery", "GALLERY", Icons.Default.VideoLibrary)
}

@Composable
fun AppNavigation(subscriptionManager: SubscriptionManager, onShowIntro: () -> Unit = {}) {
    val navController    = rememberNavController()
    val cameraViewModel: CameraViewModel = viewModel()
    val photoViewModel: PhotoViewModel   = viewModel()

    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* permissions handled in screen */ }

    Scaffold(
        containerColor = SurfaceDark,
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            NavigationBar(containerColor = SurfaceDark) {
                listOf(Screen.Video, Screen.Photo, Screen.Gallery).forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(imageVector = screen.icon, contentDescription = screen.label)
                        },
                        label = {
                            Text(
                                text = screen.label,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                letterSpacing = 1.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor   = CompassGold,
                            selectedTextColor   = CompassGold,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor      = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = Screen.Video.route,
            modifier         = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Video.route) {
                CameraScreen(
                    viewModel            = cameraViewModel,
                    subscriptionManager  = subscriptionManager,
                    onRequestPermissions = {
                        permissionsLauncher.launch(
                            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                        )
                    },
                    onShowIntro = onShowIntro
                )
            }
            composable(Screen.Photo.route) {
                PhotoScreen(
                    viewModel            = photoViewModel,
                    subscriptionManager  = subscriptionManager,
                    onRequestPermissions = {
                        permissionsLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                    },
                    onShowIntro = onShowIntro
                )
            }
            composable(Screen.Gallery.route) {
                GalleryScreen(subscriptionManager = subscriptionManager)
            }
        }
    }
}
