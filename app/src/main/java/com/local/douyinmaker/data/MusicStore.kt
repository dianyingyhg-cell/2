package com.local.douyinmaker.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

object MusicStore {
    private const val PREF = "music_pref"
    private const val KEY = "items"

    fun load(context: Context): MutableList<MusicItem> {
        val raw = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        val arr = JSONArray(raw)
        val out = mutableListOf<MusicItem>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val path = o.getString("path")
            if (File(path).exists()) {
                out += MusicItem(o.getString("id"), o.getString("name"), path, o.optLong("startMs", 0L))
            }
        }
        return out
    }

    fun save(context: Context, list: List<MusicItem>) {
        val arr = JSONArray()
        list.forEach { m ->
            arr.put(JSONObject().apply {
                put("id", m.id); put("name", m.name); put("path", m.path); put("startMs", m.startMs)
            })
        }
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY, arr.toString()).apply()
    }

    fun importUri(context: Context, uri: Uri): MusicItem {
        val resolver = context.contentResolver
        var displayName = "music_${System.currentTimeMillis()}"
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) displayName = c.getString(0) ?: displayName
        }
        val safeName = displayName.replace(Regex("[^a-zA-Z0-9._\\-\\u4e00-\\u9fa5]"), "_")
        val dir = File(context.filesDir, "music").apply { mkdirs() }
        val file = File(dir, "${System.currentTimeMillis()}_$safeName")
        resolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "无法读取所选文件" }
            file.outputStream().use { output -> input.copyTo(output) }
        }
        val item = MusicItem(UUID.randomUUID().toString(), displayName, file.absolutePath, 0L)
        val all = load(context)
        all += item
        save(context, all)
        return item
    }
}
