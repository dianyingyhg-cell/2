package com.local.douyinmaker

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.*
import com.local.douyinmaker.data.MusicItem
import com.local.douyinmaker.data.MusicStore
import java.io.File

class MusicActivity : Activity() {
    companion object { private const val REQ_MUSIC = 301 }

    private lateinit var listView: ListView
    private var items = mutableListOf<MusicItem>()
    private var player: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_list)
        findViewById<TextView>(R.id.tvTitle).text = "音乐库"
        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnAction).apply {
            text = "导入"
            setOnClickListener { importMusic() }
        }
        listView = findViewById(R.id.listView)
        listView.setOnItemClickListener { _, _, position, _ -> showActions(position) }
        reload()
    }

    private fun reload() {
        items = MusicStore.load(this)
        val rows = items.map { "${it.name}\n默认从 ${"%.1f".format(it.startMs / 1000.0)} 秒开始" }
        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, rows)
    }

    private fun importMusic() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("audio/*", "video/*"))
        }
        startActivityForResult(intent, REQ_MUSIC)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_MUSIC && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            try {
                val item = MusicStore.importUri(this, uri)
                Toast.makeText(this, "已保存：${item.name}", Toast.LENGTH_SHORT).show()
                reload()
            } catch (t: Throwable) {
                Toast.makeText(this, "导入失败：${t.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showActions(position: Int) {
        val item = items[position]
        AlertDialog.Builder(this)
            .setTitle(item.name)
            .setItems(arrayOf("试听", "停止试听", "设置默认起始秒数", "重命名", "删除")) { _, which ->
                when (which) {
                    0 -> play(item)
                    1 -> stopPlay()
                    2 -> editStart(item)
                    3 -> rename(item)
                    4 -> delete(item)
                }
            }
            .show()
    }

    private fun play(item: MusicItem) {
        stopPlay()
        try {
            player = MediaPlayer().apply {
                setDataSource(item.path)
                prepare()
                seekTo(item.startMs.toInt().coerceAtLeast(0))
                start()
            }
        } catch (t: Throwable) {
            Toast.makeText(this, "试听失败：${t.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun stopPlay() {
        try { player?.stop() } catch (_: Throwable) {}
        player?.release()
        player = null
    }

    private fun editStart(item: MusicItem) {
        val input = EditText(this).apply {
            inputType = 2 or 8192
            hint = "秒，例如 3.5"
            setText((item.startMs / 1000.0).toString())
        }
        AlertDialog.Builder(this)
            .setTitle("默认从第几秒开始")
            .setView(input)
            .setPositiveButton("保存") { _, _ ->
                val seconds = input.text.toString().toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                item.startMs = (seconds * 1000).toLong()
                MusicStore.save(this, items)
                reload()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun rename(item: MusicItem) {
        val input = EditText(this).apply { setText(item.name) }
        AlertDialog.Builder(this)
            .setTitle("重命名")
            .setView(input)
            .setPositiveButton("保存") { _, _ ->
                item.name = input.text.toString().ifBlank { item.name }
                MusicStore.save(this, items)
                reload()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun delete(item: MusicItem) {
        AlertDialog.Builder(this)
            .setTitle("删除音乐？")
            .setMessage(item.name)
            .setPositiveButton("删除") { _, _ ->
                stopPlay()
                File(item.path).delete()
                items.removeAll { it.id == item.id }
                MusicStore.save(this, items)
                reload()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onDestroy() {
        stopPlay()
        super.onDestroy()
    }
}
