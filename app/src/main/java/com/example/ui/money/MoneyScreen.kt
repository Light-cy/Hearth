package com.example.ui.money

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.model.BudgetCategory
import com.example.model.SplitType
import com.example.ui.components.DarkForestHeroCard
import com.example.ui.components.EmptyState
import com.example.ui.components.ErrorBanner
import com.example.ui.components.GlassPanelCard
import com.example.ui.components.GlassSegmentedSwitcher
import com.example.ui.components.LoadingButton
import com.example.ui.components.MemberAvatar
import com.example.ui.components.MoneyText
import com.example.ui.components.ProfileAvatarOrb
import com.example.ui.components.ResponsiveScreenContainer
import com.example.ui.components.SplitDecimalMoneyText
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberOnContainer
import com.example.ui.theme.CreditGreen
import com.example.ui.theme.CreditGreenLight
import com.example.ui.theme.DebtRed
import com.example.ui.theme.DebtRedLight
import com.example.ui.theme.Forest700
import com.example.ui.theme.Forest800
import com.example.ui.theme.Forest900
import com.example.ui.theme.HearthCardHolster
import com.example.ui.theme.HearthGlassBorder
import com.example.ui.theme.HearthGlassStrong
import com.example.ui.theme.HearthGlassSurface
import com.example.ui.theme.HearthTextDark
import com.example.ui.theme.HearthTextMuted
import com.example.ui.theme.Sage200
import com.example.ui.theme.Sage300
import com.example.ui.theme.ZeniOatMid
import com.example.ui.theme.ZeniSwitcherTrack
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyScreen(
    repository: HearthRepository,
    sessionManager: SessionManager,
    snackbarHostState: SnackbarHostState,
    onNavigateToProfile: () -> Unit = {}
) {
    val currentMember by sessionManager.currentMember.collectAsState()
    val members by repository.getMembers().collectAsState(initial = SessionManager.DEFAULT_MEMBERS)
    val balances by repository.getBalances().collectAsState(initial = emptyList())
    val expenses by repository.getExpenses().collectAsState(initial = emptyList())
    val settlements by repository.getSettlements().collectAsState(initial = emptyList())
    val suggestions by repository.getSmartSettleSuggestions().collectAsState(initial = emptyList())

    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf("Balances Table") }
    var showAddExpenseSheet by remember { mutableStateOf(false) }
    var showSettleUpSheet by remember { mutableStateOf(false) }

    // Settle Up sheet state
    var senderId by remember { mutableStateOf(currentMember.id) }
    var recipientId by remember { mutableStateOf("M01") }
    var settleAmountStr by remember { mutableStateOf("") }
    var settleError by remember { mutableStateOf<String?>(null) }
    var isSettling by remember { mutableStateOf(false) }

    // Add Expense sheet state
    var expTitle by remember { mutableStateOf("") }
    var expAmountStr by remember { mutableStateOf("") }
    var expPayerId by remember { mutableStateOf(currentMember.id) }
    var expCategory by remember { mutableStateOf(BudgetCategory.GROCERIES) }
    var expSplitType by remember { mutableStateOf(SplitType.EQUAL) }
    var expSelectedMembers by remember { mutableStateOf(setOf("M01", "M02", "M03")) }
    var customSplitInputs by remember { mutableStateOf(mapOf("M01" to "", "M02" to "", "M03" to "")) }
    var expIsRecurring by remember { mutableStateOf(false) }
    val todayIso = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    var expDateStr by remember { mutableStateOf(todayIso) }
    var addExpenseError by remember { mutableStateOf<String?>(null) }
    var isSavingExpense by remember { mutableStateOf(false) }

    val memberLookup = remember(members) { members.associateBy { it.id } }

    Box(modifier = Modifier.fillMaxSize()) {
        ResponsiveScreenContainer(
            scrollable = true,
            bottomSafePadding = 152.dp
        ) { _ ->
            // 1. Top Header Bar: Switcher + Settle Up Pill + Avatar Orb
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassSegmentedSwitcher(
                    options = listOf("Balances Table", "Activity"),
                    selectedOption = selectedTab,
                    onSelect = { selectedTab = it },
                    optionTestTags = mapOf(
                        "Balances Table" to "money_tab_balances",
                        "Activity" to "money_tab_activity"
                    )
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Forest900)
                            .clickable {
                                settleError = null
                                val firstDebtor = balances.firstOrNull { it.netPaisa < 0L }?.memberId ?: currentMember.id
                                val firstCreditor = balances.firstOrNull { it.netPaisa > 0L }?.memberId
                                    ?: members.firstOrNull { it.id != firstDebtor }?.id ?: "M01"
                                senderId = firstDebtor
                                recipientId = firstCreditor
                                val suggestedAmt = suggestions.firstOrNull()?.amountPaisa
                                settleAmountStr = if (suggestedAmt != null && suggestedAmt > 0L) {
                                    MoneyEngine.fromPaisa(suggestedAmt)
                                } else ""
                                showSettleUpSheet = true
                            }
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                            .testTag("open_settle_up_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Handshake,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Settle Up",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    ProfileAvatarOrb(
                        memberName = currentMember.name,
                        onClick = onNavigateToProfile
                    )
                }
            }

            // 2. Editorial Section Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "HOUSEHOLD LEDGER",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.4.sp,
                        color = Forest700
                    )
                    Text(
                        text = "Money & Settlements",
                        style = MaterialTheme.typography.headlineMedium,
                        color = HearthTextDark,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.78f))
                        .border(1.dp, HearthGlassBorder, CircleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${expenses.size} Entries",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Forest800
                    )
                }
            }

            if (selectedTab == "Balances Table") {
                // =====================================================================
                // TAB 0: LIVE NET BALANCE SUMMARY TABLE + SMART SETTLE SUGGESTIONS
                // =====================================================================
                GlassPanelCard(
                    cornerRadius = 28.dp,
                    containerColor = HearthGlassStrong,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("balances_table_card")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LIVE NET BALANCE SUMMARY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.1.sp,
                                color = HearthTextMuted
                            )
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(CreditGreenLight)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "PAISA EXACT",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = CreditGreen
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Table Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(HearthCardHolster)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Member",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = HearthTextMuted,
                                modifier = Modifier.weight(1.3f)
                            )
                            Text(
                                text = "Paid",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = HearthTextMuted,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "Owed",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = HearthTextMuted,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "Net",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = HearthTextMuted,
                                modifier = Modifier.weight(1.1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        balances.forEach { bal ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1.3f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MemberAvatar(
                                        name = bal.name,
                                        memberId = bal.memberId,
                                        size = 28.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = bal.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = HearthTextDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = bal.memberId,
                                            fontSize = 10.sp,
                                            color = HearthTextMuted
                                        )
                                    }
                                }

                                Text(
                                    text = MoneyEngine.fromPaisa(bal.paidPaisa),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = HearthTextDark,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = MoneyEngine.fromPaisa(bal.owedPaisa),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = HearthTextMuted,
                                    modifier = Modifier.weight(1f)
                                )

                                val netColor = when {
                                    bal.netPaisa > 0L -> CreditGreen
                                    bal.netPaisa < 0L -> DebtRed
                                    else -> HearthTextMuted
                                }
                                val netSignPrefix = if (bal.netPaisa > 0L) "+" else ""
                                Text(
                                    text = "$netSignPrefix${MoneyEngine.fromPaisa(bal.netPaisa)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = netColor,
                                    modifier = Modifier
                                        .weight(1.1f)
                                        .testTag("balance_net_${bal.memberId.lowercase()}")
                                )
                            }
                            HorizontalDivider(color = Sage200.copy(alpha = 0.6f))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val sumNet = balances.sumOf { it.netPaisa }
                        DarkForestHeroCard(
                            cornerRadius = 16.dp,
                            showDotMatrix = false,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total Net Balance: ${MoneyEngine.formatPkr(sumNet)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = if (sumNet == 0L) "Zero-Sum Verified ✓" else "Imbalance",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Sage300
                                )
                            }
                        }
                    }
                }

                // SMART SETTLE SUGGESTIONS (DEBT SIMPLIFICATION)
                GlassPanelCard(
                    cornerRadius = 26.dp,
                    containerColor = HearthGlassSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("smart_settle_card")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SMART SETTLE SUGGESTIONS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.1.sp,
                                    color = Forest800
                                )
                                Text(
                                    text = "Minimum transfers to clear all household debts",
                                    fontSize = 12.sp,
                                    color = HearthTextMuted
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (suggestions.isEmpty()) CreditGreenLight else AmberContainer)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (suggestions.isEmpty()) "All Clear" else "${suggestions.size} Transfers",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (suggestions.isEmpty()) CreditGreen else AmberOnContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (suggestions.isEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(CreditGreenLight.copy(alpha = 0.7f))
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = CreditGreen
                                )
                                Text(
                                    text = "All roommate balances are completely settled (PKR 0.00).",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CreditGreen
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                suggestions.forEach { s ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(Color.White.copy(alpha = 0.88f))
                                            .border(1.dp, HearthGlassBorder, RoundedCornerShape(18.dp))
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${s.fromName} pays ${s.toName}",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp,
                                                color = HearthTextDark
                                            )
                                            Text(
                                                text = MoneyEngine.formatPkr(s.amountPaisa),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = DebtRed
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(Forest800)
                                                .clickable {
                                                    coroutineScope.launch {
                                                        val res = repository.recordSettlement(
                                                            senderId = s.fromMemberId,
                                                            recipientId = s.toMemberId,
                                                            amountPaisa = s.amountPaisa,
                                                            recordedBy = currentMember.id
                                                        )
                                                        res.fold(
                                                            onSuccess = {
                                                                snackbarHostState.showSnackbar(
                                                                    "Settled: ${s.fromName} paid ${s.toName} ${MoneyEngine.formatPkr(s.amountPaisa)}"
                                                                )
                                                            },
                                                            onFailure = { err ->
                                                                snackbarHostState.showSnackbar(
                                                                    err.message ?: "Settlement failed"
                                                                )
                                                            }
                                                        )
                                                    }
                                                }
                                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                                .testTag("one_tap_settle_${s.fromMemberId.lowercase()}_${s.toMemberId.lowercase()}"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "One-Tap Settle",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Individual Member Cards with Exact PDF Page 4 Status Copy
                balances.forEach { bal ->
                    GlassPanelCard(
                        cornerRadius = 22.dp,
                        containerColor = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                MemberAvatar(
                                    name = bal.name,
                                    memberId = bal.memberId,
                                    size = 42.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = bal.name,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            color = HearthTextDark
                                        )
                                        if (bal.memberId == currentMember.id) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(Sage200)
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "YOU",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Forest900
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = MoneyEngine.formatNetStatusCopy(bal.netPaisa),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = when {
                                            bal.netPaisa > 0L -> CreditGreen
                                            bal.netPaisa < 0L -> DebtRed
                                            else -> HearthTextMuted
                                        }
                                    )
                                }
                            }
                            MoneyText(
                                paisa = bal.netPaisa,
                                isNetBalance = true,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            } else {
                // =====================================================================
                // TAB 1: ACTIVITY (SETTLEMENTS + SHARED EXPENSES)
                // =====================================================================
                if (settlements.isNotEmpty()) {
                    Text(
                        text = "RECENT SETTLEMENTS (${settlements.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.1.sp,
                        color = CreditGreen
                    )
                    settlements.forEach { st ->
                        val fromName = memberLookup[st.senderId]?.name ?: st.senderId
                        val toName = memberLookup[st.recipientId]?.name ?: st.recipientId
                        GlassPanelCard(
                            cornerRadius = 20.dp,
                            containerColor = CreditGreenLight.copy(alpha = 0.65f),
                            borderColor = CreditGreen.copy(alpha = 0.3f),
                            modifier = Modifier.fillMaxWidth()
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
                                        text = "$fromName paid $toName",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = CreditGreen,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Recorded by ${memberLookup[st.recordedBy]?.name ?: st.recordedBy}",
                                        fontSize = 12.sp,
                                        color = HearthTextMuted
                                    )
                                }
                                MoneyText(
                                    paisa = st.amountPaisa,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "SHARED EXPENSES (${expenses.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.1.sp,
                    color = HearthTextMuted
                )

                if (expenses.isEmpty()) {
                    EmptyState(
                        title = "No shared expenses yet",
                        message = "Tap '+ Add Expense' below to record a household bill or purchase.",
                        actionButtonText = "Add Expense",
                        onActionClick = { showAddExpenseSheet = true }
                    )
                } else {
                    expenses.forEach { exp ->
                        val payerName = memberLookup[exp.payerId]?.name ?: exp.payerId
                        val splitDetails = exp.splits.joinToString(", ") { s ->
                            val mName = memberLookup[s.memberId]?.name ?: s.memberId
                            "$mName: ${MoneyEngine.formatPkr(s.sharePaisa)}"
                        }
                        GlassPanelCard(
                            cornerRadius = 22.dp,
                            containerColor = Color.White.copy(alpha = 0.88f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = exp.title,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            color = HearthTextDark
                                        )
                                        if (exp.isRecurring) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(Sage200)
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "RECURRING",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Forest900
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Paid by $payerName • ${exp.category.displayName} • ${exp.splitType.name} • ${exp.expenseDate}",
                                        fontSize = 12.sp,
                                        color = HearthTextMuted
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Splits: $splitDetails",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = HearthTextMuted
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                MoneyText(
                                    paisa = exp.amountPaisa,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Add Expense Action Pill (above Zeni dock)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 92.dp)
        ) {
            ExtendedFloatingActionButton(
                onClick = {
                    addExpenseError = null
                    expPayerId = currentMember.id
                    showAddExpenseSheet = true
                },
                containerColor = Forest800,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("add_expense_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Expense", fontWeight = FontWeight.ExtraBold)
            }
        }
    }

    // =========================================================================
    // MODAL 1: ADD SHARED EXPENSE GLASSMORPHIC SHEET (.stitch/add_expense_glassmorphic)
    // =========================================================================
    if (showAddExpenseSheet) {
        val totalPaisa = remember(expAmountStr) {
            try {
                MoneyEngine.toPaisa(expAmountStr)
            } catch (_: Exception) {
                0L
            }
        }
        val sortedSelectedIds = remember(expSelectedMembers) { expSelectedMembers.toList().sorted() }
        val equalSplitsMap = remember(totalPaisa, sortedSelectedIds) {
            if (totalPaisa > 0L && sortedSelectedIds.isNotEmpty()) {
                MoneyEngine.splitEqual(totalPaisa, sortedSelectedIds)
            } else emptyMap()
        }
        val customEnteredSumPaisa = remember(customSplitInputs, sortedSelectedIds) {
            sortedSelectedIds.sumOf { id ->
                try {
                    MoneyEngine.toPaisa(customSplitInputs[id] ?: "0", requirePositive = false)
                } catch (_: Exception) {
                    0L
                }
            }
        }
        val customRemainingPaisa = totalPaisa - customEnteredSumPaisa

        ModalBottomSheet(
            onDismissRequest = { showAddExpenseSheet = false },
            containerColor = ZeniOatMid.copy(alpha = 0.98f),
            shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "HEARTH ROOMMATE LEDGER",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.4.sp,
                            color = Forest700
                        )
                        Text(
                            text = "Add Shared Expense",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = HearthTextDark
                        )
                    }
                    IconButton(
                        onClick = { showAddExpenseSheet = false },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.75f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = HearthTextDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (addExpenseError != null) {
                    ErrorBanner(message = addExpenseError!!)
                }

                // Amount Hero Glass Card
                GlassPanelCard(
                    cornerRadius = 24.dp,
                    containerColor = Color.White.copy(alpha = 0.88f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "TOTAL TRANSACTION VALUE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp,
                            color = HearthTextMuted
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        SplitDecimalMoneyText(
                            paisa = totalPaisa,
                            wholeFontSize = 34.sp,
                            decimalFontSize = 18.sp,
                            prefixFontSize = 14.sp,
                            wholeColor = HearthTextDark,
                            decimalColor = HearthTextMuted,
                            prefixColor = Forest800
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Sage200.copy(alpha = 0.7f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (expSplitType == SplitType.EQUAL) {
                                    "Equally distributed amongst ${sortedSelectedIds.size} flatmates"
                                } else {
                                    "Custom split allocation across ${sortedSelectedIds.size} flatmates"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Forest900
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = expAmountStr,
                            onValueChange = {
                                expAmountStr = it
                                addExpenseError = null
                            },
                            label = { Text("Enter Amount (PKR)") },
                            placeholder = { Text("e.g. 1500.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("expense_amount_input")
                        )
                    }
                }

                // Expense Title
                OutlinedTextField(
                    value = expTitle,
                    onValueChange = {
                        expTitle = it
                        addExpenseError = null
                    },
                    label = { Text("What was this expense for?") },
                    placeholder = { Text("e.g. Weekly Groceries, PTCL Fiber") },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_title_input")
                )

                // Category Horizontal Pills
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "BUDGET CATEGORY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp,
                        color = HearthTextMuted
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BudgetCategory.values().forEach { cat ->
                            val isSelected = expCategory == cat
                            Row(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isSelected) Forest800 else Color.White.copy(alpha = 0.85f))
                                    .border(
                                        1.dp,
                                        if (isSelected) Forest800 else HearthGlassBorder,
                                        CircleShape
                                    )
                                    .clickable { expCategory = cat }
                                    .padding(horizontal = 14.dp, vertical = 9.dp)
                                    .testTag("category_chip_${cat.name.lowercase()}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = categoryIconFor(cat),
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else HearthTextDark,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = cat.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else HearthTextDark
                                )
                            }
                        }
                    }
                }

                // Paid By 3-column Roommate Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "PAID BY ROOMMATE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp,
                        color = HearthTextMuted
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        members.forEach { m ->
                            val isSelected = expPayerId == m.id
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) Forest900 else Color.White.copy(alpha = 0.85f))
                                    .border(
                                        1.dp,
                                        if (isSelected) Forest900 else HearthGlassBorder,
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable { expPayerId = m.id }
                                    .padding(vertical = 10.dp)
                                    .testTag("payer_chip_${m.id.lowercase()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = m.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSelected) Color.White else HearthTextDark
                                    )
                                    Text(
                                        text = if (m.id == currentMember.id) "${m.id} (You)" else m.id,
                                        fontSize = 10.sp,
                                        color = if (isSelected) Sage300 else HearthTextMuted
                                    )
                                }
                            }
                        }
                    }
                }

                // Split Strategy Segmented Pill
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "SPLIT STRATEGY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp,
                        color = HearthTextMuted
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CircleShape)
                            .background(ZeniSwitcherTrack)
                            .padding(4.dp)
                    ) {
                        SplitType.values().forEach { st ->
                            val isSelected = expSplitType == st
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Forest800 else Color.Transparent)
                                    .clickable { expSplitType = st }
                                    .padding(vertical = 10.dp)
                                    .testTag("split_type_${st.name.lowercase()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (st == SplitType.EQUAL) {
                                        "Equal (1/${sortedSelectedIds.size.coerceAtLeast(1)} each)"
                                    } else {
                                        "Custom Split"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSelected) Color.White else HearthTextDark
                                )
                            }
                        }
                    }
                }

                // Calculated Settlement Breakdown Card
                GlassPanelCard(
                    cornerRadius = 22.dp,
                    containerColor = Color.White.copy(alpha = 0.88f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val isBalanced = if (expSplitType == SplitType.EQUAL) {
                            sortedSelectedIds.isNotEmpty()
                        } else {
                            totalPaisa > 0L && customRemainingPaisa == 0L
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CALCULATED SETTLEMENT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.1.sp,
                                color = HearthTextMuted
                            )
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isBalanced) CreditGreenLight else AmberContainer)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isBalanced) {
                                        "Zero-Sum Balanced"
                                    } else {
                                        "Diff: ${MoneyEngine.formatPkr(customRemainingPaisa)}"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isBalanced) CreditGreen else AmberOnContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        members.forEach { m ->
                            val isParticipating = expSelectedMembers.contains(m.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            expSelectedMembers = if (isParticipating) {
                                                expSelectedMembers - m.id
                                            } else {
                                                expSelectedMembers + m.id
                                            }
                                        }
                                ) {
                                    Checkbox(
                                        checked = isParticipating,
                                        onCheckedChange = { checked ->
                                            expSelectedMembers = if (checked) {
                                                expSelectedMembers + m.id
                                            } else {
                                                expSelectedMembers - m.id
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = Forest800)
                                    )
                                    Column {
                                        Text(
                                            text = "${m.name} (${m.id})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = HearthTextDark
                                        )
                                        if (expSplitType == SplitType.EQUAL && isParticipating) {
                                            val share = equalSplitsMap[m.id] ?: 0L
                                            Text(
                                                text = "Share: ${MoneyEngine.formatPkr(share)}",
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Forest800
                                            )
                                        }
                                    }
                                }

                                if (expSplitType == SplitType.CUSTOM && isParticipating) {
                                    OutlinedTextField(
                                        value = customSplitInputs[m.id] ?: "",
                                        onValueChange = { value ->
                                            customSplitInputs = customSplitInputs.toMutableMap().apply { put(m.id, value) }
                                        },
                                        label = { Text("PKR") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .width(120.dp)
                                            .testTag("custom_split_${m.id.lowercase()}")
                                    )
                                } else if (expSplitType == SplitType.EQUAL && isParticipating) {
                                    val share = equalSplitsMap[m.id] ?: 0L
                                    Text(
                                        text = MoneyEngine.formatPkr(share),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = HearthTextDark
                                    )
                                }
                            }
                        }

                        if (expSplitType == SplitType.EQUAL && totalPaisa > 0L && sortedSelectedIds.isNotEmpty() && totalPaisa % sortedSelectedIds.size != 0L) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Note: +1 paisa remainder assigned deterministically by Member ID (${sortedSelectedIds.first()}).",
                                fontSize = 11.sp,
                                color = Forest800,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Date Input & Recurring Monthly Bill Toggle
                OutlinedTextField(
                    value = expDateStr,
                    onValueChange = { expDateStr = it },
                    label = { Text("Expense Date (YYYY-MM-DD)") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_date_input")
                )

                GlassPanelCard(
                    cornerRadius = 18.dp,
                    containerColor = Color.White.copy(alpha = 0.82f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Monthly Recurring Bill",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = HearthTextDark
                            )
                            Text(
                                text = "Include in 1-tap monthly bill template generation",
                                fontSize = 11.sp,
                                color = HearthTextMuted
                            )
                        }
                        Switch(
                            checked = expIsRecurring,
                            onCheckedChange = { expIsRecurring = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Forest800
                            ),
                            modifier = Modifier.testTag("expense_recurring_switch")
                        )
                    }
                }

                LoadingButton(
                    text = "Confirm & Log Shared Expense",
                    onClick = {
                        val paisa = try {
                            MoneyEngine.toPaisa(expAmountStr)
                        } catch (e: Exception) {
                            addExpenseError = e.message ?: "Enter a valid positive amount in PKR."
                            return@LoadingButton
                        }

                        val customPaisaMap = if (expSplitType == SplitType.CUSTOM) {
                            val parsed = mutableMapOf<String, Long>()
                            for (mId in expSelectedMembers) {
                                val p = try {
                                    MoneyEngine.toPaisa(customSplitInputs[mId] ?: "0", requirePositive = false)
                                } catch (e: Exception) {
                                    addExpenseError = "Invalid custom amount for $mId"
                                    return@LoadingButton
                                }
                                parsed[mId] = p
                            }
                            parsed
                        } else emptyMap()

                        isSavingExpense = true
                        coroutineScope.launch {
                            val res = repository.addExpense(
                                title = expTitle,
                                category = expCategory,
                                amountPaisa = paisa,
                                payerId = expPayerId,
                                splitType = expSplitType,
                                customShares = customPaisaMap,
                                splitMemberIds = expSelectedMembers.toList(),
                                createdById = currentMember.id,
                                date = expDateStr.trim(),
                                isRecurring = expIsRecurring,
                                idempotencyKey = UUID.randomUUID().toString()
                            )
                            isSavingExpense = false
                            res.fold(
                                onSuccess = {
                                    expTitle = ""
                                    expAmountStr = ""
                                    customSplitInputs = mapOf("M01" to "", "M02" to "", "M03" to "")
                                    expIsRecurring = false
                                    addExpenseError = null
                                    showAddExpenseSheet = false
                                    snackbarHostState.showSnackbar("Shared expense logged!")
                                },
                                onFailure = { err ->
                                    addExpenseError = err.message ?: "Failed to save expense"
                                }
                            )
                        }
                    },
                    isLoading = isSavingExpense,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "submit_expense_button"
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // =========================================================================
    // MODAL 2: SETTLE UP DEBT GLASS SHEET
    // =========================================================================
    if (showSettleUpSheet) {
        val senderBal = balances.find { it.memberId == senderId }
        val recipientBal = balances.find { it.memberId == recipientId }
        val senderName = senderBal?.name ?: senderId
        val recipientName = recipientBal?.name ?: recipientId
        val senderDebt = maxOf(0L, -(senderBal?.netPaisa ?: 0L))
        val recipientCredit = maxOf(0L, recipientBal?.netPaisa ?: 0L)
        val maxSettlable = minOf(senderDebt, recipientCredit)

        ModalBottomSheet(
            onDismissRequest = { showSettleUpSheet = false },
            containerColor = ZeniOatMid.copy(alpha = 0.98f),
            shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "DEBT RESOLUTION ENGINE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.4.sp,
                            color = Forest700
                        )
                        Text(
                            text = "Settle Up Debt",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = HearthTextDark
                        )
                    }
                    IconButton(
                        onClick = { showSettleUpSheet = false },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.75f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = HearthTextDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Debtor selection
                Text(
                    text = "WHO IS PAYING? (DEBTOR)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                    color = HearthTextMuted
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    members.forEach { m ->
                        val isSelected = senderId == m.id
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) Forest900 else Color.White.copy(alpha = 0.85f))
                                .border(
                                    1.dp,
                                    if (isSelected) Forest900 else HearthGlassBorder,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    senderId = m.id
                                    settleError = null
                                }
                                .padding(vertical = 10.dp)
                                .testTag("settle_from_${m.id.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${m.name} (${m.id})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSelected) Color.White else HearthTextDark
                            )
                        }
                    }
                }

                // Creditor selection
                Text(
                    text = "PAYING TO WHOM? (CREDITOR)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                    color = HearthTextMuted
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    members.forEach { m ->
                        val isSelected = recipientId == m.id
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) Forest800 else Color.White.copy(alpha = 0.85f))
                                .border(
                                    1.dp,
                                    if (isSelected) Forest800 else HearthGlassBorder,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    recipientId = m.id
                                    settleError = null
                                }
                                .padding(vertical = 10.dp)
                                .testTag("settle_to_${m.id.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${m.name} (${m.id})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSelected) Color.White else HearthTextDark
                            )
                        }
                    }
                }

                // Helper Status Glass Card
                GlassPanelCard(
                    cornerRadius = 20.dp,
                    containerColor = Color.White.copy(alpha = 0.88f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = if (senderDebt > 0L) "$senderName owes ${MoneyEngine.formatPkr(senderDebt)} overall" else "$senderName has no debt",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = HearthTextDark
                        )
                        Text(
                            text = if (recipientCredit > 0L) "$recipientName is owed ${MoneyEngine.formatPkr(recipientCredit)}" else "$recipientName has no credit",
                            fontSize = 12.sp,
                            color = HearthTextMuted
                        )
                    }
                }

                Column {
                    OutlinedTextField(
                        value = settleAmountStr,
                        onValueChange = {
                            settleAmountStr = it
                            settleError = null
                        },
                        label = { Text("Amount to Settle (PKR)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settle_amount_input")
                    )

                    if (maxSettlable > 0L) {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = { settleAmountStr = MoneyEngine.fromPaisa(maxSettlable) },
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "Settle full amount (${MoneyEngine.formatPkr(maxSettlable)})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Forest800
                            )
                        }
                    }
                }

                if (settleError != null) {
                    ErrorBanner(message = settleError!!)
                }

                LoadingButton(
                    text = "Confirm Settlement Payment",
                    onClick = {
                        val amountPaisa = try {
                            MoneyEngine.toPaisa(settleAmountStr)
                        } catch (e: Exception) {
                            settleError = e.message ?: "Invalid amount"
                            return@LoadingButton
                        }

                        isSettling = true
                        coroutineScope.launch {
                            val res = repository.recordSettlement(
                                senderId = senderId,
                                recipientId = recipientId,
                                amountPaisa = amountPaisa,
                                recordedBy = currentMember.id
                            )
                            isSettling = false
                            if (res.isSuccess) {
                                snackbarHostState.showSnackbar("Settled: $senderName paid $recipientName ${MoneyEngine.formatPkr(amountPaisa)}")
                                showSettleUpSheet = false
                            } else {
                                settleError = res.exceptionOrNull()?.message ?: "Settlement failed"
                            }
                        }
                    },
                    isLoading = isSettling,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "confirm_settle_button"
                )

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

private fun categoryIconFor(category: BudgetCategory): ImageVector = when (category) {
    BudgetCategory.GROCERIES -> Icons.Default.ShoppingCart
    BudgetCategory.UTILITIES -> Icons.Default.Bolt
    BudgetCategory.CLEANING -> Icons.Default.CleaningServices
    BudgetCategory.ENTERTAINMENT -> Icons.Default.Movie
    BudgetCategory.DINING -> Icons.Default.Restaurant
    BudgetCategory.OTHER -> Icons.Default.Category
}
