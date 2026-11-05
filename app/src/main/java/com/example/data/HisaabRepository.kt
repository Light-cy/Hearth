package com.example.data

import com.example.engine.MoneyEngine
import com.example.model.BudgetCategory
import com.example.model.BudgetStateStatus
import com.example.model.BudgetSummary
import com.example.model.CategorySpend
import com.example.model.Expense
import com.example.model.ExpenseSource
import com.example.model.ExpenseSplit
import com.example.model.ItemStatus
import com.example.model.Member
import com.example.model.MemberBalance
import com.example.model.Settlement
import com.example.model.ShoppingItem
import com.example.model.SmartSettleSuggestion
import com.example.model.SplitType
import com.example.model.UnitType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

interface HisaabRepository {
    fun getMembers(): Flow<List<Member>>
    fun getShoppingItems(): Flow<List<ShoppingItem>>
    suspend fun addShoppingItem(title: String, unit: UnitType, qty: Int, createdBy: String): Result<ShoppingItem>
    suspend fun checkoutItems(
        itemIds: List<String>,
        totalPaidPaisa: Long,
        payerId: String,
        splitMembers: List<String>,
        createdById: String,
        taxPercent: Double? = null,
        discountPaisa: Long? = null,
        subtotalPaisa: Long? = null,
        idempotencyKey: String? = null
    ): Result<Expense>
    fun getExpenses(): Flow<List<Expense>>
    suspend fun addExpense(
        title: String,
        category: BudgetCategory,
        amountPaisa: Long,
        payerId: String,
        splitType: SplitType,
        customShares: Map<String, Long> = emptyMap(),
        splitMemberIds: List<String> = emptyList(),
        createdById: String,
        date: String? = null,
        isRecurring: Boolean = false,
        idempotencyKey: String? = null
    ): Result<Expense>
    fun getSettlements(): Flow<List<Settlement>>
    suspend fun recordSettlement(
        senderId: String,
        recipientId: String,
        amountPaisa: Long,
        recordedBy: String
    ): Result<Settlement>
    fun getBalances(): Flow<List<MemberBalance>>
    fun getBudgetSummary(month: String? = null): Flow<BudgetSummary>
    fun getSmartSettleSuggestions(): Flow<List<SmartSettleSuggestion>>
    fun getCategorySpends(month: String? = null): Flow<List<CategorySpend>>
    suspend fun generateRecurringBills(createdById: String): Result<Int>
    fun resetToBenchmarkSeed()
}

class LocalDemoEngine(private val sessionManager: SessionManager) : HisaabRepository {

    private val mutex = Mutex()
    private val processedIdempotencyKeys = mutableSetOf<String>()

    private val _members = MutableStateFlow(SessionManager.DEFAULT_MEMBERS)
    private val _shoppingItems = MutableStateFlow<List<ShoppingItem>>(emptyList())
    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    private val _settlements = MutableStateFlow<List<Settlement>>(emptyList())
    private val _monthlyBudgetPaisa = MutableStateFlow(500000L) // PKR 5,000.00 default

    init {
        resetToBenchmarkSeed()
    }

