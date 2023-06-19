package cn.kwq.mediasessiondemo

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.media.MediaPlayer.OnPreparedListener
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.content.PackageManagerCompat.LOG_TAG
import androidx.media.MediaBrowserServiceCompat
import java.io.IOException


class MediaService : MediaBrowserServiceCompat() {

    private lateinit var parentId: String//播放列表parent id
    private var isHaveAudioFocus = false//是否获取到音源焦点
    private var mediaSession: MediaSessionCompat? = null//session
    private lateinit var playbackState: PlaybackStateCompat//状态对象
    private lateinit var player: MediaPlayer//播放器
    private var isPrepare = false
    private var audioManager: AudioManager? = null
    private var musicList=ArrayList<Music>()// TODO: 数据库或者本地获取数据

    companion object{
        private const val TAG = "MediaService"
        private const val MY_MEDIA_ROOT_ID = "TEST"
        private const val MY_EMPTY_MEDIA_ROOT_ID = "EMPTY"
    }

    /**
     * 当前播放下标
     */
    private var musicIndex = 0

    @SuppressLint("RestrictedApi")
    override fun onCreate() {
        super.onCreate()

        playbackState= PlaybackStateCompat.Builder().apply {
            setActions(
                PlaybackStateCompat.ACTION_PLAY
                        or PlaybackStateCompat.ACTION_PLAY_PAUSE
            )
        }.build()
        // Create a MediaSessionCompat
        mediaSession = MediaSessionCompat(baseContext, LOG_TAG).apply {
            // 启用来自 MediaButtons 和 TransportControls 的回调
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                        or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            // 使用 ACTION_PLAY 设置初始 PlaybackState，以便媒体按钮可以启动播放器
            setPlaybackState(playbackState)
            isActive = true
            //设置callback，这里的callback就是客户端对服务指令到达处
            setCallback(myCallback)
            // 设置会话的令牌，以便客户端活动可以与其通信。
            setSessionToken(sessionToken)

        }
        initMediaPlayer()


    }

    private fun initMediaPlayer() {
        player = MediaPlayer()//初始化MediaPlayer
        //准备监听
        player.setOnPreparedListener(OnPreparedListener {
            Log.d(TAG, "onPrepared: ")
            isPrepare = true
            // 准备就绪
            sendPlaybackState(PlaybackStateCompat.STATE_BUFFERING)
            handlePlay()
        })
        //播放完成监听
        player.setOnCompletionListener(OnCompletionListener {
            Log.d(TAG, "onCompletion: ")
            sendPlaybackState(PlaybackStateCompat.STATE_NONE)
            // 播放完成 重置 播放器
            player.reset();
            // 下一曲
            myCallback.onSkipToNext()
        })
        //播放错误监听
        player.setOnErrorListener(MediaPlayer.OnErrorListener { mp, what, extra ->
            Log.d(TAG, "onError:  what = $what   extra = $extra")
            isPrepare = false
            sendPlaybackState(PlaybackStateCompat.STATE_ERROR)
            // 播放错误 重置 播放器
            player.reset()
            false
        })
        // 设置音频流类型
        player.setAudioStreamType(AudioManager.STREAM_MUSIC)
        //设置声音
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val volumn = audioManager!!.getStreamVolume(AudioManager.STREAM_MUSIC)
        player.setVolume(volumn.toFloat(), volumn.toFloat())
    }

    /**
     * 刷新数据
     */
    private fun getSyncData() {
        // TODO:  更新数据
//        musicList = MusicListData.getPlayListUpdate()
//        musicIndex = 0
        notifyChildrenChanged(parentId)
    }


