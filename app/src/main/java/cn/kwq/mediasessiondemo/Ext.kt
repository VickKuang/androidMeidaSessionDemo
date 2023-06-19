package cn.kwq.mediasessiondemo

import android.content.Context
import android.net.Uri
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaMetadataCompat

fun Int.raw2uri(context: Context):Uri{
    val uriStr = "android.resource://" + context.packageName + "/" + this
    return Uri.parse(uriStr)
}

fun MutableList<Music>.nextIndex(index:Int):Int{
    return (index + 1) % this.size
}

fun MutableList<Music>.preIndex(index:Int):Int{
    return (index + this.size - 1) % this.size
}

fun Music?.coverMediaItem(): MediaMetadataCompat? {
    return MediaMetadataCompat.Builder()
        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "" + this?.mediaId)
        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, this?.songName)
        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, this?.artist)
        .build()
}

