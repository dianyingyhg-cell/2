package com.local.douyinmaker.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object TemplateStore {
    private const val PREF = "templates_pref"
    private const val KEY = "templates"

    fun load(context: Context): MutableList<TextTemplate> {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val raw = sp.getString(KEY, null)
        if (raw.isNullOrBlank()) {
            val defaults = mutableListOf(
                TextTemplate(UUID.randomUUID().toString(), "白拿", "白拿{商品}\n嘿嘿", 0.52f, 0.42f, 78f, -6f),
                TextTemplate(UUID.randomUUID().toString(), "一分钱", "我没看错吧\n{商品}\n才0.01", 0.50f, 0.45f, 68f, -3f),
                TextTemplate(UUID.randomUUID().toString(), "快冲", "这个价格可以冲\n{商品}", 0.50f, 0.40f, 70f, 2f)
            )
            save(context, defaults)
            return defaults
        }
        val arr = JSONArray(raw)
        val out = mutableListOf<TextTemplate>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out += TextTemplate(
                id = o.getString("id"),
                name = o.getString("name"),
                text = o.getString("text"),
                x = o.optDouble("x", 0.5).toFloat(),
                y = o.optDouble("y", 0.45).toFloat(),
                textSize = o.optDouble("textSize", 72.0).toFloat(),
                rotation = o.optDouble("rotation", -5.0).toFloat()
            )
        }
        return out
    }

    fun save(context: Context, list: List<TextTemplate>) {
        val arr = JSONArray()
        list.forEach { t ->
            arr.put(JSONObject().apply {
                put("id", t.id); put("name", t.name); put("text", t.text)
                put("x", t.x); put("y", t.y); put("textSize", t.textSize); put("rotation", t.rotation)
            })
        }
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY, arr.toString()).apply()
    }
}
