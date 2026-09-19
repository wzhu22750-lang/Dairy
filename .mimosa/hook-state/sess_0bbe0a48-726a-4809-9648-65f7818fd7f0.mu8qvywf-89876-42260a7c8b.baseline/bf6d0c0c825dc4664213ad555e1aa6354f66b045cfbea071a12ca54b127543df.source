package com.example.inkpaperdiary.core.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 全局应用锁状态管理器
 *
 * 负责掌控应用切到后台 / 退出界面后的锁定逻辑，防止锁屏被绕过，
 * 同时允许系统级外部组件（如相册选择器、文档选择器）在临时唤起时不误触发锁屏。
 */
object AppLockManager {
    private val _isLocked = MutableStateFlow(true)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    /**
     * 是否当前正在启动外部选择器（系统相册 PickVisualMedia / 文档选择器 OpenDocument 等）
     * 为 true 时进入后台 (onStop) 不触发加锁，返回 (onResume) 时自动重置。
     */
    @Volatile
    var isPickerActive: Boolean = false

    /**
     * 主动将应用置为锁定状态（退出前台 / 切到后台时调用）
     */
    fun lock() {
        _isLocked.value = true
    }

    /**
     * 解锁成功
     */
    fun unlock() {
        _isLocked.value = false
    }
}
