package com.example.engine

import com.example.model.Expense
import com.example.model.Member
import com.example.model.MemberBalance
import com.example.model.Settlement
import com.example.model.SmartSettleSuggestion
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

object MoneyEngine {

    private val numberFormat = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
    private val plainDecimalFormat = DecimalFormat("0.00", DecimalFormatSymbols(Locale.US))

    /**
     * Converts a decimal string (e.g. "333.34" or "4200") to integer paisa.
     * Rejects numbers with more than 2 decimal places, negatives, zero (if requirePositive), or invalid numbers.
     */
    fun toPaisa(amountStr: String, requirePositive: Boolean = true): Long {
        val trimmed = amountStr.trim().replace(",", "")
        if (trimmed.isEmpty()) {
            throw IllegalArgumentException("Amount cannot be empty")
        }
        val parts = trimmed.split(".")
        if (parts.size > 2) {
            throw IllegalArgumentException("Invalid decimal format: $amountStr")
        }
        if (parts.size == 2 && parts[1].length > 2) {
            throw IllegalArgumentException("Amount cannot have more than 2 decimal places: $amountStr")
        }

        val bd = try {
            BigDecimal(trimmed)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid amount number: $amountStr")
        }

        if (requirePositive && bd <= BigDecimal.ZERO) {
            throw IllegalArgumentException("Amount must be greater than zero")
        }

        val paisa = bd.multiply(BigDecimal(100)).setScale(0, RoundingMode.UNNECESSARY).toLong()
        if (paisa > 1_000_000_000_00L) { // 10 million PKR in paisa
            throw IllegalArgumentException("Amount exceeds maximum limit of PKR 10,000,000.00")
        }
        return paisa
    }

    /**
     * Converts integer paisa to standard decimal string with 2 decimals (e.g. 33334 -> "333.34").
     */
    fun fromPaisa(paisa: Long): String {
        val isNegative = paisa < 0
        val absPaisa = abs(paisa)
        val rupees = absPaisa / 100
        val remainder = absPaisa % 100
        val formatted = String.format(Locale.US, "%d.%02d", rupees, remainder)
        return if (isNegative) "-$formatted" else formatted
    }

    /**
     * Formats paisa as standard display string: "PKR 4,200.00" or "-PKR 450.00"
     */
    fun formatPkr(paisa: Long, includePrefix: Boolean = true): String {
        val isNegative = paisa < 0
        val absPaisa = abs(paisa)
        val rupees = absPaisa.toDouble() / 100.0
        val formattedNumber = numberFormat.format(rupees)
        val prefix = if (includePrefix) "PKR " else ""
        return if (isNegative) "-$prefix$formattedNumber" else "$prefix$formattedNumber"
    }

    /**
     * Formats net balance status copy:
     * - You are owed PKR 250.00
     * - You owe PKR 450.00
     * - You are all settled up
     */
    fun formatNetStatusCopy(netPaisa: Long): String {
        return when {
            netPaisa > 0 -> "You are owed ${formatPkr(netPaisa)}"
            netPaisa < 0 -> "You owe ${formatPkr(abs(netPaisa))}"
            else -> "You are all settled up"
        }
    }

    /**
     * S4.2: Equal split with deterministic remainder distribution.
     * Ascending member ID order: the first (amount % n) members receive +1 paisa.
     */
    fun splitEqual(amountPaisa: Long, memberIds: List<String>): Map<String, Long> {
        require(amountPaisa > 0) { "Amount must be positive: $amountPaisa" }
        require(memberIds.isNotEmpty()) { "Members list cannot be empty" }

        val sortedIds = memberIds.distinct().sorted()
        val n = sortedIds.size
        val base = amountPaisa / n
        val remainder = (amountPaisa % n).toInt()

        val result = mutableMapOf<String, Long>()
        for (i in sortedIds.indices) {
            val share = base + if (i < remainder) 1L else 0L
            result[sortedIds[i]] = share
        }

        // Integrity assertion
        val sumShares = result.values.sum()
        check(sumShares == amountPaisa) {
            "Internal split error: sum of shares ($sumShares) != total ($amountPaisa)"
        }
        return result
    }

    /**
     * S4.3: Custom split validation.
     * Ensures all shares are non-negative and their sum strictly equals the total amount.
     */
    fun validateCustomSplit(amountPaisa: Long, shares: Map<String, Long>) {
        require(amountPaisa > 0) { "Amount must be positive: $amountPaisa" }
        require(shares.isNotEmpty()) { "Splits cannot be empty" }

        for ((id, share) in shares) {
            require(share >= 0) { "Share for member $id cannot be negative: $share" }
        }

        val totalShare = shares.values.sum()
        if (totalShare != amountPaisa) {
            val diff = amountPaisa - totalShare
            val diffStr = formatPkr(abs(diff))
            val direction = if (diff > 0) "under" else "over"
            throw IllegalArgumentException("Custom split is $direction by $diffStr (Total: ${formatPkr(totalShare)}, Expected: ${formatPkr(amountPaisa)})")
        }
    }

