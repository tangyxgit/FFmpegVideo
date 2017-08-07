package com.tangyx.video;

import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import com.tangyx.video.ffmpeg.FFmpegCommands;
import com.tangyx.video.ffmpeg.FFmpegRun;
import com.tangyx.video.utils.FileUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by tangyx
 * Date 2017/8/2
 * email tangyx@live.com
 */
public class MakeVideoActivity extends AppCompatActivity implements View.OnClickListener,SeekBar.OnSeekBarChangeListener{
    private final static String TAG="SLog";
    private VideoView mVideoView;
    private TextView mNext;
    private AppCompatSeekBar mAudioSeekBar;
    private AppCompatSeekBar mMusicSeekBar;
    private MediaPlayer mAudioPlayer;
    private MediaPlayer mMusicPlayer;
    private List<String> mMediaPath;
    private String mTargetPath;
    private FileUtils mFileUtils;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_make_video);
        mVideoView = (VideoView) findViewById(R.id.video);
        mAudioSeekBar = (AppCompatSeekBar) findViewById(R.id.video_seek_bar);
        mMusicSeekBar = (AppCompatSeekBar) findViewById(R.id.music_seek_bar);
        mNext = (TextView) findViewById(R.id.next);
        mAudioSeekBar.setOnSeekBarChangeListener(this);
        mMusicSeekBar.setOnSeekBarChangeListener(this);
        mNext.setOnClickListener(this);
        findViewById(R.id.back).setOnClickListener(this);
        findViewById(R.id.local_music).setOnClickListener(this);
        boolean isPlayer = getIntent().getBooleanExtra("isPlayer", false);
        Log.e(TAG,"isPlayer:"+isPlayer);
        if (isPlayer) {
            findViewById(R.id.title_layout).setVisibility(View.GONE);
            findViewById(R.id.editor_layout).setVisibility(View.GONE);
            mVideoView.setVideoPath(getIntent().getStringExtra("path"));
            mVideoView.start();
        }else{
            mFileUtils = new FileUtils(this);
            mTargetPath = mFileUtils.getStorageDirectory();
            extractVideo();
        }
    }


    /**
     * 提取视频
     */
    private void extractVideo() {
        final String outVideo = mTargetPath + "/video.mp4";
        String[] commands = FFmpegCommands.extractVideo(getIntent().getStringExtra("path"), outVideo);
        FFmpegRun.execute(commands, new FFmpegRun.FFmpegRunListener() {
            @Override
            public void onStart() {
                mMediaPath = new ArrayList<>();
                Log.e(TAG,"extractVideo ffmpeg start...");
            }

            @Override
            public void onEnd(int result) {
                Log.e(TAG,"extractVideo ffmpeg end...");
                mMediaPath.add(outVideo);
                extractAudio();
            }
        });
    }

    /**
     * 提取音频
     */
    private void extractAudio() {
        final String outVideo = mTargetPath + "/audio.aac";
        String[] commands = FFmpegCommands.extractAudio(getIntent().getStringExtra("path"), outVideo);
        FFmpegRun.execute(commands, new FFmpegRun.FFmpegRunListener() {
            @Override
            public void onStart() {
                mAudioPlayer = new MediaPlayer();
            }

            @Override
            public void onEnd(int result) {
                Log.e(TAG,"extractAudio ffmpeg end...");
                mMediaPath.add(outVideo);
                String path = mMediaPath.get(0);
                mVideoView.setVideoPath(path);
                mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        mVideoView.start();
                    }
                });
                mVideoView.start();
                try {
                    mAudioPlayer.setDataSource(mMediaPath.get(1));
                    mAudioPlayer.setLooping(true);
                    mAudioPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mediaPlayer) {
                            mAudioPlayer.setVolume(0.5f, 0.5f);
                            mAudioPlayer.start();
                        }
                    });
                    mAudioPlayer.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void cutSelectMusic(String musicUrl) {
        final String musicPath = mTargetPath + "/bgMusic.aac";
        long time = getIntent().getIntExtra("time",0);
        String[] commands = FFmpegCommands.cutIntoMusic(musicUrl, time, musicPath);
        FFmpegRun.execute(commands, new FFmpegRun.FFmpegRunListener() {
            @Override
            public void onStart() {
                    Log.e(TAG,"cutSelectMusic ffmpeg start...");
            }

            @Override
            public void onEnd(int result) {
                Log.e(TAG,"cutSelectMusic ffmpeg end...");
                if(mMusicPlayer!=null){//移除上一个选择的音乐背景
                    mMediaPath.remove(mMediaPath.size()-1);
                }
                mMediaPath.add(musicPath);
                stopMediaPlayer();
                mMusicPlayer = new MediaPlayer();
                try {
                    mMusicPlayer.setDataSource(musicPath);
                    mMusicPlayer.setLooping(true);
                    mMusicPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mediaPlayer) {
                            mediaPlayer.setVolume(0.5f, 0.5f);
                            mediaPlayer.start();
                            mMusicSeekBar.setProgress(50);
                        }
                    });
                    mMusicPlayer.prepareAsync();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.back:
                finish();
                mFileUtils.deleteFile(mTargetPath,null);
                break;
            case R.id.local_music:
                Intent intent = new Intent(this,MusicActivity.class);
                startActivityForResult(intent,0);
                break;
            case R.id.next:
                composeVideoAudio();
                mNext.setTextColor(Color.parseColor("#999999"));
                mNext.setEnabled(false);
                break;
        }
    }

    /**
     * 处理视频原声
     */
    private void composeVideoAudio() {
        int mAudioVol = mAudioSeekBar.getProgress();
        String audioUrl = mMediaPath.get(1);
        final String audioOutUrl = mTargetPath + "/tempAudio.aac";
        String[] common = FFmpegCommands.changeAudioOrMusicVol(audioUrl, mAudioVol * 10, audioOutUrl);
        FFmpegRun.execute(common, new FFmpegRun.FFmpegRunListener() {
            @Override
            public void onStart() {
                Log.e(TAG,"changeAudioVol ffmpeg start...");
                handler.sendEmptyMessage(0);
            }

            @Override
            public void onEnd(int result) {
                Log.e(TAG,"changeAudioVol ffmpeg end...");
                if (mMediaPath.size() == 3) {
                    composeVideoMusic(audioOutUrl);
                } else {
                    composeMusicAndAudio(audioOutUrl);
                }
            }
        });
    }

    /**
     * 处理背景音乐
     */
    private void composeVideoMusic(final String audioUrl) {
        final int mMusicVol = mMusicSeekBar.getProgress();
        String musicUrl;
        if (audioUrl == null) {
            musicUrl = mMediaPath.get(1);
        } else {
            musicUrl = mMediaPath.get(2);
        }
        final String musicOutUrl = mTargetPath + "/tempMusic.aac";
        final String[] common = FFmpegCommands.changeAudioOrMusicVol(musicUrl, mMusicVol * 10, musicOutUrl);
        FFmpegRun.execute(common, new FFmpegRun.FFmpegRunListener() {
            @Override
            public void onStart() {
                Log.e(TAG,"changeMusicVol ffmpeg start...");
                handler.sendEmptyMessage(0);
            }

            @Override
            public void onEnd(int result) {
                Log.e(TAG,"changeMusicVol ffmpeg end...");
                composeAudioAndMusic(audioUrl, musicOutUrl);
            }
        });
    }

    /**
     * 合成原声和背景音乐
     */
    public void composeAudioAndMusic(String audioUrl, String musicUrl) {
        if (audioUrl == null) {
            composeMusicAndAudio(musicUrl);
        } else {
            final String musicAudioPath = mTargetPath + "/audioMusic.aac";
            String[] common = FFmpegCommands.composeAudio(audioUrl, musicUrl, musicAudioPath);
            FFmpegRun.execute(common, new FFmpegRun.FFmpegRunListener() {
                @Override
                public void onStart() {
                    Log.e(TAG,"composeAudioAndMusic ffmpeg start...");
                    handler.sendEmptyMessage(0);
                }

                @Override
                public void onEnd(int result) {
                    Log.e(TAG,"composeAudioAndMusic ffmpeg end...");
                    composeMusicAndAudio(musicAudioPath);
                }
            });
        }
    }

    /**
     * 视频和背景音乐合成
     *
     * @param bgMusicAndAudio
     */
    private void composeMusicAndAudio(String bgMusicAndAudio) {
        final String videoAudioPath = mTargetPath + "/videoMusicAudio.mp4";
        final String videoUrl = mMediaPath.get(0);
        final int time = getIntent().getIntExtra("time",0) - 1;
        String[] common = FFmpegCommands.composeVideo(videoUrl, bgMusicAndAudio, videoAudioPath, time);
        FFmpegRun.execute(common, new FFmpegRun.FFmpegRunListener() {
            @Override
            public void onStart() {
                Log.e(TAG,"videoAndAudio ffmpeg start...");
                handler.sendEmptyMessage(0);
            }

            @Override
            public void onEnd(int result) {
                Log.e(TAG,"videoAndAudio ffmpeg end...");
                handleVideoNext(videoAudioPath);
            }
        });
    }

    /**
     * 适配处理完成，进入下一步
     */
    private void handleVideoNext(String videoUrl) {
        Message message = new Message();
        message.what = 1;
        message.obj = videoUrl;
        handler.sendMessage(message);
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    showProgressLoading();
                    break;
                case 1:
                    dismissProgress();
                    String videoPath = (String) msg.obj;
                    Intent intent = new Intent(MakeVideoActivity.this,MakeVideoActivity.class);
                    intent.putExtra("path",videoPath);
                    intent.putExtra("isPlayer",true);
                    startActivity(intent);
                    finish();
                    break;
                case 2:
                    dismissProgress();
                    break;
            }
        }
    };

    private void showProgressLoading(){

    }
    private void dismissProgress(){

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == 10000) {
            String music = data.getStringExtra("music");
            cutSelectMusic(music);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        float volume = i / 100f;
        if (mAudioSeekBar == seekBar) {
            mAudioPlayer.setVolume(volume, volume);
        } else if(mMusicPlayer!=null){
            mMusicPlayer.setVolume(volume, volume);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }


    private void stopMediaPlayer(){
        try {
            if (mMusicPlayer != null) {
                mMusicPlayer.stop();
                mMusicPlayer.release();
                mMusicPlayer=null;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mVideoView.stopPlayback();
        if (mAudioPlayer != null) {
            mAudioPlayer.stop();
            mAudioPlayer.release();
        }
        stopMediaPlayer();
    }

    @Override
    public void onPause() {
        super.onPause();
        mVideoView.pause();
        if (mAudioPlayer != null) {
            mAudioPlayer.pause();
        }
        if (mMusicPlayer != null) {
            mMusicPlayer.pause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mVideoView.start();
        if (mAudioPlayer != null) {
            mAudioPlayer.start();
        }
        if (mMusicPlayer != null) {
            mMusicPlayer.start();
        }
    }
}
