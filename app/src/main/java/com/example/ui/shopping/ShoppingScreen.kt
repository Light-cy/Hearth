package com.example.ui.shopping

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import com.example.model.ItemStatus
import com.example.model.ShoppingItem
import com.example.model.UnitType
import com.example.ui.components.EmptyState
import com.example.ui.components.LoadingButton
import com.example.ui.components.MemberAvatar
import com.example.ui.theme.CreditGreen
import com.example.ui.theme.CreditGreenLight
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate300
import com.example.ui.theme.TealPrimary
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingScreen(
    repository: HisaabRepository,
    sessionManager: SessionManager,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val shoppingItems by repository.getShoppingItems().collectAsState(initial = emptyList())
    val members by repository.getMembers().collectAsState(initial = SessionManager.DEFAULT_MEMBERS)
    val currentMember by sessionManager.currentMember.collectAsState()

    var filterStatus by remember { mutableStateOf<ItemStatus?>(null) } // null = All
    val selectedItemIds = remember { mutableStateListOf<String>() }

    var showAddItemSheet by remember { mutableStateOf(false) }
    var showCheckoutSheet by remember { mutableStateOf(false) }

    val filteredItems = remember(shoppingItems, filterStatus) {
        when (filterStatus) {
            null -> shoppingItems
            ItemStatus.PENDING -> shoppingItems.filter { it.status == ItemStatus.PENDING }
            ItemStatus.BOUGHT -> shoppingItems.filter { it.status == ItemStatus.BOUGHT }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header & Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterStatus == null,
                    onClick = { filterStatus = null },
                    label = { Text("All (${shoppingItems.size})") },
                    modifier = Modifier.testTag("filter_all")
                )
                FilterChip(
                    selected = filterStatus == ItemStatus.PENDING,
                    onClick = { filterStatus = ItemStatus.PENDING },
                    label = { Text("Pending (${shoppingItems.count { it.status == ItemStatus.PENDING }})") },
                    modifier = Modifier.testTag("filter_pending")
                )
                FilterChip(
                    selected = filterStatus == ItemStatus.BOUGHT,
                    onClick = { filterStatus = ItemStatus.BOUGHT },
                    label = { Text("Bought (${shoppingItems.count { it.status == ItemStatus.BOUGHT }})") },
                    modifier = Modifier.testTag("filter_bought")
                )
            }

            if (filteredItems.isEmpty()) {
                EmptyState(
                    title = "No shopping items",
                    message = "Add items to your shared roommate shopping list and check them out together.",
                    actionButtonText = "Add First Item",
                    onActionClick = { showAddItemSheet = true },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        val isPending = item.status == ItemStatus.PENDING
                        val isSelected = selectedItemIds.contains(item.id)

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isPending) MaterialTheme.colorScheme.surface else Slate100.copy(alpha = 0.6f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isPending) 2.dp else 0.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = isPending) {
                                    if (isSelected) selectedItemIds.remove(item.id)
                                    else selectedItemIds.add(item.id)
                                }
                                .testTag("shopping_item_${item.id.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isPending) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            if (checked) selectedItemIds.add(item.id)
                                            else selectedItemIds.remove(item.id)
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = TealPrimary),
                                        modifier = Modifier.testTag("checkbox_${item.id.lowercase()}")
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Bought",
                                        tint = CreditGreen,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPending) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${item.requestedQty} ${item.unit.displayName}${if (item.requestedQty > 1) "s" else ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Status tag
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isPending) Slate100 else CreditGreenLight)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (isPending) "Pending" else "Bought",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPending) MaterialTheme.colorScheme.onSurfaceVariant else CreditGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button to Add Item
        FloatingActionButton(
            onClick = { showAddItemSheet = true },
            containerColor = TealPrimary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = if (selectedItemIds.isNotEmpty()) 80.dp else 16.dp)
                .testTag("add_item_fab")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Shopping Item")
        }

        // Sticky "Checkout (n)" Bar
        AnimatedVisibility(
            visible = selectedItemIds.isNotEmpty(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedItemIds.size} item${if (selectedItemIds.size > 1) "s" else ""} selected",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    Button(
                        onClick = { showCheckoutSheet = true },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("checkout_button")
                    ) {
                        Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Checkout (${selectedItemIds.size})")
                    }
                }
            }
        }
    }

    // Modal Sheet: Add Shopping Item
    if (showAddItemSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var itemTitle by remember { mutableStateOf("") }
        var selectedUnit by remember { mutableStateOf(UnitType.PACK) }
        var quantity by remember { mutableIntStateOf(1) }
        var isSubmitting by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        ModalBottomSheet(
            onDismissRequest = { showAddItemSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Add Shopping Item",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = itemTitle,
                    onValueChange = {
                        itemTitle = it
                        errorMessage = null
                    },
                    label = { Text("Item Title (e.g. Fresh Milk, Bread)") },
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("item_title_input")
                )

                // Unit selection chips (pack, dozen, bottle, bag)
                Column {
                    Text(
                        text = "Unit",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        UnitType.values().forEach { unit ->
                            val isSelected = selectedUnit == unit
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) TealPrimary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { selectedUnit = unit }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .testTag("unit_${unit.name.lowercase()}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = unit.displayName,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                // Quantity stepper (1 - 999)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quantity",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = { if (quantity > 1) quantity-- },
                            enabled = quantity > 1,
                            modifier = Modifier
                                .border(1.dp, Slate300, CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease")
                        }

                        Text(
                            text = "$quantity",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )

                        IconButton(
                            onClick = { if (quantity < 999) quantity++ },
                            enabled = quantity < 999,
                            modifier = Modifier
                                .border(1.dp, Slate300, CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase")
                        }
                    }
                }

                LoadingButton(
                    text = "Add to Shopping List",
                    onClick = {
                        if (itemTitle.isBlank()) {
                            errorMessage = "Item title cannot be empty"
                            return@LoadingButton
                        }
                        isSubmitting = true
                        coroutineScope.launch {
                            val res = repository.addShoppingItem(
                                title = itemTitle,
                                unit = selectedUnit,
                                qty = quantity,
                                createdBy = currentMember.id
                            )
                            isSubmitting = false
                            if (res.isSuccess) {
                                snackbarHostState.showSnackbar("Added '${itemTitle.trim()}' to shopping list")
                                showAddItemSheet = false
                            } else {
                                errorMessage = res.exceptionOrNull()?.message ?: "Failed to add item"
                            }
                        }
                    },
                    isLoading = isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "submit_add_item"
                )

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    // Modal Sheet: Checkout Selected Items
    if (showCheckoutSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var totalAmountStr by remember { mutableStateOf("") }
        var selectedPayerId by remember { mutableStateOf(currentMember.id) }
        val splitMemberIds = remember { mutableStateListOf(*members.map { it.id }.toTypedArray()) }
        var isCheckingOut by remember { mutableStateOf(false) }
        var checkoutError by remember { mutableStateOf<String?>(null) }
        val idempotencyKey = remember { UUID.randomUUID().toString() }

        var showTaxDiscount by remember { mutableStateOf(false) }
        var taxPercentStr by remember { mutableStateOf("") }
        var discountStr by remember { mutableStateOf("") }

        val liveFinalTotalPaisa by remember(totalAmountStr, taxPercentStr, discountStr) {
            derivedStateOf {
                try {
                    val subtotal = MoneyEngine.toPaisa(totalAmountStr)
                    val tax = taxPercentStr.toDoubleOrNull() ?: 0.0
                    val discount = if (discountStr.isNotBlank()) MoneyEngine.toPaisa(discountStr, requirePositive = false) else 0L
                    val taxAmount = (subtotal * tax / 100.0).toLong()
                    maxOf(1L, subtotal + taxAmount - discount)
                } catch (_: Exception) {
                    0L
                }
            }
        }

        val checkoutItemsList = remember(selectedItemIds, shoppingItems) {
            shoppingItems.filter { selectedItemIds.contains(it.id) }
        }

        // Live Equal Split Preview with ascending remainder rule
        val liveShares by remember(totalAmountStr, taxPercentStr, discountStr, splitMemberIds.toList()) {
            derivedStateOf {
                try {
                    val paisa = if (showTaxDiscount && liveFinalTotalPaisa > 0L) liveFinalTotalPaisa else MoneyEngine.toPaisa(totalAmountStr)
                    if (splitMemberIds.isNotEmpty()) {
                        MoneyEngine.splitEqual(paisa, splitMemberIds.toList())
                    } else emptyMap()
                } catch (e: Exception) {
                    emptyMap()
                }
            }
        }

        ModalBottomSheet(
            onDismissRequest = { showCheckoutSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Checkout Items",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // Items summary
                Text(
                    text = "Purchasing: " + checkoutItemsList.joinToString(", ") { "${it.title} (${it.requestedQty} ${it.unit.displayName})" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Total Amount Input
                OutlinedTextField(
                    value = totalAmountStr,
                    onValueChange = {
                        totalAmountStr = it
                        checkoutError = null
                    },
                    label = { Text("Items Subtotal (PKR)") },
                    placeholder = { Text("e.g. 1200.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = checkoutError != null,
                    supportingText = checkoutError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("checkout_total_input")
                )

                // Surprise Option 2: Tax & Discount Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTaxDiscount = !showTaxDiscount }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showTaxDiscount) "− Hide Tax & Discount" else "+ Add Tax & Discount (Option 2)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealPrimary
                    )
                }

                if (showTaxDiscount) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = taxPercentStr,
                            onValueChange = { taxPercentStr = it },
                            label = { Text("Tax % (0-100)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = discountStr,
                            onValueChange = { discountStr = it },
                            label = { Text("Discount PKR") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (liveFinalTotalPaisa > 0L) {
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Slate100),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Final Net Total:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(
                                    text = MoneyEngine.formatPkr(liveFinalTotalPaisa),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = TealPrimary
                                )
                            }
                        }
                    }
                }

                // Payer selection (default: me)
                Column {
                    Text(
                        text = "Paid by",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        members.forEach { member ->
                            val isSelected = member.id == selectedPayerId
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) TealPrimary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { selectedPayerId = member.id }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("payer_${member.id.lowercase()}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (member.id == currentMember.id) "${member.name} (Me)" else member.name,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // Split Between selection
                Column {
                    Text(
                        text = "Split between (at least 1)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        members.forEach { member ->
                            val isSelected = splitMemberIds.contains(member.id)
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) TealPrimary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        if (isSelected) {
                                            if (splitMemberIds.size > 1) splitMemberIds.remove(member.id)
                                        } else {
                                            splitMemberIds.add(member.id)
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("split_${member.id.lowercase()}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = member.name,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // Live Per-Person Preview
                if (liveShares.isNotEmpty()) {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Slate100),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Split Breakdown (Deterministic Paisa Allocation)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            liveShares.forEach { (memberId, share) ->
                                val memberName = members.find { it.id == memberId }?.name ?: memberId
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = memberName, fontSize = 13.sp)
                                    Text(
                                        text = MoneyEngine.formatPkr(share),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TealPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                LoadingButton(
                    text = "Confirm Checkout & Split",
                    onClick = {
                        val subtotalPaisa = try {
                            MoneyEngine.toPaisa(totalAmountStr)
                        } catch (e: Exception) {
                            checkoutError = e.message ?: "Invalid amount"
                            return@LoadingButton
                        }

                        val finalPaidPaisa = if (showTaxDiscount && liveFinalTotalPaisa > 0L) liveFinalTotalPaisa else subtotalPaisa
                        val taxPercent = taxPercentStr.toDoubleOrNull()
                        val discountPaisa = if (discountStr.isNotBlank()) try { MoneyEngine.toPaisa(discountStr, requirePositive = false) } catch (_: Exception) { null } else null

                        isCheckingOut = true
                        coroutineScope.launch {
                            val res = repository.checkoutItems(
                                itemIds = selectedItemIds.toList(),
                                totalPaidPaisa = finalPaidPaisa,
                                payerId = selectedPayerId,
                                splitMembers = splitMemberIds.toList(),
                                createdById = currentMember.id,
                                taxPercent = taxPercent,
                                discountPaisa = discountPaisa,
                                subtotalPaisa = subtotalPaisa,
                                idempotencyKey = idempotencyKey
                            )
                            isCheckingOut = false
                            if (res.isSuccess) {
                                snackbarHostState.showSnackbar("Checked out ${selectedItemIds.size} items. Expense recorded!")
                                selectedItemIds.clear()
                                showCheckoutSheet = false
                            } else {
                                checkoutError = res.exceptionOrNull()?.message ?: "Checkout failed"
                            }
                        }
                    },
                    isLoading = isCheckingOut,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "confirm_checkout_button"
                )

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}