    /**
     * S4.4: Computes live balances from ledger entries.
     * Net = Paid - Owed + Sent - Received.
     * Asserts sum of all nets is exactly 0.00.
     */
    fun computeBalances(
        members: List<Member>,
        expenses: List<Expense>,
        settlements: List<Settlement>
    ): List<MemberBalance> {
        val memberMap = members.associateBy { it.id }

        val paidMap = mutableMapOf<String, Long>().withDefault { 0L }
        val owedMap = mutableMapOf<String, Long>().withDefault { 0L }
        val sentMap = mutableMapOf<String, Long>().withDefault { 0L }
        val receivedMap = mutableMapOf<String, Long>().withDefault { 0L }

        for (expense in expenses) {
            paidMap[expense.payerId] = (paidMap[expense.payerId] ?: 0L) + expense.amountPaisa
            for (split in expense.splits) {
                owedMap[split.memberId] = (owedMap[split.memberId] ?: 0L) + split.sharePaisa
            }
        }

        for (settlement in settlements) {
            sentMap[settlement.senderId] = (sentMap[settlement.senderId] ?: 0L) + settlement.amountPaisa
            receivedMap[settlement.recipientId] = (receivedMap[settlement.recipientId] ?: 0L) + settlement.amountPaisa
        }

        val balances = members.sortedBy { it.id }.map { member ->
            val paid = paidMap[member.id] ?: 0L
            val owed = owedMap[member.id] ?: 0L
            val sent = sentMap[member.id] ?: 0L
            val received = receivedMap[member.id] ?: 0L
            val net = paid - owed + sent - received
            MemberBalance(
                memberId = member.id,
                name = member.name,
                paidPaisa = paid,
                owedPaisa = owed,
                sentPaisa = sent,
                receivedPaisa = received,
                netPaisa = net
            )
        }

        // Invariant assertion: all nets must sum to exactly 0
        val netSum = balances.sumOf { it.netPaisa }
        if (netSum != 0L) {
            throw IllegalStateException("INTERNAL ERROR: Ledger net balance sum is not 0 (sum = $netSum paisa)")
        }

        return balances
    }

    /**
     * S4.5: Validate settlement parameters.
     * Caller debt is max(0, -net). Recipient credit is max(0, net).
     */
    fun validateSettlement(
        senderId: String,
        recipientId: String,
        amountPaisa: Long,
        balances: List<MemberBalance>
    ): Result<Unit> {
        if (amountPaisa <= 0) {
            return Result.failure(IllegalArgumentException("Settlement amount must be greater than zero"))
        }
        if (senderId == recipientId) {
            return Result.failure(IllegalArgumentException("Cannot settle with yourself"))
        }

        val senderBalance = balances.find { it.memberId == senderId }
            ?: return Result.failure(IllegalArgumentException("Sender $senderId not found"))
        val recipientBalance = balances.find { it.memberId == recipientId }
            ?: return Result.failure(IllegalArgumentException("Recipient $recipientId not found"))

        val senderDebt = maxOf(0L, -senderBalance.netPaisa)
        val recipientCredit = maxOf(0L, recipientBalance.netPaisa)

        if (senderDebt <= 0L) {
            return Result.failure(IllegalArgumentException("Nothing to settle: sender has no outstanding debt"))
        }
        if (recipientCredit <= 0L) {
            return Result.failure(IllegalArgumentException("Recipient is not owed any money"))
        }

        if (amountPaisa > senderDebt) {
            return Result.failure(IllegalArgumentException("You only owe ${formatPkr(senderDebt)}"))
        }
        if (amountPaisa > recipientCredit) {
            return Result.failure(IllegalArgumentException("Recipient is only owed ${formatPkr(recipientCredit)}"))
        }

        return Result.success(Unit)
    }

    /**
     * S4.6: Greedy debt simplification algorithm.
     * Largest debtor pays largest creditor. Ties go to lower member ID.
     * Produces at most N - 1 transfers.
     */
    fun simplify(
        balances: List<MemberBalance>,
        members: List<Member>
    ): List<SmartSettleSuggestion> {
        val memberNameMap = members.associate { it.id to it.name }

        data class Debtor(val id: String, var debtPaisa: Long)
        data class Creditor(val id: String, var creditPaisa: Long)

        val debtors = balances
            .filter { it.netPaisa < 0 }
            .map { Debtor(it.memberId, -it.netPaisa) }
            .sortedWith(compareByDescending<Debtor> { it.debtPaisa }.thenBy { it.id })
            .toMutableList()

        val creditors = balances
            .filter { it.netPaisa > 0 }
            .map { Creditor(it.memberId, it.netPaisa) }
            .sortedWith(compareByDescending<Creditor> { it.creditPaisa }.thenBy { it.id })
            .toMutableList()

        val suggestions = mutableListOf<SmartSettleSuggestion>()

        var debtorIndex = 0
        var creditorIndex = 0

        while (debtorIndex < debtors.size && creditorIndex < creditors.size) {
            val debtor = debtors[debtorIndex]
            val creditor = creditors[creditorIndex]

            val settleAmount = minOf(debtor.debtPaisa, creditor.creditPaisa)
            if (settleAmount > 0) {
                suggestions.add(
                    SmartSettleSuggestion(
                        fromMemberId = debtor.id,
                        toMemberId = creditor.id,
                        fromName = memberNameMap[debtor.id] ?: debtor.id,
                        toName = memberNameMap[creditor.id] ?: creditor.id,
                        amountPaisa = settleAmount
                    )
                )
            }

            debtor.debtPaisa -= settleAmount
            creditor.creditPaisa -= settleAmount

            if (debtor.debtPaisa == 0L) {
                debtorIndex++
            }
            if (creditor.creditPaisa == 0L) {
                creditorIndex++
            }
        }

        return suggestions
    }
}
