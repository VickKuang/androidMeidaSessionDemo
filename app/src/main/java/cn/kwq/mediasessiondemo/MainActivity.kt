package cn.kwq.mediasessiondemo

import android.content.ComponentName
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var mediaBrowser: MediaBrowserCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mediaController = MediaControllerCompat.getMediaController(this)
        mediaController.transportControls.play()

    }
    override fun onResume() {
        super.onResume()
        connectRemoteService()
    }

    private fun connectRemoteService() {
        // 声明服务名称
        val componentName = ComponentName(this, MediaService::class.java)
        // 创建浏览器对象
        mediaBrowser = MediaBrowserCompat(this, componentName, connectionCallbacks, null )
        // 建立连接
        mediaBrowser.connect()
    }

    /**
     * 连接回调
     */
    private val connectionCallbacks: MediaBrowserCompat.ConnectionCallback =
        object : MediaBrowserCompat.ConnectionCallback() {
            override fun onConnected() {
                if (mediaBrowser.isConnected) {
                    val mediaId = mediaBrowser.root
                    mediaBrowser.unsubscribe(mediaId)
                    //之前说到订阅的方法还需要一个参数，即设置订阅回调SubscriptionCallback
                    //当Service获取数据后会将数据发送回来，此时会触发SubscriptionCallback.onChildrenLoaded回调
                    mediaBrowser.subscribe(mediaId, browserSubscriptionCallback)
                    try {
                        val mediaController = MediaControllerCompat(this@MainActivity, mediaBrowser!!.sessionToken)
                        MediaControllerCompat.setMediaController(this@MainActivity, mediaController)
                        mediaController.registerCallback(mMediaControllerCallback)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            override fun onConnectionSuspended() {
            }

            override fun onConnectionFailed() {
            }
        }
    /**
     * 被动接收播放信息、状态改变
     */
    var mMediaControllerCallback: MediaControllerCompat.Callback =
        object : MediaControllerCompat.Callback() {
            override fun onSessionDestroyed() {
                // Session销毁
            }

            override fun onQueueChanged(queue: List<MediaSessionCompat.QueueItem>) {
                // 当前播放列表更新回调
            }

            override fun onMetadataChanged(metadata: MediaMetadataCompat) {
                // 数据变化
                //metadata为播放信息
            }

            override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
                // 播放状态变化 state为状态
            }

            override fun onQueueTitleChanged(title: CharSequence) {
                super.onQueueTitleChanged(title)
                //播放列表信息回调，QueueItem在文章后面会提及
            }

            override fun onSessionEvent(event: String, extras: Bundle) {
                super.onSessionEvent(event, extras)
                //自定义的事件回调，满足你各种自定义需求
            }

            override fun onExtrasChanged(extras: Bundle) {
                super.onExtrasChanged(extras)
                //额外信息回调，可以承载播放模式等信息
            }
        }

    /**
     * 向媒体浏览器服务(MediaBrowserService)发起数据订阅请求的回调接口b
     */
    private val browserSubscriptionCallback: MediaBrowserCompat.SubscriptionCallback =
        object : MediaBrowserCompat.SubscriptionCallback() {
            override fun onChildrenLoaded(
                parentId: String,
                children: List<MediaBrowserCompat.MediaItem>,
                options: Bundle
            ) {
                super.onChildrenLoaded(parentId, children, options)
                //订阅消息时添加了Bundle参数，会回调到此方法
                //即mMediaBrowser.subscribe("PARENT_ID_1", mCallback，bundle)的回调
            }

            override fun onChildrenLoaded(
                parentId: String,
                children: List<MediaBrowserCompat.MediaItem>
            ) {
                //children为播放列表
                // TODO: 渲染相关信息
            }

            override fun onError(parentId: String) {
                super.onError(parentId)
            }

            override fun onError(parentId: String, options: Bundle) {
                super.onError(parentId, options)
            }
        }

}
