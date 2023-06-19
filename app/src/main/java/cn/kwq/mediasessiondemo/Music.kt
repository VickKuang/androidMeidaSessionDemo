package cn.kwq.mediasessiondemo

import android.provider.MediaStore.Audio.Artists

data class Music(
    var songName:String,
    var artist :String,
    var mediaId: Int
)
