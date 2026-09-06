package com.example.inkpaperdiary.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import com.example.inkpaperdiary.domain.model.Mood
import com.example.inkpaperdiary.domain.model.Weather

/**
 * 手绘风格自制图标集：统一的 24x24 视口、圆头描边线条语言，
 * 替代系统 emoji，与纸质墨迹主题保持一致的视觉质感。
 */

private const val VIEWPORT = 24f

@Composable
private fun LineGlyph(
    tint: Color,
    modifier: Modifier,
    content: DrawScope.(stroke: Stroke) -> Unit
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val scale = size.minDimension / VIEWPORT
        val stroke = Stroke(
            width = 1.7f * scale,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
        withTransform({ scale(scale, scale, pivot = Offset.Zero) }) {
            content(stroke)
        }
    }
}

// ---------- 通用零件 ----------

private fun DrawScope.faceCircle(tint: Color, stroke: Stroke) {
    drawCircle(tint, radius = 8.5f, center = Offset(12f, 12f), style = stroke)
}

private fun DrawScope.openEyes(tint: Color) {
    drawCircle(tint, radius = 1.1f, center = Offset(8.9f, 10f))
    drawCircle(tint, radius = 1.1f, center = Offset(15.1f, 10f))
}

private fun DrawScope.closedEyes(tint: Color, stroke: Stroke) {
    drawArc(
        color = tint, startAngle = 15f, sweepAngle = 150f, useCenter = false,
        topLeft = Offset(6.9f, 8.4f), size = Size(4.2f, 3.2f), style = stroke
    )
    drawArc(
        color = tint, startAngle = 15f, sweepAngle = 150f, useCenter = false,
        topLeft = Offset(12.9f, 8.4f), size = Size(4.2f, 3.2f), style = stroke
    )
}

private fun DrawScope.smile(tint: Color, stroke: Stroke, top: Float, height: Float, width: Float) {
    drawArc(
        color = tint, startAngle = 15f, sweepAngle = 150f, useCenter = false,
        topLeft = Offset(12f - width / 2f, top), size = Size(width, height), style = stroke
    )
}

/** 云朵轮廓：三段圆弧包络 + 平底。坐标均为 24 视口空间。 */
private fun cloudPath(l: Float, r: Float, bottom: Float): Path {
    val w = r - l
    val bigR = w * 0.26f
    val bigC = Offset(l + w * 0.52f, bottom - bigR)
    val leftR = w * 0.19f
    val leftC = Offset(l + w * 0.22f, bottom - leftR)
    val rightR = w * 0.19f
    val rightC = Offset(r - w * 0.18f, bottom - rightR)
    return Path().apply {
        arcTo(Rect(leftC.x - leftR, leftC.y - leftR, leftC.x + leftR, leftC.y + leftR), 90f, 180f, true)
        arcTo(Rect(bigC.x - bigR, bigC.y - bigR, bigC.x + bigR, bigC.y + bigR), 150f, 240f, false)
        arcTo(Rect(rightC.x - rightR, rightC.y - rightR, rightC.x + rightR, rightC.y + rightR), 225f, 225f, false)
        lineTo(leftC.x, bottom)
        close()
    }
}

private fun flamePath(): Path = Path().apply {
    moveTo(12f, 3.2f)
    cubicTo(15.2f, 7.2f, 17.8f, 10.2f, 17.8f, 13.6f)
    cubicTo(17.8f, 17.6f, 15.2f, 20.6f, 12f, 20.6f)
    cubicTo(8.8f, 20.6f, 6.2f, 17.6f, 6.2f, 13.6f)
    cubicTo(6.2f, 11.2f, 8f, 8.8f, 9.6f, 6.4f)
    cubicTo(9.9f, 8.6f, 10.8f, 9.8f, 12f, 10.6f)
    close()
}

// ---------- 心情图标 ----------

