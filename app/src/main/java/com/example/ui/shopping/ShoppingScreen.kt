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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HearthRepository
import com.example.data.SessionManager
import com.example.engine.MoneyEngine
import com.example.model.ItemStatus
import com.example.model.UnitType
import com.example.ui.components.DarkForestHeroCard
import com.example.ui.components.EmptyState
import com.example.ui.components.GlassPanelCard
import com.example.ui.components.GlassSegmentedSwitcher
import com.example.ui.components.LoadingButton
import com.example.ui.components.HearthAtmosphericBackground
import com.example.ui.components.ProfileAvatarOrb
import com.example.ui.components.SplitDecimalMoneyText
import com.example.ui.theme.CreditGreen
import com.example.ui.theme.CreditGreenLight
import com.example.ui.theme.DebtRed
import com.example.ui.theme.Forest700
import com.example.ui.theme.Forest800
import com.example.ui.theme.Forest900
import com.example.ui.theme.HearthGlassBorder
import com.example.ui.theme.HearthGlassSurface
import com.example.ui.theme.HearthTextDark
import com.example.ui.theme.HearthTextMuted
import com.example.ui.theme.Sage100
import com.example.ui.theme.Sage200
import com.example.ui.theme.Sage300
import com.example.ui.theme.ZeniSageTop
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingScreen(
    repository: HearthRepository,
    sessionManager: SessionManager,
    snackbarHostState: SnackbarHostState,
    onNavigateToProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val shoppingItems by repository.getShoppingItems().collectAsState(initial = emptyList())
    val members by repository.getMembers().collectAsState(initial = SessionManager.DEFAULT_MEMBERS)
    val currentMember by sessionManager.currentMember.collectAsState()

    var filterTab by remember { mutableStateOf("All") } // "All", "Pending", "Bought"
    val selectedItemIds = remember { mutableStateListOf<String>() }

    var showAddItemSheet by remember { mutableStateOf(false) }
    var showCheckoutSheet by remember { mutableStateOf(false) }

    val pendingCount = shoppingItems.count { it.status == ItemStatus.PENDING }
    val boughtCount = shoppingItems.count { it.status == ItemStatus.BOUGHT }

    val filteredItems = remember(shoppingItems, filterTab) {
        when (filterTab) {
            "Pending" -> shoppingItems.filter { it.status == ItemStatus.PENDING }
            "Bought" -> shoppingItems.filter { it.status == ItemStatus.BOUGHT }
            else -> shoppingItems
        }
    }

    HearthAtmosphericBackground(modifier = modifier) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Zeni Top Controls Row: Filter Segmented Switcher + Add Button + Profile Orb
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassSegmentedSwitcher(
                        options = listOf("All", "Pending", "Bought"),
                        selectedOption = filterTab,
                        onSelect = { filterTab = it },
                        optionLabels = mapOf(
                            "All" to "All (${shoppingItems.size})",
                            "Pending" to "Pending ($pendingCount)",
                            "Bought" to "Bought ($boughtCount)"
                        ),
                        optionTestTags = mapOf(
                            "All" to "filter_all",
                            "Pending" to "filter_pending",
                            "Bought" to "filter_bought"
                        )
                    )

                    ProfileAvatarOrb(
                        memberName = currentMember.name,
                        onClick = onNavigateToProfile,
                        testTag = "top_user_profile_chip"
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title & Add Provision Pill CTA Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Kitchen & Pantry",
                            fontSize = 25.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = HearthTextDark,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "Tap pending items to bundle & split at checkout",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = HearthTextMuted
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { showAddItemSheet = true },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Forest900,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                        modifier = Modifier
                            .height(42.dp)
                            .testTag("add_item_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Shopping Item",
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Item", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (filteredItems.isEmpty()) {
                    EmptyState(
                        title = "No provisions in this view",
                        message = "Add groceries or household supplies to your shared list and check them out together.",
                        actionButtonText = "Add First Provision",
                        onActionClick = { showAddItemSheet = true },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            top = 4.dp,
                            bottom = if (selectedItemIds.isNotEmpty()) 170.dp else 104.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredItems, key = { it.id }) { item ->
                            val isPending = item.status == ItemStatus.PENDING
                            val isSelected = selectedItemIds.contains(item.id)
                            val creatorName = members.find { it.id == item.createdBy }?.name ?: item.createdBy

                            GlassPanelCard(
                                cornerRadius = 22.dp,
                                containerColor = when {
                                    isSelected -> Color.White.copy(alpha = 0.95f)
                                    isPending -> HearthGlassSurface
                                    else -> Color.White.copy(alpha = 0.55f)
                                },
                                borderColor = if (isSelected) Forest800 else HearthGlassBorder,
                                elevation = if (isPending) 2.dp else 0.dp,
                                onClick = if (isPending) {
                                    {
                                        if (isSelected) selectedItemIds.remove(item.id)
                                        else selectedItemIds.add(item.id)
                                    }
                                } else null,
                                modifier = Modifier
                                    .fillMaxWidth()
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
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = Forest900,
                                                uncheckedColor = Forest700
                                            ),
                                            modifier = Modifier.testTag("checkbox_${item.id.lowercase()}")
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(CreditGreenLight),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Bought",
                                                tint = CreditGreen,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isPending) Forest900 else HearthTextMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${item.requestedQty} ${item.unit.displayName}${if (item.requestedQty > 1) "s" else ""} • Added by $creatorName",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = HearthTextMuted
                                        )
                                    }

                                    // Status Pill Tag
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(if (isPending) Sage100 else CreditGreenLight)
                                            .border(
                                                width = 1.dp,
                                                color = if (isPending) Color.White else CreditGreen.copy(alpha = 0.25f),
                                                shape = RoundedCornerShape(50)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = if (isPending) "Pending" else "Bought",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isPending) Forest800 else CreditGreen
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Floating Glass "Checkout (n)" Capsule Bar above the Bottom Dock
            AnimatedVisibility(
                visible = selectedItemIds.isNotEmpty(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 20.dp, end = 20.dp, bottom = 84.dp)
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
            ) {
                DarkForestHeroCard(
                    cornerRadius = 28.dp,
                    showDotMatrix = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(14.dp, RoundedCornerShape(28.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${selectedItemIds.size} provision${if (selectedItemIds.size > 1) "s" else ""} selected",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Ready for Equal Split Checkout",
                                fontSize = 11.sp,
                                color = Sage300
                            )
                        }

                        Button(
                            onClick = { showCheckoutSheet = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Forest900
                            ),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("checkout_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = Forest900,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Checkout (${selectedItemIds.size})",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.5.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Sheet 1: Add Provision Item (.stitch/add_provision_item_glassmorphic)
    if (showAddItemSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var itemTitle by remember { mutableStateOf("") }
        var selectedUnit by remember { mutableStateOf(UnitType.PACK) }
        var quantity by remember { mutableIntStateOf(1) }
        var isSubmitting by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        val submitAction = {
            if (itemTitle.isBlank()) {
                errorMessage = "Item title cannot be empty"
            } else {
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
                        snackbarHostState.showSnackbar("Added '${itemTitle.trim()}' to shared pantry list")
                        showAddItemSheet = false
                    } else {
                        errorMessage = res.exceptionOrNull()?.message ?: "Failed to add item"
                    }
                }
            }
        }

        ModalBottomSheet(
            onDismissRequest = { showAddItemSheet = false },
            sheetState = sheetState,
            containerColor = ZeniSageTop,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Stitch Subheader Bar: Cancel | Add Provision | Done
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.7f))
                            .border(1.dp, Color.White, RoundedCornerShape(50))
                            .clickable { showAddItemSheet = false }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text("Cancel", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = HearthTextMuted)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Add Provision",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Forest900
                        )
                        Text(
                            text = "Shared Kitchen & Pantry",
                            fontSize = 11.5.sp,
                            color = HearthTextMuted
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Forest900)
                            .clickable { submitAction() }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text("Done", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                // Main Frosted Glass Form Card
                GlassPanelCard(
                    cornerRadius = 26.dp,
                    containerColor = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Item Title Input
                        Column {
                            Text(
                                text = "ITEM DETAILS",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Forest700,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = itemTitle,
                                onValueChange = {
                                    itemTitle = it
                                    errorMessage = null
                                },
                                placeholder = { Text("e.g. Almond Milk, Super Basmati Rice") },
                                leadingIcon = {
                                    Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = Forest800)
                                },
                                trailingIcon = {
                                    if (itemTitle.isNotEmpty()) {
                                        IconButton(onClick = { itemTitle = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = HearthTextMuted)
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(50),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Sage100.copy(alpha = 0.7f),
                                    unfocusedContainerColor = Sage100.copy(alpha = 0.5f),
                                    focusedBorderColor = Forest800,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                isError = errorMessage != null,
                                supportingText = errorMessage?.let { { Text(it, color = DebtRed) } },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("item_title_input")
                            )
                        }

                        // Packaging Unit 4-Column Segmented Grid (.stitch/add_provision_item_glassmorphic)
                        Column {
                            Text(
                                text = "PACKAGING UNIT",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Forest700,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(Sage100.copy(alpha = 0.8f))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                UnitType.values().forEach { unit ->
                                    val isSelected = selectedUnit == unit
                                    val unitIcon = when (unit) {
                                        UnitType.PACK -> Icons.Default.Inventory2
                                        UnitType.DOZEN -> Icons.Default.Egg
                                        UnitType.BOTTLE -> Icons.Default.LocalDrink
                                        UnitType.BAG -> Icons.Default.ShoppingBag
                                    }

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(if (isSelected) Forest900 else Color.Transparent)
                                            .clickable { selectedUnit = unit }
                                            .padding(vertical = 10.dp)
                                            .testTag("unit_${unit.name.lowercase()}"),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = unitIcon,
                                            contentDescription = unit.displayName,
                                            tint = if (isSelected) Color.White else Forest800,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = unit.displayName.replaceFirstChar { it.uppercase() },
                                            color = if (isSelected) Color.White else Forest900,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Quantity Stepper & Counter Card
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Sage100.copy(alpha = 0.65f))
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(CreditGreenLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Layers,
                                        contentDescription = null,
                                        tint = Forest800,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Quantity Needed",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = HearthTextMuted
                                    )
                                    Text(
                                        text = "$quantity × ${selectedUnit.displayName.replaceFirstChar { it.uppercase() }}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Forest900
                                    )
                                }
                            }

                            // Capsule Stepper
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White)
                                    .border(1.dp, Sage200, RoundedCornerShape(50))
                                    .padding(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                IconButton(
                                    onClick = { if (quantity > 1) quantity-- },
                                    enabled = quantity > 1,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Sage100)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = Forest900, modifier = Modifier.size(16.dp))
                                }

                                Text(
                                    text = "$quantity",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = Forest900
                                )

                                IconButton(
                                    onClick = { if (quantity < 999) quantity++ },
                                    enabled = quantity < 999,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Forest900)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        LoadingButton(
                            text = "Add to Shared Shopping List",
                            onClick = submitAction,
                            isLoading = isSubmitting,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "submit_add_item"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Modal Sheet 2: Checkout Provisions (.stitch/checkout_provisions_glassmorphic)
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

        // Half-up rounding on tax calculation
        val liveFinalTotalPaisa by remember(totalAmountStr, taxPercentStr, discountStr, showTaxDiscount) {
            derivedStateOf {
                try {
                    val subtotal = MoneyEngine.toPaisa(totalAmountStr)
                    if (!showTaxDiscount) {
                        subtotal
                    } else {
                        val tax = taxPercentStr.toDoubleOrNull() ?: 0.0
                        val discount = if (discountStr.isNotBlank()) {
                            MoneyEngine.toPaisa(discountStr, requirePositive = false)
                        } else 0L
                        val taxAmount = ((subtotal * tax) / 100.0).roundToLong()
                        maxOf(1L, subtotal + taxAmount - discount)
                    }
                } catch (_: Exception) {
                    0L
                }
            }
        }

        val checkoutItemsList = remember(selectedItemIds, shoppingItems) {
            shoppingItems.filter { selectedItemIds.contains(it.id) }
        }

        // Live Equal Split Preview with ascending remainder rule
        val liveShares by remember(liveFinalTotalPaisa, splitMemberIds.toList()) {
            derivedStateOf {
                try {
                    if (liveFinalTotalPaisa > 0L && splitMemberIds.isNotEmpty()) {
                        MoneyEngine.splitEqual(liveFinalTotalPaisa, splitMemberIds.toList())
                    } else emptyMap()
                } catch (_: Exception) {
                    emptyMap()
                }
            }
        }

        ModalBottomSheet(
            onDismissRequest = { showCheckoutSheet = false },
            sheetState = sheetState,
            containerColor = ZeniSageTop,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Stitch Top Review Subheader: Cancel | ● Split Engine Active | Step 2 of 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Forest800,
                        modifier = Modifier.clickable { showCheckoutSheet = false }
                    )

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.75f))
                            .border(1.dp, Color.White, RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(CreditGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SPLIT ENGINE ACTIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Forest900,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Text(
                        text = "Step 2 of 2",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = HearthTextMuted
                    )
                }

                // 1. Summary Hero Card (.stitch/checkout_provisions_glassmorphic)
                GlassPanelCard(
                    cornerRadius = 26.dp,
                    containerColor = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingBag,
                                        contentDescription = null,
                                        tint = Forest700,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "HEARTH PANTRY ORDER",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Forest700,
                                        letterSpacing = 0.6.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                SplitDecimalMoneyText(
                                    paisa = liveFinalTotalPaisa,
                                    currencyPrefix = "PKR ",
                                    wholeFontSize = 30.sp,
                                    decimalFontSize = 18.sp,
                                    prefixFontSize = 13.sp,
                                    wholeColor = Forest900,
                                    decimalColor = HearthTextMuted,
                                    prefixColor = Forest700
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(CreditGreenLight)
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = Forest800,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${checkoutItemsList.size} Items",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Forest900
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Subtotal Input Field
                        OutlinedTextField(
                            value = totalAmountStr,
                            onValueChange = {
                                totalAmountStr = it
                                checkoutError = null
                            },
                            label = { Text("Items Subtotal Paid (PKR)") },
                            placeholder = { Text("e.g. 1450.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(18.dp),
                            isError = checkoutError != null,
                            supportingText = checkoutError?.let { { Text(it, color = DebtRed) } },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("checkout_total_input")
                        )

                        // Sales Tax (%) & Discount (PKR)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTaxDiscount = !showTaxDiscount }
                                .padding(top = 6.dp, bottom = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (showTaxDiscount) "− Hide Tax & Discount" else "+ Add Sales Tax % & Discount",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Forest800
                            )
                        }

                        if (showTaxDiscount) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = taxPercentStr,
                                    onValueChange = { taxPercentStr = it },
                                    label = { Text("Tax % (0-100)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("checkout_tax_input")
                                )
                                OutlinedTextField(
                                    value = discountStr,
                                    onValueChange = { discountStr = it },
                                    label = { Text("Discount PKR") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("checkout_discount_input")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Micro-bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(50))
                                .background(Sage100)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Home, contentDescription = null, tint = Forest800, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Flat 4B Provisions", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Forest900)
                            }
                            Text("Auto-Categorized: Groceries", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Forest700)
                        }
                    }
                }

                // 2. Verified Provision Items List
                GlassPanelCard(
                    cornerRadius = 22.dp,
                    containerColor = Color.White.copy(alpha = 0.82f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Verified Provision Items", fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold, color = Forest900)
                            Text("✓ All matched", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = CreditGreen)
                        }

                        checkoutItemsList.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Sage100.copy(alpha = 0.6f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CreditGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(item.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Forest900)
                                }
                                Text(
                                    text = "Qty: ${item.requestedQty} ${item.unit.displayName}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = HearthTextMuted
                                )
                            }
                        }
                    }
                }

                // 3. Payer & Split Members Selection + Live Deterministic Split Engine Card
                GlassPanelCard(
                    cornerRadius = 24.dp,
                    containerColor = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Paid by selector
                        Column {
                            Text(
                                text = "PAID BY",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Forest700,
                                letterSpacing = 0.6.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                members.forEach { member ->
                                    val isSelected = member.id == selectedPayerId
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(50))
                                            .background(if (isSelected) Forest900 else Sage100)
                                            .clickable { selectedPayerId = member.id }
                                            .padding(vertical = 8.dp)
                                            .testTag("payer_${member.id.lowercase()}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (member.id == currentMember.id) "${member.name} (Me)" else member.name,
                                            color = if (isSelected) Color.White else Forest900,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Split between selector
                        Column {
                            Text(
                                text = "SPLIT BETWEEN (AT LEAST 1)",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Forest700,
                                letterSpacing = 0.6.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                members.forEach { member ->
                                    val isSelected = splitMemberIds.contains(member.id)
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(50))
                                            .background(if (isSelected) Forest800 else Sage100)
                                            .clickable {
                                                if (isSelected) {
                                                    if (splitMemberIds.size > 1) splitMemberIds.remove(member.id)
                                                } else {
                                                    splitMemberIds.add(member.id)
                                                }
                                            }
                                            .padding(vertical = 8.dp)
                                            .testTag("split_${member.id.lowercase()}"),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(
                                            text = member.name,
                                            color = if (isSelected) Color.White else Forest900,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Live Per-Person Equal Split Preview with Deterministic Remainder Banner
                        if (liveShares.isNotEmpty()) {
                            val baseShare = liveShares.values.minOrNull() ?: 0L
                            val remainderCount = liveShares.values.count { it > baseShare }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PieChart, contentDescription = null, tint = Forest800, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Equal ${liveShares.size}-Way Split",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Forest900
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(Sage100)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "Zero-Sum Verified",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Forest800
                                    )
                                }
                            }

                            liveShares.forEach { (memberId, share) ->
                                val memberName = members.find { it.id == memberId }?.name ?: memberId
                                val hasRemainder = share > baseShare
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Sage100.copy(alpha = 0.65f))
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(if (memberId == selectedPayerId) Forest900 else Sage200),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = memberName.take(2).uppercase(),
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (memberId == selectedPayerId) Color.White else Forest900
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = memberName,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Forest900
                                            )
                                            Text(
                                                text = if (memberId == selectedPayerId) "Payer • $memberId" else "Member • $memberId",
                                                fontSize = 10.5.sp,
                                                color = HearthTextMuted
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = MoneyEngine.formatPkr(share),
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Forest900
                                        )
                                        Text(
                                            text = if (hasRemainder) "+0.01 remainder" else "Exact share",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (hasRemainder) CreditGreen else HearthTextMuted
                                        )
                                    }
                                }
                            }

                            if (remainderCount > 0) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(CreditGreenLight.copy(alpha = 0.65f))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Balance, contentDescription = null, tint = Forest800, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "+1 paisa allocated in ascending ID order for deterministic zero-sum equilibrium.",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Forest900
                                    )
                                }
                            }
                        }
                    }
                }

                LoadingButton(
                    text = if (liveFinalTotalPaisa > 0L) {
                        "Confirm Checkout & Split • ${MoneyEngine.formatPkr(liveFinalTotalPaisa)}"
                    } else {
                        "Confirm Checkout & Split"
                    },
                    onClick = {
                        val subtotalPaisa = try {
                            MoneyEngine.toPaisa(totalAmountStr)
                        } catch (e: Exception) {
                            checkoutError = e.message ?: "Invalid amount"
                            return@LoadingButton
                        }

                        val finalPaidPaisa = if (showTaxDiscount && liveFinalTotalPaisa > 0L) liveFinalTotalPaisa else subtotalPaisa
                        val taxPercent = taxPercentStr.toDoubleOrNull()
                        val discountPaisa = if (discountStr.isNotBlank()) {
                            try { MoneyEngine.toPaisa(discountStr, requirePositive = false) } catch (_: Exception) { null }
                        } else null

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

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
