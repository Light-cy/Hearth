package com.example

import com.example.engine.MoneyEngine
import com.example.model.BudgetCategory
import com.example.model.Expense
import com.example.model.ExpenseSplit
import com.example.model.Member
import com.example.model.Role
import com.example.model.Settlement
import com.example.model.SplitType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class MoneyEngineTest {

    @Test
    fun testPaisaConversion() {
        assertEquals(33334L, MoneyEngine.toPaisa("333.34"))
        assertEquals(420000L, MoneyEngine.toPaisa("4200.00"))
        assertEquals(500000L, MoneyEngine.toPaisa("5,000.00"))
        assertEquals("333.34", MoneyEngine.fromPaisa(33334L))
        assertEquals("4200.00", MoneyEngine.fromPaisa(420000L))

        try {
            MoneyEngine.toPaisa("10.005")
            fail("Expected exception for 3 decimals")
        } catch (e: IllegalArgumentException) {
            // Success
        }

        try {
            MoneyEngine.toPaisa("0.00")
            fail("Expected exception for zero")
        } catch (e: IllegalArgumentException) {
            // Success
        }
    }

    @Test
    fun testSplitEqualRemainderAscending() {
        // 100000 / 3 gives [33334, 33333, 33333]
        val shares1 = MoneyEngine.splitEqual(100000L, listOf("M03", "M01", "M02"))
        assertEquals(33334L, shares1["M01"])
        assertEquals(33333L, shares1["M02"])
        assertEquals(33333L, shares1["M03"])

        // 1 paisa across 3 gives [1, 0, 0]
        val shares2 = MoneyEngine.splitEqual(1L, listOf("M01", "M02", "M03"))
        assertEquals(1L, shares2["M01"])
        assertEquals(0L, shares2["M02"])
        assertEquals(0L, shares2["M03"])
    }

    @Test
    fun testHouseholdExpensesAndSettlementScenario() {
        val ali = Member("M01", "Ali", "ali@hearth.pk", Role.ADMIN)
        val ayesha = Member("M02", "Ayesha", "ayesha@hearth.pk", Role.MEMBER)
        val bilal = Member("M03", "Bilal", "bilal@hearth.pk", Role.MEMBER)
        val members = listOf(ali, ayesha, bilal)

        val expenses = listOf(
            Expense(
                id = "EXP-001",
                title = "Monthly Groceries & Pantry",
                category = BudgetCategory.GROCERIES,
                amountPaisa = 120000L,
                payerId = "M01", // Ali
                splitType = SplitType.CUSTOM,
                expenseDate = "2026-11-01",
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
                expenseDate = "2026-11-02",
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
                expenseDate = "2026-11-03",
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
                expenseDate = "2026-11-04",
                createdBy = "M01",
                splits = listOf(
                    ExpenseSplit("EXP-004", "M01", 5000L),
                    ExpenseSplit("EXP-004", "M02", 8000L),
                    ExpenseSplit("EXP-004", "M03", 5000L)
                )
            )
        )

        // 1. Total spend = 420,000 paisa (PKR 4,200.00)
        val totalSpend = expenses.sumOf { it.amountPaisa }
        assertEquals(420000L, totalSpend)

        // 2. Compute balances and verify net values: Ali +250.00, Ayesha -450.00, Bilal +200.00
        val balances = MoneyEngine.computeBalances(members, expenses, emptyList())
        val aliBal = balances.first { it.memberId == "M01" }
        val ayeshaBal = balances.first { it.memberId == "M02" }
        val bilalBal = balances.first { it.memberId == "M03" }

        assertEquals(25000L, aliBal.netPaisa) // +250.00 PKR
        assertEquals(-45000L, ayeshaBal.netPaisa) // -450.00 PKR
        assertEquals(20000L, bilalBal.netPaisa) // +200.00 PKR

        // 3. Assert sum of nets is strictly 0.00
        val sumNets = balances.sumOf { it.netPaisa }
        assertEquals(0L, sumNets)

        // 4. Test Smart Settle Suggestions (greedy simplify)
        val suggestions = MoneyEngine.simplify(balances, members)
        assertEquals(2, suggestions.size)
        assertEquals("M02", suggestions[0].fromMemberId) // Ayesha
        assertEquals("M01", suggestions[0].toMemberId) // pays Ali
        assertEquals(25000L, suggestions[0].amountPaisa) // 250.00

        assertEquals("M02", suggestions[1].fromMemberId) // Ayesha
        assertEquals("M03", suggestions[1].toMemberId) // pays Bilal
        assertEquals(20000L, suggestions[1].amountPaisa) // 200.00

        // 5. Settle up: Ayesha settles 250.00 to Ali
        val s1 = Settlement("S01", senderId = "M02", recipientId = "M01", amountPaisa = 25000L, recordedBy = "M02")
        val postS1Balances = MoneyEngine.computeBalances(members, expenses, listOf(s1))
        val postS1Ali = postS1Balances.first { it.memberId == "M01" }
        val postS1Ayesha = postS1Balances.first { it.memberId == "M02" }
        assertEquals(0L, postS1Ali.netPaisa) // Ali is fully settled up
        assertEquals(-20000L, postS1Ayesha.netPaisa) // Ayesha now owes -200.00

        // 6. Settle remainder: Ayesha settles 200.00 to Bilal
        val s2 = Settlement("S02", senderId = "M02", recipientId = "M03", amountPaisa = 20000L, recordedBy = "M02")
        val postS2Balances = MoneyEngine.computeBalances(members, expenses, listOf(s1, s2))

        // All members are now 0.00
        for (b in postS2Balances) {
            assertEquals(0L, b.netPaisa)
        }

        // 7. Spending is still 4,200.00
        val spendAfterSettlements = expenses.sumOf { it.amountPaisa }
        assertEquals(420000L, spendAfterSettlements)

        // 8. A further settlement is rejected
        val overSettle = MoneyEngine.validateSettlement("M02", "M01", 1000L, postS2Balances)
        assertFalse(overSettle.isSuccess)
    }

    @Test
    fun testTaxAndDiscountCalculation() {
        // items_total = 1200.00 PKR (120,000 paisa)
        val subtotalPaisa = 120000L
        val taxPercent = 16.0 // 16% GST
        val discountPaisa = 5000L // 50.00 PKR discount

        val taxAmountPaisa = (subtotalPaisa * taxPercent / 100.0).toLong()
        assertEquals(19200L, taxAmountPaisa) // 192.00 PKR

        val finalTotalPaisa = subtotalPaisa + taxAmountPaisa - discountPaisa
        assertEquals(134200L, finalTotalPaisa) // 1,342.00 PKR
        assertEquals("1342.00", MoneyEngine.fromPaisa(finalTotalPaisa))
    }

    @Test
    fun testCategoryThresholdWarning() {
        // budget = 5,000.00 PKR (500,000 paisa)
        val budgetPaisa = 500000L
        val thresholdPaisa = (budgetPaisa * 40L) / 100L // 40% = 200,000 paisa (2,000.00 PKR)

        val utilitiesPaisa = 252000L // 2,520.00 PKR
        val groceriesPaisa = 120000L // 1,200.00 PKR

        assertTrue(utilitiesPaisa > thresholdPaisa) // Flagged!
        assertFalse(groceriesPaisa > thresholdPaisa) // Not flagged
    }

    @Test
    fun testLargeAmountPrecisionAndAscendingRemainder() {
        // 9,999,999.99 PKR in paisa = 999,999,999 paisa
        val largePaisa = MoneyEngine.toPaisa("9,999,999.99")
        assertEquals(999999999L, largePaisa)

        // Split across 3 members: 999,999,999 % 3 == 0
        val shares = MoneyEngine.splitEqual(largePaisa, listOf("M01", "M02", "M03"))
        assertEquals(333333333L, shares["M01"])
        assertEquals(333333333L, shares["M02"])
        assertEquals(333333333L, shares["M03"])
        assertEquals(largePaisa, shares.values.sum())

        // Formatted without scientific notation or loss
        assertEquals("PKR 9,999,999.99", MoneyEngine.formatPkr(largePaisa))
    }
}
