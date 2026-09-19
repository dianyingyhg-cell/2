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

    private enum class DragTarget { NONE, MAIN, SUB }

    private var source: Bitmap? = null
    private var template = TextTemplate("preview", "当前")
    private var dragTarget = DragTarget.NONE
    private var contentRect = RectF()
    private var mainRect = RectF()
    private var subRect = RectF()
    private val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    fun setSource(bitmap: Bitmap?) {
        source = bitmap
        invalidate()
    }

    fun setTemplate(t: TextTemplate) {
        template = t.copy()
        invalidate()
    }

    fun updateMainText(value: String) { template.mainText = value; invalidate() }
    fun updateSubText(value: String) { template.subText = value; invalidate() }
    fun updateMainTextSize(size: Float) { template.mainTextSize = size; invalidate() }
    fun updateSubTextSize(size: Float) { template.subTextSize = size; invalidate() }
    fun updateMainRotation(value: Float) { template.mainRotation = value; invalidate() }
    fun updateSubRotation(value: Float) { template.subRotation = value; invalidate() }

    fun currentTemplate(name: String = "当前模板"): TextTemplate = template.copy(name = name)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.rgb(18, 18, 18))
        val targetRatio = 9f / 20f
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
            val srcRatio = bitmap.width.toFloat() / bitmap.height.coerceAtLeast(1)
            val dstRatio = contentRect.width() / contentRect.height()
            val drawRect = if (srcRatio > dstRatio) {
                val drawH = contentRect.width() / srcRatio
                val top = contentRect.centerY() - drawH / 2f
                RectF(contentRect.left, top, contentRect.right, top + drawH)
            } else {
                val drawW = contentRect.height() * srcRatio
                val left = contentRect.centerX() - drawW / 2f
                RectF(left, contentRect.top, left + drawW, contentRect.bottom)
            }
            canvas.drawBitmap(bitmap, null, drawRect, imagePaint)
        } ?: run {
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.LTGRAY; textSize = 42f; textAlign = Paint.Align.CENTER
            }
            canvas.drawText("先选择一张商品截图", contentRect.centerX(), contentRect.centerY(), p)
        }

        val baseScale = contentRect.width() / 360f
        mainRect = drawSticker(
            canvas = canvas,
            text = template.mainText,
            x = template.mainX,
            y = template.mainY,
            textSize = template.mainTextSize,
            rotation = template.mainRotation,
            textColor = template.mainTextColor,
            bgColor = template.mainBgColor,
            padX = template.mainPadX,
            padY = template.mainPadY,
            baseScale = baseScale
        )
        subRect = drawSticker(
            canvas = canvas,
            text = template.subText,
            x = template.subX,
            y = template.subY,
            textSize = template.subTextSize,
            rotation = template.subRotation,
            textColor = template.subTextColor,
            bgColor = template.subBgColor,
            padX = template.subPadX,
            padY = template.subPadY,
            baseScale = baseScale
        )
    }

    private fun drawSticker(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        textSize: Float,
        rotation: Float,
        textColor: Int,
        bgColor: Int,
        padX: Float,
        padY: Float,
        baseScale: Float
    ): RectF {
        val value = text.ifBlank { " " }
        val lines = value.lines()
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            this.textSize = textSize * baseScale
        }
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
            setShadowLayer(10f * baseScale, 0f, 4f * baseScale, 0x33000000)
        }
        setLayerType(LAYER_TYPE_SOFTWARE, bgPaint)

        val cx = contentRect.left + x * contentRect.width()
        val cy = contentRect.top + y * contentRect.height()
        val lineHeight = fillPaint.textSize * 1.10f
        val widths = lines.map { fillPaint.measureText(it) }
        val maxWidth = (widths.maxOrNull() ?: 0f)
        val textBlockHeight = lineHeight * lines.size
        val hPad = padX * baseScale
        val vPad = padY * baseScale
        val boxW = maxWidth + hPad * 2f
        val boxH = textBlockHeight + vPad * 2f
        val rect = RectF(cx - boxW / 2f, cy - boxH / 2f, cx + boxW / 2f, cy + boxH / 2f)
        val rectPath = RectF(rect)

        canvas.save()
        canvas.rotate(rotation, cx, cy)
        canvas.drawRoundRect(rectPath, 5f * baseScale, 5f * baseScale, bgPaint)
        var textY = rect.top + vPad + fillPaint.textSize
        lines.forEach { line ->
            canvas.drawText(line, cx, textY, fillPaint)
            textY += lineHeight
        }
        canvas.restore()

        // 用于拖拽命中；用未旋转框即可，手感会更简单
        return rect
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (contentRect.isEmpty) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val p = PointF(event.x, event.y)
                dragTarget = when {
                    subRect.contains(p.x, p.y) -> DragTarget.SUB
                    mainRect.contains(p.x, p.y) -> DragTarget.MAIN
                    else -> DragTarget.NONE
                }
                if (dragTarget != DragTarget.NONE) {
                    parent?.requestDisallowInterceptTouchEvent(true)
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (dragTarget == DragTarget.NONE) return false
                val nx = ((event.x - contentRect.left) / contentRect.width()).coerceIn(0.08f, 0.92f)
                val ny = ((event.y - contentRect.top) / contentRect.height()).coerceIn(0.08f, 0.92f)
                when (dragTarget) {
                    DragTarget.MAIN -> { template.mainX = nx; template.mainY = ny }
                    DragTarget.SUB -> { template.subX = nx; template.subY = ny }
                    DragTarget.NONE -> {}
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dragTarget = DragTarget.NONE
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
