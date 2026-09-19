package com.example.inkpaperdiary.ui.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inkpaperdiary.data.repository.SettingsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LockUiState(
    val isLockEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val isUnlocked: Boolean = false,
    val maxPinLength: Int = 6
)

class LockViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    private companion object {
        const val MAX_PIN_LENGTH = 6
        const val MIN_PIN_LENGTH = 4
        const val MAX_FAILED_ATTEMPTS = 5
        const val LOCKOUT_MILLIS = 30_000L
    }

    val uiState: StateFlow<LockUiState> = combine(
        settingsRepository.appLockEnabled,
        settingsRepository.biometricEnabled
    ) { enabled, biometric ->
        LockUiState(
            isLockEnabled = enabled,
            biometricEnabled = biometric,
            isUnlocked = !enabled // 如果没启用锁则默认直接视为已解锁
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LockUiState())

    private val _inputPin = MutableStateFlow("")
    val inputPin: StateFlow<String> = _inputPin.asStateFlow()

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private var failedAttempts = 0
    private var lockoutUntil = 0L

    /** 冷却剩余毫秒数（>0 表示锁定中） */
    private val _lockoutRemaining = MutableStateFlow(0L)
    val lockoutRemaining: StateFlow<Long> = _lockoutRemaining.asStateFlow()

    fun appendPinDigit(digit: String) {
        if (_isUnlocked.value) return
        if (lockoutRemaining.value > 0L) return // 冷却中不可输入

        val updated = (_inputPin.value + digit).take(MAX_PIN_LENGTH)
        _inputPin.value = updated

        if (updated.length >= MIN_PIN_LENGTH) {
            viewModelScope.launch {
                if (settingsRepository.verifyAppPin(updated)) {
                    _isUnlocked.value = true
                    failedAttempts = 0
                } else if (updated.length == MAX_PIN_LENGTH) {
                    handleMismatch()
                }
            }
        }
    }

    fun deletePinDigit() {
        val current = _inputPin.value
        if (current.isNotEmpty()) {
            _inputPin.value = current.dropLast(1)
        }
    }

    fun unlockByBiometric() {
        _isUnlocked.value = true
    }

    private fun handleMismatch() {
        _inputPin.value = ""
        failedAttempts++
        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            lockoutUntil = System.currentTimeMillis() + LOCKOUT_MILLIS
            failedAttempts = 0
            startLockoutTicker()
        }
    }

    private fun startLockoutTicker() {
        viewModelScope.launch {
            while (true) {
                val remaining = lockoutUntil - System.currentTimeMillis()
                if (remaining <= 0) {
                    _lockoutRemaining.value = 0L
                    break
                }
                _lockoutRemaining.value = remaining
                delay(1_000L)
            }
        }
    }
}