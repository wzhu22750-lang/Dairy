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
    /** PIN 固定 4 位：与设置页、锁屏圆点指示器共用同一契约 */
    val maxPinLength: Int = 4
)

class LockViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    private companion object {
        const val PIN_LENGTH = 4
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
    private var isVerifying = false

    /** 冷却剩余毫秒数（>0 表示锁定中） */
    private val _lockoutRemaining = MutableStateFlow(0L)
    val lockoutRemaining: StateFlow<Long> = _lockoutRemaining.asStateFlow()

    fun appendPinDigit(digit: String) {
        if (_isUnlocked.value) return
        if (lockoutRemaining.value > 0L) return // 冷却中不可输入
        if (isVerifying) return // 等待本轮验证结果，避免竞态重复验证

        val updated = (_inputPin.value + digit).take(PIN_LENGTH)
        _inputPin.value = updated

        if (updated.length == PIN_LENGTH) {
            // 输满 4 位立即验证；无论对错都给出明确结果（成功解锁 / 清空重输）
            isVerifying = true
            viewModelScope.launch {
                try {
                    if (settingsRepository.verifyAppPin(updated)) {
                        _isUnlocked.value = true
                        failedAttempts = 0
                    } else {
                        handleMismatch()
                    }
                } finally {
                    isVerifying = false
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