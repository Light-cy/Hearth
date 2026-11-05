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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

interface HearthRepository {
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
    suspend fun updateMonthlyBudget(newBudgetPaisa: Long): Result<Long> {
        return Result.success(newBudgetPaisa)
    }
    fun restoreDefaultLedger()
}

class LocalVaultEngine(private val sessionManager: SessionManager) : HearthRepository {

    private val mutex = Mutex()
    private val processedIdempotencyKeys = mutableSetOf<String>()

    private val _shoppingItems = MutableStateFlow<List<ShoppingItem>>(emptyList())
    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    private val _settlements = MutableStateFlow<List<Settlement>>(emptyList())
    private val _monthlyBudgetPaisa = MutableStateFlow(500000L) // PKR 5,000.00 default ceiling

    init {
        restoreDefaultLedger()
    }

    override fun restoreDefaultLedger() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        val initialExpenses = listOf(
            Expense(
                id = "EXP-001",
                title = "Grocery Trip (Milk & Rice)",
                category = BudgetCategory.GROCERIES,
                amountPaisa = 78000L,
                payerId = "M01", // Ali
                splitType = SplitType.EQUAL,
                expenseDate = today,
                source = ExpenseSource.CHECKOUT,
                createdBy = "M01",
                splits = listOf(
                    ExpenseSplit("EXP-001", "M01", 26000L),
                    ExpenseSplit("EXP-001", "M02", 26000L),
                    ExpenseSplit("EXP-001", "M03", 26000L)
                )
            ),
            Expense(
                id = "EXP-002",
                title = "Cleaning Supplies",
                category = BudgetCategory.CLEANING,
                amountPaisa = 90000L,
                payerId = "M02", // Ayesha
                splitType = SplitType.EQUAL,
                expenseDate = today,
                source = ExpenseSource.MANUAL,
                createdBy = "M02",
                splits = listOf(
                    ExpenseSplit("EXP-002", "M01", 45000L),
                    ExpenseSplit("EXP-002", "M02", 45000L)
                )
            ),
            Expense(
                id = "EXP-003",
                title = "Internet Bill",
                category = BudgetCategory.UTILITIES,
                amountPaisa = 150000L,
                payerId = "M03", // Bilal
                splitType = SplitType.CUSTOM,
                expenseDate = today,
                source = ExpenseSource.MANUAL,
                createdBy = "M03",
                isRecurring = true,
                splits = listOf(
                    ExpenseSplit("EXP-003", "M01", 50000L),
                    ExpenseSplit("EXP-003", "M02", 30000L),
                    ExpenseSplit("EXP-003", "M03", 70000L)
                )
            ),
            Expense(
                id = "EXP-004",
                title = "Electricity Bill",
                category = BudgetCategory.UTILITIES,
                amountPaisa = 102000L,
                payerId = "M01", // Ali
                splitType = SplitType.EQUAL,
                expenseDate = today,
                source = ExpenseSource.MANUAL,
                createdBy = "M01",
                splits = listOf(
                    ExpenseSplit("EXP-004", "M01", 34000L),
                    ExpenseSplit("EXP-004", "M02", 34000L),
                    ExpenseSplit("EXP-004", "M03", 34000L)
                )
            )
        )

        val initialShopping = listOf(
            ShoppingItem("SHOP-001", "Full Cream Milk", UnitType.PACK, 3, ItemStatus.PENDING, createdBy = "M02"),
            ShoppingItem("SHOP-002", "Super Basmati Rice", UnitType.BAG, 1, ItemStatus.PENDING, createdBy = "M01"),
            ShoppingItem("SHOP-003", "Cooking Oil 5L", UnitType.BOTTLE, 1, ItemStatus.PENDING, createdBy = "M03"),
            ShoppingItem("SHOP-004", "Farm Fresh Eggs", UnitType.DOZEN, 2, ItemStatus.BOUGHT, boughtAt = System.currentTimeMillis() - 86400000L, createdBy = "M01")
        )

        _monthlyBudgetPaisa.value = 500000L
        _expenses.value = initialExpenses
        _settlements.value = emptyList()
        _shoppingItems.value = initialShopping
        processedIdempotencyKeys.clear()
    }

    override fun getMembers(): Flow<List<Member>> = sessionManager.registeredMembers

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

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val expenseDate = date?.trim()?.ifEmpty { today } ?: today
        if (!Regex("""^\d{4}-\d{2}-\d{2}$""").matches(expenseDate)) {
            return Result.failure(IllegalArgumentException("Date must be in YYYY-MM-DD format"))
        }
        if (expenseDate > today) {
            return Result.failure(IllegalArgumentException("Expense date cannot be in the future ($expenseDate)"))
        }

        val expenseId = "EXP-${UUID.randomUUID().toString().take(6).uppercase()}"

        val splits: List<ExpenseSplit> = when (splitType) {
            SplitType.EQUAL -> {
                val targets = if (splitMemberIds.isNotEmpty()) splitMemberIds else sessionManager.registeredMembers.value.map { it.id }
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
            source = ExpenseSource.MANUAL,
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
        val currentBalances = MoneyEngine.computeBalances(sessionManager.registeredMembers.value, _expenses.value, _settlements.value)
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

    override fun getBalances(): Flow<List<MemberBalance>> = combine(
        sessionManager.registeredMembers,
        _expenses,
        _settlements
    ) { members, expenses, settlements ->
        MoneyEngine.computeBalances(members, expenses, settlements)
    }

    override fun getBudgetSummary(month: String?): Flow<BudgetSummary> = combine(
        _expenses,
        _monthlyBudgetPaisa
    ) { expenses, budget ->
        val currentMonth = month ?: SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
        val monthlyExpenses = expenses.filter { it.expenseDate.startsWith(currentMonth) }
        val totalSpent = monthlyExpenses.sumOf { it.amountPaisa }
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

    override fun getSmartSettleSuggestions(): Flow<List<SmartSettleSuggestion>> = combine(
        sessionManager.registeredMembers,
        _expenses,
        _settlements
    ) { members, expenses, settlements ->
        val balances = MoneyEngine.computeBalances(members, expenses, settlements)
        MoneyEngine.simplify(balances, members)
    }

    override fun getCategorySpends(month: String?): Flow<List<CategorySpend>> = combine(
        _expenses,
        _monthlyBudgetPaisa
    ) { expenses, budget ->
        val currentMonth = month ?: SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
        val monthlyExpenses = expenses.filter { it.expenseDate.startsWith(currentMonth) }
        val warningThreshold = (budget * 40L) / 100L // 40% category cap

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
        val recurringTemplates = _expenses.value.filter { it.isRecurring && it.source != ExpenseSource.RECURRING }
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
                it.title.removePrefix("Recurring: ") == template.title &&
                it.expenseDate.startsWith(currentMonth)
            }
            if (!alreadyApplied) {
                val newId = "EXP-REC-${UUID.randomUUID().toString().take(6).uppercase()}"
                val newSplits = template.splits.map { it.copy(expenseId = newId) }
                val newExp = template.copy(
                    id = newId,
                    title = "Recurring: ${template.title}".take(80),
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

    override suspend fun updateMonthlyBudget(newBudgetPaisa: Long): Result<Long> = mutex.withLock {
        if (newBudgetPaisa <= 0L || newBudgetPaisa > 1_000_000_000_00L) {
            return Result.failure(IllegalArgumentException("Monthly budget must be between PKR 1.00 and PKR 10,000,000.00"))
        }
        _monthlyBudgetPaisa.value = newBudgetPaisa
        Result.success(newBudgetPaisa)
    }
}
