package com.example.inkpaperdiary.domain.model

enum class SyncStatus(val code: Int) {
    SYNCED(0),
    DIRTY(1),
    DELETED(2);

    companion object {
        fun fromCode(code: Int): SyncStatus {
            return entries.find { it.code == code } ?: DIRTY
        }
    }
}
