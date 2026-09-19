package com.example.inkpaperdiary.domain.model

import androidx.compose.ui.graphics.Color
import com.example.inkpaperdiary.core.designsystem.PaperColors

enum class Mood(
    val code: String,
    val displayName: String,
    val tintColor: Color
) {
    HAPPY("HAPPY", "开心", PaperColors.MonoBlack),
    CALM("CALM", "平静", PaperColors.SlateMedium),
    FULFILLED("FULFILLED", "充实", PaperColors.SlateGraphite),
    GRATEFUL("GRATEFUL", "感恩", PaperColors.SlateSteel),
    ANXIOUS("ANXIOUS", "焦虑", PaperColors.SlateSilver),
    TIRED("TIRED", "疲惫", PaperColors.SlateLight),
    SAD("SAD", "难过", PaperColors.SlateMedium),
    ENERGETIC("ENERGETIC", "充满干劲", PaperColors.MonoBlack);

    companion object {
        fun fromCode(code: String?): Mood {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: CALM
        }
    }
}
