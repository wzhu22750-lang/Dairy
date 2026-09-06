package com.example.inkpaperdiary.domain.model

import androidx.compose.ui.graphics.Color
import com.example.inkpaperdiary.core.designsystem.PaperColors

enum class Weather(
    val code: String,
    val displayName: String,
    val tintColor: Color
) {
    SUNNY("SUNNY", "晴朗", PaperColors.MonoBlack),
    CLOUDY("CLOUDY", "多云", PaperColors.SlateMedium),
    OVERCAST("OVERCAST", "阴天", PaperColors.SlateSteel),
    RAINY("RAINY", "细雨", PaperColors.SlateGraphite),
    STORMY("STORMY", "雷雨", PaperColors.SlateCharcoal),
    SNOWY("SNOWY", "飘雪", PaperColors.SlateSilver),
    WINDY("WINDY", "微风", PaperColors.SlateSteel);

    companion object {
        fun fromCode(code: String?): Weather {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: SUNNY
        }
    }
}
