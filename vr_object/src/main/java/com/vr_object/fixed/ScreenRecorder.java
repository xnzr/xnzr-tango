package com.vr_object.fixed;

/**
 * Created by Michael Lukin on 02.11.2017.
 */

import android.app.Activity;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.ToggleButton;

import java.io.IOException;

public class ScreenRecorder {
    private static final String TAG = ScreenRecorder.class.getSimpleName();

    private static final int PERMISSION_CODE = 1;
    private int mScreenDensity;
    private MediaProjectionManager mProjectionManager;
    private static final int DISPLAY_WIDTH = 480;
    private static final int DISPLAY_HEIGHT = 640;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaProjectionCallback mMediaProjectionCallback;
    private ToggleButton mToggleButton;
    private MediaRecorder mMediaRecorder;
    private Activity mOwner;

    public ScreenRecorder(Activity owner) {
        mOwner = owner;
        DisplayMetrics metrics = new DisplayMetrics();
        mOwner.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;
        mMediaRecorder = new MediaRecorder();
        initRecorder();
        prepareRecorder();
        mProjectionManager = (MediaProjectionManager) mOwner.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mMediaProjectionCallback = new MediaProjectionCallback();
    }

    public void destroy() {
        if (mMediaProjection != null) {
            mMediaProjection.stop();
        }
    }

    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            if (mToggleButton.isChecked()) {
                mToggleButton.setChecked(false);
                mMediaRecorder.stop();
                mMediaRecorder.reset();
                Log.v(TAG, "Recording Stopped");
                initRecorder();
                prepareRecorder();
            }
            mMediaProjection = null;
            stopRecording();
            Log.i(TAG, "MediaProjection Stopped");
        }
    }

    void stopRecording() {
        if (mVirtualDisplay == null) {
            return;
        }
        mMediaRecorder.stop();
        mMediaRecorder.reset();
        mVirtualDisplay.release();
        initRecorder();
        prepareRecorder();
    }

    void startRecording() {
        if (mMediaProjection == null) {
            Log.e(TAG, "Can not start screen recording! mMediaProjection is null");
            return;
        }
        mVirtualDisplay = createVirtualDisplay();
        mMediaRecorder.start();
    }

    private VirtualDisplay createVirtualDisplay() {
        return mMediaProjection.createVirtualDisplay("MainActivity",
                DISPLAY_WIDTH, DISPLAY_HEIGHT, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mMediaRecorder.getSurface(), null /*Callbacks*/, null /*Handler*/);
    }


    private void prepareRecorder() {
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            mOwner.finish();
        } catch (IOException e) {
            e.printStackTrace();
            mOwner.finish();
        }
    }

    private void initRecorder() {
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mMediaRecorder.setVideoEncodingBitRate(512 * 1000);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT);
        mMediaRecorder.setOutputFile("/sdcard/capture_0000.mp4");
    }
}