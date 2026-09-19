package com.local.douyinmaker.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.local.douyinmaker.data.TextTemplate

class OverlayPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var source: Bitmap? = null
    private var template = TextTemplate("preview", "当前", "在这里输入文字")
    private var overlayText: String = template.text
    private val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private var contentRect = RectF()

    fun setSource(bitmap: Bitmap?) {
        source = bitmap
        invalidate()
    }

    fun setTemplate(t: TextTemplate) {
        template = t.copy()
        overlayText = t.text
        invalidate()
    }

    fun updateText(text: String) {
        overlayText = text
        invalidate()
    }

    fun updateTextSize(size: Float) {
        template.textSize = size
        invalidate()
    }

    fun updateRotation(degrees: Float) {
        template.rotation = degrees
        invalidate()
    }

    fun updateTextColor(color: Int) {
        template.textColor = color
        invalidate()
    }

    fun currentTemplate(name: String = "当前模板"): TextTemplate = template.copy(name = name, text = overlayText)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.rgb(18, 18, 18))
        val targetRatio = 9f / 16f
        val viewRatio = width.toFloat() / height.coerceAtLeast(1)
        contentRect = if (viewRatio > targetRatio) {
            val w = height * targetRatio
            val left = (width - w) / 2f
            RectF(left, 0f, left + w, height.toFloat())
        } else {
            val h = width / targetRatio
            val top = (height - h) / 2f
            RectF(0f, top, width.toFloat(), top + h)
        }

        source?.let { bitmap ->
            val srcRatio = bitmap.width.toFloat() / bitmap.height
            val dstRatio = contentRect.width() / contentRect.height()
            val srcRect = if (srcRatio > dstRatio) {
                val cropW = (bitmap.height * dstRatio).toInt()
                val left = (bitmap.width - cropW) / 2
                Rect(left, 0, left + cropW, bitmap.height)
            } else {
                val cropH = (bitmap.width / dstRatio).toInt()
                val top = (bitmap.height - cropH) / 2
                Rect(0, top, bitmap.width, top + cropH)
            }
            canvas.drawBitmap(bitmap, srcRect, contentRect, imagePaint)
        } ?: run {
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY; textSize = 42f; textAlign = Paint.Align.CENTER }
            canvas.drawText("先选择一张商品截图", contentRect.centerX(), contentRect.centerY(), p)
        }

        drawOverlay(canvas)
    }

    private fun drawOverlay(canvas: Canvas) {
        if (contentRect.isEmpty) return
        val baseScale = contentRect.width() / 360f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = template.textSize * baseScale
            strokeJoin = Paint.Join.ROUND
            strokeMiter = 10f
        }
        val lines = overlayText.ifBlank { template.text }.split("\n")
        val cx = contentRect.left + template.x * contentRect.width()
        val cy = contentRect.top + template.y * contentRect.height()
        val lineHeight = paint.textSize * 1.12f
        val total = lineHeight * (lines.size - 1)
        canvas.save()
        canvas.rotate(template.rotation, cx, cy)
        lines.forEachIndexed { index, line ->
            val y = cy - total / 2f + index * lineHeight
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = paint.textSize * 0.09f
            paint.color = Color.BLACK
            canvas.drawText(line, cx, y, paint)
            paint.style = Paint.Style.FILL
            paint.color = template.textColor
            canvas.drawText(line, cx, y, paint)
        }
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (contentRect.isEmpty) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val nx = ((event.x - contentRect.left) / contentRect.width()).coerceIn(0.05f, 0.95f)
                val ny = ((event.y - contentRect.top) / contentRect.height()).coerceIn(0.05f, 0.95f)
                template.x = nx
                template.y = ny
                invalidate()
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
