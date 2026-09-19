package com.local.douyinmaker.data

import android.content.Context
import android.graphics.Color
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object TemplateStore {
    private const val PREF = "templates_pref"
    private const val KEY = "templates"

    private fun defaultTemplates(): MutableList<TextTemplate> = mutableListOf(
        TextTemplate(
            id = UUID.randomUUID().toString(),
            name = "参考图1·红底白字",
            mainText = "白拿{商品}",
            subText = "hhh",
            mainX = 0.50f,
            mainY = 0.45f,
            mainTextSize = 104f,
            mainRotation = -7f,
            subX = 0.47f,
            subY = 0.58f,
            subTextSize = 84f,
            subRotation = -7f,
            useSticker = true
        ),
        TextTemplate(
            id = UUID.randomUUID().toString(),
            name = "参考图1·垃圾袋",
            mainText = "白拿垃圾袋",
            subText = "嘿嘿",
            mainX = 0.50f,
            mainY = 0.43f,
            mainTextSize = 108f,
            mainRotation = -6f,
            subX = 0.48f,
            subY = 0.56f,
            subTextSize = 88f,
            subRotation = -6f,
            useSticker = true
        ),
        TextTemplate(
            id = UUID.randomUUID().toString(),
            name = "参考图1·手机支架",
            mainText = "白拿一个手机支架",
            subText = "hhh",
            mainX = 0.50f,
            mainY = 0.44f,
            mainTextSize = 100f,
            mainRotation = -7f,
            subX = 0.50f,
            subY = 0.58f,
            subTextSize = 82f,
            subRotation = -7f,
            useSticker = true
        )
    )

    fun load(context: Context): MutableList<TextTemplate> {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val raw = sp.getString(KEY, null)
        if (raw.isNullOrBlank()) {
            val defaults = defaultTemplates()
            save(context, defaults)
            return defaults
        }

        return try {
            val arr = JSONArray(raw)
            val out = mutableListOf<TextTemplate>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out += if (o.has("mainText")) {
                    TextTemplate(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        mainText = o.optString("mainText", "白拿{商品}"),
                        subText = o.optString("subText", "hhh"),
                        mainX = o.optDouble("mainX", 0.50).toFloat(),
                        mainY = o.optDouble("mainY", 0.45).toFloat(),
                        mainTextSize = o.optDouble("mainTextSize", 104.0).toFloat(),
                        mainRotation = o.optDouble("mainRotation", -7.0).toFloat(),
                        mainTextColor = o.optInt("mainTextColor", Color.WHITE),
                        mainBgColor = o.optInt("mainBgColor", Color.parseColor("#EF4444")),
                        mainPadX = o.optDouble("mainPadX", 26.0).toFloat(),
                        mainPadY = o.optDouble("mainPadY", 14.0).toFloat(),
                        subX = o.optDouble("subX", 0.47).toFloat(),
                        subY = o.optDouble("subY", 0.58).toFloat(),
                        subTextSize = o.optDouble("subTextSize", 84.0).toFloat(),
                        subRotation = o.optDouble("subRotation", -7.0).toFloat(),
                        subTextColor = o.optInt("subTextColor", Color.WHITE),
                        subBgColor = o.optInt("subBgColor", Color.parseColor("#EF4444")),
                        subPadX = o.optDouble("subPadX", 18.0).toFloat(),
                        subPadY = o.optDouble("subPadY", 12.0).toFloat(),
                        useSticker = o.optBoolean("useSticker", true)
                    )
                } else {
                    // 兼容 V0.5 老模板
                    val legacyText = o.optString("text", "白拿{商品}\n嘿嘿")
                    val lines = legacyText.lines().filter { it.isNotBlank() }
                    val main = lines.firstOrNull() ?: "白拿{商品}"
                    val sub = if (lines.size >= 2) lines.drop(1).joinToString(" ") else "hhh"
                    val x = o.optDouble("x", 0.50).toFloat()
                    val y = o.optDouble("y", 0.45).toFloat()
                    val size = o.optDouble("textSize", 84.0).toFloat()
                    val rotation = o.optDouble("rotation", -6.0).toFloat()
                    TextTemplate(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        mainText = main,
                        subText = sub,
                        mainX = x,
                        mainY = y,
                        mainTextSize = size + 20f,
                        mainRotation = rotation,
                        subX = x,
                        subY = (y + 0.12f).coerceAtMost(0.85f),
                        subTextSize = (size - 6f).coerceAtLeast(44f),
                        subRotation = rotation,
                        mainTextColor = Color.WHITE,
                        subTextColor = Color.WHITE,
                        mainBgColor = Color.parseColor("#EF4444"),
                        subBgColor = Color.parseColor("#EF4444"),
                        useSticker = true
                    )
                }
            }
            if (out.isEmpty()) defaultTemplates() else out
        } catch (_: Throwable) {
            val defaults = defaultTemplates()
            save(context, defaults)
            defaults
        }
    }

    fun save(context: Context, list: List<TextTemplate>) {
        val arr = JSONArray()
        list.forEach { t ->
            arr.put(JSONObject().apply {
                put("id", t.id)
                put("name", t.name)
                put("mainText", t.mainText)
                put("subText", t.subText)
                put("mainX", t.mainX)
                put("mainY", t.mainY)
                put("mainTextSize", t.mainTextSize)
                put("mainRotation", t.mainRotation)
                put("mainTextColor", t.mainTextColor)
                put("mainBgColor", t.mainBgColor)
                put("mainPadX", t.mainPadX)
                put("mainPadY", t.mainPadY)
                put("subX", t.subX)
                put("subY", t.subY)
                put("subTextSize", t.subTextSize)
                put("subRotation", t.subRotation)
                put("subTextColor", t.subTextColor)
                put("subBgColor", t.subBgColor)
                put("subPadX", t.subPadX)
                put("subPadY", t.subPadY)
                put("useSticker", t.useSticker)
            })
        }
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY, arr.toString()).apply()
    }
}
