package com.local.douyinmaker

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.*
import com.local.douyinmaker.data.CropStore
import com.local.douyinmaker.data.MusicItem
import com.local.douyinmaker.data.MusicStore
import com.local.douyinmaker.data.TemplateStore
import com.local.douyinmaker.data.TextTemplate
import com.local.douyinmaker.media.VideoExporter
import com.local.douyinmaker.ui.CropOverlayView
import com.local.douyinmaker.ui.OverlayPreviewView
import com.local.douyinmaker.util.FrameComposer
import java.util.UUID

class MainActivity : Activity() {
    companion object { private const val REQ_IMAGE = 201 }

    private lateinit var preview: OverlayPreviewView
    private lateinit var etMainText: EditText
    private lateinit var etSubText: EditText
    private lateinit var etDuration: EditText
    private lateinit var tvMusic: TextView
    private lateinit var tvStatus: TextView
    private lateinit var progress: ProgressBar
    private lateinit var seekMainSize: SeekBar
    private lateinit var seekSubSize: SeekBar
    private lateinit var seekMainRotation: SeekBar
    private lateinit var seekSubRotation: SeekBar
    private lateinit var tvMainSize: TextView
    private lateinit var tvSubSize: TextView
    private lateinit var tvMainRotation: TextView
    private lateinit var tvSubRotation: TextView
    private lateinit var btnGenerate: Button
    private lateinit var spResolution: Spinner
    private lateinit var tvResolutionHint: TextView

