package com.local.douyinmaker.data

data class MusicItem(
    val id: String,
    var name: String,
    var path: String,
    var startMs: Long = 0L
)
