package com.media.demo;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.media.demo.helper.MediaPlayerHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * MainActivity.
 */
public class MainActivity extends Activity implements View.OnClickListener, ServiceConnection,
        SeekBar.OnSeekBarChangeListener, MediaPlayerHelper.MediaPlayerUpdateCallBack {
    private MediaControllerCompat mMediaController;
    private AudioPlayerService musicService;
    private MediaPlayerHelper mMediaPlayerHelper;
    List<MusicEntity> list_music;
    private ImageButton imgPause, imgLast, imgNext;
    private TextView MusicTitle, timeRight, timeLeft;
    private SeekBar progressSeek;

    private MediaControllerCompat.Callback mMediaControllerCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            switch (state.getState()) {
                case PlaybackStateCompat.STATE_NONE://无任何状态
                    imgPause.setImageResource(R.drawable.img_pause);
                    break;
                case PlaybackStateCompat.STATE_PLAYING:
                    imgPause.setImageResource(R.drawable.img_pause);
                    break;
                case PlaybackStateCompat.STATE_PAUSED:
                    imgPause.setImageResource(R.drawable.img_play);
                    break;
                case PlaybackStateCompat.STATE_SKIPPING_TO_NEXT://下一首
                    break;
                case PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS://上一首
                    break;
                case PlaybackStateCompat.STATE_FAST_FORWARDING://快进
                    break;
                case PlaybackStateCompat.STATE_REWINDING://快退
                    break;
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            super.onMetadataChanged(metadata);
            MusicTitle.setText(metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imgLast = findViewById(R.id.img_last);
        imgPause = findViewById(R.id.img_pause);
        imgNext = findViewById(R.id.img_next);

        imgLast.setOnClickListener(this);
        imgPause.setOnClickListener(this);
        imgNext.setOnClickListener(this);

        MusicTitle = findViewById(R.id.Music_title);
        progressSeek = findViewById(R.id.progress_seek);
        timeLeft = findViewById(R.id.time_left);
        timeRight = findViewById(R.id.time_right);


        progressSeek.setOnSeekBarChangeListener(this);

        list_music = getMusics();

        Intent intent = new Intent(this, AudioPlayerService.class);
        getApplicationContext().bindService(intent, this, 0);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.img_pause:
                if (mMediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING) {
                    mMediaController.getTransportControls().pause();
                } else if (mMediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_PAUSED) {
                    mMediaController.getTransportControls().play();
                } else {
                    mMediaController.getTransportControls().playFromSearch("", null);
                }
                break;
            case R.id.img_last:
                mMediaController.getTransportControls().skipToPrevious();
                break;
            case R.id.img_next:
                mMediaController.getTransportControls().skipToNext();
                break;
        }

    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        if (iBinder instanceof AudioPlayerService.ServiceBinder) {
            try {
                //获取服务
                musicService = ((AudioPlayerService.ServiceBinder) iBinder).getService();
                //获取帮助类
                mMediaPlayerHelper = musicService.getMediaPlayerHelper();
                //设置媒体播放回键听
                mMediaPlayerHelper.setMediaPlayerUpdateListener(this);
                //设置数据源
                mMediaPlayerHelper.setPlayeData(list_music);
                //设置更新的seekBaar
                mMediaPlayerHelper.setSeekBar(progressSeek);
                //设置媒体控制器
                mMediaController = new MediaControllerCompat(MainActivity.this,
                        musicService.getMediaSessionToken());
                //注册回调
                mMediaController.registerCallback(mMediaControllerCallback);
            } catch (Exception e) {
                Log.e(getClass().getName(), "serviceConnectedException==" + e.getMessage());
            }
        }
    }

    private List<MusicEntity> getMusics() {
        List<MusicEntity> list = new ArrayList<MusicEntity>();
        MusicEntity entity = new MusicEntity();
        entity.setUrl("http://other.web.nf01.sycdn.kuwo.cn/resource/n3/72/31/1295078204.mp3");
        entity.setAlbum("奥运会专辑");
        entity.setMusicTitle("北京欢迎你");
        entity.setSinger("群星");
        list.add(entity);
        MusicEntity entity1 = new MusicEntity();
        entity1.setUrl("http://other.web.nf01.sycdn.kuwo.cn/resource/n1/60/10/3227644505.mp3");
        entity1.setAlbum("淘汰");
        entity1.setMusicTitle("淘汰");
        entity1.setSinger("陈奕迅");
        list.add(entity1);
        return list;
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, AudioPlayerService.class);
        startService(intent);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        //更新时实播放进度
        timeLeft.setText(turnTime(progress));

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        //更新拖动进度
        mMediaPlayerHelper.getMediaPlayer().seekTo(
                seekBar.getProgress() * mMediaPlayerHelper.getMediaPlayer().getDuration()
                        / seekBar.getMax());
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        //自动播放下一首
        mMediaController.getTransportControls().skipToNext();
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int percent) {
        //设置二级缓冲显示位置。
        progressSeek.setSecondaryProgress(percent);
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        int musicTime = mediaPlayer.getDuration() / 1000;
        int minute = musicTime / 60;
        int second = musicTime % 60;
        timeRight.setText(minute + ":" + (second > 9 ? second : "0" + second));
    }

    /**
     * 秒转为分:秒
     *
     * @param second
     * @return
     */
    public String turnTime(int second) {
        int d = 0;
        int s = 0;
        int temp = second % 3600;
        if (second > 3600) {
            if (temp != 0) {
                if (temp > 60) {
                    d = temp / 60;
                    if (temp % 60 != 0) {
                        s = temp % 60;
                    }
                } else {
                    s = temp;
                }
            }
        } else {
            d = second / 60;
            if (second % 60 != 0) {
                s = second % 60;
            }
        }
        return (d > 0 ? d > 9 ? d : "0" + d : "00") + ":" + (s > 9 ? s : "0" + s);
    }
}
