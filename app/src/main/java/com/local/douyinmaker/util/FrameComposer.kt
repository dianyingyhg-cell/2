package com.local.douyinmaker.util

import android.graphics.*
import com.local.douyinmaker.data.TextTemplate

object FrameComposer {
    fun compose(source: Bitmap, template: TextTemplate, text: String, width: Int = 720, height: Int = 1280): Bitmap {
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.BLACK)

        val srcRatio = source.width.toFloat() / source.height
        val dstRatio = width.toFloat() / height
        val srcRect = if (srcRatio > dstRatio) {
            val cropW = (source.height * dstRatio).toInt()
            val left = (source.width - cropW) / 2
            Rect(left, 0, left + cropW, source.height)
        } else {
            val cropH = (source.width / dstRatio).toInt()
            val top = (source.height - cropH) / 2
            Rect(0, top, source.width, top + cropH)
        }
        canvas.drawBitmap(source, srcRect, Rect(0, 0, width, height), Paint(Paint.ANTI_ALIAS_FLAG))

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
            paint.color = Color.WHITE
            canvas.drawText(line, cx, y, paint)
        }
        canvas.restore()
        return out
    }
}
