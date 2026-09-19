package com.local.douyinmaker.media

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.*
import com.local.douyinmaker.data.MusicItem
import java.io.File
import java.io.FileOutputStream

@UnstableApi
object VideoExporter {
    data class Callbacks(
        val onProgress: (Int) -> Unit,
        val onSuccess: (Uri) -> Unit,
        val onError: (Throwable) -> Unit
    )

    fun export(
        context: Context,
        frame: Bitmap,
        durationMs: Long,
        music: MusicItem?,
        callbacks: Callbacks
    ) {
        require(durationMs in 700L..15_000L) { "视频时长需在 0.7～15 秒之间" }

        val cacheDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val stamp = System.currentTimeMillis()
        val imageFile = File(cacheDir, "frame_$stamp.png")
        FileOutputStream(imageFile).use { frame.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val tempOutput = File(cacheDir, "video_$stamp.mp4")
        if (tempOutput.exists()) tempOutput.delete()

        val imageMediaItem = MediaItem.Builder()
            .setUri(Uri.fromFile(imageFile))
            .setImageDurationMs(durationMs)
            .build()
        val editedImage = EditedMediaItem.Builder(imageMediaItem)
            .setFrameRate(30)
            .build()
        val videoSequence = EditedMediaItemSequence.withVideoFrom(listOf(editedImage))

        val sequences = mutableListOf(videoSequence)
        if (music != null) {
            val clipping = MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(music.startMs.coerceAtLeast(0L))
                .build()
            val audioMediaItem = MediaItem.Builder()
                .setUri(Uri.fromFile(File(music.path)))
                .setClippingConfiguration(clipping)
                .build()
            val editedAudio = EditedMediaItem.Builder(audioMediaItem).build()
            val audioSequence = EditedMediaItemSequence.withAudioFrom(listOf(editedAudio))
                .buildUpon()
                .setIsLooping(true)
                .build()
            sequences += audioSequence
        }

        val composition = Composition.Builder(sequences).build()
        val handler = Handler(Looper.getMainLooper())
        lateinit var transformer: Transformer
        val progressHolder = ProgressHolder()

        val listener = object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                callbacks.onProgress(100)
                try {
                    val saved = copyToGallery(context, tempOutput)
                    imageFile.delete()
                    tempOutput.delete()
                    callbacks.onSuccess(saved)
                } catch (t: Throwable) {
                    callbacks.onError(t)
                }
            }

            override fun onError(
                composition: Composition,
                exportResult: ExportResult,
                exportException: ExportException
            ) {
                imageFile.delete()
                tempOutput.delete()
                callbacks.onError(exportException)
            }
        }

        transformer = Transformer.Builder(context)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .addListener(listener)
            .build()

        val progressRunnable = object : Runnable {
            override fun run() {
                try {
                    if (transformer.getProgress(progressHolder) == Transformer.PROGRESS_STATE_AVAILABLE) {
                        callbacks.onProgress(progressHolder.progress)
                    }
                    handler.postDelayed(this, 250)
                } catch (_: Throwable) {
                }
            }
        }
        handler.post(progressRunnable)
        transformer.start(composition, tempOutput.absolutePath)
    }

    private fun copyToGallery(context: Context, src: File): Uri {
        val resolver = context.contentResolver
        val name = "DY_${System.currentTimeMillis()}.mp4"
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/DouyinLocalMaker")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("无法创建相册文件")
        resolver.openOutputStream(uri).use { output ->
            requireNotNull(output) { "无法写入相册" }
            src.inputStream().use { input -> input.copyTo(output) }
        }
        values.clear()
        values.put(MediaStore.Video.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return uri
    }
}
