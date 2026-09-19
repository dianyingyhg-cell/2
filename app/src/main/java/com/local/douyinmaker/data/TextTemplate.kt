package com.local.douyinmaker.data

import android.graphics.Color

/**
 * V0.6 双贴纸模板。
 * 参考用户提供的达人样式：主标题大红底白字 + 次标题小红底白字。
 */
data class TextTemplate(
    val id: String,
    var name: String,
    var mainText: String = "白拿{商品}",
    var subText: String = "hhh",
    var mainX: Float = 0.50f,
    var mainY: Float = 0.45f,
    var mainTextSize: Float = 104f,
    var mainRotation: Float = -7f,
    var mainTextColor: Int = Color.WHITE,
    var mainBgColor: Int = Color.parseColor("#EF4444"),
    var mainPadX: Float = 26f,
    var mainPadY: Float = 14f,
    var subX: Float = 0.47f,
    var subY: Float = 0.58f,
    var subTextSize: Float = 84f,
    var subRotation: Float = -7f,
    var subTextColor: Int = Color.WHITE,
    var subBgColor: Int = Color.parseColor("#EF4444"),
    var subPadX: Float = 18f,
    var subPadY: Float = 12f,
    var useSticker: Boolean = true
)
