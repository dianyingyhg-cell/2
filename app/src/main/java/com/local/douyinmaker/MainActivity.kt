package com.local.douyinmaker

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
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
    companion object {
        private const val REQ_IMAGE = 201
    }

    private lateinit var webView: WebView
    private lateinit var preview: OverlayPreviewView
    private lateinit var etUrl: EditText
    private lateinit var etText: EditText
    private lateinit var etDuration: EditText
    private lateinit var tvMusic: TextView
    private lateinit var tvStatus: TextView
    private lateinit var progress: ProgressBar
    private lateinit var seekTextSize: SeekBar
    private lateinit var seekRotation: SeekBar
    private lateinit var tvTextSize: TextView
    private lateinit var tvRotation: TextView
    private lateinit var btnGenerate: Button

    private var sourceBitmap: Bitmap? = null
    private var lastOriginalBitmap: Bitmap? = null
    private var selectedMusic: MusicItem? = null
    private var activeTemplate = TextTemplate(UUID.randomUUID().toString(), "当前", "白拿{商品}\n嘿嘿")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindViews()
        setupWebView()
        setupActions()
        applyTemplate(TemplateStore.load(this).first())
    }

    @Suppress("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.databaseEnabled = true
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
    }

    private fun bindViews() {
        webView = findViewById(R.id.webView)
        preview = findViewById(R.id.preview)
        etUrl = findViewById(R.id.etUrl)
        etText = findViewById(R.id.etOverlayText)
        etDuration = findViewById(R.id.etDuration)
        tvMusic = findViewById(R.id.tvMusic)
        tvStatus = findViewById(R.id.tvStatus)
        progress = findViewById(R.id.progress)
        seekTextSize = findViewById(R.id.seekTextSize)
        seekRotation = findViewById(R.id.seekRotation)
        tvTextSize = findViewById(R.id.tvTextSize)
        tvRotation = findViewById(R.id.tvRotation)
        btnGenerate = findViewById(R.id.btnGenerate)
    }

    private fun setupActions() {
        findViewById<Button>(R.id.btnTemplates).setOnClickListener { startActivity(Intent(this, TemplateActivity::class.java)) }
        findViewById<Button>(R.id.btnMusic).setOnClickListener { startActivity(Intent(this, MusicActivity::class.java)) }
        findViewById<Button>(R.id.btnHistory).setOnClickListener { startActivity(Intent(this, HistoryActivity::class.java)) }

        findViewById<Button>(R.id.btnPaste).setOnClickListener { pasteLink() }
        findViewById<Button>(R.id.btnOpen).setOnClickListener { openLink() }
        findViewById<Button>(R.id.btnCapture).setOnClickListener { captureWebView(showCropEditor = true) }
        findViewById<Button>(R.id.btnQuickCapture).setOnClickListener { captureWebView(showCropEditor = false) }
        findViewById<Button>(R.id.btnRecrop).setOnClickListener {
            val original = lastOriginalBitmap ?: sourceBitmap
            if (original == null) toast("先截图或选择图片") else showCropDialog(original)
        }
        findViewById<Button>(R.id.btnPickImage).setOnClickListener { pickImage() }
        findViewById<Button>(R.id.btnChooseTemplate).setOnClickListener { chooseTemplate() }
        findViewById<Button>(R.id.btnSaveTemplate).setOnClickListener { saveCurrentTemplate() }
        findViewById<Button>(R.id.btnChooseMusic).setOnClickListener { chooseMusic() }
        btnGenerate.setOnClickListener { generateVideo() }

        etText.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) preview.updateText(etText.text.toString()) }
        etText.addTextChangedListener(SimpleTextWatcher { preview.updateText(it) })

        seekTextSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {
                val size = 30f + value
                tvTextSize.text = "字号：${size.toInt()}"
                preview.updateTextSize(size)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekRotation.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {
                val degrees = value - 15f
                tvRotation.text = "旋转：${degrees.toInt()}°"
                preview.updateRotation(degrees)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun pasteLink() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = cm.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
        if (text.isBlank()) toast("剪贴板没有文字") else etUrl.setText(extractUrl(text))
    }

    private fun extractUrl(text: String): String {
        val match = Regex("https?://\\S+").find(text)?.value
        return (match ?: text.trim()).trimEnd('。', '，', ',', ')', '）')
    }

    private fun openLink() {
        var url = etUrl.text.toString().trim()
        if (url.isBlank()) return toast("先粘贴商品链接")
        if (!url.startsWith("http://") && !url.startsWith("https://")) url = "https://$url"
        webView.loadUrl(url)
        tvStatus.text = "已打开链接；滚动到想要的画面后点“截图当前画面”"
    }

    private fun captureWebView(showCropEditor: Boolean) {
        if (webView.width <= 0 || webView.height <= 0) return toast("网页还没准备好")
        val bitmap = Bitmap.createBitmap(webView.width, webView.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        webView.draw(canvas)
        lastOriginalBitmap = bitmap

        if (showCropEditor) {
            showCropDialog(bitmap)
        } else {
            val saved = CropStore.load(this)
            if (saved == null) {
                toast("还没有保存过裁剪区域，先手动裁剪一次")
                showCropDialog(bitmap)
            } else {
                sourceBitmap = cropBitmap(bitmap, saved)
                preview.setSource(sourceBitmap)
                tvStatus.text = "已按上次区域快速截图；可直接加字和生成视频"
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
            text = "拖动框内可移动；拖四个白点可调整大小。点“使用裁剪”后会记住这个区域。"
            setPadding(0, 0, 0, (8 * density).toInt())
        }
        val cropView = CropOverlayView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (520 * density).toInt()
            )
            setBitmap(bitmap, CropStore.load(this@MainActivity))
        }
        holder.addView(hint)
        holder.addView(cropView)

        val dialog = AlertDialog.Builder(this)
            .setTitle("裁剪商品画面")
            .setView(holder)
            .setNeutralButton("重置", null)
            .setNegativeButton("取消", null)
            .setPositiveButton("使用裁剪", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener { cropView.resetCrop() }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                try {
                    val rect = cropView.normalizedCrop()
                    CropStore.save(this, rect)
                    sourceBitmap = cropView.cropBitmap()
                    preview.setSource(sourceBitmap)
                    tvStatus.text = "裁剪完成，区域已记住；下次可点“快速截(上次区域)”"
                    dialog.dismiss()
                } catch (t: Throwable) {
                    toast("裁剪失败：${t.message}")
                }
            }
        }
        dialog.show()
    }

    private fun cropBitmap(bitmap: Bitmap, rect: RectF): Bitmap {
        val left = (rect.left * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val top = (rect.top * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        val right = (rect.right * bitmap.width).toInt().coerceIn(left + 1, bitmap.width)
        val bottom = (rect.bottom * bitmap.height).toInt().coerceIn(top + 1, bitmap.height)
        return Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
    }

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
                val bitmap = ImageDecoder.decodeBitmap(source) { decoder, _, _ -> decoder.isMutableRequired = false }
                lastOriginalBitmap = bitmap
                showCropDialog(bitmap)
                tvStatus.text = "已载入图片，请选择裁剪区域"
            } catch (t: Throwable) {
                toast("图片读取失败：${t.message}")
            }
        }
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
        etText.setText(activeTemplate.text)
        seekTextSize.progress = (activeTemplate.textSize - 30f).toInt().coerceIn(0, 90)
        seekRotation.progress = (activeTemplate.rotation + 15f).toInt().coerceIn(0, 30)
    }

    private fun saveCurrentTemplate() {
        val input = EditText(this).apply { hint = "模板名称" }
        AlertDialog.Builder(this)
            .setTitle("保存当前文字样式")
            .setView(input)
            .setPositiveButton("保存") { _, _ ->
                val name = input.text.toString().ifBlank { "模板${System.currentTimeMillis() % 10000}" }
                val t = preview.currentTemplate(name).copy(id = UUID.randomUUID().toString(), text = etText.text.toString())
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
            AlertDialog.Builder(this)
                .setTitle("音乐库为空")
                .setMessage("先去音乐库导入你提供的音频或带音轨的视频。")
                .setPositiveButton("打开音乐库") { _, _ -> startActivity(Intent(this, MusicActivity::class.java)) }
                .setNegativeButton("取消", null)
                .show()
            return
        }
        val labels = arrayOf("不加音乐") + items.map { it.name }.toTypedArray()
        AlertDialog.Builder(this).setTitle("选择音乐").setItems(labels) { _, which ->
            selectedMusic = if (which == 0) null else items[which - 1]
            tvMusic.text = selectedMusic?.name ?: "不加音乐"
        }.show()
    }

    private fun generateVideo() {
        val src = sourceBitmap ?: return toast("先截图或选择商品图片")
        val seconds = etDuration.text.toString().toDoubleOrNull() ?: return toast("请输入正确的视频时长")
        if (seconds < 0.7 || seconds > 15.0) return toast("视频时长支持 0.7～15 秒")
        val durationMs = (seconds * 1000).toLong()
        val template = preview.currentTemplate("导出").copy(text = etText.text.toString())

        btnGenerate.isEnabled = false
        progress.progress = 1
        tvStatus.text = "正在准备画面…"

        Thread {
            try {
                val frame = FrameComposer.compose(src, template, etText.text.toString())
                runOnUiThread {
                    tvStatus.text = "正在生成 MP4…"
                    VideoExporter.export(this, frame, durationMs, selectedMusic,
                        VideoExporter.Callbacks(
                            onProgress = { value -> progress.progress = value },
                            onSuccess = { uri ->
                                btnGenerate.isEnabled = true
                                progress.progress = 100
                                tvStatus.text = "完成：已保存到 Movies/DouyinLocalMaker"
                                toast("视频已保存到相册")
                            },
                            onError = { error ->
                                btnGenerate.isEnabled = true
                                tvStatus.text = "生成失败：${error.message}"
                                toast("生成失败：${error.message}")
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
