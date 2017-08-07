package com.tangyx.video;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.tangyx.video.helper.PermissionHelper;
import com.tangyx.video.utils.FileUtils;
import com.tangyx.video.helper.MediaHelper;

import java.io.File;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private SurfaceView mSurfaceView;
    private ImageView mStartVideo;
    private ImageView mStartVideoIng;
    private TextView mTime;
    private ProgressBar mProgress;
    private MediaHelper mMediaHelper;
    private int mProgressNumber=0;
    private PermissionHelper mPermissionHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        WindowManager.LayoutParams p = this.getWindow().getAttributes();
        p.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;//|=：或等于，取其一
        getWindow().setAttributes(p);
        setContentView(R.layout.activity_main);

        mSurfaceView = (SurfaceView) findViewById(R.id.video_surface_view);
        mStartVideo = (ImageView) findViewById(R.id.start_video);
        mStartVideoIng = (ImageView) findViewById(R.id.start_video_ing);
        mProgress = (ProgressBar) findViewById(R.id.progress);
        mTime = (TextView) findViewById(R.id.time);
        findViewById(R.id.close).setOnClickListener(this);
        findViewById(R.id.inversion).setOnClickListener(this);

        mStartVideo.setOnClickListener(this);
        mStartVideoIng.setOnClickListener(this);
        //初始化工具类
        mMediaHelper = new MediaHelper(this);
        //设置视频存放地址的主目录
        mMediaHelper.setTargetDir(new File(new FileUtils(this).getStorageDirectory()));
        //设置录制视频的名字
        mMediaHelper.setTargetName(UUID.randomUUID() + ".mp4");
        mPermissionHelper = new PermissionHelper(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.close:
                mMediaHelper.stopRecordUnSave();
                finish();
                break;
            case R.id.start_video:
                mProgressNumber = 0;
                mProgress.setProgress(0);
                mMediaHelper.record();
                startView();
                break;
            case R.id.start_video_ing:
                if(mProgressNumber == 0){
                    stopView(false);
                    break;
                }

                if (mProgressNumber < 8) {
                    //时间太短不保存
                    Toast.makeText(this,"请至少录制到红线位置",Toast.LENGTH_LONG).show();
                    mMediaHelper.stopRecordUnSave();
                    stopView(false);
                    break;
                }
                //停止录制
                mMediaHelper.stopRecordSave();
                stopView(true);
                break;
            case R.id.inversion:
                mMediaHelper.stopRecordUnSave();
                stopView(false);
                mMediaHelper.autoChangeCamera();
                break;
        }
    }

    private void startView(){
        mStartVideo.setVisibility(View.GONE);
        mStartVideoIng.setVisibility(View.VISIBLE);
        mProgressNumber = 0;
        mTime.setText("00:00");
        handler.removeMessages(0);
        handler.sendMessage(handler.obtainMessage(0));
    }

    private void stopView(boolean isSave){
        int timer = mProgressNumber;
        mProgressNumber = 0;
        mProgress.setProgress(0);
        handler.removeMessages(0);
        mTime.setText("00:00");
        if(isSave) {
            String path = mMediaHelper.getTargetFilePath();
            Intent intent = new Intent(this,MakeVideoActivity.class);
            intent.putExtra("path",path);
            intent.putExtra("time",timer);
            startActivity(intent);
        }
        mStartVideoIng.setVisibility(View.GONE);
        mStartVideo.setVisibility(View.VISIBLE);
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    mProgress.setProgress(mProgressNumber);
                    mTime.setText("00:"+(mProgressNumber<10?"0"+mProgressNumber:mProgressNumber));
                    if(mProgress.getProgress() >= mProgress.getMax()){
                        mMediaHelper.stopRecordSave();
                        stopView(true);
                    }else if (mMediaHelper.isRecording()){
                        mProgressNumber = mProgressNumber + 1;
                        sendMessageDelayed(handler.obtainMessage(0), 1000);
                    }
                    break;
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if(mPermissionHelper.lacksPermissions(PermissionsActivity.PERMISSIONS)){
            PermissionsActivity.startActivityForResult(this,PermissionsActivity.REQUEST_CODE,PermissionsActivity.PERMISSIONS);
        }else{
            //启动相机
            mMediaHelper.setSurfaceView(mSurfaceView);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == PermissionsActivity.PERMISSIONS_GRANTED){
            //启动相机
            mMediaHelper.setSurfaceView(mSurfaceView);
        }else if(resultCode == -100){
            finish();
        }
    }
}