package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import com.example.ui.theme.CreditGreen
import com.example.ui.theme.Forest600
import com.example.ui.theme.Forest700
import com.example.ui.theme.Forest800
import com.example.ui.theme.Forest900
import com.example.ui.theme.HearthAtmosphereBg
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.ui.components.MemberAvatar
import com.example.ui.home.HomeScreen
import com.example.ui.money.MoneyScreen
import com.example.ui.profile.ProfileScreen
import com.example.ui.shopping.ShoppingScreen
import com.example.ui.theme.HearthTheme
import com.example.ui.theme.TealPrimary

class MainActivity : ComponentActivity() {

    private val appContainer by lazy { AppContainer() }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            HearthTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                var isLoggedIn by remember { mutableStateOf(true) }
                var authScreen by remember { mutableStateOf("welcome") }

                if (!isLoggedIn) {
                    if (authScreen != "welcome") {
                        BackHandler { authScreen = "welcome" }
                    }

                    Scaffold(
                        contentWindowInsets = WindowInsets.safeDrawing,
                        snackbarHost = { SnackbarHost(snackbarHostState) }
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            when (authScreen) {
                                "welcome" -> WelcomeScreen(
                                    onNavigateToLogin = { authScreen = "login" },
                                    onNavigateToRegister = { authScreen = "register" },
                                    onQuickDemoLogin = { member ->
                                        appContainer.sessionManager.switchActiveMember(member)
                                        isLoggedIn = true
                                    }
                                )
                                "login" -> LoginScreen(
                                    onLoginSuccess = { member ->
                                        appContainer.sessionManager.switchActiveMember(member)
                                        isLoggedIn = true
                                    },
                                    onNavigateToRegister = { authScreen = "register" },
                                    onBackToWelcome = { authScreen = "welcome" },
                                    snackbarHostState = snackbarHostState
                                )
                                "register" -> RegisterScreen(
                                    onRegisterSuccess = { member ->
                                        appContainer.sessionManager.switchActiveMember(member)
                                        isLoggedIn = true
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
                    val currentMember by appContainer.sessionManager.currentMember.collectAsState()
                    val shoppingItems by appContainer.repository.getShoppingItems().collectAsState(initial = emptyList())
                    val pendingCount = shoppingItems.count { it.status == ItemStatus.PENDING }

                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route ?: "home"

                    Scaffold(
                        contentWindowInsets = WindowInsets.safeDrawing,
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        topBar = {
                            CenterAlignedTopAppBar(
                                title = {
                                    Text(
                                        text = "HEARTH",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        letterSpacing = 1.sp,
                                        color = Forest900
                                    )
                                },
                                actions = {
                                    // Profile Avatar Button with green ring and online dot
                                    Box(
                                        modifier = Modifier
                                            .padding(end = 16.dp)
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(Forest800, Forest600)
                                                )
                                            )
                                            .border(2.dp, Color.White, CircleShape)
                                            .clickable {
                                                navController.navigate("profile") {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                            .testTag("top_user_profile_chip"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = currentMember.name.take(2).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            letterSpacing = 0.5.sp
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(CreditGreen)
                                                .border(1.5.dp, Color.White, CircleShape)
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                    containerColor = HearthAtmosphereBg
                                )
                            )
                        },
                        bottomBar = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    modifier = Modifier
                                        .shadow(elevation = 16.dp, shape = RoundedCornerShape(36.dp))
                                        .clip(RoundedCornerShape(36.dp))
                                        .background(Color(0xD8D7E4DA))
                                        .border(1.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(36.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Tab 1: Home
                                    val isHome = currentRoute == "home"
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(24.dp))
                                            .background(if (isHome) Color.White.copy(alpha = 0.95f) else Color.Transparent)
                                            .border(if (isHome) 1.dp else 0.dp, if (isHome) Color.White else Color.Transparent, RoundedCornerShape(24.dp))
                                            .clickable {
                                                navController.navigate("home") {
                                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                            .padding(horizontal = 14.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Home,
                                            contentDescription = "Home",
                                            tint = if (isHome) Forest900 else Forest700,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        if (isHome) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Home", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Forest900)
                                        }
                                    }

                                    // Tab 2: Shopping
                                    val isShopping = currentRoute == "shopping"
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(if (isShopping) Color.White.copy(alpha = 0.95f) else Color.Transparent)
                                            .border(if (isShopping) 1.dp else 0.dp, if (isShopping) Color.White else Color.Transparent, CircleShape)
                                            .clickable {
                                                navController.navigate("shopping") {
                                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        BadgedBox(
                                            badge = {
                                                if (pendingCount > 0) {
                                                    Badge(containerColor = Forest800) {
                                                        Text("$pendingCount", color = Color.White, fontSize = 9.sp)
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.ShoppingCart,
                                                contentDescription = "Shopping",
                                                tint = if (isShopping) Forest900 else Forest700,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    // Tab 3: Money
                                    val isMoney = currentRoute == "money"
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(if (isMoney) Color.White.copy(alpha = 0.95f) else Color.Transparent)
                                            .border(if (isMoney) 1.dp else 0.dp, if (isMoney) Color.White else Color.Transparent, CircleShape)
                                            .clickable {
                                                navController.navigate("money") {
                                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.AccountBalanceWallet,
                                            contentDescription = "Money",
                                            tint = if (isMoney) Forest900 else Forest700,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Tab 4: Profile
                                    val isProfile = currentRoute == "profile"
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(if (isProfile) Color.White.copy(alpha = 0.95f) else Color.Transparent)
                                            .border(if (isProfile) 1.dp else 0.dp, if (isProfile) Color.White else Color.Transparent, CircleShape)
                                            .clickable {
                                                navController.navigate("profile") {
                                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Person,
                                            contentDescription = "Profile",
                                            tint = if (isProfile) Forest900 else Forest700,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = "home",
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable("home") {
                                HomeScreen(
                                    repository = appContainer.repository,
                                    sessionManager = appContainer.sessionManager,
                                    snackbarHostState = snackbarHostState,
                                    onNavigateToShopping = {
                                        navController.navigate("shopping") {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    onNavigateToMoney = {
                                        navController.navigate("money") {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }

                            composable("shopping") {
                                ShoppingScreen(
                                    repository = appContainer.repository,
                                    sessionManager = appContainer.sessionManager,
                                    snackbarHostState = snackbarHostState
                                )
                            }

                            composable("money") {
                                MoneyScreen(
                                    repository = appContainer.repository,
                                    sessionManager = appContainer.sessionManager,
                                    snackbarHostState = snackbarHostState
                                )
                            }

                            composable("profile") {
                                ProfileScreen(
                                    appContainer = appContainer,
                                    snackbarHostState = snackbarHostState,
                                    onSignOut = {
                                        isLoggedIn = false
                                        authScreen = "welcome"
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
