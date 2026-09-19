package com.local.douyinmaker.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.min

/**
 * Lightweight local crop editor. The crop rectangle is stored as normalized bitmap coordinates
 * (0..1), so the same crop can be reused on screenshots with different pixel dimensions.
 */
class CropOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var bitmap: Bitmap? = null
    private val imageRect = RectF()
    private var cropNorm = RectF(0.08f, 0.08f, 0.92f, 0.92f)
    private val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val shadePaint = Paint().apply { color = 0x99000000.toInt() }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private enum class DragMode { NONE, MOVE, LEFT_TOP, RIGHT_TOP, LEFT_BOTTOM, RIGHT_BOTTOM }
    private var mode = DragMode.NONE
    private var lastX = 0f
    private var lastY = 0f
    private val minNormSize = 0.10f

    fun setBitmap(value: Bitmap, initialCrop: RectF? = null) {
        bitmap = value
        cropNorm = initialCrop?.let { sanitize(it) } ?: defaultCrop(value)
        invalidate()
    }

    fun resetCrop() {
        bitmap?.let {
            cropNorm = defaultCrop(it)
            invalidate()
        }
    }

    fun normalizedCrop(): RectF = RectF(cropNorm)

    fun cropBitmap(): Bitmap {
        val src = requireNotNull(bitmap) { "没有可裁剪的图片" }
        val left = (cropNorm.left * src.width).toInt().coerceIn(0, src.width - 1)
        val top = (cropNorm.top * src.height).toInt().coerceIn(0, src.height - 1)
        val right = (cropNorm.right * src.width).toInt().coerceIn(left + 1, src.width)
        val bottom = (cropNorm.bottom * src.height).toInt().coerceIn(top + 1, src.height)
        val cropped = Bitmap.createBitmap(src, left, top, right - left, bottom - top)
        return if (cropped.config == Bitmap.Config.HARDWARE) {
            cropped.copy(Bitmap.Config.ARGB_8888, false)
                ?: error("无法转换裁剪图片格式")
        } else {
            cropped
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.rgb(20, 20, 20))
        val src = bitmap ?: return
        calculateImageRect(src)
        canvas.drawBitmap(src, null, imageRect, imagePaint)

        val c = cropViewRect()
        canvas.drawRect(imageRect.left, imageRect.top, imageRect.right, c.top, shadePaint)
        canvas.drawRect(imageRect.left, c.bottom, imageRect.right, imageRect.bottom, shadePaint)
        canvas.drawRect(imageRect.left, c.top, c.left, c.bottom, shadePaint)
        canvas.drawRect(c.right, c.top, imageRect.right, c.bottom, shadePaint)
        canvas.drawRect(c, borderPaint)

        val radius = 12f * resources.displayMetrics.density
        canvas.drawCircle(c.left, c.top, radius, handlePaint)
        canvas.drawCircle(c.right, c.top, radius, handlePaint)
        canvas.drawCircle(c.left, c.bottom, radius, handlePaint)
        canvas.drawCircle(c.right, c.bottom, radius, handlePaint)
    }

    private fun calculateImageRect(src: Bitmap) {
        val vw = width.toFloat().coerceAtLeast(1f)
        val vh = height.toFloat().coerceAtLeast(1f)
        val scale = min(vw / src.width, vh / src.height)
        val w = src.width * scale
        val h = src.height * scale
        val l = (vw - w) / 2f
        val t = (vh - h) / 2f
        imageRect.set(l, t, l + w, t + h)
    }

    private fun cropViewRect(): RectF {
        return RectF(
            imageRect.left + cropNorm.left * imageRect.width(),
            imageRect.top + cropNorm.top * imageRect.height(),
            imageRect.left + cropNorm.right * imageRect.width(),
            imageRect.top + cropNorm.bottom * imageRect.height()
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (bitmap == null || imageRect.isEmpty) return false
        val c = cropViewRect()
        val threshold = 30f * resources.displayMetrics.density
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mode = when {
                    near(event.x, event.y, c.left, c.top, threshold) -> DragMode.LEFT_TOP
                    near(event.x, event.y, c.right, c.top, threshold) -> DragMode.RIGHT_TOP
                    near(event.x, event.y, c.left, c.bottom, threshold) -> DragMode.LEFT_BOTTOM
                    near(event.x, event.y, c.right, c.bottom, threshold) -> DragMode.RIGHT_BOTTOM
                    c.contains(event.x, event.y) -> DragMode.MOVE
                    else -> DragMode.NONE
                }
                lastX = event.x
                lastY = event.y
                if (mode != DragMode.NONE) {
                    parent?.requestDisallowInterceptTouchEvent(true)
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (mode == DragMode.NONE) return false
                val dx = (event.x - lastX) / imageRect.width().coerceAtLeast(1f)
                val dy = (event.y - lastY) / imageRect.height().coerceAtLeast(1f)
                applyDrag(dx, dy)
                lastX = event.x
                lastY = event.y
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mode = DragMode.NONE
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return true
    }

    private fun applyDrag(dx: Float, dy: Float) {
        var l = cropNorm.left
        var t = cropNorm.top
        var r = cropNorm.right
        var b = cropNorm.bottom

        when (mode) {
            DragMode.MOVE -> {
                val w = r - l
                val h = b - t
                l = (l + dx).coerceIn(0f, 1f - w)
                t = (t + dy).coerceIn(0f, 1f - h)
                r = l + w
                b = t + h
            }
            DragMode.LEFT_TOP -> {
                l = (l + dx).coerceIn(0f, r - minNormSize)
                t = (t + dy).coerceIn(0f, b - minNormSize)
            }
            DragMode.RIGHT_TOP -> {
                r = (r + dx).coerceIn(l + minNormSize, 1f)
                t = (t + dy).coerceIn(0f, b - minNormSize)
            }
            DragMode.LEFT_BOTTOM -> {
                l = (l + dx).coerceIn(0f, r - minNormSize)
                b = (b + dy).coerceIn(t + minNormSize, 1f)
            }
            DragMode.RIGHT_BOTTOM -> {
                r = (r + dx).coerceIn(l + minNormSize, 1f)
                b = (b + dy).coerceIn(t + minNormSize, 1f)
            }
            DragMode.NONE -> Unit
        }
        cropNorm = RectF(l, t, r, b)
    }

    private fun near(x: Float, y: Float, hx: Float, hy: Float, threshold: Float): Boolean =
        abs(x - hx) <= threshold && abs(y - hy) <= threshold

    private fun defaultCrop(src: Bitmap): RectF {
        // Centered 9:20 selection when possible, matching modern full-screen phone screenshots.
        val target = 9f / 20f
        val ratio = src.width.toFloat() / src.height.coerceAtLeast(1)
        return if (ratio > target) {
            val widthNorm = (target / ratio).coerceIn(0.1f, 1f)
            val l = (1f - widthNorm) / 2f
            RectF(l, 0f, 1f - l, 1f)
        } else {
            val heightNorm = (ratio / target).coerceIn(0.1f, 1f)
            val t = (1f - heightNorm) / 2f
            RectF(0f, t, 1f, 1f - t)
        }
    }

    private fun sanitize(input: RectF): RectF {
        val l = input.left.coerceIn(0f, 0.9f)
        val t = input.top.coerceIn(0f, 0.9f)
        val r = input.right.coerceIn(l + minNormSize, 1f)
        val b = input.bottom.coerceIn(t + minNormSize, 1f)
        return RectF(l, t, r, b)
    }
}
