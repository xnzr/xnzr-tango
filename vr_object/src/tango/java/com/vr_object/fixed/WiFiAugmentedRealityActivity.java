package com.vr_object.fixed;

import android.app.FragmentTransaction;
import android.content.Context;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.util.AndroidRuntimeException;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.Tango.OnTangoUpdateListener;
import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoEvent;
import com.google.atap.tangoservice.TangoException;
import com.google.atap.tangoservice.TangoInvalidException;
import com.google.atap.tangoservice.TangoOutOfDateException;
import com.google.atap.tangoservice.TangoPointCloudData;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;
import com.projecttango.tangosupport.TangoPointCloudManager;
import com.projecttango.tangosupport.TangoSupport;
import com.vr_object.fixed.xnzrw24b.AimView;
import com.vr_object.fixed.xnzrw24b.ChannelInfoFragment;
import com.vr_object.fixed.xnzrw24b.MessageFields;
import com.vr_object.fixed.xnzrw24b.NetworkInfoFragment;
import com.vr_object.fixed.xnzrw24b.PacketFromDevice;
import com.vr_object.fixed.xnzrw24b.SpatialIntersect;
import com.vr_object.fixed.xnzrw24b.WFParseException;
import com.vr_object.map_editor.MapFragment;
import com.vr_object.screencast.ScreenRecorderService;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.vr_object.fixed.R.id.circle_container;
import static com.vr_object.fixed.R.id.tw_scanning_message;

