package com.example.model

enum class Role {
    ADMIN,
    MEMBER
}

enum class UnitType(val displayName: String) {
    PACK("pack"),
    DOZEN("dozen"),
    BOTTLE("bottle"),
    BAG("bag")
}

enum class ItemStatus {
    PENDING,
    BOUGHT
}

enum class SplitType {
    EQUAL,
    CUSTOM
}

enum class ExpenseSource {
    MANUAL,
    CHECKOUT,
    RECURRING
}

enum class BudgetCategory(val displayName: String) {
    GROCERIES("Groceries"),
    UTILITIES("Utilities"),
    CLEANING("Cleaning"),
    ENTERTAINMENT("Entertainment"),
    DINING("Dining"),
    OTHER("Other")
}

enum class BudgetStateStatus {
    ON_TRACK,
    NEAR_LIMIT,
    OVER_BUDGET
}

data class Member(
    val id: String, // "M01", "M02", "M03"
    val name: String,
    val email: String = "",
    val role: Role = Role.MEMBER
)

data class ShoppingItem(
    val id: String,
    val title: String,
    val unit: UnitType,
    val requestedQty: Int,
    val status: ItemStatus = ItemStatus.PENDING,
    val expenseId: String? = null,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val boughtAt: Long? = null
)

data class ExpenseSplit(
    val expenseId: String,
    val memberId: String,
    val sharePaisa: Long
)

data class Expense(
    val id: String,
    val title: String,
    val category: BudgetCategory,
    val amountPaisa: Long,
    val payerId: String,
    val splitType: SplitType,
    val expenseDate: String, // YYYY-MM-DD
    val source: ExpenseSource = ExpenseSource.MANUAL,
    val createdBy: String,
    val splits: List<ExpenseSplit> = emptyList(),
    val subtotalPaisa: Long? = null,
    val taxPercent: Double? = null,
    val discountPaisa: Long? = null,
    val isRecurring: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class CategorySpend(
    val category: BudgetCategory,
    val totalSpentPaisa: Long,
    val isExceedingWarning: Boolean // Exceeds 40% of budget (PKR 2,000.00)
)

data class Settlement(
    val id: String,
    val senderId: String,
    val recipientId: String,
    val amountPaisa: Long,
    val recordedBy: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class MemberBalance(
    val memberId: String,
    val name: String,
    val paidPaisa: Long,
    val owedPaisa: Long,
    val sentPaisa: Long,
    val receivedPaisa: Long,
    val netPaisa: Long // Paid - Owed + Sent - Received
)

data class SmartSettleSuggestion(
    val fromMemberId: String,
    val toMemberId: String,
    val fromName: String,
    val toName: String,
    val amountPaisa: Long
)

data class BudgetSummary(
    val month: String, // YYYY-MM
    val monthlyBudgetPaisa: Long,
    val totalSpentPaisa: Long,
    val remainingPaisa: Long,
    val percentUsed: Int,
    val status: BudgetStateStatus
)
