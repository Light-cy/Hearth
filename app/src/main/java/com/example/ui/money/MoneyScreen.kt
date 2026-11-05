package com.example.ui.money

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HisaabRepository
import com.example.data.SessionManager
import com.example.engine.MoneyEngine
import com.example.model.BudgetCategory
import com.example.model.Expense
import com.example.model.Settlement
import com.example.model.SplitType
import com.example.ui.components.LoadingButton
import com.example.ui.components.MemberAvatar
import com.example.ui.components.MoneyText
import com.example.ui.theme.CreditGreen
import com.example.ui.theme.CreditGreenLight
import com.example.ui.theme.DebtRed
import com.example.ui.theme.DebtRedLight
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate300
import com.example.ui.theme.TealPrimary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyScreen(
    repository: HisaabRepository,
    sessionManager: SessionManager,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val members by repository.getMembers().collectAsState(initial = SessionManager.DEFAULT_MEMBERS)
    val balances by repository.getBalances().collectAsState(initial = emptyList())
    val expenses by repository.getExpenses().collectAsState(initial = emptyList())
    val settlements by repository.getSettlements().collectAsState(initial = emptyList())
    val currentMember by sessionManager.currentMember.collectAsState()

    var showAddExpenseSheet by remember { mutableStateOf(false) }
    var showSettleUpSheet by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Balances & Settle, 1 = Recent Activity

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Screen Title & Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Money & Splits",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = TealPrimary
                )

                Button(
                    onClick = { showSettleUpSheet = true },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("open_settle_up_button")
                ) {
                    Icon(imageVector = Icons.Default.Handshake, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Settle Up")
                }
            }

            // Tabs: Balances vs Activity
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Balances Table", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Activity (${expenses.size + settlements.size})", fontWeight = FontWeight.Bold) }
                )
            }

            if (selectedTab == 0) {
                // (a) Balance Table Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("balances_table_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "LIVE NET BALANCE SUMMARY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Table Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Member", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1.4f))
                            Text("Paid", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            Text("Owed", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            Text("Net", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        }

                        HorizontalDivider(color = Slate100, thickness = 1.dp)

                        // Member Balance Rows
                        balances.forEach { b ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1.4f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MemberAvatar(name = b.name, memberId = b.memberId, size = 26.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = b.name,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }

                                Text(
                                    text = MoneyEngine.fromPaisa(b.paidPaisa),
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = MoneyEngine.fromPaisa(b.owedPaisa),
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f)
                                )

                                val netColor = when {
                                    b.netPaisa > 0 -> CreditGreen
                                    b.netPaisa < 0 -> DebtRed
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                                Text(
                                    text = MoneyEngine.fromPaisa(b.netPaisa),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = netColor,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            HorizontalDivider(color = Slate100.copy(alpha = 0.5f), thickness = 0.5.dp)
                        }

                        // Required: Total row showing PKR 0.00
                        val totalNet = balances.sumOf { it.netPaisa }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total Net Balance",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(3.4f)
                            )
                            Text(
                                text = MoneyEngine.formatPkr(totalNet),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = if (totalNet == 0L) TealPrimary else DebtRed,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            } else {
                // (d) Recent Activity List (Expenses & Settlements)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Settlements
                    settlements.forEach { s ->
                        val sender = members.find { it.id == s.senderId }?.name ?: s.senderId
                        val recipient = members.find { it.id == s.recipientId }?.name ?: s.recipientId
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = CreditGreenLight.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Handshake,
                                        contentDescription = null,
                                        tint = CreditGreen,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "$sender paid $recipient",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "Settlement Payment",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                MoneyText(
                                    paisa = s.amountPaisa,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Expenses
                    expenses.forEach { exp ->
                        val payer = members.find { it.id == exp.payerId }?.name ?: exp.payerId
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Receipt,
                                        contentDescription = null,
                                        tint = TealPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = exp.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "${exp.category.displayName} • Paid by $payer • ${exp.expenseDate}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                MoneyText(
                                    paisa = exp.amountPaisa,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(70.dp))
        }

        // Floating Action Button: Add Expense
        FloatingActionButton(
            onClick = { showAddExpenseSheet = true },
            containerColor = TealPrimary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_expense_fab")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Expense")
        }
    }

    // Modal Sheet: Add Expense
    if (showAddExpenseSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var title by remember { mutableStateOf("") }
        var amountStr by remember { mutableStateOf("") }
        var selectedCategory by remember { mutableStateOf(BudgetCategory.GROCERIES) }
        var selectedPayerId by remember { mutableStateOf(currentMember.id) }
        var splitType by remember { mutableStateOf(SplitType.EQUAL) }
        var categoryExpanded by remember { mutableStateOf(false) }
        var isRecurring by remember { mutableStateOf(false) }

        // Custom shares map: memberId -> string amount
        val customSharesInput = remember {
            mutableStateMapOf<String, String>().apply {
                members.forEach { put(it.id, "") }
            }
        }

        var isSubmitting by remember { mutableStateOf(false) }
        var errorMsg by remember { mutableStateOf<String?>(null) }
        val idempotencyKey = remember { UUID.randomUUID().toString() }

        // Running custom remaining check
        val runningCustomPaisaSum by remember(amountStr, customSharesInput.toMap()) {
            derivedStateOf {
                var total = 0L
                for ((_, str) in customSharesInput) {
                    if (str.isNotBlank()) {
                        try {
                            total += MoneyEngine.toPaisa(str, requirePositive = false)
                        } catch (_: Exception) {}
                    }
                }
                total
            }
        }

        ModalBottomSheet(
            onDismissRequest = { showAddExpenseSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Add Shared Expense",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; errorMsg = null },
                    label = { Text("Expense Title (e.g. WiFi Bill, Groceries)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_title_input")
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it; errorMsg = null },
                    label = { Text("Total Amount (PKR)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_amount_input")
                )

                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        BudgetCategory.values().forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.displayName) },
                                onClick = {
                                    selectedCategory = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                // Paid By Selector
                Column {
                    Text("Paid By", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        members.forEach { m ->
                            val isSelected = m.id == selectedPayerId
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) TealPrimary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { selectedPayerId = m.id }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (m.id == currentMember.id) "${m.name} (Me)" else m.name,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // Equal vs Custom Toggle
                Column {
                    Text("Split Strategy", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (splitType == SplitType.EQUAL) TealPrimary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { splitType = SplitType.EQUAL }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text("Equal Split", color = if (splitType == SplitType.EQUAL) Color.White else MaterialTheme.colorScheme.onSurface)
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (splitType == SplitType.CUSTOM) TealPrimary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { splitType = SplitType.CUSTOM }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text("Custom Split", color = if (splitType == SplitType.CUSTOM) Color.White else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                // Custom split fields if Custom is selected
                if (splitType == SplitType.CUSTOM) {
                    val targetPaisa = try { MoneyEngine.toPaisa(amountStr) } catch (_: Exception) { 0L }
                    val remainingPaisa = targetPaisa - runningCustomPaisaSum

                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Slate100),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Custom Member Shares", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(
                                    text = if (remainingPaisa == 0L) "Exact match!" else "Remaining: ${MoneyEngine.formatPkr(remainingPaisa)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (remainingPaisa == 0L) CreditGreen else DebtRed
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            members.forEach { m ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(m.name, modifier = Modifier.weight(1f), fontSize = 13.sp)
                                    OutlinedTextField(
                                        value = customSharesInput[m.id] ?: "",
                                        onValueChange = { customSharesInput[m.id] = it },
                                        placeholder = { Text("0.00") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier
                                            .width(120.dp)
                                            .height(52.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isRecurring = !isRecurring }
                        .padding(vertical = 4.dp)
                ) {
                    androidx.compose.material3.Checkbox(
                        checked = isRecurring,
                        onCheckedChange = { isRecurring = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mark as Monthly Recurring Bill (Option 4)", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                if (errorMsg != null) {
                    Text(text = errorMsg!!, color = DebtRed, fontSize = 13.sp)
                }

                LoadingButton(
                    text = "Save Shared Expense",
                    onClick = {
                        val totalPaisa = try {
                            MoneyEngine.toPaisa(amountStr)
                        } catch (e: Exception) {
                            errorMsg = e.message ?: "Invalid amount"
                            return@LoadingButton
                        }

                        val sharesMap = mutableMapOf<String, Long>()
                        if (splitType == SplitType.CUSTOM) {
                            try {
                                for (m in members) {
                                    val str = customSharesInput[m.id] ?: ""
                                    val paisa = if (str.isBlank()) 0L else MoneyEngine.toPaisa(str, requirePositive = false)
                                    sharesMap[m.id] = paisa
                                }
                                MoneyEngine.validateCustomSplit(totalPaisa, sharesMap)
                            } catch (e: Exception) {
                                errorMsg = e.message ?: "Custom shares mismatch"
                                return@LoadingButton
                            }
                        }

                        isSubmitting = true
                        coroutineScope.launch {
                            val res = repository.addExpense(
                                title = title,
                                category = selectedCategory,
                                amountPaisa = totalPaisa,
                                payerId = selectedPayerId,
                                splitType = splitType,
                                customShares = sharesMap,
                                splitMemberIds = members.map { it.id },
                                createdById = currentMember.id,
                                isRecurring = isRecurring,
                                idempotencyKey = idempotencyKey
                            )
                            isSubmitting = false
                            if (res.isSuccess) {
                                snackbarHostState.showSnackbar("Recorded: $title (${MoneyEngine.formatPkr(totalPaisa)})")
                                showAddExpenseSheet = false
                            } else {
                                errorMsg = res.exceptionOrNull()?.message ?: "Failed to record expense"
                            }
                        }
                    },
                    isLoading = isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "submit_expense_button"
                )

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    // Modal Sheet: Settle Up Dialog
    if (showSettleUpSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var senderId by remember { mutableStateOf(currentMember.id) }
        var recipientId by remember {
            mutableStateOf(members.firstOrNull { it.id != currentMember.id }?.id ?: "M02")
        }
        var settleAmountStr by remember { mutableStateOf("") }
        var isSettling by remember { mutableStateOf(false) }
        var settleError by remember { mutableStateOf<String?>(null) }

        val senderBalance = balances.find { it.memberId == senderId }
        val recipientBalance = balances.find { it.memberId == recipientId }

        val senderDebt = maxOf(0L, -(senderBalance?.netPaisa ?: 0L))
        val recipientCredit = maxOf(0L, recipientBalance?.netPaisa ?: 0L)
        val maxSettlable = minOf(senderDebt, recipientCredit)

        ModalBottomSheet(
            onDismissRequest = { showSettleUpSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Settle Up Debt",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // Sender picker
                Column {
                    Text("Who is paying?", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        members.forEach { m ->
                            val isSelected = m.id == senderId
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) TealPrimary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        senderId = m.id
                                        if (recipientId == m.id) {
                                            recipientId = members.firstOrNull { it.id != m.id }?.id ?: ""
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (m.id == currentMember.id) "${m.name} (Me)" else m.name,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // Recipient picker
                Column {
                    Text("Paying to whom?", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        members.filter { it.id != senderId }.forEach { m ->
                            val isSelected = m.id == recipientId
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) TealPrimary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { recipientId = m.id }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = m.name,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // Helper text showing current debt
                val senderName = members.find { it.id == senderId }?.name ?: senderId
                val recipientName = members.find { it.id == recipientId }?.name ?: recipientId

                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate100),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (senderDebt > 0) "$senderName owes ${MoneyEngine.formatPkr(senderDebt)} overall" else "$senderName has no debt",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (recipientCredit > 0) "$recipientName is owed ${MoneyEngine.formatPkr(recipientCredit)}" else "$recipientName has no credit",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Amount input + "Settle full amount" shortcut
                Column {
                    OutlinedTextField(
                        value = settleAmountStr,
                        onValueChange = {
                            settleAmountStr = it
                            settleError = null
                        },
                        label = { Text("Amount to Settle (PKR)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settle_amount_input")
                    )

                    if (maxSettlable > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = {
                                settleAmountStr = MoneyEngine.fromPaisa(maxSettlable)
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Settle full amount (${MoneyEngine.formatPkr(maxSettlable)})", fontSize = 12.sp)
                        }
                    }
                }

                if (settleError != null) {
                    Text(text = settleError!!, color = DebtRed, fontSize = 13.sp)
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

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}
