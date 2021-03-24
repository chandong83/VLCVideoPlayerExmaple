package com.higenis.vlcvideoplayerexmaple;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    public final static String TAG = "VLCVideoPlayerExample";
    private static final int REQUEST_READ_EXTERNAL_STORAGE_PERMISSION = 1;

    /*
     파일 재생 위치
     String, Uri, FileDescriptor, AssetFileDescriptor 지원 함
    */
    private String mFilePath = "/sdcard/Download/ddudu.mp4";


    // USE_TEXTURE_VIEW가 true이면 android.view.SurfaceView 대신 android.view.TextureView 을 사용한다.
    //  - API 24 이상의 경우 android.view.SurfaceView로 사용하는 것을 권장함
    private static final boolean USE_TEXTURE_VIEW = false;

    // ENABLE_SUBTITLES가 true이면 자막 ON
    private static final boolean ENABLE_SUBTITLES = true;

    // 비디오 레이아웃
    private VLCVideoLayout mVideoLayout = null;

    // LibVLC 클래스
    private LibVLC mLibVLC = null;

    // 미디어 컨트롤러
    private MediaPlayer mMediaPlayer = null;

    // 퍼미션 요청 함수
    private void requestReadExternalStoragePermission() {
        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_EXTERNAL_STORAGE_PERMISSION);
    }

    // 퍼미션 결과 콜백 함수
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

        // VLC 옵션
        final ArrayList<String> args = new ArrayList<>();
        args.add("-vv");

        // LibVLC 클래스 생성
        mLibVLC = new LibVLC(this, args);

        // 미디어 컨트롤러 생성
        mMediaPlayer = new MediaPlayer(mLibVLC);

        // 비디오 재생 레이아웃
        mVideoLayout = findViewById(R.id.video_layout);
    }

    //종료
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 미디어 컨트롤러 제거
        mMediaPlayer.release();

        // VLC 제거
        mLibVLC.release();
    }

    @Override
    protected void onStart() {
        super.onStart();
        /*
            attachViews: 비디오 레이아웃 View 에 연결

            surfaceFrame(VLCVideoLayout): VLCVideoLayout 비디오가 출력될 레이아웃 변수
            dm(DisplayManager): 렌더링 전환용 변수 옵션
            subtitles(boolean): 자막 활성/비활성
            textureView(boolean): View 선택
         */
        mMediaPlayer.attachViews(mVideoLayout, null, ENABLE_SUBTITLES, USE_TEXTURE_VIEW);

        /*
            Media 미디어 로드
            ILibVLC(ILibVLC): LibVLC 클래스 변수
            path(String, Uri, FileDescriptor, AssetFileDescriptor): 미디어 객체
         */
        final Media media = new Media(mLibVLC, mFilePath);

        // 미디어 컨트롤러 클래스에 미디어 적용
        mMediaPlayer.setMedia(media);

        media.release();

        // 재생 시작
        mMediaPlayer.play();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // 재생 중지
        mMediaPlayer.stop();

        // 연결된 View 제거
        mMediaPlayer.detachViews();
    }
}