    private var sourceBitmap: Bitmap? = null
    private var lastOriginalBitmap: Bitmap? = null
    private var selectedMusic: MusicItem? = null
    private var activeTemplate = referenceTemplate()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindViews()
        setupSpinners()
        setupActions()
        applyTemplate(activeTemplate)
    }

    private fun bindViews() {
        preview = findViewById(R.id.preview)
        etMainText = findViewById(R.id.etMainText)
        etSubText = findViewById(R.id.etSubText)
        etDuration = findViewById(R.id.etDuration)
        tvMusic = findViewById(R.id.tvMusic)
        tvStatus = findViewById(R.id.tvStatus)
        progress = findViewById(R.id.progress)
        seekMainSize = findViewById(R.id.seekMainSize)
        seekSubSize = findViewById(R.id.seekSubSize)
        seekMainRotation = findViewById(R.id.seekMainRotation)
        seekSubRotation = findViewById(R.id.seekSubRotation)
        tvMainSize = findViewById(R.id.tvMainSize)
        tvSubSize = findViewById(R.id.tvSubSize)
        tvMainRotation = findViewById(R.id.tvMainRotation)
        tvSubRotation = findViewById(R.id.tvSubRotation)
        btnGenerate = findViewById(R.id.btnGenerate)
        spResolution = findViewById(R.id.spResolution)
        tvResolutionHint = findViewById(R.id.tvResolutionHint)
    }

    private fun setupSpinners() {
        val options = arrayOf("1080全屏 · 1080×2400", "4K级全屏 · 2160×4800")
        spResolution.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)
        spResolution.setSelection(0)
        spResolution.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                tvResolutionHint.text = if (position == 0) {
                    "1080全屏：1080×2400（9:20，推荐）"
                } else {
                    "4K级全屏：2160×4800（更大更慢，部分手机可能不支持）"
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupActions() {
        findViewById<Button>(R.id.btnTemplates).setOnClickListener { startActivity(Intent(this, TemplateActivity::class.java)) }
        findViewById<Button>(R.id.btnMusic).setOnClickListener { startActivity(Intent(this, MusicActivity::class.java)) }
        findViewById<Button>(R.id.btnHistory).setOnClickListener { startActivity(Intent(this, HistoryActivity::class.java)) }

        findViewById<Button>(R.id.btnPickImage).setOnClickListener { pickImage() }
        findViewById<Button>(R.id.btnRecrop).setOnClickListener {
            val original = lastOriginalBitmap ?: sourceBitmap
            if (original == null) toast("先选择商品截图") else showCropDialog(original)
        }
        findViewById<Button>(R.id.btnChooseTemplate).setOnClickListener { chooseTemplate() }
        findViewById<Button>(R.id.btnSaveTemplate).setOnClickListener { saveCurrentTemplate() }
        findViewById<Button>(R.id.btnChooseMusic).setOnClickListener { chooseMusic() }
        findViewById<Button>(R.id.btnPresetRef).setOnClickListener { applyTemplate(referenceTemplate()) }
        findViewById<Button>(R.id.btnPresetAlt).setOnClickListener { applyTemplate(referenceTemplateAlt()) }
        btnGenerate.setOnClickListener { generateVideo() }

        etMainText.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) preview.updateMainText(etMainText.text.toString()) }
        etSubText.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) preview.updateSubText(etSubText.text.toString()) }
        etMainText.addTextChangedListener(SimpleTextWatcher { preview.updateMainText(it) })
        etSubText.addTextChangedListener(SimpleTextWatcher { preview.updateSubText(it) })

        seekMainSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {
                val size = 50f + value
                tvMainSize.text = "主标题字号：${size.toInt()}"
                preview.updateMainTextSize(size)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekSubSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {
                val size = 40f + value
                tvSubSize.text = "副标题字号：${size.toInt()}"
                preview.updateSubTextSize(size)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekMainRotation.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {
                val degrees = value - 20f
                tvMainRotation.text = "主标题旋转：${degrees.toInt()}°"
                preview.updateMainRotation(degrees)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekSubRotation.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {
                val degrees = value - 20f
                tvSubRotation.text = "副标题旋转：${degrees.toInt()}°"
                preview.updateSubRotation(degrees)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun referenceTemplate(): TextTemplate = TextTemplate(
        id = UUID.randomUUID().toString(),
        name = "参考图1·红底白字",
        mainText = "白拿一个手机支架",
        subText = "hhh",
        mainX = 0.50f, mainY = 0.45f, mainTextSize = 104f, mainRotation = -7f,
        subX = 0.47f, subY = 0.58f, subTextSize = 84f, subRotation = -7f
    )

    private fun referenceTemplateAlt(): TextTemplate = TextTemplate(
        id = UUID.randomUUID().toString(),
        name = "参考图1·垃圾袋",
        mainText = "白拿垃圾袋",
        subText = "嘿嘿",
        mainX = 0.50f, mainY = 0.43f, mainTextSize = 108f, mainRotation = -6f,
        subX = 0.49f, subY = 0.56f, subTextSize = 88f, subRotation = -6f
    )

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        startActivityForResult(intent, REQ_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_IMAGE && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            try {
                val source = ImageDecoder.createSource(contentResolver, uri)
                val bitmap = ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = false
                }
                lastOriginalBitmap = bitmap
                tvStatus.text = "截图已载入，请裁剪商品和价格区域"
                showCropDialog(bitmap)
            } catch (t: Throwable) {
                toast("图片读取失败：${t.message}")
            }
        }
    }

    private fun showCropDialog(bitmap: Bitmap) {
        val density = resources.displayMetrics.density
        val holder = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((12 * density).toInt(), (8 * density).toInt(), (12 * density).toInt(), 0)
        }
        val hint = TextView(this).apply {
            text = "把商品、券后价、规格等要展示的内容框进去。默认是 9:20 裁剪框。"
            setPadding(0, 0, 0, (8 * density).toInt())
        }
        val cropView = CropOverlayView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (520 * density).toInt())
            setBitmap(bitmap, CropStore.load(this@MainActivity))
        }
        holder.addView(hint)
        holder.addView(cropView)

        val dialog = AlertDialog.Builder(this)
            .setTitle("裁剪商品截图")
            .setView(holder)
            .setNeutralButton("重置", null)
            .setNegativeButton("取消", null)
            .setPositiveButton("使用裁剪", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener { cropView.resetCrop() }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                try {
                    CropStore.save(this, cropView.normalizedCrop())
                    sourceBitmap = cropView.cropBitmap()
                    preview.setSource(sourceBitmap)
                    tvStatus.text = "截图已准备好：拖动两个红底贴纸即可排版"
                    dialog.dismiss()
                } catch (t: Throwable) {
                    toast("裁剪失败：${t.message}")
                }
            }
        }
        dialog.show()
    }

    private fun chooseTemplate() {
        val templates = TemplateStore.load(this)
        val labels = templates.map { it.name }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("选择文字模板")
            .setItems(labels) { _, which -> applyTemplate(templates[which]) }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun applyTemplate(t: TextTemplate) {
        activeTemplate = t.copy()
        preview.setTemplate(activeTemplate)
        etMainText.setText(activeTemplate.mainText)
        etSubText.setText(activeTemplate.subText)
        seekMainSize.progress = (activeTemplate.mainTextSize - 50f).toInt().coerceIn(0, 100)
        seekSubSize.progress = (activeTemplate.subTextSize - 40f).toInt().coerceIn(0, 100)
        seekMainRotation.progress = (activeTemplate.mainRotation + 20f).toInt().coerceIn(0, 40)
        seekSubRotation.progress = (activeTemplate.subRotation + 20f).toInt().coerceIn(0, 40)
    }

    private fun saveCurrentTemplate() {
        val input = EditText(this).apply { hint = "模板名称" }
        AlertDialog.Builder(this)
            .setTitle("保存当前模板")
            .setView(input)
            .setPositiveButton("保存") { _, _ ->
                val name = input.text.toString().ifBlank { "模板${System.currentTimeMillis() % 10000}" }
                val t = preview.currentTemplate(name).copy(id = UUID.randomUUID().toString())
                val all = TemplateStore.load(this)
                all += t
                TemplateStore.save(this, all)
                activeTemplate = t
                toast("已保存模板：$name")
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun chooseMusic() {
        val items = MusicStore.load(this)
        if (items.isEmpty()) {
            selectedMusic = null
            tvMusic.text = "不加音乐"
            toast("音乐库为空，当前按无音乐生成")
            return
        }
        val labels = arrayOf("不加音乐") + items.map { it.name }.toTypedArray()
        AlertDialog.Builder(this).setTitle("选择音乐").setItems(labels) { _, which ->
            selectedMusic = if (which == 0) null else items[which - 1]
            tvMusic.text = selectedMusic?.name ?: "不加音乐"
        }.show()
    }

    private fun generateVideo() {
        val src = sourceBitmap ?: return toast("先选择并裁剪商品截图")
        val seconds = etDuration.text.toString().toDoubleOrNull() ?: return toast("请输入正确的视频时长")
        if (seconds < 0.7 || seconds > 15.0) return toast("视频时长支持 0.7～15 秒")

        val (outputWidth, outputHeight, label) = if (spResolution.selectedItemPosition == 1) {
            Triple(2160, 4800, "4K级全屏")
        } else {
            Triple(1080, 2400, "1080全屏")
        }

        val durationMs = (seconds * 1000).toLong()
        val template = preview.currentTemplate("导出").copy(
            mainText = etMainText.text.toString(),
            subText = etSubText.text.toString()
        )

        btnGenerate.isEnabled = false
        progress.progress = 1
        tvStatus.text = "正在准备 ${label} 画面…"

        Thread {
            try {
                val frame = FrameComposer.compose(src, template, outputWidth, outputHeight)
                runOnUiThread {
                    tvStatus.text = "正在生成 ${label} MP4…"
                    VideoExporter.export(this, frame, durationMs, selectedMusic,
                        VideoExporter.Callbacks(
                            onProgress = { value -> progress.progress = value },
                            onSuccess = {
                                btnGenerate.isEnabled = true
                                progress.progress = 100
                                tvStatus.text = "完成：${label} · ${outputWidth}×${outputHeight}，已保存到相册"
                                toast("${label} 视频已保存到相册")
                            },
                            onError = { error ->
                                btnGenerate.isEnabled = true
                                tvStatus.text = "生成失败：${error.message}"
                                toast(if (label == "4K级全屏") "4K级全屏生成失败，手机可能不支持 2160×4800 编码，可改用1080全屏" else "生成失败：${error.message}")
                            }
                        )
                    )
                }
            } catch (t: Throwable) {
                runOnUiThread {
                    btnGenerate.isEnabled = true
                    tvStatus.text = "准备失败：${t.message}"
                    toast("准备失败：${t.message}")
                }
            }
        }.start()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

private class SimpleTextWatcher(private val onChanged: (String) -> Unit) : android.text.TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = onChanged(s?.toString().orEmpty())
    override fun afterTextChanged(s: android.text.Editable?) {}
}
