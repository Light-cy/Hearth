package com.example.di

import com.example.data.HisaabRepository
import com.example.data.LocalDemoEngine
import com.example.data.SessionManager
import com.example.data.remote.RemoteRetrofitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppContainer {
    val sessionManager: SessionManager by lazy { SessionManager() }
    val localEngine: LocalDemoEngine by lazy { LocalDemoEngine(sessionManager) }

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

    val repository: HisaabRepository
        get() = if (_isLiveMode.value) remoteRepository else localEngine

    fun toggleLiveMode(enabled: Boolean) {
        _isLiveMode.value = enabled
    }

    fun updateBaseUrl(url: String) {
        _baseUrl.value = url
        remoteRepository.updateBaseUrl(url)
    }
}
