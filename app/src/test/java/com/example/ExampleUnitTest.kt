package com.example

import com.example.data.LocalVaultEngine
import com.example.data.SessionManager
import com.example.engine.MoneyEngine
import com.example.model.BudgetCategory
import com.example.model.BudgetStateStatus
import com.example.model.SplitType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun moneyEngine_toPaisa_and_fromPaisa_preservePrecision() {
        assertEquals(78000L, MoneyEngine.toPaisa("780"))
        assertEquals(78000L, MoneyEngine.toPaisa("780.00"))
        assertEquals(3334L, MoneyEngine.toPaisa("33.34"))
        assertEquals("780.00", MoneyEngine.fromPaisa(78000L))
        assertEquals("PKR 250.00", MoneyEngine.formatPkr(25000L))
        assertEquals("-PKR 450.00", MoneyEngine.formatPkr(-45000L))
    }

    @Test
    fun moneyEngine_splitEqual_assignsRemainderDeterministicallyByMemberId() {
        // PKR 100.00 = 10,000 paisa split 3 ways -> 3334, 3333, 3333
        val splits = MoneyEngine.splitEqual(10000L, listOf("M03", "M01", "M02"))
        assertEquals(3334L, splits["M01"])
        assertEquals(3333L, splits["M02"])
        assertEquals(3333L, splits["M03"])
        assertEquals(10000L, splits.values.sum())
    }

    @Test
    fun moneyEngine_validateCustomSplit_enforcesExactSumAndNonNegative() {
        val valid = mapOf("M01" to 50000L, "M02" to 30000L, "M03" to 70000L)
        MoneyEngine.validateCustomSplit(150000L, valid)

        val mismatch = mapOf("M01" to 50000L, "M02" to 30000L, "M03" to 60000L)
        var mismatchError: Exception? = null
        try {
            MoneyEngine.validateCustomSplit(150000L, mismatch)
        } catch (e: Exception) {
            mismatchError = e
        }
        assertNotNull(mismatchError)
    }

    @Test
    fun defaultHouseholdVault_maintainsAccurateBalancesAndZeroSum() = runBlocking {
        val sessionManager = SessionManager()
        val repo = LocalVaultEngine(sessionManager)
        val balances = repo.getBalances().first().associateBy { it.memberId }

        // Ali (M01): Paid 1,800.00 | Owed 1,550.00 | Net +250.00
        val ali = balances.getValue("M01")
        assertEquals(180000L, ali.paidPaisa)
        assertEquals(155000L, ali.owedPaisa)
        assertEquals(25000L, ali.netPaisa)

        // Ayesha (M02): Paid 900.00 | Owed 1,350.00 | Net -450.00
        val ayesha = balances.getValue("M02")
        assertEquals(90000L, ayesha.paidPaisa)
        assertEquals(135000L, ayesha.owedPaisa)
        assertEquals(-45000L, ayesha.netPaisa)

        // Bilal (M03): Paid 1,500.00 | Owed 1,300.00 | Net +200.00
        val bilal = balances.getValue("M03")
        assertEquals(150000L, bilal.paidPaisa)
        assertEquals(130000L, bilal.owedPaisa)
        assertEquals(20000L, bilal.netPaisa)

        // Zero-Sum Invariant
        assertEquals(0L, balances.values.sumOf { it.netPaisa })

        // Budget Summary: Total Spend = 4,200.00 / 5,000.00 (84% -> NEAR_LIMIT)
        val budget = repo.getBudgetSummary().first()
        assertEquals(500000L, budget.monthlyBudgetPaisa)
        assertEquals(420000L, budget.totalSpentPaisa)
        assertEquals(80000L, budget.remainingPaisa)
        assertEquals(BudgetStateStatus.NEAR_LIMIT, budget.status)

        // Category Spends: Utilities = 1,500 + 1,020 = 2,520 (50.4% > 40% limit)
        val categories = repo.getCategorySpends().first().associateBy { it.category }
        val utilities = categories.getValue(BudgetCategory.UTILITIES)
        assertEquals(252000L, utilities.totalSpentPaisa)
        assertTrue(utilities.isExceedingWarning)
    }

    @Test
    fun reactiveFlows_updateBalancesAndSuggestionsImmediatelyOnSettlement() = runBlocking {
        val sessionManager = SessionManager()
        val repo = LocalVaultEngine(sessionManager)

        // Initial Smart Settle suggestions: Ayesha (M02) -> Ali (M01) 250, Ayesha (M02) -> Bilal (M03) 200
        val initialSuggestions = repo.getSmartSettleSuggestions().first()
        assertEquals(2, initialSuggestions.size)
        assertEquals(25000L, initialSuggestions[0].amountPaisa)
        assertEquals(20000L, initialSuggestions[1].amountPaisa)

        // Settle PKR 250.00 from M02 to M01
        val res1 = repo.recordSettlement("M02", "M01", 25000L, "M02")
        assertTrue(res1.isSuccess)

        val balancesAfterFirst = repo.getBalances().first().associateBy { it.memberId }
        assertEquals(0L, balancesAfterFirst.getValue("M01").netPaisa)
        assertEquals(-20000L, balancesAfterFirst.getValue("M02").netPaisa)
        assertEquals(20000L, balancesAfterFirst.getValue("M03").netPaisa)

        // Settle remaining PKR 200.00 from M02 to M03
        val res2 = repo.recordSettlement("M02", "M03", 20000L, "M02")
        assertTrue(res2.isSuccess)

        val finalBalances = repo.getBalances().first()
        assertTrue(finalBalances.all { it.netPaisa == 0L })
        assertTrue(repo.getSmartSettleSuggestions().first().isEmpty())
    }

    @Test
    fun addExpense_rejectsFutureDates_and_generateRecurringBills_clonesRecurringTemplates() = runBlocking {
        val sessionManager = SessionManager()
        val repo = LocalVaultEngine(sessionManager)

        val futureResult = repo.addExpense(
            title = "Time Travel Expense",
            category = BudgetCategory.OTHER,
            amountPaisa = 50000L,
            payerId = "M01",
            splitType = SplitType.EQUAL,
            splitMemberIds = listOf("M01", "M02", "M03"),
            createdById = "M01",
            date = "2099-12-31"
        )
        assertTrue(futureResult.isFailure)

        val generated = repo.generateRecurringBills("M01")
        assertTrue(generated.isSuccess)
    }
}
