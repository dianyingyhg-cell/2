package com.local.douyinmaker.data

import android.content.Context
import android.graphics.RectF

object CropStore {
    private const val PREF = "crop_pref"
    private const val L = "left"
    private const val T = "top"
    private const val R = "right"
    private const val B = "bottom"
    private const val HAS = "has_crop"

    fun load(context: Context): RectF? {
        val p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        if (!p.getBoolean(HAS, false)) return null
        val rect = RectF(
            p.getFloat(L, 0f),
            p.getFloat(T, 0f),
            p.getFloat(R, 1f),
            p.getFloat(B, 1f)
        )
        return sanitize(rect)
    }

    fun save(context: Context, rect: RectF) {
        val r = sanitize(rect)
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putBoolean(HAS, true)
            .putFloat(L, r.left)
            .putFloat(T, r.top)
            .putFloat(R, r.right)
            .putFloat(B, r.bottom)
            .apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun sanitize(input: RectF): RectF {
        val left = input.left.coerceIn(0f, 0.95f)
        val top = input.top.coerceIn(0f, 0.95f)
        val right = input.right.coerceIn(left + 0.05f, 1f)
        val bottom = input.bottom.coerceIn(top + 0.05f, 1f)
        return RectF(left, top, right, bottom)
    }
}
