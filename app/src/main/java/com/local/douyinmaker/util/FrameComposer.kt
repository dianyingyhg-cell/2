package com.local.douyinmaker.util

import android.graphics.*
import com.local.douyinmaker.data.TextTemplate

object FrameComposer {
    fun compose(source: Bitmap, template: TextTemplate, text: String, width: Int = 720, height: Int = 1280): Bitmap {
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.BLACK)

        val safeSource = if (source.config == Bitmap.Config.HARDWARE) {
            source.copy(Bitmap.Config.ARGB_8888, false)
                ?: error("无法把截图转换为可渲染格式")
        } else {
            source
        }

        // FIT_CENTER instead of center-crop.
        // This preserves every pixel the user selected in the crop editor.
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
        canvas.drawBitmap(
            safeSource,
            null,
            dstRect,
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        )

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = template.textSize * (width / 360f)
            strokeJoin = Paint.Join.ROUND
            strokeMiter = 10f
        }
        val lines = text.ifBlank { template.text }.split("\n")
        val cx = template.x * width
        val cy = template.y * height
        val lineHeight = paint.textSize * 1.12f
        val total = lineHeight * (lines.size - 1)

        canvas.save()
        canvas.rotate(template.rotation, cx, cy)
        lines.forEachIndexed { i, line ->
            val y = cy - total / 2f + i * lineHeight
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = paint.textSize * 0.09f
            paint.color = Color.BLACK
            canvas.drawText(line, cx, y, paint)
            paint.style = Paint.Style.FILL
            paint.color = template.textColor
            canvas.drawText(line, cx, y, paint)
        }
        canvas.restore()
        return out
    }
}