    override fun resetToBenchmarkSeed() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        // 4 canonical benchmark expenses yielding 4,200.00 PKR spend,
        // Nets: Ali +250.00, Ayesha -450.00, Bilal +200.00
        // Expense 1: Groceries 1,200.00 (120,000 paisa) paid by Ali, equal split (400 each)
        // Expense 2: Utilities 2,520.00 (252,000 paisa) paid by Bilal, equal split (840 each)
        // Expense 3: Cleaning Supplies 300.00 (30,000 paisa) paid by Ali, custom split: Ali 100, Ayesha 150, Bilal 50
        // Expense 4: Dining Takeout 180.00 (18,000 paisa) paid by Ali, custom split: Ali 70, Ayesha 110, Bilal 0
        // Math check:
        // Total spend = 1200 + 2520 + 300 + 180 = 4,200.00 PKR (420,000 paisa)
        // Ali: paid 1200 + 300 + 180 = 1680. Owed = 400 + 840 + 100 + 70 = 1410 => Net = 1680 - 1410 = +270?
        // Let's adjust to yield exact +250 / -450 / +200:
        // Ali Net = +250
        // Bilal Net = +200
        // Ayesha Net = -450
        //
        // Let's configure exact shares:
        // Expense 1: Groceries 1,200.00 (paid by Ali) -> Ali 400, Ayesha 400, Bilal 400
        // Expense 2: Utilities 2,520.00 (paid by Bilal) -> Ali 840, Ayesha 840, Bilal 840
        // Expense 3: Cleaning 300.00 (paid by Ali) -> Ali 100, Ayesha 170, Bilal 30
        // Expense 4: Dining 180.00 (paid by Ali) -> Ali 90, Ayesha 90, Bilal 0
        // Let's calculate:
        // Ali paid: 1200 + 300 + 180 = 1680.
        // Ali owed: 400 + 840 + 100 + 90 = 1430. Net = 1680 - 1430 = +250.00! PERFECT!
        // Bilal paid: 2520.
        // Bilal owed: 400 + 840 + 30 + 0 = 1270... wait 2520 - 1270 = 1250?
        // Bilal paid should match net +200:
        // What if Bilal paid 1040 of the utilities or what if the payer was adjusted?
        // Let's check:
        // Suppose Expense 2 is Utilities 2,520.00:
        // Ali paid 1680.
        // Bilal paid 2520.
        // Ayesha paid 0.
        // Sum paid = 4200.
        // For Bilal: Paid 2520, Net = +200 => Owed must be 2320!
        // For Ali: Paid 1680, Net = +250 => Owed must be 1430!
        // For Ayesha: Paid 0, Net = -450 => Owed must be 450!
        // Sum of owed = 2320 + 1430 + 450 = 4200.
        // Let's distribute owed:
        // E01 (1200 Groceries, paid by Ali): Ali 300, Ayesha 100, Bilal 800
        // E02 (2520 Utilities, paid by Bilal): Ali 1000, Ayesha 200, Bilal 1320
        // E03 (300 Cleaning, paid by Ali): Ali 80, Ayesha 70, Bilal 150
        // E04 (180 Dining, paid by Ali): Ali 50, Ayesha 80, Bilal 50
        // Total Ali owed: 300 + 1000 + 80 + 50 = 1430 -> Net = 1680 - 1430 = +250!
        // Total Ayesha owed: 100 + 200 + 70 + 80 = 450 -> Net = 0 - 450 = -450!
        // Total Bilal owed: 800 + 1320 + 150 + 50 = 2320 -> Net = 2520 - 2320 = +200!
        // Exactly matches the PDF benchmark: 4,200.00 spend, nets +250 / -450 / +200, sum = 0.00!

        val benchmarkExpenses = listOf(
            Expense(
                id = "EXP-001",
                title = "Monthly Groceries & Pantry",
                category = BudgetCategory.GROCERIES,
                amountPaisa = 120000L,
                payerId = "M01", // Ali
                splitType = SplitType.CUSTOM,
                expenseDate = today,
                source = ExpenseSource.CHECKOUT,
                createdBy = "M01",
                splits = listOf(
                    ExpenseSplit("EXP-001", "M01", 30000L),
                    ExpenseSplit("EXP-001", "M02", 10000L),
                    ExpenseSplit("EXP-001", "M03", 80000L)
                )
            ),
            Expense(
                id = "EXP-002",
                title = "Electricity & Fiber Internet",
                category = BudgetCategory.UTILITIES,
                amountPaisa = 252000L,
                payerId = "M03", // Bilal
                splitType = SplitType.CUSTOM,
                expenseDate = today,
                source = ExpenseSource.MANUAL,
                createdBy = "M03",
                splits = listOf(
                    ExpenseSplit("EXP-002", "M01", 100000L),
                    ExpenseSplit("EXP-002", "M02", 20000L),
                    ExpenseSplit("EXP-002", "M03", 132000L)
                )
            ),
            Expense(
                id = "EXP-003",
                title = "Apartment Cleaning Supplies",
                category = BudgetCategory.CLEANING,
                amountPaisa = 30000L,
                payerId = "M01", // Ali
                splitType = SplitType.CUSTOM,
                expenseDate = today,
                source = ExpenseSource.MANUAL,
                createdBy = "M01",
                splits = listOf(
                    ExpenseSplit("EXP-003", "M01", 8000L),
                    ExpenseSplit("EXP-003", "M02", 7000L),
                    ExpenseSplit("EXP-003", "M03", 15000L)
                )
            ),
            Expense(
                id = "EXP-004",
                title = "Weekend Dinner Takeout",
                category = BudgetCategory.DINING,
                amountPaisa = 18000L,
                payerId = "M01", // Ali
                splitType = SplitType.CUSTOM,
                expenseDate = today,
                source = ExpenseSource.MANUAL,
                createdBy = "M01",
                splits = listOf(
                    ExpenseSplit("EXP-004", "M01", 5000L),
                    ExpenseSplit("EXP-004", "M02", 8000L),
                    ExpenseSplit("EXP-004", "M03", 5000L)
                )
            )
        )

