package com.airtago.xnzrw24b;

import android.content.Context;
import android.database.ContentObserver;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.util.Log;
import android.view.View;

/**
 * Created by alexe on 11.01.2017.
 */

public final class AimView extends View {
    public AimView(Context context) {
        super(context);
        circleFill = new Paint();
        circleFill.setColor(Color.GREEN);
        //_circleFill.setStrokeWidth(5);
        circleFill.setStyle(Paint.Style.FILL);
        circleFill.setAntiAlias(true);
        circleFill.setAlpha(95);

        circleStroke = new Paint();
        circleStroke.setColor(Color.GREEN);
        circleStroke.setStrokeWidth(5);
        circleStroke.setStyle(Paint.Style.STROKE);
        circleStroke.setAntiAlias(true);

        circlePath = new Path();

        AudioManager am = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        int volume_level= (int)(100d * am.getStreamVolume(AudioManager.STREAM_MUSIC) / am.getStreamMaxVolume(AudioManager.STREAM_MUSIC));

        mToneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, volume_level);

        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                beepLoop();
            }
        });
        thread.start();

        mSettingsContentObserver = new SettingsContentObserver(getContext(), new android.os.Handler());
        getContext().getContentResolver().registerContentObserver(
                android.provider.Settings.System.CONTENT_URI, true,
                mSettingsContentObserver );
    }

    private SettingsContentObserver mSettingsContentObserver;

    public class SettingsContentObserver extends ContentObserver {

        private final String TAG = SettingsContentObserver.class.getName();

        private AudioManager audioManager;

        public SettingsContentObserver(Context context, android.os.Handler handler) {
            super(handler);
            audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            mMaxLevel = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        }

        private int mMaxLevel;

        @Override
        public boolean deliverSelfNotifications() {
            return false;
        }

        @Override
        public void onChange(boolean selfChange) {
            int currentVolume= (int)(100d * audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) / audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));

            Log.d(TAG, "Volume now " + currentVolume);

            boolean startThread = false;
            if (thread != null && !thread.isInterrupted()) {
                thread.interrupt();
                startThread = true;
            }
            mToneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, currentVolume);
            if (startThread) {
                startSoundThread();
            }
        }
    }

    private void startSoundThread() {
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                beepLoop();
            }
        });
        thread.start();
    }

    private ToneGenerator mToneGenerator;
    private Thread thread;

    private void beepLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            if (mRadius > 0 && MaxRadius > 0) {
                mToneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 100);
                long pause = 2000;
                float rate = mRadius / MaxRadius;
                if (rate < 0.6) {
                    if (rate > 0.5)
                        pause = 1000;
                    else if (rate > 0.2)
                        pause = 500;
                    else if (rate > 0.1)
                        pause = 250;
                    else
                        pause = 100;
                }
                try {
                    Thread.sleep(pause);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    mToneGenerator.stopTone();
                }
            }
            mToneGenerator.stopTone();
        }
        mToneGenerator.stopTone();
    }

//    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//
//        width = widthMeasureSpec / 2;
//        height = heightMeasureSpec / 2;
//        MaxRadius = Math.min(widthMeasureSpec, heightMeasureSpec) / 2;
//
//        setMeasuredDimension(Radius, Radius);
//    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mRadius > 0) {
            circlePath.reset();
            circlePath.addCircle(width, height, mRadius, Path.Direction.CW);
            canvas.drawPath(circlePath, circleFill);
            canvas.drawCircle(width, height, mRadius, circleStroke);
        }
    }

    private int width;
    private int height;
    private Paint circleStroke;
    private Paint circleFill;
    private Path circlePath;

    public int MaxRadius;
    private float mRadius;
    private double mLevel;
    private boolean mHasSize = false;
    private int mWidth;
    private int mHeight;

    public void setLevel(double level) {
        mLevel = level;
        if (mHasSize) {
            width = mWidth / 2;
            height = mHeight / 2;
        } else {
            width = getWidth() / 2;
            height = getHeight() / 2;
        }
        MaxRadius = Math.min(width, height) / 2;
        double d = MaxRadius * 2 / 100f * level;
        d = Math.min(MaxRadius * 2, Math.max(15, d));
        mRadius = (float)d / 2;
        if (thread == null) {
            startSoundThread();
        }
    }

    public void setSizes(int width, int height) {
        mHasSize = true;
        mWidth = width;
        mHeight = height;
    }

    public float getRadius() {
        return mRadius;
    }

    public void setRadius(float mRadius) {
        this.mRadius = mRadius;
        if (mRadius == 0) {
            if (thread != null) {
                thread.interrupt();
                thread = null;
            }
        }
        else if (thread == null) {
            startSoundThread();
        }
    }
}
