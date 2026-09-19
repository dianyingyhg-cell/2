package com.local.douyinmaker

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryActivity : Activity() {
    data class Row(val name: String, val uri: Uri, val dateMs: Long)
    private lateinit var listView: ListView
    private var rows = mutableListOf<Row>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_list)
        findViewById<TextView>(R.id.tvTitle).text = "已生成视频"
        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnAction).apply {
            text = "刷新"
            setOnClickListener { reload() }
        }
        listView = findViewById(R.id.listView)
        listView.setOnItemClickListener { _, _, position, _ -> openVideo(rows[position].uri) }
        listView.setOnItemLongClickListener { _, _, position, _ ->
            shareVideo(rows[position].uri)
            true
        }
        reload()
    }

    private fun reload() {
        rows.clear()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.RELATIVE_PATH
        )
        val selection = "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?"
        val args = arrayOf("%DouyinLocalMaker%")
        contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            args,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { c ->
            val idIdx = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameIdx = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val dateIdx = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            while (c.moveToNext()) {
                val id = c.getLong(idIdx)
                val name = c.getString(nameIdx)
                val date = c.getLong(dateIdx) * 1000L
                val uri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id.toString())
                rows += Row(name, uri, date)
            }
        }
        val df = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        val labels = rows.map { "${it.name}\n${df.format(Date(it.dateMs))}  ·  点击播放 / 长按分享" }
        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, labels)
    }

    private fun openVideo(uri: Uri) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/mp4")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            })
        } catch (t: Throwable) {
            Toast.makeText(this, "没有可播放该视频的应用", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareVideo(uri: Uri) {
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, "分享视频"))
    }
}