    /**
     * 客户端连接
     */
    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): MediaBrowserServiceCompat.BrowserRoot {
        // （可选）控制指定包名称的访问级别
        return if (allowBrowsing(clientPackageName, clientUid)) {
            // 返回一个根 ID，客户端可以使用它与 onLoadChildren() 一起检索内容层次结构
            MediaBrowserServiceCompat.BrowserRoot(MY_MEDIA_ROOT_ID, null)
        } else {
            // 客户端可以连接，但此 BrowserRoot 是一个空的层次结构，因此 onLoadChildren 不返回任何内容。这将禁用浏览内容的能力。
            MediaBrowserServiceCompat.BrowserRoot(MY_EMPTY_MEDIA_ROOT_ID, null)
        }
    }

    private fun allowBrowsing(clientPackageName: String, clientUid: Int): Boolean {
        return true
    }

    /**
     * 播放列表
     * @param parentId parentId
     * @param result   result
     */
    override fun onLoadChildren(
        parentMediaId: String,
        result: MediaBrowserServiceCompat.Result<List<MediaBrowserCompat.MediaItem>>
    ) {
        //  不允许浏览
        if (MY_EMPTY_MEDIA_ROOT_ID == parentMediaId) {
            result.sendResult(null)
            return
        }
        // 将信息从当前线程中移除，允许后续调用sendResult方法
        result.detach();
        // 如，假设音乐目录已经加载缓存
        val mediaItems = emptyList<MediaBrowserCompat.MediaItem>()
        // 我们模拟获取数据的过程，真实情况应该是异步从网络或本地读取数据 todo
        //ArrayList<MediaBrowserCompat.MediaItem> mediaItems = MusicListData.transformPlayList(mPlayBeanList);

        // 检查这是否是根菜单
        if (MY_MEDIA_ROOT_ID == parentMediaId) {
            // 为顶层构建 MediaItem 对象，并将它们放入 mediaItems 列表中...
        } else {
            // 检查传递的 parentMediaId 以查看我们所在的子菜单，并将该菜单的子项放入 mediaItems 列表中
        }
        result.sendResult(mediaItems)
        getSyncData()//加载数据
        this.parentId=parentMediaId

    }


    //mediaSession设置的callback，也是客户端控制指令所到达处
    private val myCallback: MediaSessionCompat.Callback = object : MediaSessionCompat.Callback() {
        //重写的方法都是选择性重写的，不完全列列举
        override fun onPlay() {
            super.onPlay()
            Log.d(TAG, "onPlay: isPrepare = $isPrepare")
            //客户端mMediaController.getTransportControls().play()就会调用到这里，以下类推
            //处理播放逻辑
            //处理完成后通知客户端更新，这里就会回调给客户端的MediaController.Callback
            if (!isPrepare) {
                prepareMusicUri(getPlayMusic()?.mediaId?.raw2uri(this@MediaService))
            } else {
                handlePlay()
            }
        }

        override fun onPause() {
            super.onPause()
            Log.d(TAG, "onPause: ")
            handlePause(true)
        }

        override fun onSeekTo(pos: Long) {
            super.onSeekTo(pos)
            seekTo(pos)
            //设置到指定进度时触发
        }

        override fun onSkipToPrevious() {
            val pos: Int = musicList.preIndex(musicIndex)
            Log.e(TAG, "onSkipToPrevious  pos = $pos")
            prepareMusicUri(setPlayPosition(pos)?.mediaId?.raw2uri(this@MediaService))
        }

        override fun onSkipToNext() {
            super.onSkipToNext()
            //下一首
            val pos: Int = musicList.nextIndex(musicIndex)
            Log.d(TAG, "onSkipToNext: pos = $pos")
            prepareMusicUri(setPlayPosition(pos)?.mediaId?.raw2uri(this@MediaService))
        }

        /**
         * 响应MediaControllerCompat.getTransportControls().playFromUri
         *
         * @param uri uri
         * @param extras extras
         */
        override fun onPlayFromUri(uri: Uri, extras: Bundle) {
            Log.e(TAG, "onPlayFromUri")
            val position = extras.getInt("playPosition")
            setPlayPosition(position)
            prepareMusicUri(uri)
        }

        override fun onCustomAction(action: String, extras: Bundle) {
            super.onCustomAction(action, extras)
            //自定义指令发送到的地方
            //对应客户端 mMediaController.getTransportControls().sendCustomAction(...)
            // TODO: 可以做更新列表操作

        }
    }

    /**
     * 申请音源焦点
     * @return 焦点
     */
    private fun requestAudioFocus(): Int {
        val result: Int = audioManager!!.requestAudioFocus(
            onAudioFocusChangeListener, AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )
        isHaveAudioFocus = AudioManager.AUDIOFOCUS_REQUEST_GRANTED == result
        return result
    }


    /**
     * 切换音乐url
     * @param uri uri
     */
    private fun prepareMusicUri(uri: Uri?) {
        Log.d(TAG, "handleOpenUri: uri = $uri")
        if (uri == null) {
            Log.d(TAG, "handlePlayUri: URL == NULL ")
            return
        }
        if (requestAudioFocus() != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.d(
                TAG,
                "handlePlayUri: requestAudioFocus() != AudioManager.AUDIOFOCUS_REQUEST_GRANTED"
            )
            return
        }
        isPrepare = false
        player.reset()
        player.isLooping = false
        try {
            player.setDataSource(this, uri)
        } catch (e: IOException) {
            Log.e(TAG, "handlePlayUri: ", e)
            e.printStackTrace()
        }
        player.prepareAsync()
    }

    /**
     * 播放
     */
    private fun handlePlay() {
        Log.d(TAG, "handlePlay: play   isPrepare = $isPrepare")
        if (!isPrepare) {
            Log.d(TAG, "handlePlay: null == mMediaPlayer || isPrepare $isPrepare")
            return
        }
        if (!player.isPlaying) {
            player.start()
        }
        sendPlaybackState(PlaybackStateCompat.STATE_CONNECTING)
        //我们可以保存当前播放音乐的信息，以便客户端刷新UI
        mediaSession!!.setMetadata(getPlayMusic().coverMediaItem())

    }

    /**
     * 暂停
     * @param isAbandFocus 焦点
     */
    private fun handlePause(isAbandFocus: Boolean) {
        Log.d(TAG, "handlePause: isAbandFocus = $isAbandFocus")
        if ( !isPrepare) {
            return
        }
        if (player.isPlaying) {
            player.pause()
        }
        sendPlaybackState(PlaybackStateCompat.STATE_PAUSED)
    }

    private fun seekTo(pos: Long){
        if ( !isPrepare) {
            return
        }
        if (player.isPlaying) {
            player.seekTo(pos.toInt())
            player.start()
        }
        sendPlaybackState(PlaybackStateCompat.STATE_PLAYING)
    }

    /**
     * 获取当前播放 歌曲
     *
     * @return playbean
     */
    private fun getPlayMusic(): Music? {
        return if (musicIndex >= 0 && musicIndex < musicList.size) {
            musicList[musicIndex]
        } else null
    }

    /**
     * 设置列表 播放下标
     * @param pos pos
     * @return playbean
     */
    private fun setPlayPosition(pos: Int): Music? {
        if (pos >= 0 && pos < musicList.size) {
            musicIndex = pos
            return musicList[musicIndex]
        }
        return null
    }


    /**
     * Set the current capabilities available on this session. This should
     * use a bitmask of the available capabilities.
     * @param state 歌曲状态
     * @return 可用的操作Actions
     */
    private fun getAvailableActions(state: Int): Long {
        var actions = (PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                or PlaybackStateCompat.ACTION_REWIND
                or PlaybackStateCompat.ACTION_FAST_FORWARD
                or PlaybackStateCompat.ACTION_SEEK_TO)
        actions = if (state == PlaybackStateCompat.STATE_PLAYING) {
            actions or PlaybackStateCompat.ACTION_PAUSE
        } else {
            actions or PlaybackStateCompat.ACTION_PLAY
        }
        return actions
    }

    /**
     * 音源切换监听
     */
    private var onAudioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS -> {
                    // 音源丢失
                    isHaveAudioFocus = false
                    myCallback.onPause()
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    // 音源短暂丢失
                    isHaveAudioFocus = false
                    Log.d(TAG, " AUDIOFOCUS_LOSS_TRANSIENT  ")
                    handlePause(false)
                }

                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {}
                AudioManager.AUDIOFOCUS_GAIN -> {
                    // 获得音源
                    isHaveAudioFocus = true
                    myCallback.onPlay()
                }
                AudioManager.AUDIOFOCUS_REQUEST_FAILED -> {}
                else -> {}
            }
        }

    /**
     * 发送歌曲状态
     * @param state 状态
     */
    private fun sendPlaybackState(state: Int, extras: Bundle? = null) {
        playbackState = PlaybackStateCompat.Builder()
            .setState(state, musicIndex.toLong(), 1.0f)
            .setActions(getAvailableActions(state))
            .setExtras(extras)
            .build()
        mediaSession!!.setPlaybackState(playbackState)
    }
}