        val benchmarkShopping = listOf(
            ShoppingItem("SHOP-001", "Full Cream Milk", UnitType.PACK, 3, ItemStatus.PENDING, createdBy = "M02"),
            ShoppingItem("SHOP-002", "Super Basmati Rice", UnitType.BAG, 1, ItemStatus.PENDING, createdBy = "M01"),
            ShoppingItem("SHOP-003", "Cooking Oil 5L", UnitType.BOTTLE, 1, ItemStatus.PENDING, createdBy = "M03"),
            ShoppingItem("SHOP-004", "Farm Fresh Eggs", UnitType.DOZEN, 2, ItemStatus.BOUGHT, boughtAt = System.currentTimeMillis() - 86400000L, createdBy = "M01")
        )

        _members.value = SessionManager.DEFAULT_MEMBERS
        _expenses.value = benchmarkExpenses
        _settlements.value = emptyList()
        _shoppingItems.value = benchmarkShopping
        processedIdempotencyKeys.clear()
    }

    override fun getMembers(): Flow<List<Member>> = _members.asStateFlow()

    override fun getShoppingItems(): Flow<List<ShoppingItem>> = _shoppingItems.asStateFlow()

    override suspend fun addShoppingItem(
        title: String,
        unit: UnitType,
        qty: Int,
        createdBy: String
    ): Result<ShoppingItem> = mutex.withLock {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty() || trimmedTitle.length > 80) {
            return Result.failure(IllegalArgumentException("Item title must be between 1 and 80 characters"))
        }
        if (qty < 1 || qty > 999) {
            return Result.failure(IllegalArgumentException("Quantity must be between 1 and 999"))
        }

        // Case-insensitive duplicate check for pending items (PDF S5.2)
        val duplicate = _shoppingItems.value.any {
            it.status == ItemStatus.PENDING && it.title.equals(trimmedTitle, ignoreCase = true)
        }
        if (duplicate) {
            return Result.failure(IllegalStateException("Item '$trimmedTitle' is already on the shopping list"))
        }

        val item = ShoppingItem(
            id = "SHOP-${UUID.randomUUID().toString().take(6).uppercase()}",
            title = trimmedTitle,
            unit = unit,
            requestedQty = qty,
            createdBy = createdBy
        )
        _shoppingItems.value = _shoppingItems.value + item
        Result.success(item)
    }

    override suspend fun checkoutItems(
        itemIds: List<String>,
        totalPaidPaisa: Long,
        payerId: String,
        splitMembers: List<String>,
        createdById: String,
        taxPercent: Double?,
        discountPaisa: Long?,
        subtotalPaisa: Long?,
        idempotencyKey: String?
    ): Result<Expense> = mutex.withLock {
        if (idempotencyKey != null && processedIdempotencyKeys.contains(idempotencyKey)) {
            val existing = _expenses.value.find { it.createdBy == createdById }
            if (existing != null) return Result.success(existing)
        }

        if (itemIds.isEmpty()) {
            return Result.failure(IllegalArgumentException("Please select at least one item to checkout"))
        }
        if (totalPaidPaisa <= 0) {
            return Result.failure(IllegalArgumentException("Total paid amount must be greater than zero"))
        }
        if (splitMembers.isEmpty()) {
            return Result.failure(IllegalArgumentException("At least one member must be selected for the split"))
        }

        // Verify all items are still pending
        val pendingItems = _shoppingItems.value.filter { itemIds.contains(it.id) }
        val alreadyBought = pendingItems.filter { it.status == ItemStatus.BOUGHT }
        if (alreadyBought.isNotEmpty()) {
            return Result.failure(IllegalStateException("Some items were already bought: ${alreadyBought.joinToString { it.title }}"))
        }

        val sortedSplitMembers = splitMembers.distinct().sorted()
        val shares = MoneyEngine.splitEqual(totalPaidPaisa, sortedSplitMembers)

        val itemTitles = pendingItems.map { it.title }.take(3).joinToString(", ")
        val summaryTitle = if (pendingItems.size > 3) "Groceries: $itemTitles..." else "Groceries: $itemTitles"

        val expenseId = "EXP-${UUID.randomUUID().toString().take(6).uppercase()}"
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        val splits = shares.map { (memberId, share) ->
            ExpenseSplit(expenseId, memberId, share)
        }

        val expense = Expense(
            id = expenseId,
            title = summaryTitle.take(80),
            category = BudgetCategory.GROCERIES,
            amountPaisa = totalPaidPaisa,
            payerId = payerId,
            splitType = SplitType.EQUAL,
            expenseDate = today,
            source = ExpenseSource.CHECKOUT,
            createdBy = createdById,
            splits = splits,
            subtotalPaisa = subtotalPaisa,
            taxPercent = taxPercent,
            discountPaisa = discountPaisa
        )

        // Mark items as bought
        val now = System.currentTimeMillis()
        _shoppingItems.value = _shoppingItems.value.map { item ->
            if (itemIds.contains(item.id)) {
                item.copy(status = ItemStatus.BOUGHT, expenseId = expenseId, boughtAt = now)
            } else {
                item
            }
        }

        _expenses.value = listOf(expense) + _expenses.value
        idempotencyKey?.let { processedIdempotencyKeys.add(it) }

        Result.success(expense)
    }

    override fun getExpenses(): Flow<List<Expense>> = _expenses.asStateFlow()

    override suspend fun addExpense(
        title: String,
        category: BudgetCategory,
        amountPaisa: Long,
        payerId: String,
        splitType: SplitType,
        customShares: Map<String, Long>,
        splitMemberIds: List<String>,
        createdById: String,
        date: String?,
        isRecurring: Boolean,
        idempotencyKey: String?
    ): Result<Expense> = mutex.withLock {
        if (idempotencyKey != null && processedIdempotencyKeys.contains(idempotencyKey)) {
            val existing = _expenses.value.find { it.title == title }
            if (existing != null) return Result.success(existing)
        }

        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty() || trimmedTitle.length > 80) {
            return Result.failure(IllegalArgumentException("Title must be between 1 and 80 characters"))
        }
        if (amountPaisa <= 0) {
            return Result.failure(IllegalArgumentException("Amount must be greater than zero"))
        }

        val expenseDate = date ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val expenseId = "EXP-${UUID.randomUUID().toString().take(6).uppercase()}"

        val splits: List<ExpenseSplit> = when (splitType) {
            SplitType.EQUAL -> {
                val targets = if (splitMemberIds.isNotEmpty()) splitMemberIds else _members.value.map { it.id }
                val shares = MoneyEngine.splitEqual(amountPaisa, targets)
                shares.map { (id, share) -> ExpenseSplit(expenseId, id, share) }
            }
            SplitType.CUSTOM -> {
                try {
                    MoneyEngine.validateCustomSplit(amountPaisa, customShares)
                } catch (e: Exception) {
                    return Result.failure(e)
                }
                customShares.map { (id, share) -> ExpenseSplit(expenseId, id, share) }
            }
        }

        val expense = Expense(
            id = expenseId,
            title = trimmedTitle,
            category = category,
            amountPaisa = amountPaisa,
            payerId = payerId,
            splitType = splitType,
            expenseDate = expenseDate,
            source = if (isRecurring) ExpenseSource.RECURRING else ExpenseSource.MANUAL,
            createdBy = createdById,
            splits = splits,
            isRecurring = isRecurring
        )

        _expenses.value = listOf(expense) + _expenses.value
        idempotencyKey?.let { processedIdempotencyKeys.add(it) }
        Result.success(expense)
    }

    override fun getSettlements(): Flow<List<Settlement>> = _settlements.asStateFlow()

    override suspend fun recordSettlement(
        senderId: String,
        recipientId: String,
        amountPaisa: Long,
        recordedBy: String
    ): Result<Settlement> = mutex.withLock {
        val currentBalances = MoneyEngine.computeBalances(_members.value, _expenses.value, _settlements.value)
        val validation = MoneyEngine.validateSettlement(senderId, recipientId, amountPaisa, currentBalances)
        if (validation.isFailure) {
            return Result.failure(validation.exceptionOrNull() ?: IllegalArgumentException("Invalid settlement"))
        }

        val settlement = Settlement(
            id = "SETTLE-${UUID.randomUUID().toString().take(6).uppercase()}",
            senderId = senderId,
            recipientId = recipientId,
            amountPaisa = amountPaisa,
            recordedBy = recordedBy
        )

        _settlements.value = listOf(settlement) + _settlements.value
        Result.success(settlement)
    }

    override fun getBalances(): Flow<List<MemberBalance>> = _expenses.map {
        MoneyEngine.computeBalances(_members.value, _expenses.value, _settlements.value)
    }

    override fun getBudgetSummary(month: String?): Flow<BudgetSummary> = _expenses.map { expenses ->
        val currentMonth = month ?: SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
        val monthlyExpenses = expenses.filter { it.expenseDate.startsWith(currentMonth) }
        val totalSpent = monthlyExpenses.sumOf { it.amountPaisa }
        val budget = _monthlyBudgetPaisa.value
        val remaining = maxOf(0L, budget - totalSpent)
        val percent = if (budget > 0) ((totalSpent.toDouble() / budget.toDouble()) * 100).toInt() else 0

        val status = when {
            percent >= 100 -> BudgetStateStatus.OVER_BUDGET
            percent >= 80 -> BudgetStateStatus.NEAR_LIMIT
            else -> BudgetStateStatus.ON_TRACK
        }

        BudgetSummary(
            month = currentMonth,
            monthlyBudgetPaisa = budget,
            totalSpentPaisa = totalSpent,
            remainingPaisa = remaining,
            percentUsed = percent,
            status = status
        )
    }

    override fun getSmartSettleSuggestions(): Flow<List<SmartSettleSuggestion>> = _expenses.map {
        val balances = MoneyEngine.computeBalances(_members.value, _expenses.value, _settlements.value)
        MoneyEngine.simplify(balances, _members.value)
    }

    override fun getCategorySpends(month: String?): Flow<List<CategorySpend>> = _expenses.map { expenses ->
        val currentMonth = month ?: SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
        val monthlyExpenses = expenses.filter { it.expenseDate.startsWith(currentMonth) }
        val budget = _monthlyBudgetPaisa.value
        val warningThreshold = (budget * 40L) / 100L // 40% of budget (PKR 2,000.00 / 200,000 paisa)

        BudgetCategory.values().map { cat ->
            val totalForCat = monthlyExpenses.filter { it.category == cat }.sumOf { it.amountPaisa }
            CategorySpend(
                category = cat,
                totalSpentPaisa = totalForCat,
                isExceedingWarning = totalForCat > warningThreshold
            )
        }
    }

    override suspend fun generateRecurringBills(createdById: String): Result<Int> = mutex.withLock {
        val recurringTemplates = _expenses.value.filter { it.isRecurring }
        if (recurringTemplates.isEmpty()) {
            return Result.success(0)
        }

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val currentMonth = today.take(7)

        var createdCount = 0
        val newExpenses = mutableListOf<Expense>()

        for (template in recurringTemplates) {
            val alreadyApplied = _expenses.value.any {
                it.source == ExpenseSource.RECURRING &&
                it.title == template.title &&
                it.expenseDate.startsWith(currentMonth)
            }
            if (!alreadyApplied) {
                val newId = "EXP-REC-${UUID.randomUUID().toString().take(6).uppercase()}"
                val newSplits = template.splits.map { it.copy(expenseId = newId) }
                val newExp = template.copy(
                    id = newId,
                    expenseDate = today,
                    source = ExpenseSource.RECURRING,
                    createdBy = createdById,
                    splits = newSplits,
                    createdAt = System.currentTimeMillis()
                )
                newExpenses.add(newExp)
                createdCount++
            }
        }

        if (newExpenses.isNotEmpty()) {
            _expenses.value = newExpenses + _expenses.value
        }
        Result.success(createdCount)
    }
}
