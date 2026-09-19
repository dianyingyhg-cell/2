package com.local.douyinmaker.data

data class TextTemplate(
    val id: String,
    var name: String,
    var text: String,
    var x: Float = 0.5f,
    var y: Float = 0.45f,
    var textSize: Float = 72f,
    var rotation: Float = -5f
)
