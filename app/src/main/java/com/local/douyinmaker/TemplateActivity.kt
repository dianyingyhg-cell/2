package com.local.douyinmaker

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
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
        listView.setOnItemLongClickListener { _, _, position, _ -> confirmDelete(position); true }
        reload()
    }

    private fun reload() {
        templates = TemplateStore.load(this)
        val rows = templates.map {
            "${it.name}\n主：${it.mainText} · ${it.mainTextSize.toInt()}   副：${it.subText} · ${it.subTextSize.toInt()}"
        }
        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, rows)
    }

    private fun editTemplate(existing: TextTemplate?) {
        val wrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 10, 40, 10)
        }
        val nameInput = EditText(this).apply { hint = "模板名称"; setText(existing?.name.orEmpty()) }
        val mainInput = EditText(this).apply { hint = "主标题"; setText(existing?.mainText.orEmpty()) }
        val subInput = EditText(this).apply { hint = "副标题"; setText(existing?.subText.orEmpty()) }
        val mainSizeInput = EditText(this).apply { hint = "主标题字号"; inputType = 2 or 8192; setText((existing?.mainTextSize ?: 104f).toString()) }
        val subSizeInput = EditText(this).apply { hint = "副标题字号"; inputType = 2 or 8192; setText((existing?.subTextSize ?: 84f).toString()) }
        wrap.addView(nameInput)
        wrap.addView(mainInput)
        wrap.addView(subInput)
        wrap.addView(mainSizeInput)
        wrap.addView(subSizeInput)

        AlertDialog.Builder(this)
            .setTitle(if (existing == null) "新增模板" else "编辑模板")
            .setView(wrap)
            .setPositiveButton("保存") { _, _ ->
                val item = existing ?: TextTemplate(UUID.randomUUID().toString(), "")
                item.name = nameInput.text.toString().ifBlank { "未命名模板" }
                item.mainText = mainInput.text.toString().ifBlank { "白拿{商品}" }
                item.subText = subInput.text.toString().ifBlank { "hhh" }
                item.mainTextSize = mainSizeInput.text.toString().toFloatOrNull()?.coerceIn(50f, 150f) ?: 104f
                item.subTextSize = subSizeInput.text.toString().toFloatOrNull()?.coerceIn(40f, 130f) ?: 84f
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
