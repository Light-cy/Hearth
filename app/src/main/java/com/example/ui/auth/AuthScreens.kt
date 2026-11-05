package com.example.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.di.AppContainer
import com.example.engine.MoneyEngine
import com.example.model.Member
import com.example.ui.components.ErrorBanner
import com.example.ui.components.GlassPanelCard
import com.example.ui.components.GlassSegmentedSwitcher
import com.example.ui.components.LoadingButton
import com.example.ui.components.MemberAvatar
import com.example.ui.components.ResponsiveScreenContainer
import com.example.ui.theme.CreditGreen
import com.example.ui.theme.CreditGreenLight
import com.example.ui.theme.DebtRed
import com.example.ui.theme.Forest700
import com.example.ui.theme.Forest800
import com.example.ui.theme.Forest900
import com.example.ui.theme.HearthCardHolster
import com.example.ui.theme.HearthGlassBorder
import com.example.ui.theme.HearthGlassStrong
import com.example.ui.theme.HearthGlassSurface
import com.example.ui.theme.HearthTextDark
import com.example.ui.theme.HearthTextMuted
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun WelcomeScreen(
    appContainer: AppContainer,
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onSelectResident: (Member) -> Unit
) {
    var selectedTopTab by remember { mutableStateOf("Residence") }
    val residents by appContainer.sessionManager.registeredMembers.collectAsState()
    val householdName by appContainer.sessionManager.householdName.collectAsState()
    val inviteCode by appContainer.sessionManager.inviteCode.collectAsState()
    val balances by appContainer.repository.getBalances().collectAsState(initial = emptyList())
    val balanceMap = remember(balances) { balances.associateBy { it.memberId } }

    ResponsiveScreenContainer(
        scrollable = true,
        bottomSafePadding = 36.dp
    ) { _ ->
        // 1. Top Bar: Residence / Vaults Switcher + LAHORE Status Capsule
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassSegmentedSwitcher(
                options = listOf("Residence", "Vaults"),
                selectedOption = selectedTopTab,
                onSelect = { selectedTopTab = it }
            )

            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.76f))
                    .border(1.dp, HearthGlassBorder, CircleShape)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(CreditGreen)
                )
                Text(
                    text = "LAHORE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = HearthTextDark
                )
            }
        }

        // 2. Hero Editorial Section
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Forest800.copy(alpha = 0.12f))
                    .border(1.dp, Forest800.copy(alpha = 0.25f), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    tint = Forest800,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "HEARTH • CO-LIVING VAULT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                    color = Forest800
                )
            }

            Text(
                text = if (selectedTopTab == "Residence") {
                    "Communal Living,\nZero Friction."
                } else {
                    "Deterministic\nPaisa Vaults."
                },
                style = MaterialTheme.typography.displayMedium,
                color = HearthTextDark,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 38.sp
            )

            Text(
                text = "Split it. Settle it. Done.",
                style = MaterialTheme.typography.titleMedium,
                color = Forest800,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = "Shared household ledger, real-time grocery provision sync, and zero-sum paisa settlement built for modern co-living.",
                style = MaterialTheme.typography.bodyMedium,
                color = HearthTextMuted
            )
        }

        // 3. Verified Household Glass Card
        GlassPanelCard(
            cornerRadius = 26.dp,
            containerColor = HearthGlassStrong,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "VERIFIED HOUSEHOLD",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp,
                            color = HearthTextMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = householdName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = HearthTextDark
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = "Verified",
                        tint = Forest800,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = HearthGlassBorder)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                            residents.forEach { m ->
                                MemberAvatar(name = m.name, memberId = m.id, size = 28.dp)
                            }
                        }
                        Text(
                            text = "${residents.size} Active Co-Residents",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = HearthTextDark
                        )
                    }
                    Text(
                        text = "Vault #$inviteCode",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Forest800
                    )
                }
            }
        }

        // 4. Primary & Secondary Action Pills
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onNavigateToLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("welcome_login_button"),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Forest900)
            ) {
                Text(
                    text = "Sign In to Household",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.85f))
                    .border(1.dp, HearthGlassBorder, CircleShape)
                    .clickable(onClick = onNavigateToRegister)
                    .testTag("welcome_register_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Create Account / Join Household",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = Forest800
                )
            }
        }

        // 5. Verified Resident Vault Profiles Glass Panel
        GlassPanelCard(
            cornerRadius = 26.dp,
            containerColor = HearthGlassSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "RESIDENT VAULT PROFILES",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.1.sp,
                            color = Forest800
                        )
                        Text(
                            text = "Select your verified profile for 1-tap household access",
                            fontSize = 11.sp,
                            color = HearthTextMuted
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(CreditGreenLight)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "VERIFIED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CreditGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    residents.forEach { member ->
                        val netPaisa = balanceMap[member.id]?.netPaisa ?: 0L
                        val statusText = when {
                            netPaisa > 0L -> "+${MoneyEngine.formatPkr(netPaisa)} Net Surplus"
                            netPaisa < 0L -> "-${MoneyEngine.formatPkr(abs(netPaisa))} Due Settling"
                            else -> "All Settled Up (PKR 0.00)"
                        }
                        val statusColor = when {
                            netPaisa < 0L -> DebtRed
                            netPaisa > 0L -> CreditGreen
                            else -> HearthTextMuted
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.White.copy(alpha = 0.88f))
                                .border(1.dp, HearthGlassBorder, RoundedCornerShape(18.dp))
                                .clickable { onSelectResident(member) }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                .testTag("demo_login_${member.id.lowercase()}"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MemberAvatar(name = member.name, memberId = member.id, size = 38.dp)
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = member.name,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp,
                                            color = HearthTextDark
                                        )
                                        Text(
                                            text = "• ${member.id}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = HearthTextMuted
                                        )
                                    }
                                    Text(
                                        text = statusText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = statusColor
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Enter as ${member.name}",
                                tint = Forest800
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoginScreen(
    appContainer: AppContainer,
    onLoginSuccess: (Member) -> Unit,
    onNavigateToRegister: () -> Unit,
    onBackToWelcome: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    var email by remember { mutableStateOf("ali@hearth.pk") }
    var password by remember { mutableStateOf("hearth2026") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    ResponsiveScreenContainer(
        scrollable = true,
        bottomSafePadding = 36.dp
    ) { _ ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(
                onClick = onBackToWelcome,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.8f))
                    .border(1.dp, HearthGlassBorder, CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = HearthTextDark)
            }
            Column {
                Text(
                    text = "RESIDENT AUTHENTICATION",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                    color = Forest700
                )
                Text(
                    text = "Sign In to Hearth",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = HearthTextDark
                )
            }
        }

        if (errorMessage != null) {
            ErrorBanner(message = errorMessage!!)
        }

        GlassPanelCard(
            cornerRadius = 28.dp,
            containerColor = HearthGlassStrong,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Forest800,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Enter your registered email & password",
                        style = MaterialTheme.typography.bodyMedium,
                        color = HearthTextMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        errorMessage = null
                    },
                    label = { Text("Email Address") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_email_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_password_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(HearthCardHolster)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Registered Household Emails: ali@hearth.pk, ayesha@hearth.pk, bilal@hearth.pk",
                        style = MaterialTheme.typography.bodySmall,
                        color = HearthTextDark,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                LoadingButton(
                    text = "Sign In",
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            errorMessage = "Please enter your email and password"
                            return@LoadingButton
                        }
                        isLoading = true
                        coroutineScope.launch {
                            val result = appContainer.authenticate(email, password)
                            isLoading = false
                            result.fold(
                                onSuccess = { member ->
                                    snackbarHostState.showSnackbar("Signed in as ${member.name}")
                                    onLoginSuccess(member)
                                },
                                onFailure = { err ->
                                    errorMessage = err.message ?: "Authentication failed"
                                }
                            )
                        }
                    },
                    isLoading = isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "submit_login_button"
                )
            }
        }

        TextButton(
            onClick = onNavigateToRegister,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = "Don't have an account? Create one",
                color = Forest800,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun RegisterScreen(
    appContainer: AppContainer,
    onRegisterSuccess: (Member) -> Unit,
    onNavigateToLogin: () -> Unit,
    onBackToWelcome: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val activeInviteCode by appContainer.sessionManager.inviteCode.collectAsState()
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var inviteCode by remember(activeInviteCode) { mutableStateOf(activeInviteCode) }
    var isCreatingHousehold by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    ResponsiveScreenContainer(
        scrollable = true,
        bottomSafePadding = 36.dp
    ) { _ ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(
                onClick = onBackToWelcome,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.8f))
                    .border(1.dp, HearthGlassBorder, CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = HearthTextDark)
            }
            Column {
                Text(
                    text = "NEW CO-RESIDENT ONBOARDING",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                    color = Forest700
                )
                Text(
                    text = "Join Shared Household",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = HearthTextDark
                )
            }
        }

        if (errorMessage != null) {
            ErrorBanner(message = errorMessage!!)
        }

        GlassPanelCard(
            cornerRadius = 28.dp,
            containerColor = HearthGlassStrong,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Text(
                    text = "Register your profile and link to your shared apartment vault.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = HearthTextMuted,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Full Name (e.g., Zainab)") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("register_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        errorMessage = null
                    },
                    label = { Text("Email Address") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("register_email_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    label = { Text("Password (min 8 chars)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("register_password_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Create a brand new household?",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HearthTextDark
                    )
                    Switch(
                        checked = isCreatingHousehold,
                        onCheckedChange = { isCreatingHousehold = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Forest800
                        )
                    )
                }

                if (!isCreatingHousehold) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inviteCode,
                        onValueChange = {
                            inviteCode = it
                            errorMessage = null
                        },
                        label = { Text("Household Invite Code") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_invite_code_input")
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                LoadingButton(
                    text = "Create Account & Join",
                    onClick = {
                        isLoading = true
                        coroutineScope.launch {
                            val result = appContainer.register(
                                name = name,
                                email = email,
                                password = password,
                                inviteCode = inviteCode,
                                isCreatingHousehold = isCreatingHousehold
                            )
                            isLoading = false
                            result.fold(
                                onSuccess = { newMember ->
                                    snackbarHostState.showSnackbar("Welcome to Hearth, ${newMember.name}!")
                                    onRegisterSuccess(newMember)
                                },
                                onFailure = { err ->
                                    errorMessage = err.message ?: "Registration failed"
                                }
                            )
                        }
                    },
                    isLoading = isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "submit_register_button"
                )
            }
        }

        TextButton(
            onClick = onNavigateToLogin,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = "Already in a household? Sign In",
                color = Forest800,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
