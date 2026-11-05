package com.example.di

import com.example.data.HearthRepository
import com.example.data.LocalVaultEngine
import com.example.data.SessionManager
import com.example.data.remote.RemoteRetrofitRepository
import com.example.model.Member
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppContainer {
    val sessionManager: SessionManager by lazy { SessionManager() }
    val localEngine: LocalVaultEngine by lazy { LocalVaultEngine(sessionManager) }

    private val _baseUrl = MutableStateFlow("http://10.0.2.2:3000")
    val baseUrl: StateFlow<String> = _baseUrl.asStateFlow()

    private val _isLiveMode = MutableStateFlow(false)
    val isLiveMode: StateFlow<Boolean> = _isLiveMode.asStateFlow()

    val remoteRepository: RemoteRetrofitRepository by lazy {
        RemoteRetrofitRepository(
            baseUrl = _baseUrl.value,
            sessionManager = sessionManager,
            localFallback = localEngine
        )
    }

    val repository: HearthRepository
        get() = if (_isLiveMode.value) remoteRepository else localEngine

    fun toggleLiveMode(enabled: Boolean) {
        _isLiveMode.value = enabled
    }

    fun updateBaseUrl(url: String) {
        _baseUrl.value = url
        remoteRepository.updateBaseUrl(url)
    }

    suspend fun authenticate(email: String, password: String): Result<Member> {
        return if (_isLiveMode.value) {
            remoteRepository.loginRemote(email, password)
        } else {
            sessionManager.authenticateLocal(email, password)
        }
    }

    suspend fun register(
        name: String,
        email: String,
        password: String,
        inviteCode: String,
        isCreatingHousehold: Boolean
    ): Result<Member> {
        return if (_isLiveMode.value) {
            remoteRepository.registerRemote(name, email, password, inviteCode, isCreatingHousehold)
        } else {
            sessionManager.registerLocal(name, email, password, inviteCode, isCreatingHousehold)
        }
    }
}
