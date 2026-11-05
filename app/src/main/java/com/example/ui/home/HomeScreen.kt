package com.example.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Hexagon
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HearthRepository
import com.example.data.SessionManager
import com.example.engine.MoneyEngine
import com.example.model.BudgetStateStatus
import com.example.model.BudgetSummary
import com.example.model.CategorySpend
import com.example.model.Expense
import com.example.model.ItemStatus
import com.example.model.Member
import com.example.model.MemberBalance
import com.example.model.Role
import com.example.model.SmartSettleSuggestion
import com.example.model.SplitType
import com.example.ui.components.DarkForestHeroCard
import com.example.ui.components.GlassPanelCard
import com.example.ui.components.GlassSegmentedSwitcher
import com.example.ui.components.ProfileAvatarOrb
import com.example.ui.components.ResponsiveScreenContainer
import com.example.ui.components.SplitDecimalMoneyText
import com.example.ui.components.StatusChip
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CreditGreen
import com.example.ui.theme.DebtRed
import com.example.ui.theme.Forest600
import com.example.ui.theme.Forest700
import com.example.ui.theme.Forest800
import com.example.ui.theme.Forest850
import com.example.ui.theme.Forest900
import com.example.ui.theme.HearthCardHolster
import com.example.ui.theme.HearthGlassBorder
import com.example.ui.theme.HearthGlassSurface
import com.example.ui.theme.HearthTextDark
import com.example.ui.theme.HearthTextMuted
import com.example.ui.theme.HearthTextSubtle
import com.example.ui.theme.Sage100
import com.example.ui.theme.Sage200
import com.example.ui.theme.Sage300
import com.example.ui.theme.SageChallengeButton
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun HomeScreen(
    repository: HearthRepository,
    sessionManager: SessionManager,
    snackbarHostState: SnackbarHostState,
    onNavigateToShopping: () -> Unit,
    onNavigateToMoney: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val members by repository.getMembers().collectAsState(initial = SessionManager.DEFAULT_MEMBERS)
    val balances by repository.getBalances().collectAsState(initial = emptyList())
    val expenses by repository.getExpenses().collectAsState(initial = emptyList())
    val budgetSummary by repository.getBudgetSummary().collectAsState(
        initial = BudgetSummary("2026-11", 500000L, 420000L, 80000L, 84, BudgetStateStatus.NEAR_LIMIT)
    )
    val shoppingItems by repository.getShoppingItems().collectAsState(initial = emptyList())
    val suggestions by repository.getSmartSettleSuggestions().collectAsState(initial = emptyList())
    val categorySpends by repository.getCategorySpends().collectAsState(initial = emptyList())
    val currentMember by sessionManager.currentMember.collectAsState()

    // "View as" member selection (defaults to logged-in user)
    var viewAsMemberId by remember(currentMember.id) { mutableStateOf(currentMember.id) }
    val viewAsMember = members.find { it.id == viewAsMemberId } ?: currentMember
    val activeBalance = balances.find { it.memberId == viewAsMemberId }

    // Segmented tab toggle: Overview vs Budget
    var selectedTab by remember { mutableStateOf("Overview") }

    val pendingCount = shoppingItems.count { it.status == ItemStatus.PENDING }
    val boughtCount = shoppingItems.count { it.status == ItemStatus.BOUGHT }
    val pendingPreview = shoppingItems
        .filter { it.status == ItemStatus.PENDING }
        .take(3)
        .joinToString(", ") { it.title }

    val warningCategories = categorySpends.filter { it.isExceedingWarning }

    var settleConfirmDialog by remember { mutableStateOf<Triple<String, String, Long>?>(null) }
    var showAdjustBudgetDialog by remember { mutableStateOf(false) }

    ResponsiveScreenContainer(modifier = modifier) { isCompact ->
        // Top Controls Row: Segmented Switcher (Overview | Budget) + Sync + Profile Orb
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassSegmentedSwitcher(
                options = listOf("Overview", "Budget"),
                selectedOption = selectedTab,
                onSelect = { selectedTab = it },
                optionTestTags = mapOf(
                    "Overview" to "home_tab_overview",
                    "Budget" to "home_tab_budget"
                )
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ledger Sync Action
                IconButton(
                    onClick = {
                        repository.restoreDefaultLedger()
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Household ledger synchronized")
                        }
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.72f))
                        .border(1.dp, Color.White, CircleShape)
                        .testTag("reset_benchmark_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sync household ledger",
                        tint = Forest800,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Zeni Profile Avatar Orb
                ProfileAvatarOrb(
                    memberName = currentMember.name,
                    onClick = onNavigateToProfile,
                    testTag = "top_user_profile_chip"
                )
            }
        }

        // Greeting Header
        Column {
            Text(
                text = if (selectedTab == "Overview") "Welcome Back, ${viewAsMember.name}!" else "Budget & Challenges",
                fontSize = if (isCompact) 23.sp else 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = HearthTextDark,
                letterSpacing = (-0.6).sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (selectedTab == "Overview") {
                    "Here's your account overview • Flat 4B, Lahore"
                } else {
                    "Monthly spend limit & category stewardship • ${budgetSummary.month}"
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = HearthTextMuted
            )
        }

        // "View as" Persona Selector Chips (horizontally scrollable on narrow screens)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "View as:",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = HearthTextMuted
            )
            members.forEach { m ->
                val isSelected = m.id == viewAsMemberId
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (isSelected) Forest900 else Color.White.copy(alpha = 0.72f))
                        .border(1.dp, if (isSelected) Forest900 else Color.White, RoundedCornerShape(50))
                        .clickable { viewAsMemberId = m.id }
                        .padding(horizontal = 13.dp, vertical = 6.dp)
                        .testTag("view_as_${m.id.lowercase()}")
                ) {
                    Text(
                        text = m.name + if (m.id == currentMember.id) " (You)" else "",
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isSelected) Color.White else Forest900
                    )
                }
            }
        }

        AnimatedContent(
            targetState = selectedTab,
            label = "home_tab_content"
        ) { tab ->
            if (tab == "Overview") {
                OverviewTabContent(
                    isCompact = isCompact,
                    viewAsMember = viewAsMember,
                    activeBalance = activeBalance,
                    members = members,
                    balances = balances,
                    suggestions = suggestions,
                    expenses = expenses,
                    budgetSummary = budgetSummary,
                    warningCategories = warningCategories,
                    pendingCount = pendingCount,
                    boughtCount = boughtCount,
                    totalShoppingCount = shoppingItems.size,
                    pendingPreview = pendingPreview,
                    onNavigateToMoney = onNavigateToMoney,
                    onNavigateToShopping = onNavigateToShopping,
                    onSwitchToBudgetTab = { selectedTab = "Budget" },
                    onOpenAdjustBudget = { showAdjustBudgetDialog = true },
                    onPromptSettlement = { senderId, recipientId, amountPaisa ->
                        settleConfirmDialog = Triple(senderId, recipientId, amountPaisa)
                    },
                    onShowMessage = { msg ->
                        coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                    }
                )
            } else {
                BudgetChallengeTabContent(
                    budgetSummary = budgetSummary,
                    categorySpends = categorySpends,
                    expenses = expenses,
                    onOpenAdjustBudget = { showAdjustBudgetDialog = true },
                    onGenerateRecurringBills = {
                        coroutineScope.launch {
                            val res = repository.generateRecurringBills(currentMember.id)
                            res.fold(
                                onSuccess = { count ->
                                    if (count > 0) {
                                        snackbarHostState.showSnackbar("Generated $count monthly recurring bill(s) for ${budgetSummary.month}!")
                                    } else {
                                        snackbarHostState.showSnackbar("All recurring bills are already applied for ${budgetSummary.month}.")
                                    }
                                },
                                onFailure = { err ->
                                    snackbarHostState.showSnackbar("Failed to generate recurring bills: ${err.message}")
                                }
                            )
                        }
                    }
                )
            }
        }
    }

    // Settlement Confirmation Dialog
    settleConfirmDialog?.let { (senderId, recipientId, amountPaisa) ->
        val sender = members.find { it.id == senderId }?.name ?: senderId
        val recipient = members.find { it.id == recipientId }?.name ?: recipientId
        AlertDialog(
            onDismissRequest = { settleConfirmDialog = null },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Record Settlement", fontWeight = FontWeight.ExtraBold) },
            text = {
                Text("Confirm payment of ${MoneyEngine.formatPkr(amountPaisa)} from $sender ($senderId) to $recipient ($recipientId)?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val settleAction = settleConfirmDialog
                        settleConfirmDialog = null
                        if (settleAction != null) {
                            coroutineScope.launch {
                                val result = repository.recordSettlement(
                                    senderId = settleAction.first,
                                    recipientId = settleAction.second,
                                    amountPaisa = settleAction.third,
                                    recordedBy = currentMember.id
                                )
                                result.fold(
                                    onSuccess = {
                                        snackbarHostState.showSnackbar(
                                            "Settled: $sender paid $recipient ${MoneyEngine.formatPkr(settleAction.third)}!"
                                        )
                                    },
                                    onFailure = { err ->
                                        snackbarHostState.showSnackbar("Settlement error: ${err.message}")
                                    }
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Forest900)
                ) {
                    Text("Confirm Pay", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { settleConfirmDialog = null }) {
                    Text("Cancel", color = Forest800, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    // Adjust Monthly Budget Ceiling Dialog
    if (showAdjustBudgetDialog) {
        var budgetInput by remember {
            mutableStateOf(MoneyEngine.fromPaisa(budgetSummary.monthlyBudgetPaisa))
        }
        var budgetError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAdjustBudgetDialog = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Adjust Monthly Ceiling", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Set the monthly household spending cap in PKR.",
                        fontSize = 13.sp,
                        color = HearthTextMuted
                    )
                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = {
                            budgetInput = it
                            budgetError = null
                        },
                        label = { Text("Monthly Cap (PKR)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = budgetError != null,
                        supportingText = budgetError?.let { { Text(it, color = DebtRed) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newPaisa = try {
                            MoneyEngine.toPaisa(budgetInput)
                        } catch (e: Exception) {
                            budgetError = e.message ?: "Invalid amount"
                            return@Button
                        }
                        coroutineScope.launch {
                            val res = repository.updateMonthlyBudget(newPaisa)
                            if (res.isSuccess) {
                                showAdjustBudgetDialog = false
                                snackbarHostState.showSnackbar("Monthly ceiling updated to ${MoneyEngine.formatPkr(newPaisa)}")
                            } else {
                                budgetError = res.exceptionOrNull()?.message
                            }
                        }
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Forest900)
                ) {
                    Text("Save Cap", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdjustBudgetDialog = false }) {
                    Text("Cancel", color = Forest800)
                }
            }
        )
    }
}

@Composable
private fun OverviewTabContent(
    isCompact: Boolean,
    viewAsMember: Member,
    activeBalance: MemberBalance?,
    members: List<Member>,
    balances: List<MemberBalance>,
    suggestions: List<SmartSettleSuggestion>,
    expenses: List<Expense>,
    budgetSummary: BudgetSummary,
    warningCategories: List<CategorySpend>,
    pendingCount: Int,
    boughtCount: Int,
    totalShoppingCount: Int,
    pendingPreview: String,
    onNavigateToMoney: () -> Unit,
    onNavigateToShopping: () -> Unit,
    onSwitchToBudgetTab: () -> Unit,
    onOpenAdjustBudget: () -> Unit,
    onPromptSettlement: (String, String, Long) -> Unit,
    onShowMessage: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 1. Zeni Notched Hero Debit Card Holster (Phone 1 Reference UI)
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = HearthCardHolster),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, HearthGlassBorder, RoundedCornerShape(28.dp))
                .testTag("hero_resident_card")
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Top Notched Holster Header (Matches "Nico Pazz   x1976" in Zeni Phone 1)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${viewAsMember.name} Khan",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Forest900
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Forest900.copy(alpha = 0.08f))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (viewAsMember.role == Role.ADMIN) "Admin • ${viewAsMember.id}" else "Member • ${viewAsMember.id}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Forest800
                            )
                        }
                    }
                    Text(
                        text = "x1976",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Forest800
                    )
                }

                // Inner Deep Forest Physical Card with Zeni Dot-Matrix Texture
                DarkForestHeroCard(
                    cornerRadius = 22.dp,
                    showDotMatrix = true,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // Top Row: Paid / Owed summary on left, Iridescent "HEARTH" chip on right
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val paidStr = MoneyEngine.formatPkr(activeBalance?.paidPaisa ?: 0L)
                            val owedStr = MoneyEngine.formatPkr(activeBalance?.owedPaisa ?: 0L)
                            Text(
                                text = "Paid $paidStr • Share $owedStr",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Sage300.copy(alpha = 0.88f)
                            )

                            // Iridescent Metallic HEARTH Badge (Matches "zeni" badge on Phone 1)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFFE3F2EA),
                                                Color(0xFFC2E2D4),
                                                Color(0xFFEAF4F8)
                                            )
                                        )
                                    )
                                    .border(1.dp, Color.White, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "hearth",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Forest900,
                                    letterSpacing = 0.4.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(26.dp))

                        // Bottom Row: Split-Decimal Net Balance & PDF Verbatim Net Status Copy
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            val netPaisa = activeBalance?.netPaisa ?: 0L
                            Column(modifier = Modifier.weight(1f)) {
                                SplitDecimalMoneyText(
                                    paisa = netPaisa,
                                    currencyPrefix = "PKR ",
                                    wholeFontSize = if (isCompact) 26.sp else 30.sp,
                                    decimalFontSize = if (isCompact) 16.sp else 19.sp,
                                    prefixFontSize = 13.sp,
                                    showPlusSign = true
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                // Verbatim Net Status Copy required by Hackathon PDF:
                                // "You are owed PKR 250.00" / "You owe PKR 450.00" / "You are all settled up"
                                Text(
                                    text = MoneyEngine.formatNetStatusCopy(netPaisa),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = when {
                                        netPaisa > 0 -> Color(0xFF6EE7B7)
                                        netPaisa < 0 -> Color(0xFFFDA4AF)
                                        else -> Sage300
                                    }
                                )
                            }

                            // 4-dot Zeni Indicator Pill
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(50))
                                    .padding(horizontal = 9.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.White))
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.4f)))
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.4f)))
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.4f)))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Holster Action Buttons Row: Manage Ledger (Ghost) & + Settle / Add (Solid White Pill)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onNavigateToMoney,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Forest900
                        ),
                        elevation = ButtonDefaults.buttonElevation(0.dp),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Hexagon, contentDescription = null, tint = Forest800, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Manage Ledger",
                            color = Forest900,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = onNavigateToMoney,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Forest900
                        ),
                        shape = RoundedCornerShape(50),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, Color.White, RoundedCornerShape(50))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Forest900, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Settle / Add",
                            color = Forest900,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 2. Monthly Household Ceiling Glass Widget (with Live Radial Gauge & StatusChip)
        GlassPanelCard(
            cornerRadius = 24.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("monthly_ceiling_widget")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "MONTHLY CEILING",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Forest700,
                            letterSpacing = 0.6.sp
                        )
                        StatusChip(status = budgetSummary.status)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = MoneyEngine.formatPkr(budgetSummary.totalSpentPaisa),
                            fontSize = if (isCompact) 18.sp else 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Forest900
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "/ ${MoneyEngine.formatPkr(budgetSummary.monthlyBudgetPaisa)}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = HearthTextMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${MoneyEngine.formatPkr(budgetSummary.remainingPaisa)} remaining this month",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Forest600
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onSwitchToBudgetTab() }
                        ) {
                            Text(
                                text = "Challenge View",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Forest900
                            )
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Forest900, modifier = Modifier.size(14.dp))
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onOpenAdjustBudget() }
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = Forest700, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Adjust Cap",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Forest700
                            )
                        }
                    }
                }

                // Live Animated Radial Progress Gauge
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(74.dp)
                        .padding(4.dp)
                ) {
                    val targetFraction = (budgetSummary.percentUsed.coerceIn(0, 100)) / 100f
                    val progressAnim by animateFloatAsState(targetValue = targetFraction, label = "gauge_anim")
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.07f),
                            radius = size.minDimension / 2,
                            style = Stroke(width = 8.dp.toPx())
                        )
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(CreditGreen, AmberAccent, DebtRed)
                            ),
                            startAngle = -90f,
                            sweepAngle = progressAnim * 360f,
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${budgetSummary.percentUsed}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Forest900,
                            lineHeight = 14.sp
                        )
                        Text(
                            text = "USED",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (budgetSummary.status) {
                                BudgetStateStatus.ON_TRACK -> CreditGreen
                                BudgetStateStatus.NEAR_LIMIT -> AmberAccent
                                BudgetStateStatus.OVER_BUDGET -> DebtRed
                            },
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        // 3. Dynamic Roommate Splits & Smart Settle Cards (No Hardcoded Values!)
        val otherMembers = members.filter { it.id != viewAsMember.id }
        if (otherMembers.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ROOMMATE SPLITS",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Forest700,
                        letterSpacing = 0.6.sp
                    )
                    Text(
                        text = "Full Breakdown ↗",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Forest900,
                        modifier = Modifier.clickable { onNavigateToMoney() }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    otherMembers.forEach { roommate ->
                        val rmBalance = balances.find { it.memberId == roommate.id }
                        val rmNet = rmBalance?.netPaisa ?: 0L

                        // Find direct SmartSettleSuggestion involving this roommate, or fallback to any suggestion for this roommate
                        val directSuggestion = suggestions.find {
                            (it.fromMemberId == roommate.id && it.toMemberId == viewAsMember.id) ||
                                (it.fromMemberId == viewAsMember.id && it.toMemberId == roommate.id)
                        } ?: suggestions.find {
                            it.fromMemberId == roommate.id || it.toMemberId == roommate.id
                        }

                        val (statusLabel, statusColor, displayAmountPaisa) = when {
                            directSuggestion != null && directSuggestion.fromMemberId == roommate.id && directSuggestion.toMemberId == viewAsMember.id ->
                                Triple("OWES YOU", CreditGreen, directSuggestion.amountPaisa)
                            directSuggestion != null && directSuggestion.fromMemberId == viewAsMember.id && directSuggestion.toMemberId == roommate.id ->
                                Triple("YOU OWE", AmberAccent, directSuggestion.amountPaisa)
                            rmNet < 0 ->
                                Triple("OWES HOUSEHOLD", DebtRed, abs(rmNet))
                            rmNet > 0 ->
                                Triple("OWED BY FLAT", CreditGreen, rmNet)
                            else ->
                                Triple("ALL SETTLED", HearthTextMuted, 0L)
                        }

                        GlassPanelCard(
                            cornerRadius = 22.dp,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(if (roommate.id == "M02") Forest700 else Forest850),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = roommate.name.take(2).uppercase(),
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = roommate.name,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Forest900,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Roommate • ${roommate.id}",
                                            fontSize = 10.sp,
                                            color = HearthTextMuted
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = statusLabel,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = statusColor,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = MoneyEngine.formatPkr(displayAmountPaisa),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Forest900
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        if (directSuggestion != null) {
                                            onPromptSettlement(
                                                directSuggestion.fromMemberId,
                                                directSuggestion.toMemberId,
                                                directSuggestion.amountPaisa
                                            )
                                        } else {
                                            onShowMessage("${roommate.name} is already settled up!")
                                        }
                                    },
                                    enabled = directSuggestion != null,
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White.copy(alpha = 0.9f),
                                        contentColor = Forest900,
                                        disabledContainerColor = Color.White.copy(alpha = 0.45f),
                                        disabledContentColor = HearthTextMuted
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color.White, RoundedCornerShape(50))
                                ) {
                                    val btnLabel = when {
                                        directSuggestion == null -> "Settled ✓"
                                        directSuggestion.fromMemberId == viewAsMember.id -> "Pay Split"
                                        directSuggestion.toMemberId == viewAsMember.id -> "Settle Now"
                                        else -> "Settle ${directSuggestion.fromName}"
                                    }
                                    Text(
                                        text = btnLabel,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Pantry Provisions Promo Card (Zeni Phone 1 Bottom White Card Style)
        GlassPanelCard(
            cornerRadius = 24.dp,
            containerColor = Color.White.copy(alpha = 0.88f),
            onClick = onNavigateToShopping,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(CreditGreen.copy(alpha = 0.16f))
                                .border(1.dp, CreditGreen.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = null,
                                tint = Forest800,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Pantry Run: $pendingCount Items Needed",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Forest900
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Sage100)
                            .border(1.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NorthEast,
                            contentDescription = "Open Shopping List",
                            tint = Forest900,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (pendingPreview.isNotEmpty()) {
                        "Pending provisions: $pendingPreview — tap to check out & split equally."
                    } else {
                        "All household provisions are stocked up! Tap to add new kitchen items."
                    },
                    fontSize = 12.sp,
                    color = HearthTextMuted,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                val totalItems = maxOf(1, totalShoppingCount)
                val progress = boughtCount.toFloat() / totalItems.toFloat()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$boughtCount/$totalShoppingCount stocked",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Forest900
                    )
                    Text(
                        text = "$pendingCount pending",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = HearthTextSubtle
                    )
                }

                Spacer(modifier = Modifier.height(5.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    color = Forest900,
                    trackColor = Sage200,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                )
            }
        }

        // 5. Dynamic Recent Ledger Activity Card
        GlassPanelCard(
            cornerRadius = 22.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(15.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RECENT ACTIVITY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Forest700,
                        letterSpacing = 0.6.sp
                    )
                    Text(
                        text = "View All (${expenses.size}) ↗",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Forest900,
                        modifier = Modifier.clickable { onNavigateToMoney() }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                val recentExpenses = expenses.take(4)
                recentExpenses.forEachIndexed { index, exp ->
                    val payerName = members.find { it.id == exp.payerId }?.name ?: exp.payerId
                    val splitDesc = if (exp.splitType == SplitType.EQUAL) {
                        "${exp.splits.size}-way equal"
                    } else {
                        "Custom split"
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (index % 2 == 0) CreditGreen else Forest700)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = exp.title,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Forest900,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Paid by $payerName • $splitDesc • ${exp.category.displayName}",
                                    fontSize = 10.5.sp,
                                    color = HearthTextMuted
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = MoneyEngine.formatPkr(exp.amountPaisa),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Forest900
                        )
                    }

                    if (index < recentExpenses.lastIndex) {
                        HorizontalDivider(
                            color = Color.Black.copy(alpha = 0.05f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // 6. Category Budget Warning Banner (>40%)
        if (warningCategories.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DebtRed.copy(alpha = 0.12f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DebtRed.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                    .clickable { onSwitchToBudgetTab() }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = DebtRed, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "⚠️ Category Limit Exceeded (>40% of Budget)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DebtRed
                        )
                        warningCategories.forEach { catSpend ->
                            val percent = if (budgetSummary.monthlyBudgetPaisa > 0) {
                                ((catSpend.totalSpentPaisa * 100.0) / budgetSummary.monthlyBudgetPaisa).toInt()
                            } else 0
                            Text(
                                text = "${catSpend.category.displayName}: ${MoneyEngine.formatPkr(catSpend.totalSpentPaisa)} ($percent% of monthly ceiling)",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Forest900
                            )
                        }
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = DebtRed, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

/**
 * Dark Forest-Emerald "Challenges / Budget" Card
 * housing Per-Category Breakdown & >40% Warning and Recurring Bills Generator.
 */
@Composable
private fun BudgetChallengeTabContent(
    budgetSummary: BudgetSummary,
    categorySpends: List<CategorySpend>,
    expenses: List<Expense>,
    onOpenAdjustBudget: () -> Unit,
    onGenerateRecurringBills: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DarkForestHeroCard(
            cornerRadius = 28.dp,
            showDotMatrix = true,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(14.dp, RoundedCornerShape(28.dp))
                .testTag("budget_challenge_card")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header Row: "Monthly Budget Challenge" + Hexagon Icon Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Monthly Stewardship Challenge",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Sage300
                    )

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            .clickable { onOpenAdjustBudget() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hexagon,
                            contentDescription = "Adjust Spend Limit",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Large Split-Decimal Total Spent Display (Matches $1,976.00 on Phone 3)
                SplitDecimalMoneyText(
                    paisa = budgetSummary.totalSpentPaisa,
                    currencyPrefix = "PKR ",
                    wholeFontSize = 34.sp,
                    decimalFontSize = 20.sp,
                    prefixFontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Spent of ${MoneyEngine.formatPkr(budgetSummary.monthlyBudgetPaisa)} spend limit",
                        fontSize = 12.5.sp,
                        color = Sage300.copy(alpha = 0.9f)
                    )
                    StatusChip(status = budgetSummary.status)
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Zeni Segmented Glowing Progress Bar with Vertical Pin Marker
                val clampedPercent = budgetSummary.percentUsed.coerceIn(0, 100)
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        val segments = 6
                        for (i in 0 until segments) {
                            val segStart = (i * 100) / segments
                            val segEnd = ((i + 1) * 100) / segments
                            val fillFraction = when {
                                clampedPercent >= segEnd -> 1f
                                clampedPercent <= segStart -> 0f
                                else -> (clampedPercent - segStart).toFloat() / (segEnd - segStart).toFloat()
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                            ) {
                                if (fillFraction > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(fillFraction)
                                            .fillMaxHeight()
                                            .background(
                                                Brush.horizontalGradient(
                                                    colors = listOf(
                                                        Sage300.copy(alpha = 0.35f),
                                                        Color.White.copy(alpha = 0.85f)
                                                    )
                                                )
                                            )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${MoneyEngine.formatPkr(budgetSummary.remainingPaisa)} remaining",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${budgetSummary.percentUsed}% used",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Sage300
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Inner White Card: Category Checkpoints & Breakdown
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val healthyCount = categorySpends.count { !it.isExceedingWarning }
                        Text(
                            text = "$healthyCount of ${categorySpends.size} Categories Within 40% Cap",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Forest900
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 6 Circular Category Checkpoint Nodes (Matches T F S S M T W on Zeni Phone 3)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            categorySpends.forEach { catSpend ->
                                val isWarning = catSpend.isExceedingWarning
                                val hasSpend = catSpend.totalSpentPaisa > 0L
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = catSpend.category.displayName.take(3).uppercase(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = HearthTextMuted
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isWarning -> DebtRed.copy(alpha = 0.18f)
                                                    hasSpend -> SageChallengeButton
                                                    else -> Sage100
                                                }
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = when {
                                                    isWarning -> DebtRed
                                                    hasSpend -> Forest700.copy(alpha = 0.3f)
                                                    else -> Color.Transparent
                                                },
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isWarning) {
                                            Icon(
                                                imageVector = Icons.Default.PriorityHigh,
                                                contentDescription = "Warning",
                                                tint = DebtRed,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        } else if (hasSpend) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Within cap",
                                                tint = Forest900,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Sage100)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Per-category spending rows
                        categorySpends.filter { it.totalSpentPaisa > 0L }.forEach { catSpend ->
                            val pct = if (budgetSummary.monthlyBudgetPaisa > 0) {
                                ((catSpend.totalSpentPaisa * 100.0) / budgetSummary.monthlyBudgetPaisa).toInt()
                            } else 0
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = catSpend.category.displayName,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Forest900
                                    )
                                    if (catSpend.isExceedingWarning) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .background(DebtRed.copy(alpha = 0.14f))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "⚠️ Limit Exceeded (>40%)",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = DebtRed
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "${MoneyEngine.formatPkr(catSpend.totalSpentPaisa)} ($pct%)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (catSpend.isExceedingWarning) DebtRed else Forest800
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Sage-Green Pill CTA: Generate Monthly Recurring Bills
                val recurringCount = expenses.count { it.isRecurring }
                Button(
                    onClick = onGenerateRecurringBills,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SageChallengeButton,
                        contentColor = Forest900
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("generate_recurring_bills_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Autorenew,
                        contentDescription = null,
                        tint = Forest900,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Generate Monthly Recurring Bills ($recurringCount active)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}
