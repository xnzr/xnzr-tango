package com.vr_object.fixed;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.media.projection.MediaProjectionManager;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.AndroidRuntimeException;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TabHost;
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
import com.vr_object.firebase.DatabaseProxy;
import com.vr_object.fixed.xnzrw24b.AimView;
import com.vr_object.fixed.xnzrw24b.ChannelInfoFragment;
import com.vr_object.fixed.xnzrw24b.DeviceNotFoundException;
import com.vr_object.fixed.xnzrw24b.DeviceOpenFailedException;
import com.vr_object.fixed.xnzrw24b.ItsPacketCreator;
import com.vr_object.fixed.xnzrw24b.LevelCalculator;
import com.vr_object.fixed.xnzrw24b.MessageFields;
import com.vr_object.fixed.xnzrw24b.NetworkInfoFragment;
import com.vr_object.fixed.xnzrw24b.PacketFromDevice;
import com.vr_object.fixed.xnzrw24b.SpatialIntersect;
import com.vr_object.fixed.xnzrw24b.UsbSerialPortTi;
import com.vr_object.fixed.xnzrw24b.WFPacketCreator;
import com.vr_object.fixed.xnzrw24b.WFParseException;
import com.vr_object.fixed.xnzrw24b.data.ChannelInfo;
import com.vr_object.fixed.xnzrw24b.data.ChannelsWiFi;
import com.vr_object.fixed.xnzrw24b.data.GlobalSettings;
import com.vr_object.fixed.xnzrw24b.data.NetworkInfo;
import com.vr_object.screencast.ScreenRecorderService;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.vr_object.fixed.R.id.b_clear_sagittae;
import static com.vr_object.fixed.R.id.b_hide_clear_sagittae;
import static com.vr_object.fixed.R.id.b_on_off_circle;
import static com.vr_object.fixed.R.id.b_on_off_sound;
import static com.vr_object.fixed.R.id.b_start_recording;
import static com.vr_object.fixed.R.id.b_stop_recording;
import static com.vr_object.fixed.R.id.b_wifi_ble_switch;
import static com.vr_object.fixed.R.id.circle_container;
import static com.vr_object.fixed.R.id.options_scroll_view;
import static com.vr_object.fixed.R.id.sagittae_length_setter;
import static com.vr_object.fixed.R.id.threshold_setter;
import static com.vr_object.fixed.R.id.tw_scanning_message;

