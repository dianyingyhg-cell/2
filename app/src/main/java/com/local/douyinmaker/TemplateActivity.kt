package com.local.douyinmaker

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import com.local.douyinmaker.data.TemplateStore
import com.local.douyinmaker.data.TextTemplate
import java.util.UUID

class TemplateActivity : Activity() {
    private lateinit var listView: ListView
    private var templates = mutableListOf<TextTemplate>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_list)
        findViewById<TextView>(R.id.tvTitle).text = "文字模板"
        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnAction).apply {
            text = "新增"
            setOnClickListener { editTemplate(null) }
        }
        listView = findViewById(R.id.listView)
        listView.setOnItemClickListener { _, _, position, _ -> editTemplate(templates[position]) }
        listView.setOnItemLongClickListener { _, _, position, _ ->
            confirmDelete(position)
            true
        }
        reload()
    }

    private fun reload() {
        templates = TemplateStore.load(this)
        val rows = templates.map { "${it.name}\n${it.text.replace("\n", " / ")}  ·  字号${it.textSize.toInt()}  ·  ${it.rotation.toInt()}°" }
        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, rows)
    }

    private fun editTemplate(existing: TextTemplate?) {
        val wrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 10, 40, 10)
        }
        val name = EditText(this).apply { hint = "模板名称"; setText(existing?.name.orEmpty()) }
        val text = EditText(this).apply { hint = "模板文字，可换行"; minLines = 3; setText(existing?.text.orEmpty()) }
        val size = EditText(this).apply { hint = "字号，例如 72"; inputType = 2 or 8192; setText((existing?.textSize ?: 72f).toString()) }
        val rotation = EditText(this).apply { hint = "旋转角度，例如 -5"; inputType = 2 or 4096; setText((existing?.rotation ?: -5f).toString()) }
        wrap.addView(name); wrap.addView(text); wrap.addView(size); wrap.addView(rotation)

        AlertDialog.Builder(this)
            .setTitle(if (existing == null) "新增模板" else "编辑模板")
            .setView(wrap)
            .setPositiveButton("保存") { _, _ ->
                val item = existing ?: TextTemplate(UUID.randomUUID().toString(), "", "")
                item.name = name.text.toString().ifBlank { "未命名模板" }
                item.text = text.text.toString()
                item.textSize = size.text.toString().toFloatOrNull()?.coerceIn(30f, 120f) ?: 72f
                item.rotation = rotation.text.toString().toFloatOrNull()?.coerceIn(-15f, 15f) ?: 0f
                if (existing == null) templates += item
                TemplateStore.save(this, templates)
                reload()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun confirmDelete(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("删除模板？")
            .setMessage(templates[position].name)
            .setPositiveButton("删除") { _, _ ->
                templates.removeAt(position)
                TemplateStore.save(this, templates)
                reload()
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