public class WiFiAugmentedRealityActivity extends BaseFinderActivity
        implements View.OnClickListener,
        View.OnTouchListener,
        NetworkInfoFragment.OnListFragmentInteractionListener,
        ChannelInfoFragment.OnListFragmentInteractionListener,
        MapFragment.OnFragmentInteractionListener  {

    private TangoCameraIntrinsics mIntrinsics;
    private Tango mTango;
    private TangoConfig mConfig;
    private boolean mIsConnected = false;
    private SpatialIntersect intersector;

    private boolean debugSagitta = false;

    // Texture rendering related fields.
    // NOTE: Naming indicates which thread is in charge of updating this variable
    private int mConnectedTextureIdGlThread = INVALID_TEXTURE_ID;
    private AtomicBoolean mIsFrameAvailableTangoThread = new AtomicBoolean(false);
    private double mRgbTimestampGlThread;

    // Transform from the Earth and Moon center to OpenGL frame. This is a fixed transform.
    private float[] mOpenGLTEarthMoonCenter = new float[16];
    // Transform from Earth to Earth and Moon center. This will change over time, as the Earth is
    // rotating.
    private float[] mEarthMoonCenterTEarth = new float[16];
    // Translation from the Moon to the Earth and Moon center. This is a fixed transform.
    private float[] mEarthMoonCenterTTranslation = new float[16];
    // Rotation from the Moon to the Earth and Moon center. This will change over time, as the Moon
    // is rotating.
    private float[] mEarthMoonCenterTMoonRotation = new float[16];

    private TangoPointCloudManager mPointCloudManager;

    private final float defaultSagittaeWidth = 0.001f;

    private long numUpdates = 0;
    private long lastUpdateTime = 0;
    private final long TIME_PERIOD = 3 * 1000; //ms


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            Log.d(TAG, "recovering instance state");
        }

        //full screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        int tangoVersion = Tango.getVersion(this.getApplicationContext());

        mPointCloudManager = new TangoPointCloudManager();

        setContentView(R.layout.activity_main);
        mSurfaceView = (GLSurfaceView) findViewById(R.id.surfaceview);
        setupRenderer();

        firebaseProxy.CreateConnection();

        mSurfaceView.setOnTouchListener(this);

        //progress bar
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        lastUpdateTime = System.currentTimeMillis();

        mTextView = (TextView) findViewById(R.id.textView);
        //mTextView2 = (TextView) findViewById(R.id.textView2);

        mThresholdSetter = (SeekBar) findViewById(R.id.threshold_setter);
        mThresholdView = (TextView) findViewById(R.id.threshold_view);

        mSagittaeLenghtSetter = (SeekBar) findViewById(R.id.sagittae_length_setter);
        mSagittaeLengthView = (TextView) findViewById(R.id.sagittae_length_view);


        mAimView = new AimView(this);
        mAimView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        mCircleFrameLayout = (FrameLayout) findViewById(circle_container);
        mCircleFrameLayout.addView(mAimView);

        mScanningMessage = (TextView) findViewById(tw_scanning_message);
        mScanningMessage.setVisibility(View.GONE);

        loadOptions();
        setupThresholdSetter();
        setupSaggitaeLengthSetter();
        setupTabbedOptions();

        /// usb
        networksFragment = NetworkInfoFragment.newInstance(this);
        mChannelsFragment = ChannelInfoFragment.newInstance(this);

        getFragmentManager().beginTransaction().setTransition(FragmentTransaction.TRANSIT_NONE).replace(R.id.networks_list_content_frame, networksFragment).commit();
        getFragmentManager().beginTransaction().setTransition(FragmentTransaction.TRANSIT_NONE).replace(R.id.channels_list_content_frame, mChannelsFragment).commit();

        handler = new android.os.Handler() {

            public void handleMessage(android.os.Message msg) {
                if (msg.what == MessageFields.CODE_DATA) {
                    Log.d(TAG, "ant=" + msg.getData().getInt(MessageFields.FIELD_ANT_INT) + " ch=" + msg.getData().getInt(MessageFields.FIELD_CH_INT) + " ssid=" + msg.getData().getString(MessageFields.FIELD_SSID_STR) + " mac=" + msg.getData().getString(MessageFields.FIELD_MAC_STR) + " rssi=" + msg.getData().getDouble(MessageFields.FIELD_RSSI_DOUBLE));
                    try {
                        PacketFromDevice packet = new PacketFromDevice(msg.getData().getString(MessageFields.FIELD_RAW_STR));
                        networksFragment.addInfo(packet);
                        if (mSelectedNetwork != null && mSelectedNetwork.Ssid.equals(packet.apName) && mSelectedNetwork.Mac.equals(packet.mac)) {
                            mChannelsFragment.addInfo(packet);

                            if (mLevelCalculator != null) {
                                mLevelCalculator.handleInfo(packet);
                                mLevelCalculator.handleInfo(packet);
                                double level = mLevelCalculator.getAvg();

                                WiFiAugmentedRealityActivity.this.updateLevelDiff(level);
                                WiFiAugmentedRealityActivity.this.updateLevelProgress(mLevelCalculator.getAvgAnt0());
                            }
                        }
                    } catch (WFParseException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        if (mReceiver == null) {
            mReceiver = new MyBroadcastReceiver(this);
        }

        PowerManager mgr = (PowerManager)getSystemService(Context.POWER_SERVICE);
        wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakeLock");
        wakeLock.acquire();
    }

    private void setupSaggitaeLengthSetter() {
        int mSagittaeLength = mSagittaeLenghtSetter.getProgress();
        mSagittaeLengthView.setText(String.format("%s %d", getString(R.string.sagittae_length_text), mSagittaeLength));

        mSagittaeLenghtSetter.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setLength(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                setLength(seekBar.getProgress());
            }

            private void setLength(int length) {
                mSagittaeLengthView.setText(String.format("%s%d", getResources().getString(R.string.sagittae_length_text), length));
            }
        });
    }

    private void setupThresholdSetter() {
        mThreshold = mThresholdSetter.getProgress();
        mThresholdView.setText(String.format("%s %d", getString(R.string.threshold_text), mThreshold));

        mThresholdSetter.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setThreshold(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                setThreshold(seekBar.getProgress());
            }

            private void setThreshold(int val) {
                mThresholdView.setText(String.format("%s%d", getResources().getString(R.string.threshold_text), val));
            }
        });
    }


    @Override
    protected void updateLevelDiff(double levelDiff) {
        long deltaTime = System.currentTimeMillis() - lastUpdateTime;
        int progress = (int) Math.floor(100.0 * levelDiff);
        int sigma = (int)progressSigmoid(progress);

        String text = String.format(Locale.ENGLISH, "%d%%", sigma);
        mTextView.setText(text);

        // lines
        if (deltaTime > TIME_PERIOD) {
            if (sigma >= mThreshold) {
                addBearing();
                lastUpdateTime = System.currentTimeMillis();
                numUpdates++;
            }
        }

        setLevel(levelDiff);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mScanningMessage.setVisibility(View.GONE);
            }
        });

    }


    @Override
    protected void onResume() {
        super.onResume();
        mSurfaceView.onResume();
        // Set render mode to RENDERMODE_CONTINUOUSLY to force getting onDraw callbacks until the
        // Tango service is properly set-up and we start getting onFrameAvailable callbacks.
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        // Initialize Tango Service as a normal Android Service, since we call mTango.disconnect()
        // in onPause, this will unbind Tango Service, so every time when onResume gets called, we
        // should create a new Tango object.
        mTango = new Tango(WiFiAugmentedRealityActivity.this, new Runnable() {
            // Pass in a Runnable to be called from UI thread when Tango is ready, this Runnable
            // will be running on a new thread.
            // When Tango is ready, we can call Tango functions safely here only when there is no UI
            // thread changes involved.
            @Override
            public void run() {
                // Synchronize against disconnecting while the service is being used in the OpenGL
                // thread or in the UI thread.
                synchronized (WiFiAugmentedRealityActivity.this) {
                    try {
                        TangoSupport.initialize();
                        mConfig = setupTangoConfig(mTango);
                        mTango.connect(mConfig);
                        startupTango();
                        mIsConnected = true;
                    } catch (TangoOutOfDateException e) {
                        Log.e(TAG, getString(R.string.exception_out_of_date), e);
                    } catch (TangoErrorException e) {
                        Log.e(TAG, getString(R.string.exception_tango_error), e);
                    } catch (TangoInvalidException e) {
                        Log.e(TAG, getString(R.string.exception_tango_invalid), e);
                    }
                }
            }
        });

        intersector = new SpatialIntersect();
        mRenderer.setTangoService(mTango);

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ScreenRecorderService.ACTION_QUERY_STATUS_RESULT);
        registerReceiver(mReceiver, intentFilter);
        queryRecordingStatus();

    }

    @Override
    protected void onPause() {
        super.onPause();
        mSurfaceView.onPause();
        // Synchronize against disconnecting while the service is being used in the OpenGL thread or
        // in the UI thread.
        // NOTE: DO NOT lock against this same object in the Tango callback thread. Tango.disconnect
        // will block here until all Tango callback calls are finished. If you lock against this
        // object in a Tango callback thread it will cause a deadlock.
        synchronized (this) {
            try {
                mIsConnected = false;
                mTango.disconnectCamera(TangoCameraIntrinsics.TANGO_CAMERA_COLOR);
                // We need to invalidate the connected texture ID so that we cause a
                // re-connection in the OpenGL thread after resume
                mConnectedTextureIdGlThread = INVALID_TEXTURE_ID;
                mTango.disconnect();
            } catch (TangoErrorException e) {
                Toast.makeText(getApplicationContext(),
                        R.string.exception_tango_error,
                        Toast.LENGTH_SHORT).show();
                Log.e(TAG, getString(R.string.exception_tango_error), e);
            }
        }

        unregisterReceiver(mReceiver);
    }




    private boolean checkAndRequestPermissions() {
        if (!hasCameraPermission()) {
            requestCameraPermission();
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (!hasCameraPermission()) {
            Toast.makeText(this, "WiFiAugumentedReality requires camera permission",
                    Toast.LENGTH_LONG).show();
        }
    }

    ///usb
    @Override
    protected void onStart() {
        super.onStart();

        if (checkAndRequestPermissions()) {
            ; //init tango
        }

        IntentFilter filter = new IntentFilter();
        //filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(usbReceiver, filter);

        changeState(STATE_INIT);
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                usbLoop();
            }
        });
        thread.start();
    }

    @Override
    protected void onStop() {
        super.onStop();

        thread.interrupt();
    }

    private void bindTangoService() {
        if (!mIsConnected) {
            // Initialize Tango Service as a normal Android Service, since we call mTango.disconnect()
            // in onPause, this will unbind Tango Service, so every time when onResume gets called, we
            // should create a new Tango object.
            mTango = new Tango(WiFiAugmentedRealityActivity.this, new Runnable() {
                // Pass in a Runnable to be called from UI thread when Tango is ready, this Runnable
                // will be running on a new thread.
                // When Tango is ready, we can call Tango functions safely here only when there is no UI
                // thread changes involved.
                @Override
                public void run() {
                    // Synchronize against disconnecting while the service is being used in the OpenGL
                    // thread or in the UI thread.
                    synchronized (WiFiAugmentedRealityActivity.this) {
                        try {
                            TangoSupport.initialize();
                            mConfig = setupTangoConfig(mTango);
                            mTango.connect(mConfig);
                            startupTango();
                            mIsConnected = true;
                        } catch (TangoOutOfDateException e) {
                            Log.e(TAG, getString(R.string.exception_out_of_date), e);
                        } catch (TangoErrorException e) {
                            Log.e(TAG, getString(R.string.exception_tango_error), e);
                        } catch (TangoInvalidException e) {
                            Log.e(TAG, getString(R.string.exception_tango_invalid), e);
                        }
                    }
                }
            });
        }
    }

    /**
     * Sets up the tango configuration object. Make sure mTango object is initialized before
     * making this call.
     */
    private TangoConfig setupTangoConfig(Tango tango) {
        // Use default configuration for Tango Service, plus color camera, low latency
        // IMU integration and drift correction.
        TangoConfig config = tango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_COLORCAMERA, true);
        // NOTE: Low latency integration is necessary to achieve a precise alignment of
        // virtual objects with the RBG image and produce a good AR effect.
        config.putBoolean(TangoConfig.KEY_BOOLEAN_LOWLATENCYIMUINTEGRATION, true);
        // Drift correction allows motion tracking to recover after it loses tracking.
        // The drift corrected pose is is available through the frame pair with
        // base frame AREA_DESCRIPTION and target frame DEVICE.
        config.putBoolean(TangoConfig.KEY_BOOLEAN_DRIFT_CORRECTION, true);

        config.putBoolean(TangoConfig.KEY_BOOLEAN_DEPTH, true);
        config.putInt(TangoConfig.KEY_INT_DEPTH_MODE, TangoConfig.TANGO_DEPTH_MODE_POINT_CLOUD);

        return config;
    }

    /**
     * Set up the callback listeners for the Tango service and obtain other parameters required
     * after Tango connection.
     * Listen to updates from the RGB camera.
     */
    private void startupTango() {
        // No need to add any coordinate frame pairs since we aren't using pose data from callbacks.
        ArrayList<TangoCoordinateFramePair> framePairs = new ArrayList<>();

        mTango.connectListener(framePairs, new OnTangoUpdateListener() {
            @Override
            public void onPoseAvailable(TangoPoseData pose) {
                // We are not using onPoseAvailable for this app.
            }


            @Override
            public void onXyzIjAvailable(TangoXyzIjData xyzIj) {
                // We are not using onXyzIjAvailable for this app.
            }

            @Override
            public void onPointCloudAvailable(TangoPointCloudData pointCloud) {
                mPointCloudManager.updatePointCloud(pointCloud);
            }

            @Override
            public void onTangoEvent(TangoEvent event) {
                // We are not using onTangoEvent for this app.
            }

            @Override
            public void onFrameAvailable(int cameraId) {
                // Check if the frame available is for the camera we want and update its frame
                // on the view.
                if (cameraId == TangoCameraIntrinsics.TANGO_CAMERA_COLOR) {
                    // Now that we are receiving onFrameAvailable callbacks, we can switch
                    // to RENDERMODE_WHEN_DIRTY to drive the render loop from this callback.
                    // This will result on a frame rate of  approximately 30FPS, in synchrony with
                    // the RGB camera driver.
                    // If you need to render at a higher rate (i.e.: if you want to render complex
                    // animations smoothly) you  can use RENDERMODE_CONTINUOUSLY throughout the
                    // application lifecycle.
                    if (mSurfaceView.getRenderMode() != GLSurfaceView.RENDERMODE_WHEN_DIRTY) {
                        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
                    }

                    // Mark a camera frame is available for rendering in the OpenGL thread.
                    mIsFrameAvailableTangoThread.set(true);
                    // Trigger an OpenGL render to update the OpenGL scene with the new RGB data.
                    mSurfaceView.requestRender();
                }
            }
        });

        // Obtain the intrinsic parameters of the color camera.
        mIntrinsics = mTango.getCameraIntrinsics(TangoCameraIntrinsics.TANGO_CAMERA_COLOR);

        intersector.setInitPose(TangoSupport.getPoseAtTime(
                mRgbTimestampGlThread,
                TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                TangoPoseData.COORDINATE_FRAME_CAMERA_COLOR,
                TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                0));

    }

    /**
     * Here is where you would set-up your rendering logic. We're replacing it with a minimalistic,
     * dummy example using a standard GLSurfaceView and a basic renderer, for illustration purposes
     * only.
     */
    private void setupRenderer() {
        mSurfaceView.setEGLContextClientVersion(2);
        mRenderer = new WiFiAugmentedRealityRenderer(this,
                new WiFiAugmentedRealityRenderer.RenderCallback() {
                    private double lastRenderedTimeStamp;

                    @Override
                    public void preRender() {
                        // This is the work that you would do on your main OpenGL render thread.

                        try {
                            // Synchronize against concurrently disconnecting the service triggered
                            // from the UI thread.
                            synchronized (WiFiAugmentedRealityActivity.this) {
                                // We need to be careful to not run any Tango-dependent code in the
                                // OpenGL thread unless we know the Tango service to be properly
                                // set-up and connected.
                                if (!mIsConnected) {
                                    return;
                                }

                                // Connect the Tango SDK to the OpenGL texture ID where we are
                                // going to render the camera.
                                // NOTE: This must be done after both the texture is generated
                                // and the Tango service is connected.
                                if (mConnectedTextureIdGlThread != mRenderer.getTextureId()) {
                                    mTango.connectTextureId(
                                            TangoCameraIntrinsics.TANGO_CAMERA_COLOR,
                                            mRenderer.getTextureId());
                                    mConnectedTextureIdGlThread = mRenderer.getTextureId();
                                    Log.d(TAG, "connected to texture id: " +
                                            mRenderer.getTextureId());

                                    // Set-up scene camera projection to match RGB camera intrinsics
                                    mRenderer.setProjectionMatrix(
                                            projectionMatrixFromCameraIntrinsics(mIntrinsics));
                                }
                                // If there is a new RGB camera frame available, update the texture
                                // and scene camera pose.
                                if (mIsFrameAvailableTangoThread.compareAndSet(true, false)) {
                                    // {@code mRgbTimestampGlThread} contains the exact timestamp at
                                    // which the rendered RGB frame was acquired.
                                    mRgbTimestampGlThread =
                                            mTango.updateTexture(TangoCameraIntrinsics.
                                                    TANGO_CAMERA_COLOR);

                                    // Get the transform from color camera to Start of Service
                                    // at the timestamp of the RGB image in OpenGL coordinates.
                                    //
                                    // When drift correction mode is enabled in config file, we need
                                    // to query the device with respect to Area Description pose in
                                    // order to use the drift corrected pose.
                                    //
                                    // Note that if you don't want to use the drift corrected pose,
                                    // the normal device with respect to start of service pose is
                                    // still available.
                                    TangoSupport.TangoMatrixTransformData transform =
                                            TangoSupport.getMatrixTransformAtTime(
                                                    mRgbTimestampGlThread,
                                                    TangoPoseData
                                                            .COORDINATE_FRAME_AREA_DESCRIPTION,
                                                    TangoPoseData.COORDINATE_FRAME_CAMERA_COLOR,
                                                    TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                                                    TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                                                    0);
                                    if (transform.statusCode == TangoPoseData.POSE_VALID) {

                                        mRenderer.updateViewMatrix(transform.matrix);
                                        double deltaTime = mRgbTimestampGlThread
                                                - lastRenderedTimeStamp;
                                        lastRenderedTimeStamp = mRgbTimestampGlThread;
                                    }
//                                    } else {
//                                        // When the pose status is not valid, it indicates tracking
//                                        // has been lost. In this case, we simply stop rendering.
//                                        //
//                                        // This is also the place to display UI to suggest the user
//                                        // walk to recover tracking.
//                                        Log.w(TAG, "Could not get a valid transform at time " +
//                                                mRgbTimestampGlThread);
//                                    }
                                }
                            }
                            // Avoid crashing the application due to unhandled exceptions
                        } catch (TangoErrorException e) {
                            Log.e(TAG, "Tango API call error within the OpenGL render thread", e);
                        } catch (Throwable t) {
                            Log.e(TAG, "Exception on the OpenGL thread", t);
                        }
                    }
                });

        // Set the starting position and orientation of the Earth and Moon respect the OpenGL frame.
        Matrix.setIdentityM(mOpenGLTEarthMoonCenter, 0);
        Matrix.translateM(mOpenGLTEarthMoonCenter, 0, 0, 0, -1f);
        Matrix.setIdentityM(mEarthMoonCenterTEarth, 0);
        Matrix.setIdentityM(mEarthMoonCenterTMoonRotation, 0);
        Matrix.setIdentityM(mEarthMoonCenterTTranslation, 0);
        Matrix.translateM(mEarthMoonCenterTTranslation, 0, 0.5f, 0, 0);

        mSurfaceView.setRenderer(mRenderer);
    }

    /**
     * Use Tango camera intrinsics to calculate the projection Matrix for the OpenGL scene.
     */
    private static float[] projectionMatrixFromCameraIntrinsics(TangoCameraIntrinsics intrinsics) {
        // Uses frustumM to create a projection matrix taking into account calibrated camera
        // intrinsic parameter.
        // Reference: http://ksimek.github.io/2013/06/03/calibrated_cameras_in_opengl/
        float near = 0.1f;
        float far = 100;

        float xScale = near / (float) intrinsics.fx;
        float yScale = near / (float) intrinsics.fy;
        float xOffset = (float) (intrinsics.cx - (intrinsics.width / 2.0)) * xScale;
        // Color camera's coordinates has y pointing downwards so we negate this term.
        float yOffset = (float) -(intrinsics.cy - (intrinsics.height / 2.0)) * yScale;

        float m[] = new float[16];
        Matrix.frustumM(m, 0,
                xScale * (float) -intrinsics.width / 2.0f - xOffset,
                xScale * (float) intrinsics.width / 2.0f - xOffset,
                yScale * (float) -intrinsics.height / 2.0f - yOffset,
                yScale * (float) intrinsics.height / 2.0f - yOffset,
                near, far);
        return m;
    }


    public float[] updateHighlightedPlane() {
        //fixed position
        final float u = 0.5f;
        final float v = 0.5f;

        try {
            // Fit a plane on the clicked point using the latest poiont cloud data
            // Synchronize against concurrent access to the RGB timestamp in the OpenGL thread
            // and a possible service disconnection due to an onPause event.
            float[] planeFitTransform;
            synchronized (this) {
                planeFitTransform = doFitPlane(u, v, mRgbTimestampGlThread);
            }

//            if (planeFitTransform != null) {
//                // Update the position of the rendered cube to the pose of the detected plane
//                // This update is made thread safe by the renderer
//                mRenderer.updateObjectPose(planeFitTransform);
//            }

            return planeFitTransform;

        } catch (TangoException t) {
            Toast.makeText(getApplicationContext(),
                    R.string.failed_measurement,
                    Toast.LENGTH_SHORT).show();
            Log.e(TAG, getString(R.string.failed_measurement), t);
        } catch (SecurityException t) {
            Toast.makeText(getApplicationContext(),
                    R.string.failed_permissions,
                    Toast.LENGTH_SHORT).show();
            Log.e(TAG, getString(R.string.failed_permissions), t);
        }

        return null;
    }

    /**
     * Use the TangoSupport library with point cloud data to calculate the plane
     * of the world feature pointed at the location the camera is looking.
     * It returns the transform of the fitted plane in a double array.
     *
     * @param u
     * @param v
     * @param rgbTimestamp
     * @return float[] or null
     * @throws TangoErrorException, TangoInvalidException
     */
    private float[] doFitPlane(float u, float v, double rgbTimestamp) {
        TangoPointCloudData pointCloud = mPointCloudManager.getLatestPointCloud();

        if (pointCloud == null) {
            return null;
        }

        // We need to calculate the transform between the color camera at the
        // time the user clicked and the depth camera at the time the depth
        // cloud was acquired.
        TangoPoseData colorTdepthPose = TangoSupport.calculateRelativePose(
                rgbTimestamp, TangoPoseData.COORDINATE_FRAME_CAMERA_COLOR,
                pointCloud.timestamp, TangoPoseData.COORDINATE_FRAME_CAMERA_DEPTH);

        // Perform plane fitting with the latest available point cloud data.
        TangoSupport.IntersectionPointPlaneModelPair intersectionPointPlaneModelPair =
                TangoSupport.fitPlaneModelNearPoint(pointCloud, colorTdepthPose, u, v);

        // Get the transform from depth camera to OpenGL world at the timestamp of the cloud.
        TangoSupport.TangoMatrixTransformData transform =
                TangoSupport.getMatrixTransformAtTime(pointCloud.timestamp,
                        TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                        TangoPoseData.COORDINATE_FRAME_CAMERA_DEPTH,
                        TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                        TangoSupport.TANGO_SUPPORT_ENGINE_TANGO, 0);

        if (transform.statusCode == TangoPoseData.POSE_VALID) {
            float[] openGlTPlane = calculatePlaneTransform(
                    intersectionPointPlaneModelPair.intersectionPoint,
                    intersectionPointPlaneModelPair.planeModel, transform.matrix);

            return openGlTPlane;
        } else {
            Log.w(TAG, "Can't get depth camera transform at time " + pointCloud.timestamp);
            return null;
        }
    }

    /**
     * Calculate the pose of the plane based on the position and normal orientation of the plane
     * and align it with gravity.
     */
    private float[] calculatePlaneTransform(double[] point, double normal[],
                                            float[] openGlTdepth) {
        // Vector aligned to gravity.
        float[] openGlUp = new float[]{0, 1, 0, 0};
        float[] depthTOpenGl = new float[16];
        Matrix.invertM(depthTOpenGl, 0, openGlTdepth, 0);

        float[] depthUp = new float[4];
        Matrix.multiplyMV(depthUp, 0, depthTOpenGl, 0, openGlUp, 0);

        // Create the plane matrix transform in depth frame from a point,
        // the plane normal and the up vector.
        float[] depthTplane = matrixFromPointNormalUp(point, normal, depthUp);
        float[] openGlTplane = new float[16];
        Matrix.multiplyMM(openGlTplane, 0, openGlTdepth, 0, depthTplane, 0);

        return openGlTplane;
    }

    /**
     * Calculates a transformation matrix based on a point, a normal and the up gravity vector.
     * The coordinate frame of the target transformation will a right handed system with Z+ in
     * the direction of the normal and Y+ up.
     */
    private float[] matrixFromPointNormalUp(double[] point, double[] normal, float[] up) {
        float[] zAxis = new float[]{
                (float) normal[0], (float) normal[1], (float) normal[2]
        };

        normalize(zAxis);

        float[] xAxis = crossProduct(up, zAxis);
        normalize(xAxis);

        float[] yAxis = crossProduct(zAxis, xAxis);
        normalize(yAxis);

        float[] m = new float[16];
        Matrix.setIdentityM(m, 0);

        m[0] = xAxis[0];
        m[1] = xAxis[1];
        m[2] = xAxis[2];
        m[4] = yAxis[0];
        m[5] = yAxis[1];
        m[6] = yAxis[2];
        m[8] = zAxis[0];
        m[9] = zAxis[1];
        m[10] = zAxis[2];
        m[12] = (float) point[0];
        m[13] = (float) point[1];
        m[14] = (float) point[2];

        return m;
    }

    /**
     * Normalize a vector.
     */
    private void normalize(float[] v) {
        double norm = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        v[0] /= norm;
        v[1] /= norm;
        v[2] /= norm;
    }

    /**
     * Cross product between two vectors following the right hand rule.
     */
    private float[] crossProduct(float[] v1, float[] v2) {
        float[] result = new float[3];
        result[0] = v1[1] * v2[2] - v2[1] * v1[2];
        result[1] = v1[2] * v2[0] - v2[2] * v1[0];
        result[2] = v1[0] * v2[1] - v2[0] * v1[1];
        return result;
    }

    public final void quatToMatrix(float[] q, float[] m) {
        float sqw = q[3] * q[3];
        float sqx = q[0] * q[0];
        float sqy = q[1] * q[1];
        float sqz = q[2] * q[2];

        // invs (inverse square length) is only required if quaternion is not already normalised
        float invs = 1 / (sqx + sqy + sqz + sqw);
        m[0] = (sqx - sqy - sqz + sqw) * invs; // since sqw + sqx + sqy + sqz =1/invs*invs
        m[5] = (-sqx + sqy - sqz + sqw) * invs; //1 1
        m[10] = (-sqx - sqy + sqz + sqw) * invs; // 2 2

        float tmp1 = q[0] * q[1];
        float tmp2 = q[2] * q[3];
        m[4] = 2.0f * (tmp1 + tmp2) * invs;
        m[1] = 2.0f * (tmp1 - tmp2) * invs;

        tmp1 = q[0] * q[2];
        tmp2 = q[1] * q[3];
        m[8] = 2.0f * (tmp1 - tmp2) * invs;
        m[2] = 2.0f * (tmp1 + tmp2) * invs;
        tmp1 = q[1] * q[2];
        tmp2 = q[0] * q[3];
        m[9] = 2.0f * (tmp1 + tmp2) * invs;
        m[6] = 2.0f * (tmp1 - tmp2) * invs;
    }


    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (motionEvent.getAction() == MotionEvent.ACTION_UP && debugSagitta) {
            // Calculate click location in u,v (0;1) coordinates.
            float u = motionEvent.getX() / view.getWidth();
            float v = motionEvent.getY() / view.getHeight();

            return addBearing();
        }
        return true;
    }


    public boolean addBearing() {
        try {
            /// area -> camera
            TangoPoseData startPose = TangoSupport.getPoseAtTime(
                    mRgbTimestampGlThread,
                    TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                    TangoPoseData.COORDINATE_FRAME_CAMERA_COLOR,
                    TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                    0);

            if (startPose.statusCode == TangoPoseData.POSE_VALID) {

                float[] end = new float[4];
                float[] start = new float[4];

                start[0] = (float) startPose.translation[0];
                start[1] = (float) startPose.translation[1];
                start[2] = (float) startPose.translation[2];
                start[3] = 0.0f;

                TangoPointCloudData pointCloud = mPointCloudManager.getLatestPointCloud();
                if (pointCloud == null) {
                    return false; ///???
                }

                // We need to calculate the transform between the color camera at the
                // time the user clicked and the depth camera at the time the depth
                // cloud was acquired.
                TangoPoseData colorTdepthPose = TangoSupport.calculateRelativePose(
                        mRgbTimestampGlThread,
                        TangoPoseData.COORDINATE_FRAME_CAMERA_COLOR,
                        pointCloud.timestamp,
                        TangoPoseData.COORDINATE_FRAME_CAMERA_DEPTH);

                if (colorTdepthPose.statusCode == TangoPoseData.POSE_VALID) {

                    /// from camera_depth to area_description
                    TangoSupport.TangoMatrixTransformData depthTarea =
                            TangoSupport.getMatrixTransformAtTime(pointCloud.timestamp,
                                    TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                                    TangoPoseData.COORDINATE_FRAME_CAMERA_DEPTH,
                                    TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                                    TangoSupport.TANGO_SUPPORT_ENGINE_TANGO,
                                    0);

                    if (depthTarea.statusCode == TangoPoseData.POSE_VALID) {

                        float[] depthTOpenGl = depthTarea.matrix;

                        mRenderer.addBearing(start, depthTOpenGl);

                        end[0] += start[0];
                        end[1] += start[1];
                        end[2] += start[2];

                        double[] scoord = intersector.getMaximum();
                        float[] m = new float[16];
                        Matrix.setIdentityM(m, 0);

                        m[12] = (float) scoord[0];
                        m[13] = (float) scoord[1];
                        m[14] = (float) scoord[2];

                        mRenderer.setSphereTransform(m);
                        mRenderer.setShouldDrawSphere(intersector.isHasMaximum());
                        mRenderer.setLinePos(start, end);
                        return true;
                    }
                }
            }

        } catch (TangoErrorException e) {
            Toast.makeText(getApplicationContext(),
                    R.string.exception_tango_error,
                    Toast.LENGTH_SHORT).show();
            Log.e(TAG, getString(R.string.exception_tango_error), e);
        } catch (TangoInvalidException e) {
            Toast.makeText(getApplicationContext(),
                    R.string.exception_tango_params,
                    Toast.LENGTH_SHORT).show();
            Log.e(TAG, getString(R.string.exception_tango_params), e);
        } catch (AndroidRuntimeException e) {
            Toast.makeText(getApplicationContext(),
                    R.string.exception_unknown,
                    Toast.LENGTH_SHORT).show();
            Log.e(TAG, getString(R.string.exception_unknown), e);
        } catch (SecurityException e) {
            Toast.makeText(getApplicationContext(),
                    R.string.failed_permissions,
                    Toast.LENGTH_SHORT).show();
            Log.e(TAG, getString(R.string.failed_permissions), e);
        }
        return false;
    }

    @Override
    public void onFragmentInteraction(@NotNull Uri uri) {}
}