public class WiFiAugmentedRealityActivity extends Activity
        implements View.OnClickListener,
        View.OnTouchListener,
        NetworkInfoFragment.OnListFragmentInteractionListener,
        ChannelInfoFragment.OnListFragmentInteractionListener {
    private static final String TAG = WiFiAugmentedRealityActivity.class.getSimpleName();
    private static final int INVALID_TEXTURE_ID = 0;
    private static final boolean DEBUG = false;
    private static final int REQUEST_CODE_SCREEN_CAPTURE = 1;
    private static final int EXTERNAL_STORAGE_REQUEST_CODE = 2;
    private static final int RECORD_AUDIO_REQUEST_CODE = 2;
    public static final int MAX_PROGRESS_RSSI = -40;
    public static final int MIN_PROGRESS_RSSI = -100;
    private MyBroadcastReceiver mReceiver;

    private TextView mTextView;
    //private TextView mTextView2;
    private ProgressBar mProgressBar;
    private GLSurfaceView mSurfaceView;
    private WiFiAugmentedRealityRenderer mRenderer;
    private TangoCameraIntrinsics mIntrinsics;
    private Tango mTango;
    private TangoConfig mConfig;
    private boolean mIsConnected = false;
    private SpatialIntersect intersector;

    private boolean debugSagitta = false;

    private DatabaseProxy firebaseProxy = new DatabaseProxy();

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

    private TabHost optionsTabbedWindow;

    ///usb
    private NetworkInfo mSelectedNetwork;
    private int mSelectedChannel = 0;
    private LevelCalculator mLevelCalculator;

    private android.os.Handler handler;

    private NetworkInfoFragment networksFragment;
    private ChannelInfoFragment mChannelsFragment;

    private SeekBar mThresholdSetter;
    private TextView mThresholdView;
    private int mThreshold;

    private SeekBar mSagittaeLenghtSetter;
    private TextView mSagittaeLengthView;

    private AimView mAimView;
    private TextView mScanningMessage;
    FrameLayout mCircleFrameLayout;


    private final float defaultSagittaeWidth = 0.001f;

    private Thread thread;
    private UsbSerialPortTi port = null;
//    private ScreenRecorder mScreenRecorder;


    private final int INIT_INTERVAL_MS = 3000;
    private final int READ_TIMEOUT_MS = 500;
    private final int READ_BUF_SIZE = ItsPacketCreator.PACK_LEN * 16;

    private final int STATE_INIT = 0;
    private final int STATE_READING = 1;
    private final int STATE_EXIT = 255;
    private int state = STATE_INIT;

    private int curChan = 1;
    private int setChan = 1;

    private WFPacketCreator wfCreator = new WFPacketCreator();
    ///

    private long numUpdates = 0;
    private long lastUpdateTime = 0;
    private final long TIME_PERIOD = 3 * 1000; //ms

    private OptionsHolder optionsHolder = new OptionsHolder(this);

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

        int tangoVersion = Tango.getVersion(this.getApplicationContext());

        //mScreenRecorder = new ScreenRecorder(this);

        mPointCloudManager = new TangoPointCloudManager();

        setContentView(R.layout.activity_main);
        mSurfaceView = (GLSurfaceView) findViewById(R.id.surfaceview);
        setupRenderer();

        firebaseProxy.CreateConnection();

        mSurfaceView.setOnTouchListener(this);

        //progress bar
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mProgressBar.setMax(MAX_PROGRESS_RSSI - MIN_PROGRESS_RSSI);
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

    private void setupTabbedOptions() {
        optionsTabbedWindow = (TabHost) findViewById(R.id.options_tab_host);
        optionsTabbedWindow.setup();

        TabHost.TabSpec bearingOptions = optionsTabbedWindow.newTabSpec(getString(R.string.peleng_tab_head));
        bearingOptions.setContent(R.id.options_peleng_tab);
        bearingOptions.setIndicator(this.getString(R.string.peleng_tab_head));
        optionsTabbedWindow.addTab(bearingOptions);

        TabHost.TabSpec screenOptions = optionsTabbedWindow.newTabSpec(getString(R.string.screen_tab_head));
        screenOptions.setContent(R.id.options_screen_tab);
        screenOptions.setIndicator(this.getString(R.string.screen_tab_head));
        optionsTabbedWindow.addTab(screenOptions);
    }

    private void updateLevelProgress(final double level) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                double pLevel = Math.min(level, MAX_PROGRESS_RSSI);
                int progress = (int)(pLevel - MIN_PROGRESS_RSSI);
                mProgressBar.setProgress(progress);
            }
        });
    }

    private void updateLevelDiff(double levelDiff) {
        long deltaTime = System.currentTimeMillis() - lastUpdateTime;
        int progress = (int) Math.floor(100.0 * levelDiff);

        String text = String.format("%d (%a)", progress, levelDiff);
        mTextView.setText(text);

        // lines
        if (deltaTime > TIME_PERIOD) {
            if (progress < mThreshold) {
                addBearing();
                numUpdates++;
            }
            lastUpdateTime = System.currentTimeMillis();
        }

        setLevel(levelDiff);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mScanningMessage.setVisibility(View.GONE);
            }
        });

    }

    public void setLevel(double level) {
        mAimView.setSizes(mCircleFrameLayout.getMeasuredWidth(), mCircleFrameLayout.getMeasuredHeight());
        mAimView.setLevel(level);
        mAimView.invalidate();

    }

    public void clearRadius() {
        mAimView.setRadius(0);
        mAimView.invalidate();
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

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    // permissions
    private static final String CAMERA_PERMISSION = android.Manifest.permission.CAMERA;
    private static final int CAMERA_PERMISSION_CODE = 0;

    boolean hasCameraPermission() {
        int result = ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION);

        return result == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, CAMERA_PERMISSION)) {
            showRequestPermissionRationale();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{CAMERA_PERMISSION},
                    CAMERA_PERMISSION_CODE);
        }
    }

    /**
     * If the user has declined the permission before, we have to explain that the app needs this
     * permission.
     */
    private void showRequestPermissionRationale() {
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage("WiFiAugumentedReality requires camera permission")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ActivityCompat.requestPermissions(WiFiAugmentedRealityActivity.this,
                                new String[]{CAMERA_PERMISSION}, CAMERA_PERMISSION_CODE);
                    }
                })
                .create();
        dialog.show();
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

    /// button
    @Override
    public void onClick(View view) {
        Log.d(TAG, "onClick");
    }

    public void restartScan(View view) {
        changeState(STATE_INIT);
        clearRadius();
    }

    public void showCreateBleButton(final boolean show) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (show) {
                    findViewById(R.id.b_create_ble_name).setVisibility(View.VISIBLE);
                } else {
                    findViewById(R.id.b_create_ble_name).setVisibility(View.GONE);
                }
            }
        });
    }

    public void showOptions(View view) {
        loadOptions();
        findViewById(options_scroll_view).setVisibility(View.VISIBLE);
        findViewById(R.id.options_tab_container).setVisibility(View.VISIBLE);
    }

    public void closeAndSaveOptions(View view) {
        closeAndSaveOptions();
    }

    private void closeAndSaveOptions() {
        closeOptions();
        saveOptions();
    }
    public void closeOptions(View view) {
        closeOptions();
    }

    private void closeOptions() {
        View optionsScreen = findViewById(options_scroll_view);
        optionsScreen.setVisibility(View.GONE);
        findViewById(R.id.options_tab_container).setVisibility(View.GONE);
    }

    public void clearPelengs(View view) {
        mRenderer.clearPelengs();
    }

    private void saveOptions() {
        SeekBar thrSB = (SeekBar) findViewById(threshold_setter);
        int thr = thrSB.getProgress();
        mThreshold = thr - thr % 100;
        optionsHolder.saveThreshold(mThreshold);

        SeekBar slSB = (SeekBar) findViewById(sagittae_length_setter);
        int sl = slSB.getProgress();
        int prevSl = optionsHolder.loadSagittaLength();
        if (sl != prevSl) {
            optionsHolder.saveSagittaLength(sl);
            mRenderer.setSagittaLength(sl);
            Toast.makeText(getApplicationContext(), R.string.sagittae_length_changed, Toast.LENGTH_LONG).show();
        }

        Switch clearSagittaeSwitch = (Switch) findViewById(b_hide_clear_sagittae);
        boolean cssChecked = clearSagittaeSwitch.isChecked();
        optionsHolder.saveShowClearSagittaeButton(cssChecked);
        setClearSagittaeButtonVisibility(cssChecked);

        Switch circle = (Switch) findViewById(b_on_off_circle);
        boolean circleOn = circle.isChecked();
        optionsHolder.saveCircleVisibility(circleOn);
        setCircleVisibility(circleOn);

        Switch sound = (Switch) findViewById(b_on_off_sound);
        boolean soundState = sound.isChecked();
        optionsHolder.saveSoundState(soundState);
        setSoundState(soundState);

        Switch wifi_ble_switch = (Switch) findViewById(b_wifi_ble_switch);
        boolean wifi_ble = wifi_ble_switch.isChecked();
        optionsHolder.saveWifiBleState(wifi_ble);
        setWorkMode(wifi_ble);
    }

    private void setClearSagittaeButtonVisibility(boolean cssChecked) {
        View clearSagittaeButton = findViewById(b_clear_sagittae);
        if (cssChecked) {
            clearSagittaeButton.setVisibility(View.VISIBLE);
        } else {
            clearSagittaeButton.setVisibility(View.GONE);
        }
    }

    private void setCircleVisibility(boolean value) {
        if (value) {
            mAimView.setVisibility(View.VISIBLE);
        } else {
            mAimView.setVisibility(View.GONE);
        }
    }

    private void setSoundState(boolean state) {
        mAimView.setSound(state);
    }

    /**
     * Set Wi-Fi / BLE workmode. Implemented using control Switch. Note: it produces boolean values.
     * @param mode true: Wi-Fi false: BLE
     */
    private void setWorkMode(boolean mode) {
        if (mode) {
            GlobalSettings.setMode(GlobalSettings.WorkMode.WIFI);
        } else {
            GlobalSettings.setMode(GlobalSettings.WorkMode.BLE);
        }
        showCreateBleButton(!mode);
    }

    private void loadOptions() {
        int thr = optionsHolder.loadThreshold();

        if (mThresholdSetter == null) {
            mThresholdSetter = (SeekBar) findViewById(threshold_setter);
        }
        mThresholdSetter.setProgress(thr);

        int sagittaLength = optionsHolder.loadSagittaLength();
        if (mSagittaeLenghtSetter == null) {
            mSagittaeLenghtSetter = (SeekBar) findViewById(sagittae_length_setter);
        }
        mSagittaeLenghtSetter.setProgress(sagittaLength);

        boolean cssChecked = optionsHolder.loadShowClearSagittaeButton();
        setClearSagittaeButtonVisibility(cssChecked);
        Switch clearSagittaeSwitch = (Switch) findViewById(b_hide_clear_sagittae);
        clearSagittaeSwitch.setChecked(cssChecked);

        boolean circleOn = optionsHolder.loadCircleVisibility();
        setCircleVisibility(circleOn);
        Switch circle = (Switch) findViewById(b_on_off_circle);
        circle.setChecked(circleOn);

        boolean soundOn = optionsHolder.loadSoundState();
        setSoundState(soundOn);
        Switch sound = (Switch) findViewById(b_on_off_sound);
        sound.setChecked(soundOn);

        boolean wifi_ble_state = optionsHolder.loadWifiBleState();
        setWorkMode(wifi_ble_state);
        Switch wifi_ble = (Switch) findViewById(b_wifi_ble_switch);
        wifi_ble.setChecked(wifi_ble_state);
    }

    public void showNameBlePanel(View view) {
        findViewById(R.id.create_ble_name_panel).setVisibility(View.VISIBLE);
    }

    public void closeNameBlePanel(View view) {
        findViewById(R.id.create_ble_name_panel).setVisibility(View.GONE);
    }

    public void saveBleName(View view) {
        findViewById(R.id.create_ble_name_panel).setVisibility(View.GONE);

        if (mSelectedNetwork == null) {
            Toast.makeText(this, R.string.ble_device_not_chosen, Toast.LENGTH_LONG).show();
            return;
        }

        String name = ((EditText)findViewById(R.id.et_ble_name)).getText().toString();
        mSelectedNetwork.BleName = name;

        firebaseProxy.PushData(mSelectedNetwork.Mac, name);
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

    ///usb
    private void usbLoop() {
        boolean isRunning = true;

        while (!Thread.currentThread().isInterrupted() && isRunning) {
            if (state == STATE_INIT) {
                stepInit();
            } else if (state == STATE_READING) {
                stepRead();
            } else if (state == STATE_EXIT) {
                isRunning = false;
            } else {
                Log.e(TAG, "Logic error");
                isRunning = false;
            }
        }

        Log.d(TAG, "I was interrupted");
        closeDevice();
    }

    private Thread changer = null;

    private void stepInit() {
        port = new UsbSerialPortTi(getApplicationContext());

        boolean inited = false;

        while (!inited && !Thread.currentThread().isInterrupted()) {
            try {
                port.init();
                showCreateBleButton(GlobalSettings.getMode() == GlobalSettings.WorkMode.BLE);
                //sending to enable old protocol CODE=2122239
                String cmd = "$2122239";
                byte[] cmdBytes = new byte[cmd.length()];
                for (int i = 0; i < cmd.length(); ++i) {
                    cmdBytes[i] = (byte) cmd.charAt(i);
                }
                port.write(cmdBytes, READ_TIMEOUT_MS);
                sendInfo("Device init OK");
                inited = true;
                changeState(STATE_READING);

                if (changer != null) {
                    changer.interrupt();
                }

                changer = createChangerThread();
                changer.start();

            } catch (DeviceNotFoundException e) {
                sendError("Device was not found");
            } catch (DeviceOpenFailedException e) {
                sendError("Device init error. Check Permissions.");
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (!inited) {
                try {
                    Thread.sleep(INIT_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Log.d(TAG, "I was interrupted");
                    changeState(STATE_EXIT);
                    return;
                }
            }

        }
    }

    private Thread createChangerThread() {
        return new Thread() {
            @Override
            public void run() {
                try {
                    while (!Thread.interrupted()) {
                        Thread.sleep(INIT_INTERVAL_MS);
                        Log.d(TAG, "Going to change channel");
                        changeChannel();
                        Log.d(TAG, "---------------------");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "  *** CHANGER EXIT ***");
            }
        };
    }

    private void stepRead() {
        byte[] buf = new byte[READ_BUF_SIZE];
        int read = 0;
        try {
            read = port.read(buf, READ_TIMEOUT_MS);
            Log.d(TAG, "Read " + Arrays.toString(buf));


            wfCreator.putData(buf, read);
            ArrayList<PacketFromDevice> packets = wfCreator.getPackets();
            //Log.d(TAG, "Have " + packets.size() + " wfifi packets" );

            if (packets.size() > 0) {
                //TODO: Send packets
                for (PacketFromDevice p : packets) {
                    sendInfo(p.toString());
                    sendData(p);
                }
            }
        } catch (IOException e) {
            sendError("IOError");
            closeDevice();
            changeState(STATE_INIT);
//        }
//        catch (BadRssiException e) {
//            // nop
//            Log.d( TAG, "Bad Rssi" );
        } catch (Exception e) {
            Log.e(TAG, "Parse error: read " + read + " bytes");
            String sss = "data: ";
            String str = "";
            for (int i = 0; i < read; i++) {
                sss += String.format("0x%02x ", buf[i]);
                if (buf[i] != 0x0d) {
                    str += Character.toString((char) buf[i]);
                }
            }
            Log.e(TAG, sss);
            Log.e(TAG, str);

            sendError("ERROR");

            //changeChannel();

        }
    }

    private void changeChannel() {
        changeChannel(0);
    }

    private void changeChannel(int chan) {
        Log.d(TAG, "changeChannel ");

        switch (GlobalSettings.getMode()) {
            case WIFI:
                setChan = chan;
                break;
            case BLE:
                setChan = 1;
                break;
        }

        if (port != null) {
            try {
                if (setChan > 0) {
                    port.write(getChannelChar(setChan), READ_TIMEOUT_MS);
                } else {
                    port.write(getChannelChar(curChan), READ_TIMEOUT_MS);
                    setChan = curChan;
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            if (++curChan > 13) {
                curChan = 1;
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView tw = (TextView) findViewById(R.id.tw_current_frequency);
                    tw.setVisibility(View.VISIBLE);
                    tw.setText(String.format("%.3f %s", ChannelsWiFi.channelFreq24GHz(setChan), getString(R.string.ghz_notion)));
                }
            });
        }
    }

    private BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    Log.d(TAG, "Device was detached");
                    sendInfo("Device was detached");
                    closeDevice();
                    changeState(STATE_INIT);
                }
            }
        }
    };

    private void changeState(int newState) {
        state = newState;

        if (state == STATE_READING) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mScanningMessage.setVisibility(View.VISIBLE);
                }
            });
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mScanningMessage.setVisibility(View.GONE);
                }
            });
        }
    }

    private void closeDevice() {
        if (port != null) {
            port.close();
        }
    }

    private void sendInfo(String infoString) {
        Message message = handler.obtainMessage(MessageFields.CODE_INFO);
        //message.setAction(ServiceConstants.ACTION_NAME);
        message.getData().putInt(MessageFields.FIELD_CODE_INT, MessageFields.CODE_INFO);
        message.getData().putString(MessageFields.FIELD_INFO_STR, infoString);
        //sendBroadcast(intent);
        handler.sendMessage(message);
    }

    private void sendError(String errorString) {
        Message message = handler.obtainMessage(MessageFields.CODE_ERROR);
        //intent.setAction(ServiceConstants.ACTION_NAME);
        message.getData().putInt(MessageFields.FIELD_CODE_INT, MessageFields.CODE_ERROR);
        message.getData().putString(MessageFields.FIELD_INFO_STR, errorString);
        //sendBroadcast(intent);
        handler.sendMessage(message);
    }

    private void sendData(PacketFromDevice packet) {
        Message message = handler.obtainMessage(MessageFields.CODE_DATA);
        //intent.setAction(ServiceConstants.ACTION_NAME);
        message.getData().putInt(MessageFields.FIELD_CODE_INT, MessageFields.CODE_DATA);
        message.getData().putString(MessageFields.FIELD_SSID_STR, packet.apName);
        message.getData().putString(MessageFields.FIELD_MAC_STR, packet.mac);
        message.getData().putLong(MessageFields.FIELD_TIME_MS_LONG, packet.time);
        message.getData().putInt(MessageFields.FIELD_ANT_INT, packet.antIdx);
        message.getData().putInt(MessageFields.FIELD_CH_INT, packet.wifiCh);
        message.getData().putDouble(MessageFields.FIELD_RSSI_DOUBLE, packet.power);
        message.getData().putString(MessageFields.FIELD_RAW_STR, packet.raw);
        //sendBroadcast(intent);
        handler.sendMessage(message);
    }

    private byte getChannelChar(int channel) {
        if (channel < 10) {
            return (byte) (48 + channel);
        } else {
            return (byte) (65 + channel - 10);
        }
    }

    @Override
    public void onListFragmentInteraction(NetworkInfo item) {
        if (mSelectedNetwork == null || mSelectedNetwork != item) {
            mSelectedChannel = 0;
            mLevelCalculator = null;
            mSelectedNetwork = item;
            mChannelsFragment.setNetwork(item);
            //mCameraFragmentInterface.clearRadius();
            if (changer == null) {
                changer = createChangerThread();
                changer.start();
            }
        }
    }

    @Override
    public void onListFragmentInteraction(ChannelInfo item) {
        if (mSelectedChannel != item.Channel) {
            //mCameraFragmentInterface.clearRadius();
            mLevelCalculator = new LevelCalculator(mSelectedNetwork.Ssid, mSelectedNetwork.Mac);
            mSelectedChannel = item.Channel;
            if (changer != null) {
                changer.interrupt();
                changer = null;
            }
            changeChannel(mSelectedChannel);
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (DEBUG) Log.v(TAG, "onActivityResult:resultCode=" + resultCode + ",data=" + data);
        super.onActivityResult(requestCode, resultCode, data);
        if (REQUEST_CODE_SCREEN_CAPTURE == requestCode) {
            if (resultCode != Activity.RESULT_OK) {
                // when no permission
                Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_LONG).show();
                return;
            }
            startScreenRecorder(resultCode, data);
        }
    }

    public void startRecording(View view) {
//        if (mScreenRecorder != null) {
//            mScreenRecorder.startRecording();
//        }

        //Check permission.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    EXTERNAL_STORAGE_REQUEST_CODE);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO_REQUEST_CODE);
        }


        findViewById(b_start_recording).setVisibility(View.GONE);
        findViewById(b_stop_recording).setVisibility(View.VISIBLE);
        final MediaProjectionManager manager
                = (MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        final Intent permissionIntent = manager.createScreenCaptureIntent();
        startActivityForResult(permissionIntent, REQUEST_CODE_SCREEN_CAPTURE);
    }

    public void stopRecording(View view) {
//        if (mScreenRecorder != null) {
//            mScreenRecorder.stopRecording();
//        }
        findViewById(b_start_recording).setVisibility(View.VISIBLE);
        findViewById(b_stop_recording).setVisibility(View.GONE);

        final Intent intent = new Intent(WiFiAugmentedRealityActivity.this, ScreenRecorderService.class);
        intent.setAction(ScreenRecorderService.ACTION_STOP);
        startService(intent);
    }

    private void queryRecordingStatus() {
        if (DEBUG) Log.v(TAG, "queryRecording:");
        final Intent intent = new Intent(this, ScreenRecorderService.class);
        intent.setAction(ScreenRecorderService.ACTION_QUERY_STATUS);
        startService(intent);
    }

    private void startScreenRecorder(final int resultCode, final Intent data) {
        final Intent intent = new Intent(this, ScreenRecorderService.class);
        intent.setAction(ScreenRecorderService.ACTION_START);
        intent.putExtra(ScreenRecorderService.EXTRA_RESULT_CODE, resultCode);
        intent.putExtras(data);
        startService(intent);
    }

    private void updateRecording(final boolean isRecording, final boolean isPausing) {
        if (DEBUG) Log.v(TAG, "updateRecording:isRecording=" + isRecording + ",isPausing=" + isPausing);
        //TODO: update buttons
    }


    private static final class MyBroadcastReceiver extends BroadcastReceiver {
        private final WeakReference<WiFiAugmentedRealityActivity> mWeakParent;
        public MyBroadcastReceiver(final WiFiAugmentedRealityActivity parent) {
            mWeakParent = new WeakReference<WiFiAugmentedRealityActivity>(parent);
        }

        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (DEBUG) Log.v(TAG, "onReceive:" + intent);
            final String action = intent.getAction();
            if (ScreenRecorderService.ACTION_QUERY_STATUS_RESULT.equals(action)) {
                final boolean isRecording = intent.getBooleanExtra(ScreenRecorderService.EXTRA_QUERY_RESULT_RECORDING, false);
                final boolean isPausing = intent.getBooleanExtra(ScreenRecorderService.EXTRA_QUERY_RESULT_PAUSING, false);
                final WiFiAugmentedRealityActivity parent = mWeakParent.get();
                if (parent != null) {
                    parent.updateRecording(isRecording, isPausing);
                }
            }
        }
    }
}
