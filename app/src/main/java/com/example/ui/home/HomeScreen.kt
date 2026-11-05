package com.example.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HisaabRepository
import com.example.data.SessionManager
import com.example.engine.MoneyEngine
import com.example.model.BudgetStateStatus
import com.example.model.ItemStatus
import com.example.model.Member
import com.example.model.Role
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CreditGreen
import com.example.ui.theme.DebtRed
import com.example.ui.theme.Forest600
import com.example.ui.theme.Forest700
import com.example.ui.theme.Forest800
import com.example.ui.theme.Forest850
import com.example.ui.theme.Forest900
import com.example.ui.theme.HearthAtmosphereBg
import com.example.ui.theme.HearthCardHolster
import com.example.ui.theme.HearthGlassBorder
import com.example.ui.theme.HearthGlassSurface
import com.example.ui.theme.HearthTextDark
import com.example.ui.theme.HearthTextMuted
import com.example.ui.theme.HearthTextSubtle
import com.example.ui.theme.Sage100
import com.example.ui.theme.Sage200
import com.example.ui.theme.Sage300
import com.example.ui.theme.Sage50
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    repository: HisaabRepository,
    sessionManager: SessionManager,
    snackbarHostState: SnackbarHostState,
    onNavigateToShopping: () -> Unit,
    onNavigateToMoney: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val members by repository.getMembers().collectAsState(initial = SessionManager.DEFAULT_MEMBERS)
    val balances by repository.getBalances().collectAsState(initial = emptyList())
    val budgetSummary by repository.getBudgetSummary().collectAsState(
        initial = com.example.model.BudgetSummary("2026-11", 500000L, 420000L, 80000L, 84, BudgetStateStatus.NEAR_LIMIT)
    )
    val shoppingItems by repository.getShoppingItems().collectAsState(initial = emptyList())
    val suggestions by repository.getSmartSettleSuggestions().collectAsState(initial = emptyList())
    val currentMember by sessionManager.currentMember.collectAsState()

    // "View as" member selection (defaults to logged-in user Ali)
    var viewAsMemberId by remember(currentMember.id) { mutableStateOf(currentMember.id) }
    val viewAsMember = members.find { it.id == viewAsMemberId } ?: currentMember
    val activeBalance = balances.find { it.memberId == viewAsMemberId }

    // Segmented tab toggle: Overview vs Budget
    var selectedTab by remember { mutableStateOf("Overview") }

    val pendingCount = shoppingItems.count { it.status == ItemStatus.PENDING }
    val pendingPreview = shoppingItems.filter { it.status == ItemStatus.PENDING }.take(3).joinToString(", ") { it.title }

    val categorySpends by repository.getCategorySpends().collectAsState(initial = emptyList())
    val warningCategories = categorySpends.filter { it.isExceedingWarning }

    var settleConfirmDialog by remember { mutableStateOf<Triple<String, String, Long>?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        HearthAtmosphereBg,
                        Sage100,
                        Sage200
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Controls: Segmented Pill Control (Overview / Budget) & Benchmark Reset
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Segmented Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                        .padding(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (selectedTab == "Overview") Color.White else Color.Transparent)
                                .clickable { selectedTab = "Overview" }
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Overview",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selectedTab == "Overview") Forest900 else HearthTextMuted
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (selectedTab == "Budget") Color.White else Color.Transparent)
                                .clickable { selectedTab = "Budget" }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Budget",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selectedTab == "Budget") Forest900 else HearthTextMuted
                            )
                        }
                    }
                }

                // Benchmark Reset Icon Action
                IconButton(
                    onClick = {
                        repository.resetToBenchmarkSeed()
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Reset to benchmark state (PKR 4,200.00 spent)")
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.7f))
                        .border(1.dp, Color.White, CircleShape)
                        .testTag("reset_benchmark_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset benchmark data",
                        tint = Forest800,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Hero Greeting Header
            Column {
                Text(
                    text = "Welcome Back, ${viewAsMember.name}!",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = HearthTextDark,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Household Ledger",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = HearthTextMuted
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(HearthTextSubtle)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "3 Gulberg, Lahore",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = HearthTextMuted
                    )
                }
            }

            // "View as" Persona Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                members.forEach { m ->
                    val isSelected = m.id == viewAsMemberId
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) Forest800 else Color.White.copy(alpha = 0.65f))
                            .border(1.dp, if (isSelected) Forest800 else Color.White, RoundedCornerShape(20.dp))
                            .clickable { viewAsMemberId = m.id }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = m.name + if (m.id == currentMember.id) " (You)" else "",
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Forest800
                        )
                    }
                }
            }

            // Hero Debit Card Holster (Frosted Container + Deep Forest Physical Card)
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = HearthCardHolster),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, HearthGlassBorder, RoundedCornerShape(28.dp))
                    .testTag("hero_resident_card")
            ) {
                Column(
                    modifier = Modifier.padding(10.dp)
                ) {
                    // Inner Embossed Physical Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Forest900,
                                        Forest850,
                                        Forest700
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            // Top Row: User Name & HEARTH Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${viewAsMember.name} Khan",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White.copy(alpha = 0.95f)
                                    )
                                    Text(
                                        text = "x1976",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Sage300.copy(alpha = 0.6f)
                                    )
                                }

                                // Hearth Brand Pill
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color.White.copy(alpha = 0.12f))
                                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(CreditGreen)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "HEARTH",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White.copy(alpha = 0.9f),
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Mid Row: Balance & Indicator dots
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column {
                                    Text(
                                        text = "NET STEWARDSHIP",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Sage300.copy(alpha = 0.8f),
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))

                                    val netPaisa = activeBalance?.netPaisa ?: 0L
                                    val sign = if (netPaisa > 0) "+" else if (netPaisa < 0) "-" else ""
                                    val absAmount = MoneyEngine.fromPaisa(kotlin.math.abs(netPaisa))
                                    val parts = absAmount.split(".")
                                    val whole = parts.getOrNull(0) ?: "0"
                                    val decimal = parts.getOrNull(1) ?: "00"

                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = "PKR ",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Sage300
                                        )
                                        Text(
                                            text = "$sign$whole",
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = ".$decimal",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                }

                                // NFC / Indicator dots
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black.copy(alpha = 0.25f))
                                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.8f)))
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.4f)))
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.4f)))
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Bottom Micro Details & PDF Verbatim Net Copy
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(width = 0.5.dp, color = Color.White.copy(alpha = 0.1f))
                                    .padding(top = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (viewAsMember.role == Role.ADMIN) "Room #2 • Head Steward" else "Room #1 • Member",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Sage300.copy(alpha = 0.7f)
                                )

                                val netCopy = when {
                                    (activeBalance?.netPaisa ?: 0L) > 0 -> "You are owed funds"
                                    (activeBalance?.netPaisa ?: 0L) < 0 -> "You owe funds"
                                    else -> "You are all settled up"
                                }
                                Text(
                                    text = netCopy,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Sage300
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Holster Action Buttons Row: Manage Ledger & Settle / Add
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onNavigateToMoney,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = Forest900, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Manage Ledger", color = Forest900, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onNavigateToMoney,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(20.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, Color.White, RoundedCornerShape(20.dp))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Forest900, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Settle / Add", color = Forest900, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            // Monthly Household Ceiling Widget (Radial Progress Gauge)
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = HearthGlassSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, HearthGlassBorder, RoundedCornerShape(24.dp))
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
                        Text(
                            text = "MONTHLY HOUSEHOLD CEILING",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Forest700,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "PKR ${MoneyEngine.fromPaisa(budgetSummary.totalSpentPaisa).split(".").first()}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Forest900
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "/ PKR ${MoneyEngine.fromPaisa(budgetSummary.monthlyBudgetPaisa).split(".").first()} cap",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = HearthTextMuted
                            )
                        }

                        Text(
                            text = "PKR ${MoneyEngine.fromPaisa(budgetSummary.remainingPaisa).split(".").first()} remaining for 11 days",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Forest600
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onNavigateToMoney() }
                        ) {
                            Text(
                                text = "Adjust Ceiling",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Forest800
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Forest800, modifier = Modifier.size(14.dp))
                        }
                    }

                    // Circular 84% Used Gauge
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .padding(4.dp)
                    ) {
                        val progressAnim by animateFloatAsState(targetValue = 0.84f, label = "gauge_anim")
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Background track
                            drawCircle(
                                color = Color.Black.copy(alpha = 0.06f),
                                radius = size.minDimension / 2,
                                style = Stroke(width = 8.dp.toPx())
                            )
                            // Progress arc
                            drawArc(
                                brush = Brush.sweepGradient(
                                    listOf(
                                        CreditGreen,
                                        AmberAccent,
                                        DebtRed
                                    )
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
                                color = AmberAccent,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }

            // Roommate Splits Section
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
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Forest700,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Full Breakdown ↗",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Forest800,
                        modifier = Modifier.clickable { onNavigateToMoney() }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Split Card 1: Ayesha
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = HearthGlassSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, HearthGlassBorder, RoundedCornerShape(22.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Forest700),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("AB", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Ayesha B.", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Forest900)
                                    Text("Room 1", fontSize = 10.sp, color = HearthTextMuted)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text("OWES YOU", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = CreditGreen, letterSpacing = 0.5.sp)
                            Text("PKR 450.00", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Forest900)

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Payment reminder sent to Ayesha (PKR 450.00)")
                                    }
                                },
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.85f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color.White, RoundedCornerShape(20.dp))
                            ) {
                                Text("Remind", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Forest800)
                            }
                        }
                    }

                    // Split Card 2: Bilal
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = HearthGlassSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, HearthGlassBorder, RoundedCornerShape(22.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Forest850),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("BM", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Bilal M.", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Forest900)
                                    Text("Room 3", fontSize = 10.sp, color = HearthTextMuted)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text("YOU OWE HIM", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = AmberAccent, letterSpacing = 0.5.sp)
                            Text("PKR 200.00", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = AmberAccent)

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    settleConfirmDialog = Triple("M01", "M03", 20000L)
                                },
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.85f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color.White, RoundedCornerShape(20.dp))
                            ) {
                                Text("Pay Split", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Forest800)
                            }
                        }
                    }
                }
            }

            // Pantry Provisions Card (Pending Items)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = HearthGlassSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, HearthGlassBorder, RoundedCornerShape(20.dp))
                    .clickable { onNavigateToShopping() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Forest700.copy(alpha = 0.12f))
                                .border(1.dp, Forest700.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = Forest800, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Pantry Run: $pendingCount Items Needed",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Forest900
                            )
                            Text(
                                text = if (pendingPreview.isNotEmpty()) pendingPreview else "Milk, eggs, pantry goods",
                                fontSize = 11.sp,
                                color = HearthTextMuted
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.8f))
                            .border(1.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Forest900, modifier = Modifier.size(14.dp))
                    }
                }
            }

            // Recent Ledger Activity
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = HearthGlassSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, HearthGlassBorder, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RECENT ACTIVITY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Forest700,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "History",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Forest800,
                            modifier = Modifier.clickable { onNavigateToMoney() }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Item 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CreditGreen))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Grocery Trip (Jalal Sons)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Forest900)
                                Text("Paid by Ali • 3-way split", fontSize = 10.sp, color = HearthTextMuted)
                            }
                        }
                        Text("PKR 780.00", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Forest900)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.Black.copy(alpha = 0.05f)))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Item 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Forest700))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Electricity Bill (LESCO)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Forest900)
                                Text("Paid by Bilal • Settled", fontSize = 10.sp, color = HearthTextMuted)
                            }
                        }
                        Text("PKR 2,520.00", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Forest900)
                    }
                }
            }

            // Category Warnings & Smart Settle (Surprise Options)
            if (warningCategories.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DebtRed.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth().border(1.dp, DebtRed.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = DebtRed)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Category Budget Warning (>40%)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DebtRed)
                            warningCategories.forEach {
                                val percent = if (budgetSummary.monthlyBudgetPaisa > 0) ((it.totalSpentPaisa * 100) / budgetSummary.monthlyBudgetPaisa).toInt() else 0
                                Text("${it.category.displayName}: PKR ${MoneyEngine.fromPaisa(it.totalSpentPaisa)} ($percent% of budget)", fontSize = 11.sp, color = Forest900)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(60.dp)) // Padding for bottom floating dock
        }

        // Settle confirmation dialog
        settleConfirmDialog?.let { (senderId, recipientId, amountPaisa) ->
            val sender = members.find { it.id == senderId }?.name ?: senderId
            val recipient = members.find { it.id == recipientId }?.name ?: recipientId
            AlertDialog(
                onDismissRequest = { settleConfirmDialog = null },
                title = { Text("Record Settlement") },
                text = { Text("Confirm payment of PKR ${MoneyEngine.fromPaisa(amountPaisa)} from $sender to $recipient?") },
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
                                            snackbarHostState.showSnackbar("Settlement recorded successfully!")
                                        },
                                        onFailure = { err ->
                                            snackbarHostState.showSnackbar("Settlement error: ${err.message}")
                                        }
                                    )
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Forest800)
                    ) {
                        Text("Confirm Pay")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { settleConfirmDialog = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
