package com.vr_object.fixed;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Message;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import com.vr_object.map_editor.MapFragment;
import com.vr_object.screencast.ScreenRecorderService;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
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

public class WiFiAugmentedRealityActivity extends BaseFinderActivity
        implements View.OnClickListener,
        View.OnTouchListener,
        NetworkInfoFragment.OnListFragmentInteractionListener,
        ChannelInfoFragment.OnListFragmentInteractionListener,
        MapFragment.OnFragmentInteractionListener  {
    private static final String TAG = WiFiAugmentedRealityActivity.class.getSimpleName();
    private static final int INVALID_TEXTURE_ID = 0;
    private MyBroadcastReceiver mReceiver;

    private TextView mTextView;
    //private TextView mTextView2;
    private ProgressBar mProgressBar;
    private GLSurfaceView mSurfaceView;
    private WiFiAugmentedRealityRenderer mRenderer;
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

    private CameraManager cameraManager;


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

    private PowerManager.WakeLock wakeLock;

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

    /**
     * Sigmoid with followimg params:
     * f(0) ~ 100%
     * f(3000) ~ 80%
     * after that swiftly descend
     * @param x
     * @return sigmoid
     */
    private double progressSigmoid(double x) {
        return 100-100/(1+Math.exp(-(x-4500)/1000));
    }

    private void updateLevelDiff(double levelDiff) {
        long deltaTime = System.currentTimeMillis() - lastUpdateTime;
        int progress = (int) Math.floor(100.0 * levelDiff);
        int sigma = (int)progressSigmoid(progress);

        String text = String.format(Locale.ENGLISH, "%d%%", sigma);
        mTextView.setText(text);

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

        try {
            cameraManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
            String camId = getCameraWithFacing(cameraManager, CameraCharacteristics.LENS_FACING_BACK);
        } catch (CameraAccessException ex) {
            Toast.makeText(this, "Could not use camera", Toast.LENGTH_LONG).show();
        }

        mSurfaceView.onResume();
        // Set render mode to RENDERMODE_CONTINUOUSLY to force getting onDraw callbacks until the
        // Tango service is properly set-up and we start getting onFrameAvailable callbacks.
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        // Initialize Tango Service as a normal Android Service, since we call mTango.disconnect()
        // in onPause, this will unbind Tango Service, so every time when onResume gets called, we
        // should create a new Tango object.

        intersector = new SpatialIntersect();

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ScreenRecorderService.ACTION_QUERY_STATUS_RESULT);
        registerReceiver(mReceiver, intentFilter);
        queryRecordingStatus();

    }

    @Override
    protected void onPause() {
        super.onPause();
        mSurfaceView.onPause();

        unregisterReceiver(mReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        wakeLock.release();
    }

    @Nullable
    private String getCameraWithFacing(@NonNull CameraManager manager, int lensFacing) throws CameraAccessException {
        String possibleCandidate = null;
        String[] cameraIdList = manager.getCameraIdList();
        if (cameraIdList.length == 0) {
            return null;
        }
        for (String cameraId : cameraIdList) {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);

            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map == null) {
                continue;
            }

            Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (facing != null && facing == lensFacing) {
                return cameraId;
            }

            //just in case device don't have any camera with given facing
            possibleCandidate = cameraId;
        }
        if (possibleCandidate != null) {
            return possibleCandidate;
        }
        return cameraIdList[0];
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
                    public void preRender() {}
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
        mThreshold = thrSB.getProgress();
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
    
    public void showMap(View view) {
        Fragment mapFragment = new com.vr_object.map_editor.MapFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.map_frame, mapFragment);
        transaction.addToBackStack(null);
        transaction.commit();
        Toast.makeText(this, "Map!", Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        return super.onTouch(view, motionEvent);
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
        Log.d(TAG, String.format("changeChannel: %d", chan));

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
            } catch (NullPointerException e1) {
                e1.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), R.string.check_usb_connection, Toast.LENGTH_LONG).show();
                    }
                });
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

    @Override
    public void onFragmentInteraction(@NotNull Uri uri) {

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
