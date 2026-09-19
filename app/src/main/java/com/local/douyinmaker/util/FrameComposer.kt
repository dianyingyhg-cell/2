package com.local.douyinmaker.util

import android.graphics.*
import com.local.douyinmaker.data.TextTemplate

object FrameComposer {
    fun compose(source: Bitmap, template: TextTemplate, width: Int = 1080, height: Int = 2400): Bitmap {
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.BLACK)

        val safeSource = if (source.config == Bitmap.Config.HARDWARE) {
            source.copy(Bitmap.Config.ARGB_8888, false) ?: error("无法把截图转换为可渲染格式")
        } else source

        val srcRatio = safeSource.width.toFloat() / safeSource.height.coerceAtLeast(1)
        val dstRatio = width.toFloat() / height
        val dstRect = if (srcRatio > dstRatio) {
            val drawH = width / srcRatio
            val top = (height - drawH) / 2f
            RectF(0f, top, width.toFloat(), top + drawH)
        } else {
            val drawW = height * srcRatio
            val left = (width - drawW) / 2f
            RectF(left, 0f, left + drawW, height.toFloat())
        }
        canvas.drawBitmap(safeSource, null, dstRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))

        drawSticker(
            canvas, template.mainText, template.mainX * width, template.mainY * height,
            template.mainTextSize * (width / 360f), template.mainRotation,
            template.mainTextColor, template.mainBgColor,
            template.mainPadX * (width / 360f), template.mainPadY * (width / 360f)
        )
        drawSticker(
            canvas, template.subText, template.subX * width, template.subY * height,
            template.subTextSize * (width / 360f), template.subRotation,
            template.subTextColor, template.subBgColor,
            template.subPadX * (width / 360f), template.subPadY * (width / 360f)
        )
        return out
    }

    private fun drawSticker(
        canvas: Canvas,
        text: String,
        centerX: Float,
        centerY: Float,
        textSizePx: Float,
        rotation: Float,
        textColor: Int,
        bgColor: Int,
        padXPx: Float,
        padYPx: Float
    ) {
        val value = text.ifBlank { " " }
        val lines = value.split("
")
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = textSizePx
        }
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
            setShadowLayer(14f, 0f, 5f, 0x30000000)
        }
        val lineHeight = textPaint.textSize * 1.10f
        val maxWidth = lines.maxOfOrNull { textPaint.measureText(it) } ?: 0f
        val boxW = maxWidth + padXPx * 2f
        val boxH = lineHeight * lines.size + padYPx * 2f
        val rect = RectF(centerX - boxW / 2f, centerY - boxH / 2f, centerX + boxW / 2f, centerY + boxH / 2f)

        canvas.save()
        canvas.rotate(rotation, centerX, centerY)
        canvas.drawRoundRect(rect, 14f, 14f, bgPaint)
        var y = rect.top + padYPx + textPaint.textSize
        lines.forEach {
            canvas.drawText(it, centerX, y, textPaint)
            y += lineHeight
        }
        canvas.restore()
    }
}
