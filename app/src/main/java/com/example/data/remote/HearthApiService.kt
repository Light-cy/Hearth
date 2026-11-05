package com.example.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val invite_code: String? = null
)

data class AuthUserDto(
    val id: String,
    val name: String,
    val email: String,
    val role: String = "member"
)

data class AuthResponse(
    val user: AuthUserDto,
    val access_token: String,
    val refresh_token: String? = null,
    val expires_in: Int? = null
)

data class ApiMemberDto(
    val id: String,
    val name: String
)

data class ApiShoppingItemDto(
    val id: String,
    val title: String,
    val unit: String,
    val requested_qty: Int,
    val status: String,
    val bought_at: Long? = null
)

data class AddShoppingItemRequest(
    val title: String,
    val unit: String,
    val requested_qty: Int
)

data class CheckoutRequest(
    val item_ids: List<String>,
    val total_paid: Double,
    val payer_id: String,
    val split_members: List<String>,
    val tax_percent: Double? = null,
    val discount_amount: Double? = null
)

data class ApiSplitDto(
    val member_id: String,
    val amount: Double
)

data class ApiExpenseDto(
    val id: String,
    val title: String,
    val category: String,
    val amount: Double,
    val payer_id: String,
    val split_type: String,
    val expense_date: String? = null,
    val splits: List<ApiSplitDto> = emptyList(),
    val is_recurring: Boolean = false
)

data class AddExpenseRequest(
    val title: String,
    val category: String,
    val amount: Double,
    val payer_id: String,
    val split_type: String,
    val splits: List<ApiSplitDto> = emptyList(),
    val expense_date: String? = null,
    val is_recurring: Boolean = false
)

data class ApiBalanceItemDto(
    val member_id: String,
    val name: String,
    val paid: Double,
    val owed: Double,
    val sent: Double,
    val received: Double,
    val net: Double
)

data class ApiBalancesResponse(
    val balances: List<ApiBalanceItemDto>,
    val sum: Double
)

data class SettleRequest(
    val sender_id: String,
    val recipient_id: String,
    val amount: Double
)

data class ApiSettlementDto(
    val id: String,
    val sender_id: String,
    val recipient_id: String,
    val amount: Double,
    val recorded_by: String
)

data class ApiSettleResponse(
    val settlement: ApiSettlementDto,
    val balances: List<ApiBalanceItemDto> = emptyList()
)

data class ApiSimplifyDto(
    val from: String,
    val to: String,
    val amount: Double
)

data class ApiBudgetResponse(
    val month: String,
    val monthly_budget: Double,
    val total_spent: Double,
    val remaining: Double,
    val percent_used: Int,
    val status: String
)

data class ApiCategorySpendDto(
    val category: String,
    val total: Double,
    val percent_of_budget: Double,
    val exceeds_threshold: Boolean
)

data class ApiHealthResponse(
    val status: String,
    val db: String? = null
)

interface HearthApiService {

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @GET("api/members")
    suspend fun getMembers(): Response<List<ApiMemberDto>>

    @GET("api/shopping")
    suspend fun getShoppingItems(@Query("status") status: String? = null): Response<List<ApiShoppingItemDto>>

    @POST("api/shopping")
    suspend fun addShoppingItem(@Body request: AddShoppingItemRequest): Response<ApiShoppingItemDto>

    @POST("api/shopping/checkout")
    suspend fun checkout(
        @Header("Idempotency-Key") idempotencyKey: String?,
        @Body request: CheckoutRequest
    ): Response<Map<String, Any>>

    @GET("api/expenses")
    suspend fun getExpenses(@Query("month") month: String? = null): Response<List<ApiExpenseDto>>

    @POST("api/expenses")
    suspend fun addExpense(
        @Header("Idempotency-Key") idempotencyKey: String?,
        @Body request: AddExpenseRequest
    ): Response<ApiExpenseDto>

    @GET("api/balances")
    suspend fun getBalances(): Response<ApiBalancesResponse>

    @GET("api/balances/simplify")
    suspend fun getSimplifiedBalances(): Response<List<ApiSimplifyDto>>

    @POST("api/settle")
    suspend fun recordSettlement(
        @Header("Idempotency-Key") idempotencyKey: String?,
        @Body request: SettleRequest
    ): Response<ApiSettleResponse>

    @GET("api/budget")
    suspend fun getBudgetSummary(@Query("month") month: String? = null): Response<ApiBudgetResponse>

    @GET("api/budget/categories")
    suspend fun getCategorySpends(): Response<List<ApiCategorySpendDto>>

    @POST("api/expenses/recurring/apply")
    suspend fun applyRecurringBills(): Response<Map<String, Int>>

    @GET("api/health")
    suspend fun getHealth(): Response<ApiHealthResponse>
}
