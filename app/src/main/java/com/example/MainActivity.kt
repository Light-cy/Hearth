package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.di.AppContainer
import com.example.model.ItemStatus
import com.example.ui.auth.LoginScreen
import com.example.ui.auth.RegisterScreen
import com.example.ui.auth.WelcomeScreen
import com.example.ui.home.HomeScreen
import com.example.ui.money.MoneyScreen
import com.example.ui.profile.ProfileScreen
import com.example.ui.shopping.ShoppingScreen
import com.example.ui.theme.Forest800
import com.example.ui.theme.Forest900
import com.example.ui.theme.HearthTheme
import com.example.ui.theme.ZeniSageTop

private data class DockTabItem(
    val route: String,
    val label: String,
    val contentDesc: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector,
    val badgeCount: Int = 0
)

class MainActivity : ComponentActivity() {

    private val appContainer by lazy { AppContainer() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            HearthTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                val isLoggedIn by appContainer.sessionManager.isAuthenticated.collectAsState()
                var authScreen by remember { mutableStateOf("welcome") }

                if (!isLoggedIn) {
                    if (authScreen != "welcome") {
                        BackHandler { authScreen = "welcome" }
                    }

                    Scaffold(
                        containerColor = ZeniSageTop,
                        contentWindowInsets = WindowInsets.safeDrawing,
                        snackbarHost = { SnackbarHost(snackbarHostState) }
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            when (authScreen) {
                                "welcome" -> WelcomeScreen(
                                    appContainer = appContainer,
                                    onNavigateToLogin = { authScreen = "login" },
                                    onNavigateToRegister = { authScreen = "register" },
                                    onSelectResident = { member ->
                                        appContainer.sessionManager.switchActiveMember(member)
                                    }
                                )
                                "login" -> LoginScreen(
                                    appContainer = appContainer,
                                    onLoginSuccess = {
                                        authScreen = "welcome"
                                    },
                                    onNavigateToRegister = { authScreen = "register" },
                                    onBackToWelcome = { authScreen = "welcome" },
                                    snackbarHostState = snackbarHostState
                                )
                                "register" -> RegisterScreen(
                                    appContainer = appContainer,
                                    onRegisterSuccess = {
                                        authScreen = "welcome"
                                    },
                                    onNavigateToLogin = { authScreen = "login" },
                                    onBackToWelcome = { authScreen = "welcome" },
                                    snackbarHostState = snackbarHostState
                                )
                            }
                        }
                    }
                } else {
                    val navController = rememberNavController()
                    val shoppingItems by appContainer.repository.getShoppingItems().collectAsState(initial = emptyList())
                    val pendingCount = shoppingItems.count { it.status == ItemStatus.PENDING }

                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route ?: "home"

                    val navigateToTab: (String) -> Unit = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }

                    val dockTabs = listOf(
                        DockTabItem(
                            route = "home",
                            label = "Home",
                            contentDesc = "Home",
                            activeIcon = Icons.Filled.Home,
                            inactiveIcon = Icons.Outlined.Home
                        ),
                        DockTabItem(
                            route = "shopping",
                            label = "Pantry",
                            contentDesc = "Shopping",
                            activeIcon = Icons.Filled.ShoppingBag,
                            inactiveIcon = Icons.Outlined.ShoppingBag,
                            badgeCount = pendingCount
                        ),
                        DockTabItem(
                            route = "money",
                            label = "Ledger",
                            contentDesc = "Money",
                            activeIcon = Icons.Filled.AccountBalanceWallet,
                            inactiveIcon = Icons.Outlined.AccountBalanceWallet
                        ),
                        DockTabItem(
                            route = "profile",
                            label = "Steward",
                            contentDesc = "Profile",
                            activeIcon = Icons.Filled.Person,
                            inactiveIcon = Icons.Outlined.Person
                        )
                    )

                    Scaffold(
                        containerColor = ZeniSageTop,
                        contentWindowInsets = WindowInsets.safeDrawing,
                        snackbarHost = {
                            SnackbarHost(
                                hostState = snackbarHostState,
                                modifier = Modifier.padding(bottom = 84.dp)
                            )
                        },
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            NavHost(
                                navController = navController,
                                startDestination = "home",
                                modifier = Modifier.fillMaxSize()
                            ) {
                                composable("home") {
                                    HomeScreen(
                                        repository = appContainer.repository,
                                        sessionManager = appContainer.sessionManager,
                                        snackbarHostState = snackbarHostState,
                                        onNavigateToShopping = { navigateToTab("shopping") },
                                        onNavigateToMoney = { navigateToTab("money") },
                                        onNavigateToProfile = { navigateToTab("profile") }
                                    )
                                }

                                composable("shopping") {
                                    ShoppingScreen(
                                        repository = appContainer.repository,
                                        sessionManager = appContainer.sessionManager,
                                        snackbarHostState = snackbarHostState,
                                        onNavigateToProfile = { navigateToTab("profile") }
                                    )
                                }

                                composable("money") {
                                    MoneyScreen(
                                        repository = appContainer.repository,
                                        sessionManager = appContainer.sessionManager,
                                        snackbarHostState = snackbarHostState,
                                        onNavigateToProfile = { navigateToTab("profile") }
                                    )
                                }

                                composable("profile") {
                                    ProfileScreen(
                                        appContainer = appContainer,
                                        snackbarHostState = snackbarHostState,
                                        onSignOut = {
                                            appContainer.sessionManager.signOut()
                                            authScreen = "welcome"
                                        }
                                    )
                                }
                            }

                            // Zeni Floating Glass Bottom Dock (Overlaid at bottom over the sage atmosphere)
                            ZeniFloatingBottomDock(
                                tabs = dockTabs,
                                currentRoute = currentRoute,
                                onSelectTab = navigateToTab,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(horizontal = 20.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ZeniFloatingBottomDock(
    tabs: List<DockTabItem>,
    currentRoute: String,
    onSelectTab: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(50),
                    ambientColor = Forest900.copy(alpha = 0.35f),
                    spotColor = Forest900.copy(alpha = 0.35f)
                )
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xB893A89A),
                            Color(0xCC7B9384)
                        )
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.65f), RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isSelected = currentRoute == tab.route

                if (isSelected) {
                    // Expanded Zeni Active Pill: [ Label   (Glowing White Circle Icon) ]
                    Row(
                        modifier = Modifier
                            .animateContentSize(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.24f))
                            .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(50))
                            .clickable { onSelectTab(tab.route) }
                            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                            .testTag("nav_tab_${tab.route}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tab.label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.2.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        // Glowing White Circle Icon
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .shadow(
                                    elevation = 10.dp,
                                    shape = CircleShape,
                                    ambientColor = Color.White,
                                    spotColor = Color.White
                                )
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            BadgedBox(
                                badge = {
                                    if (tab.badgeCount > 0) {
                                        Badge(containerColor = Forest800) {
                                            Text("${tab.badgeCount}", color = Color.White, fontSize = 9.sp)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = tab.activeIcon,
                                    contentDescription = tab.contentDesc,
                                    tint = Forest900,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Inactive Tab: Symmetrical Frosted Glass Circle (44.dp)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f))
                            .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape)
                            .clickable { onSelectTab(tab.route) }
                            .testTag("nav_tab_${tab.route}"),
                        contentAlignment = Alignment.Center
                    ) {
                        BadgedBox(
                            badge = {
                                if (tab.badgeCount > 0) {
                                    Badge(containerColor = Forest800) {
                                        Text("${tab.badgeCount}", color = Color.White, fontSize = 9.sp)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = tab.inactiveIcon,
                                contentDescription = tab.contentDesc,
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
