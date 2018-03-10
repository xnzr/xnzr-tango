package com.vr_object.fixed;

import android.Manifest;
import android.annotation.SuppressLint;
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
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.media.ImageReader;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
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
import com.vr_object.fixed.xnzrw24b.AutoFitTextureView;
import com.vr_object.fixed.xnzrw24b.ChannelInfoFragment;
import com.vr_object.fixed.xnzrw24b.CompareSizesByArea;
import com.vr_object.fixed.xnzrw24b.DeviceNotFoundException;
import com.vr_object.fixed.xnzrw24b.DeviceOpenFailedException;
import com.vr_object.fixed.xnzrw24b.LevelCalculator;
import com.vr_object.fixed.xnzrw24b.MessageFields;
import com.vr_object.fixed.xnzrw24b.NetworkInfoFragment;
import com.vr_object.fixed.xnzrw24b.PacketFromDevice;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

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

    private PowerManager.WakeLock wakeLock;

    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }

    };

    private String mCameraId;
    private AutoFitTextureView mTextureView;
    private CameraDevice mCameraDevice;
    private Size mPreviewSize;
    private CameraCaptureSession mCaptureSession;
    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
//            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
//            process(result);
        }
    };

    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private ImageReader mImageReader;
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
//            mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage(), mFile));
        }

    };

    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CaptureRequest mPreviewRequest;
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private int mSensorOrientation;

    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(WiFiAugmentedRealityActivity.this, "Camera error!!!", Toast.LENGTH_LONG).show();
                }
            });
        }
    };

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

        mTextureView = (AutoFitTextureView) findViewById(R.id.camera_texture);

        getFragmentManager().beginTransaction().setTransition(FragmentTransaction.TRANSIT_NONE).
                replace(R.id.networks_list_content_frame, networksFragment).commit();
        getFragmentManager().beginTransaction().setTransition(FragmentTransaction.TRANSIT_NONE).
                replace(R.id.channels_list_content_frame, mChannelsFragment).commit();

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

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     *                          class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @param aspectRatio       The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
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

        startBackgroundThread();
        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }

        mSurfaceView.onResume();
        // Set render mode to RENDERMODE_CONTINUOUSLY to force getting onDraw callbacks until the
        // Tango service is properly set-up and we start getting onFrameAvailable callbacks.
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        // Initialize Tango Service as a normal Android Service, since we call mTango.disconnect()
        // in onPause, this will unbind Tango Service, so every time when onResume gets called, we
        // should create a new Tango object.

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ScreenRecorderService.ACTION_QUERY_STATUS_RESULT);
        registerReceiver(mReceiver, intentFilter);
        queryRecordingStatus();

    }

    @Override
    protected void onPause() {
        super.onPause();
        mSurfaceView.onPause();

        closeCamera();
        stopBackgroundThread();

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

//        if (checkAndRequestPermissions()) {
//            ; //init tango
//        }

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

    /**
     * Sets up member variables related to camera.
     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
    @SuppressWarnings("SuspiciousNameCombination")
    private void setUpCameraOutputs(int width, int height) {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics
                        = manager.getCameraCharacteristics(cameraId);

                // We don't use a front facing camera in this sample.
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }

                // For still image captures, we use the largest available size.
                Size largest = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new CompareSizesByArea());
                mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
                        ImageFormat.JPEG, /*maxImages*/2);
                mImageReader.setOnImageAvailableListener(
                        mOnImageAvailableListener, mBackgroundHandler);

                //TODO: remove all about orientation. Orientation is landscape.
                // Find out if we need to swap dimension to get the preview size relative to sensor
                // coordinate.
                int displayRotation = getWindowManager().getDefaultDisplay().getRotation();
                //noinspection ConstantConditions
                mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                boolean swappedDimensions = false;
                switch (displayRotation) {
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
                        if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                            swappedDimensions = true;
                        }
                        break;
                    case Surface.ROTATION_90:
                    case Surface.ROTATION_270:
                        if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                            swappedDimensions = true;
                        }
                        break;
                    default:
                        Log.e(TAG, "Display rotation is invalid: " + displayRotation);
                }

                Point displaySize = new Point();
                getWindowManager().getDefaultDisplay().getSize(displaySize);
                int rotatedPreviewWidth = width;
                int rotatedPreviewHeight = height;
                int maxPreviewWidth = displaySize.x;
                int maxPreviewHeight = displaySize.y;

                if (swappedDimensions) {
                    rotatedPreviewWidth = height;
                    rotatedPreviewHeight = width;
                    maxPreviewWidth = displaySize.y;
                    maxPreviewHeight = displaySize.x;
                }

//                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
//                    maxPreviewWidth = MAX_PREVIEW_WIDTH;
//                }
//
//                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
//                    maxPreviewHeight = MAX_PREVIEW_HEIGHT;
//                }

                // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
                // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                // garbage capture data.
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                        rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                        maxPreviewHeight, largest);

                // We fit the aspect ratio of TextureView to the size of preview we picked. Orientation is landscape.
                mTextureView.setAspectRatio(
                        mPreviewSize.getWidth(), mPreviewSize.getHeight());

                // Check if the flash is supported.
                Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);

                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            //ErrorDialog.newInstance(getString(R.string.camera_error)).show(getChildFragmentManager(), FRAGMENT_DIALOG);
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("MissingPermission") //Static analyzer is not too smart and does not see permission check.
    private void openCamera(int width, int height) {
        if (!checkAndRequestPermissions()) {
            Toast.makeText(this, getText(R.string.permission_camera), Toast.LENGTH_LONG).show();
            return;
        }

        setUpCameraOutputs(width, height);
        configureTransform(width, height);
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Camera error", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Closes the current {@link CameraDevice}.
     */
    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a new {@link CameraCaptureSession} for camera preview.
     */
    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            // This is the output Surface we need to start preview.
            Surface surface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession;
                            try {
                                // Auto focus should be continuous for camera preview.
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                // Flash is automatically enabled when necessary.

                                // Finally, we start displaying the camera preview.
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                mCaptureSession.setRepeatingRequest(mPreviewRequest,
                                        mCaptureCallback, mBackgroundHandler);
                            } catch (Exception e) {
                                e.printStackTrace();
                                Toast.makeText(WiFiAugmentedRealityActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            Toast.makeText(WiFiAugmentedRealityActivity.this, "Camera failed", Toast.LENGTH_LONG).show();
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void configureTransform(int viewWidth, int viewHeight) {
        if (null == mTextureView || null == mPreviewSize) {
            return;
        }
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        android.graphics.Matrix matrix = new android.graphics.Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, android.graphics.Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mTextureView.setTransform(matrix);
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

    @Override
    public void onFragmentInteraction(@NotNull Uri uri) {

    }
}
