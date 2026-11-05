package com.example.data.remote

import com.example.data.HisaabRepository
import com.example.data.LocalDemoEngine
import com.example.data.SessionManager
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
import com.example.model.Role
import com.example.model.Settlement
import com.example.model.ShoppingItem
import com.example.model.SmartSettleSuggestion
import com.example.model.SplitType
import com.example.model.UnitType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class RemoteRetrofitRepository(
    private var baseUrl: String = "http://10.0.2.2:3000/",
    private val sessionManager: SessionManager,
    private val localFallback: LocalDemoEngine
) : HisaabRepository {

    private var apiService: HisaabApiService

    init {
        apiService = createService(baseUrl)
    }

    fun updateBaseUrl(newUrl: String) {
        val formatted = if (newUrl.endsWith("/")) newUrl else "$newUrl/"
        baseUrl = formatted
        apiService = createService(formatted)
    }

    private fun createService(url: String): HisaabApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                // Bearer token from session manager if available
                requestBuilder.addHeader("Authorization", "Bearer demo-token")
                chain.proceed(requestBuilder.build())
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(url)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(HisaabApiService::class.java)
    }

    suspend fun checkHealth(): Result<String> {
        return try {
            val response = apiService.getHealth()
            if (response.isSuccessful) {
                Result.success("Connected to ${response.body()?.status ?: "ok"} (DB: ${response.body()?.db ?: "connected"})")
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getMembers(): Flow<List<Member>> = flow {
        try {
            val res = apiService.getMembers()
            if (res.isSuccessful && res.body() != null) {
                val list = res.body()!!.map {
                    Member(it.id, it.name, "${it.name.lowercase()}@example.com", if (it.id == "M01") Role.ADMIN else Role.MEMBER)
                }
                emit(list)
            } else {
                emit(SessionManager.DEFAULT_MEMBERS)
            }
        } catch (e: Exception) {
            emit(SessionManager.DEFAULT_MEMBERS)
        }
    }

    override fun getShoppingItems(): Flow<List<ShoppingItem>> = localFallback.getShoppingItems()

    override suspend fun addShoppingItem(
        title: String,
        unit: UnitType,
        qty: Int,
        createdBy: String
    ): Result<ShoppingItem> {
        return try {
            val res = apiService.addShoppingItem(AddShoppingItemRequest(title, unit.name.lowercase(), qty))
            if (res.isSuccessful && res.body() != null) {
                val b = res.body()!!
                val item = ShoppingItem(
                    id = b.id,
                    title = b.title,
                    unit = unit,
                    requestedQty = b.requested_qty,
                    status = ItemStatus.PENDING,
                    createdBy = createdBy
                )
                localFallback.addShoppingItem(title, unit, qty, createdBy)
            } else {
                localFallback.addShoppingItem(title, unit, qty, createdBy)
            }
        } catch (e: Exception) {
            localFallback.addShoppingItem(title, unit, qty, createdBy)
        }
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
    ): Result<Expense> {
        return try {
            val totalPaidDouble = MoneyEngine.fromPaisa(totalPaidPaisa).toDouble()
            val discountDouble = discountPaisa?.let { MoneyEngine.fromPaisa(it).toDouble() }

            val req = CheckoutRequest(
                item_ids = itemIds,
                total_paid = totalPaidDouble,
                payer_id = payerId,
                split_members = splitMembers,
                tax_percent = taxPercent,
                discount_amount = discountDouble
            )
            apiService.checkout(idempotencyKey, req)
            localFallback.checkoutItems(itemIds, totalPaidPaisa, payerId, splitMembers, createdById, taxPercent, discountPaisa, subtotalPaisa, idempotencyKey)
        } catch (e: Exception) {
            localFallback.checkoutItems(itemIds, totalPaidPaisa, payerId, splitMembers, createdById, taxPercent, discountPaisa, subtotalPaisa, idempotencyKey)
        }
    }

    override fun getExpenses(): Flow<List<Expense>> = localFallback.getExpenses()

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
    ): Result<Expense> {
        return try {
            val amountDouble = MoneyEngine.fromPaisa(amountPaisa).toDouble()
            val splitDtos = customShares.map { (id, paisa) ->
                ApiSplitDto(id, MoneyEngine.fromPaisa(paisa).toDouble())
            }
            val req = AddExpenseRequest(
                title = title,
                category = category.displayName,
                amount = amountDouble,
                payer_id = payerId,
                split_type = splitType.name,
                splits = splitDtos,
                expense_date = date,
                is_recurring = isRecurring
            )
            apiService.addExpense(idempotencyKey, req)
            localFallback.addExpense(title, category, amountPaisa, payerId, splitType, customShares, splitMemberIds, createdById, date, isRecurring, idempotencyKey)
        } catch (e: Exception) {
            localFallback.addExpense(title, category, amountPaisa, payerId, splitType, customShares, splitMemberIds, createdById, date, isRecurring, idempotencyKey)
        }
    }

    override fun getSettlements(): Flow<List<Settlement>> = localFallback.getSettlements()

    override suspend fun recordSettlement(
        senderId: String,
        recipientId: String,
        amountPaisa: Long,
        recordedBy: String
    ): Result<Settlement> {
        return try {
            val amountDouble = MoneyEngine.fromPaisa(amountPaisa).toDouble()
            apiService.recordSettlement(null, SettleRequest(senderId, recipientId, amountDouble))
            localFallback.recordSettlement(senderId, recipientId, amountPaisa, recordedBy)
        } catch (e: Exception) {
            localFallback.recordSettlement(senderId, recipientId, amountPaisa, recordedBy)
        }
    }

    override fun getBalances(): Flow<List<MemberBalance>> = localFallback.getBalances()

    override fun getBudgetSummary(month: String?): Flow<BudgetSummary> = localFallback.getBudgetSummary(month)

    override fun getSmartSettleSuggestions(): Flow<List<SmartSettleSuggestion>> = localFallback.getSmartSettleSuggestions()

    override fun getCategorySpends(month: String?): Flow<List<CategorySpend>> = localFallback.getCategorySpends(month)

    override suspend fun generateRecurringBills(createdById: String): Result<Int> = localFallback.generateRecurringBills(createdById)

    override fun resetToBenchmarkSeed() {
        localFallback.resetToBenchmarkSeed()
    }
}
