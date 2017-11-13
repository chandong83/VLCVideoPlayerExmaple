package com.higenis.vlcvideoplayerexmaple;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity  implements IVLCVout.Callback {
    public final static String TAG = "VLCVideoPlayerExample";
    private static final int REQUEST_READ_EXTERNAL_STORAGE_PERMISSION = 1;
    ConstraintLayout mConstraintLayout;
    // 화면 서페이스 변수
    private SurfaceView mSurface;
    private SurfaceHolder holder;

    // vlc 변수들
    private LibVLC libvlc;
    private MediaPlayer mMediaPlayer = null;
    private int mVideoWidth;
    private int mVideoHeight;

    private String mFilePath ;
    private boolean isRtsp=false;


    private void requestReadExternalStoragePermission() {
        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_EXTERNAL_STORAGE_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_EXTERNAL_STORAGE_PERMISSION) {
            if (permissions.length != 1 || grantResults.length != 1 ||
                    grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "external storage read permission not granted.");
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //퍼미션 체크하기
        if (savedInstanceState == null) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                //퍼미션 요청 하기
                requestReadExternalStoragePermission();
            } else {

            }
        }

        //전체화면 UI로 변경하기 위함
        mConstraintLayout = (ConstraintLayout)findViewById(R.id.constraintLayout);
        hideSystemUI();

        //재생할 파일 경로
        mFilePath= "/sdcard/sprite.mp4";
        //mFilePath에 url 경로 지정
        //mFilePath= "rtsp://192.168.25.41:8554/live.ts";
        //rtsp의 경우 true
        isRtsp = false;
        //isRtsp = true;

        //서페이스 연결 작업
        mSurface = (SurfaceView) findViewById(R.id.surface);
        holder = mSurface.getHolder();

    }




    @Override
    protected void onResume() {
        super.onResume();
        createPlayer(mFilePath, isRtsp);
    }


    @Override
    protected void onPause() {
        super.onPause();
        releasePlayer();
    }

    //종료
    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        //설정 변경이 이뤄졌을때...
        //화면이 회전 되었다면...
        super.onConfigurationChanged(newConfig);
        setSize(mVideoWidth, mVideoHeight);
    }

    //시스템 UI 표시
    private void showSystemUI() {
        mConstraintLayout.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    //전체 화면
    private void hideSystemUI() {
        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        mConstraintLayout.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    /*
    *  동영상 플레이어 시작
    *  String mediaPath : 파일 경로
    */
    private void createPlayer(String mediaPath, boolean isURL) {
        //플레이어가 있다면 종료(제거)
        releasePlayer();
        Log.d(TAG, "createPlayer");
        try {
            // 미디어 파일 경로 메시지 풍선으로 화면에 표시
            if (mediaPath.length() > 0) {
                Toast toast = Toast.makeText(this, mediaPath, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0,
                        0);
                toast.show();
            }


            //libvlc 생성
            // 옵션 추가 하기
            // 다른 옵션 추가시 여기에 add로 추가해주면 됨.
            ArrayList<String> options = new ArrayList<String>();
            //options.add("--subsdec-encoding <encoding>");
            options.add("--aout=opensles");
            options.add("--audio-time-stretch"); // time stretching
            options.add("-vvv"); // verbosity
            //옵셕 적용하여 libvlc 생성
            libvlc = new LibVLC(this, options);

            // 화면 자동을 꺼지는 것 방지
            holder.setKeepScreenOn(true);

            // mediaplay 클래스 생성  (libvlc 라이브러리)
            mMediaPlayer = new MediaPlayer(libvlc);
            // 이벤트 리스너 연결
            mMediaPlayer.setEventListener(mPlayerListener);

            // 영상을 surface 뷰와 연결 시킴
            final IVLCVout vout = mMediaPlayer.getVLCVout();
            vout.setVideoView(mSurface);

            //콜백 함수 등록
            vout.addCallback(this);
            //서페이스 홀더와 연결
            vout.attachViews();

            //동영상 파일 로딩
            Media m;
            if(isURL) {
                m = new Media(libvlc, Uri.parse(mediaPath));
            }
            else {
                m = new Media(libvlc, mediaPath);
            }
            mMediaPlayer.setMedia(m);
            // 재생 시작
            mMediaPlayer.play();

        } catch (Exception e) {
            Toast.makeText(this, "Error creating player!", Toast.LENGTH_LONG).show();
        }
    }
    /*
    *  동영상 플레이어 종료
    */
    private void releasePlayer() {
        //라이브러리가 없다면
        //바로 종료
        if (libvlc == null)
            return;
        if(mMediaPlayer != null) {
            //플레이 중지

            mMediaPlayer.stop();

            final IVLCVout vout = mMediaPlayer.getVLCVout();
            //콜백함수 제거
            vout.removeCallback(this);

            //연결된 뷰 분리
            vout.detachViews();
        }

        holder = null;
        libvlc.release();
        libvlc = null;

        mVideoWidth = 0;
        mVideoHeight = 0;
    }

    //영상 사이즈 변경
    private void setSize(int width, int height) {
        mVideoWidth = width;
        mVideoHeight = height;
        if (mVideoWidth * mVideoHeight <= 1)
            return;

        if(holder == null || mSurface == null)
            return;

        // get screen size
        int w = getWindow().getDecorView().getWidth();
        int h = getWindow().getDecorView().getHeight();

        // getWindow().getDecorView() doesn't always take orientation into
        // account, we have to correct the values
        boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        if (w > h && isPortrait || w < h && !isPortrait) {
            int i = w;
            w = h;
            h = i;
        }

        float videoAR = (float) mVideoWidth / (float) mVideoHeight;
        float screenAR = (float) w / (float) h;

        if (screenAR < videoAR)
            h = (int) (w / videoAR);
        else
            w = (int) (h * videoAR);

        // force surface buffer size
        holder.setFixedSize(mVideoWidth, mVideoHeight);

        // set display size
        ViewGroup.LayoutParams lp = mSurface.getLayoutParams();
        lp.width = w;
        lp.height = h;
        mSurface.setLayoutParams(lp);
        mSurface.invalidate();
    }

    /* IVLCVout.Callback override 함수 시작 */
    @Override
    public void onNewLayout(IVLCVout vout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
        //화면에 변화가 있거나 처음 생성 될때
        if (width * height == 0)
            return;

        // store video size
        mVideoWidth = width;
        mVideoHeight = height;
        setSize(mVideoWidth, mVideoHeight);
    }




    @Override
    public void onSurfacesCreated(IVLCVout vout) {
        //서페이스 생성 시
    }

    @Override
    public void onSurfacesDestroyed(IVLCVout vout) {
        //서페이스 종료 시
    }

    @Override
    public void onHardwareAccelerationError(IVLCVout vlcVout) {
        //하드웨어 가속 에러시 발생 함.
        Log.e(TAG, "Error with hardware acceleration");
        this.releasePlayer();
        Toast.makeText(this, "Error with hardware acceleration", Toast.LENGTH_LONG).show();
    }

    /* IVLCVout.Callback override 함수 끝 */



    /* MediaPlayer리스너 */
    private MediaPlayer.EventListener mPlayerListener = new MediaPlayerListener(this);
    private static class MediaPlayerListener implements MediaPlayer.EventListener {
        private WeakReference<MainActivity> mOwner;

        public MediaPlayerListener(MainActivity owner) {
            mOwner = new WeakReference<MainActivity>(owner);
        }

        @Override
        public void onEvent(MediaPlayer.Event event) {
            MainActivity player = mOwner.get();

            switch(event.type) {
                case MediaPlayer.Event.EndReached:
                    //동영상 끝까지 재생되었다면..
                    player.releasePlayer();
                    break;
                case MediaPlayer.Event.Playing:
                case MediaPlayer.Event.Paused:
                case MediaPlayer.Event.Stopped:
                    break;

                    //아래 두 이벤트는 계속 발생됨
                case MediaPlayer.Event.TimeChanged: //재생 시간 변화시
                    break;
                case MediaPlayer.Event.PositionChanged: //동영상 재생 구간 변화시
                    //Log.d(TAG, "PositionChanged");
                    break;
                default:
                    break;
            }
        }
    }

}