@Composable
fun MoodIcon(
    mood: Mood,
    modifier: Modifier = Modifier,
    tint: Color = mood.tintColor
) {
    LineGlyph(tint = tint, modifier = modifier) { stroke ->
        when (mood) {
            Mood.HAPPY -> {
                faceCircle(tint, stroke)
                openEyes(tint)
                smile(tint, stroke, top = 10.6f, height = 7f, width = 8.8f)
            }
            Mood.CALM -> {
                faceCircle(tint, stroke)
                closedEyes(tint, stroke)
                smile(tint, stroke, top = 11.8f, height = 4.6f, width = 5.8f)
            }
            Mood.FULFILLED -> {
                // 四角星光
                val star = Path().apply {
                    moveTo(12f, 4f)
                    cubicTo(12.9f, 9.6f, 14.4f, 11.1f, 20f, 12f)
                    cubicTo(14.4f, 12.9f, 12.9f, 14.4f, 12f, 20f)
                    cubicTo(11.1f, 14.4f, 9.6f, 12.9f, 4f, 12f)
                    cubicTo(9.6f, 11.1f, 11.1f, 9.6f, 12f, 4f)
                    close()
                }
                drawPath(star, tint, style = stroke)
                val spark = Path().apply {
                    moveTo(18.6f, 3.6f)
                    cubicTo(18.9f, 5.2f, 19.4f, 5.7f, 21f, 6f)
                    cubicTo(19.4f, 6.3f, 18.9f, 6.8f, 18.6f, 8.4f)
                    cubicTo(18.3f, 6.8f, 17.8f, 6.3f, 16.2f, 6f)
                    cubicTo(17.8f, 5.7f, 18.3f, 5.2f, 18.6f, 3.6f)
                    close()
                }
                drawPath(spark, tint)
            }
            Mood.GRATEFUL -> {
                // 叶芽
                val leaf = Path().apply {
                    moveTo(5f, 19f)
                    cubicTo(5f, 10f, 10f, 5f, 19f, 5f)
                    cubicTo(19f, 14f, 14f, 19f, 5f, 19f)
                    close()
                }
                drawPath(leaf, tint, style = stroke)
                drawLine(tint, Offset(6.4f, 17.6f), Offset(14.5f, 9.5f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            Mood.ANXIOUS -> {
                faceCircle(tint, stroke)
                openEyes(tint)
                val wavy = Path().apply {
                    moveTo(8.4f, 16.2f)
                    quadraticBezierTo(10f, 14.6f, 11.6f, 16.2f)
                    quadraticBezierTo(13.2f, 17.8f, 15.6f, 16.2f)
                }
                drawPath(wavy, tint, style = stroke)
            }
            Mood.TIRED -> {
                faceCircle(tint, stroke)
                closedEyes(tint, stroke)
                drawLine(tint, Offset(9.5f, 16.2f), Offset(14.5f, 16.2f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            Mood.SAD -> {
                faceCircle(tint, stroke)
                openEyes(tint)
                drawArc(
                    color = tint, startAngle = 200f, sweepAngle = 140f, useCenter = false,
                    topLeft = Offset(8.2f, 14.8f), size = Size(7.6f, 6.2f), style = stroke
                )
                val tear = Path().apply {
                    moveTo(17.9f, 13.4f)
                    cubicTo(19.2f, 15.2f, 19.2f, 16.4f, 17.9f, 16.4f)
                    cubicTo(16.6f, 16.4f, 16.6f, 15.2f, 17.9f, 13.4f)
                    close()
                }
                drawPath(tear, tint)
            }
            Mood.ENERGETIC -> drawPath(flamePath(), tint, style = stroke)
        }
    }
}

// ---------- 天气图标 ----------

@Composable
fun WeatherIcon(
    weather: Weather,
    modifier: Modifier = Modifier,
    tint: Color = weather.tintColor
) {
    LineGlyph(tint = tint, modifier = modifier) { stroke ->
        when (weather) {
            Weather.SUNNY -> {
                drawCircle(tint, radius = 5f, center = Offset(12f, 12f), style = stroke)
                for (i in 0 until 8) {
                    val a = Math.toRadians(i * 45.0)
                    val c = kotlin.math.cos(a).toFloat()
                    val s = kotlin.math.sin(a).toFloat()
                    drawLine(
                        tint,
                        Offset(12f + 7.3f * c, 12f + 7.3f * s),
                        Offset(12f + 9.7f * c, 12f + 9.7f * s),
                        strokeWidth = stroke.width,
                        cap = StrokeCap.Round
                    )
                }
            }
            Weather.CLOUDY -> {
                drawCircle(tint, radius = 3f, center = Offset(8f, 8f), style = stroke)
                // 上半圈光芒
                listOf(200f, 245f, 290f, 335f).forEach { deg ->
                    val a = Math.toRadians(deg.toDouble())
                    val c = kotlin.math.cos(a).toFloat()
                    val s = kotlin.math.sin(a).toFloat()
                    drawLine(
                        tint,
                        Offset(8f + 4.4f * c, 8f + 4.4f * s),
                        Offset(8f + 5.9f * c, 8f + 5.9f * s),
                        strokeWidth = stroke.width,
                        cap = StrokeCap.Round
                    )
                }
                drawPath(cloudPath(9f, 21.5f, 19f), tint, style = stroke)
            }
            Weather.OVERCAST -> {
                drawPath(cloudPath(3.5f, 20.5f, 17.5f), tint, style = stroke)
            }
            Weather.RAINY -> {
                drawPath(cloudPath(4f, 20f, 14.5f), tint, style = stroke)
                listOf(8.5f, 12f, 15.5f).forEach { x ->
                    drawLine(tint, Offset(x, 16.8f), Offset(x - 1f, 20.4f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                }
            }
            Weather.STORMY -> {
                drawPath(cloudPath(4f, 20f, 14.5f), tint, style = stroke)
                val bolt = Path().apply {
                    moveTo(12.8f, 15.8f)
                    lineTo(10f, 19.6f)
                    lineTo(12.6f, 19.6f)
                    lineTo(10.6f, 23f)
                }
                drawPath(bolt, tint, style = stroke)
            }
            Weather.SNOWY -> {
                drawPath(cloudPath(4f, 20f, 14.5f), tint, style = stroke)
                drawCircle(tint, radius = 1.1f, center = Offset(9f, 17.8f))
                drawCircle(tint, radius = 1.1f, center = Offset(12f, 20.6f))
                drawCircle(tint, radius = 1.1f, center = Offset(15f, 17.8f))
            }
            Weather.WINDY -> {
                val w1 = Path().apply {
                    moveTo(3.5f, 9f)
                    cubicTo(9f, 7.2f, 13.5f, 7.4f, 16f, 9.2f)
                    cubicTo(17.6f, 10.4f, 16.6f, 12f, 15f, 11.4f)
                }
                drawPath(w1, tint, style = stroke)
                val w2 = Path().apply {
                    moveTo(3.5f, 14.5f)
                    cubicTo(10.5f, 12.8f, 16.5f, 13.4f, 19f, 15.4f)
                    cubicTo(20.4f, 16.6f, 19.2f, 18.2f, 17.6f, 17.4f)
                }
                drawPath(w2, tint, style = stroke)
            }
        }
    }
}

// ---------- 装饰符号（统计卡片 / 空状态） ----------

/** 火焰：连续记录 */
@Composable
fun FlameGlyph(tint: Color, modifier: Modifier = Modifier) {
    LineGlyph(tint = tint, modifier = modifier) { stroke ->
        drawPath(flamePath(), tint, style = stroke)
    }
}

/** 横向卷轴：累计篇数 / 空状态 */
@Composable
fun ScrollGlyph(tint: Color, modifier: Modifier = Modifier) {
    LineGlyph(tint = tint, modifier = modifier) { stroke ->
        drawCircle(tint, radius = 2.4f, center = Offset(5.8f, 12f), style = stroke)
        drawCircle(tint, radius = 2.4f, center = Offset(18.2f, 12f), style = stroke)
        drawLine(tint, Offset(5.8f, 9.6f), Offset(18.2f, 9.6f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(tint, Offset(5.8f, 14.4f), Offset(18.2f, 14.4f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(tint, Offset(9.6f, 11.2f), Offset(14.4f, 11.2f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(tint, Offset(9.6f, 12.9f), Offset(13f, 12.9f), strokeWidth = stroke.width, cap = StrokeCap.Round)
    }
}

/** 钢笔尖：创作总字数 */
@Composable
fun QuillGlyph(tint: Color, modifier: Modifier = Modifier) {
    LineGlyph(tint = tint, modifier = modifier) { stroke ->
        val nib = Path().apply {
            moveTo(12f, 3.5f)
            cubicTo(14.5f, 6.5f, 16.5f, 9f, 17f, 11.5f)
            lineTo(12f, 20.5f)
            lineTo(7f, 11.5f)
            cubicTo(7.5f, 9f, 9.5f, 6.5f, 12f, 3.5f)
            close()
        }
        drawPath(nib, tint, style = stroke)
        drawCircle(tint, radius = 1.2f, center = Offset(12f, 11.5f), style = stroke)
        drawLine(tint, Offset(12f, 12.7f), Offset(12f, 18.5f), strokeWidth = stroke.width, cap = StrokeCap.Round)
    }
}
