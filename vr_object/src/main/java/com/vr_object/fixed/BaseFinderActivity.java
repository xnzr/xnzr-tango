package com.vr_object.fixed;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.media.projection.MediaProjectionManager;
import android.opengl.GLSurfaceView;
import android.os.Message;
import android.os.PowerManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
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
import com.vr_object.fixed.xnzrw24b.UsbSerialPortTi;
import com.vr_object.fixed.xnzrw24b.WFPacketCreator;
import com.vr_object.fixed.xnzrw24b.data.ChannelInfo;
import com.vr_object.fixed.xnzrw24b.data.ChannelsWiFi;
import com.vr_object.fixed.xnzrw24b.data.GlobalSettings;
import com.vr_object.fixed.xnzrw24b.data.NetworkInfo;
import com.vr_object.screencast.ScreenRecorderService;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;

import static com.vr_object.fixed.R.id.b_clear_sagittae;
import static com.vr_object.fixed.R.id.b_hide_clear_sagittae;
import static com.vr_object.fixed.R.id.b_on_off_circle;
import static com.vr_object.fixed.R.id.b_on_off_sound;
import static com.vr_object.fixed.R.id.b_wifi_ble_switch;
import static com.vr_object.fixed.R.id.options_scroll_view;
import static com.vr_object.fixed.R.id.sagittae_length_setter;
import static com.vr_object.fixed.R.id.threshold_setter;

/**
 * Created by Michael Lukin on 16.12.2017.
 */

public abstract class BaseFinderActivity extends Activity
        implements View.OnClickListener,
        View.OnTouchListener,
        NetworkInfoFragment.OnListFragmentInteractionListener,
        ChannelInfoFragment.OnListFragmentInteractionListener{

    protected static final int MAX_PROGRESS_RSSI = -40;
    protected static final int MIN_PROGRESS_RSSI = -100;
    protected final int REQUEST_CODE_SCREEN_CAPTURE = 1;
    protected static final int EXTERNAL_STORAGE_REQUEST_CODE = 2;
    protected static final int RECORD_AUDIO_REQUEST_CODE = 2;
    protected static final boolean DEBUG = false;

    protected static final String TAG = "WiFiARActivity";

    protected static final int INVALID_TEXTURE_ID = 0;

    protected TextView mTextView;

    protected MyBroadcastReceiver mReceiver;
    protected ProgressBar mProgressBar;
    protected GLSurfaceView mSurfaceView;

    protected WiFiAugmentedRealityRenderer mRenderer;

    protected TabHost optionsTabbedWindow;

    ///usb
    protected NetworkInfo mSelectedNetwork;
    protected int mSelectedChannel = 0;
    protected LevelCalculator mLevelCalculator;

    protected android.os.Handler handler;

    protected NetworkInfoFragment networksFragment;
    protected ChannelInfoFragment mChannelsFragment;

    protected SeekBar mThresholdSetter;
    protected TextView mThresholdView;
    protected int mThreshold;

    protected SeekBar mSagittaeLenghtSetter;
    protected TextView mSagittaeLengthView;

    protected AimView mAimView;
    protected TextView mScanningMessage;
    protected FrameLayout mCircleFrameLayout;

    protected CameraManager cameraManager;
    protected Thread thread;
    protected UsbSerialPortTi port = null;

    protected final int INIT_INTERVAL_MS = 3000;
    protected final int READ_TIMEOUT_MS = 500;
    protected final int READ_BUF_SIZE = ItsPacketCreator.PACK_LEN * 16;

    protected final int STATE_INIT = 0;
    protected final int STATE_READING = 1;
    protected final int STATE_EXIT = 255;
    protected int state = STATE_INIT;

    protected int curChan = 1;
    protected int setChan = 1;

    protected DatabaseProxy firebaseProxy = new DatabaseProxy();

    protected WFPacketCreator wfCreator = new WFPacketCreator();
    protected OptionsHolder optionsHolder = new OptionsHolder(this);

    protected PowerManager.WakeLock wakeLock;


    protected void setupTabbedOptions() {
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

    protected void updateLevelProgress(final double level) {
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
    protected double progressSigmoid(double x) {
        return 100-100/(1+Math.exp(-(x-4500)/1000));
    }

    protected abstract void updateLevelDiff(double levelDiff);

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
    public void onDestroy() {
        super.onDestroy();
        wakeLock.release();
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return false;
    }


    protected void updateRecording(final boolean isRecording, final boolean isPausing) {
        if (DEBUG) Log.v(TAG, "updateRecording:isRecording=" + isRecording + ",isPausing=" + isPausing);
        //TODO: update buttons
    }

    // permissions
    private static final String CAMERA_PERMISSION = android.Manifest.permission.CAMERA;
    private static final int CAMERA_PERMISSION_CODE = 0;

    protected boolean hasCameraPermission() {
        int result = ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION);

        return result == PackageManager.PERMISSION_GRANTED;
    }

    protected void requestCameraPermission() {
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
                        ActivityCompat.requestPermissions(BaseFinderActivity.this,
                                new String[]{CAMERA_PERMISSION}, CAMERA_PERMISSION_CODE);
                    }
                })
                .create();
        dialog.show();
    }

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

    protected void closeAndSaveOptions() {
        closeOptions();
        saveOptions();
    }
    public void closeOptions(View view) {
        closeOptions();
    }

    protected void closeOptions() {
        View optionsScreen = findViewById(options_scroll_view);
        optionsScreen.setVisibility(View.GONE);
        findViewById(R.id.options_tab_container).setVisibility(View.GONE);
    }

    public void clearPelengs(View view) {
        mRenderer.clearPelengs();
    }

    protected void saveOptions() {
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

    protected void setClearSagittaeButtonVisibility(boolean cssChecked) {
        View clearSagittaeButton = findViewById(b_clear_sagittae);
        if (cssChecked) {
            clearSagittaeButton.setVisibility(View.VISIBLE);
        } else {
            clearSagittaeButton.setVisibility(View.GONE);
        }
    }

    protected void setCircleVisibility(boolean value) {
        if (value) {
            mAimView.setVisibility(View.VISIBLE);
        } else {
            mAimView.setVisibility(View.GONE);
        }
    }

    protected void setSoundState(boolean state) {
        mAimView.setSound(state);
    }

    /**
     * Set Wi-Fi / BLE workmode. Implemented using control Switch. Note: it produces boolean values.
     * @param mode true: Wi-Fi false: BLE
     */
    protected void setWorkMode(boolean mode) {
        if (mode) {
            GlobalSettings.setMode(GlobalSettings.WorkMode.WIFI);
        } else {
            GlobalSettings.setMode(GlobalSettings.WorkMode.BLE);
        }
        showCreateBleButton(!mode);
    }

    protected void loadOptions() {
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

    ///usb
    protected void usbLoop() {
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

    protected Thread changer = null;

    protected void stepInit() {
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

    protected Thread createChangerThread() {
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

    protected void stepRead() {
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

    protected void changeChannel() {
        changeChannel(0);
    }

    protected void changeChannel(int chan) {
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

    protected BroadcastReceiver usbReceiver = new BroadcastReceiver() {
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

    protected void changeState(int newState) {
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

    protected void closeDevice() {
        if (port != null) {
            port.close();
        }
    }

    protected void sendInfo(String infoString) {
        Message message = handler.obtainMessage(MessageFields.CODE_INFO);
        //message.setAction(ServiceConstants.ACTION_NAME);
        message.getData().putInt(MessageFields.FIELD_CODE_INT, MessageFields.CODE_INFO);
        message.getData().putString(MessageFields.FIELD_INFO_STR, infoString);
        //sendBroadcast(intent);
        handler.sendMessage(message);
    }

    protected void sendError(String errorString) {
        Message message = handler.obtainMessage(MessageFields.CODE_ERROR);
        //intent.setAction(ServiceConstants.ACTION_NAME);
        message.getData().putInt(MessageFields.FIELD_CODE_INT, MessageFields.CODE_ERROR);
        message.getData().putString(MessageFields.FIELD_INFO_STR, errorString);
        //sendBroadcast(intent);
        handler.sendMessage(message);
    }

    protected void sendData(PacketFromDevice packet) {
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

    protected byte getChannelChar(int channel) {
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
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    EXTERNAL_STORAGE_REQUEST_CODE);
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {android.Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO_REQUEST_CODE);
        }


        findViewById(R.id.b_start_recording).setVisibility(View.GONE);
        findViewById(R.id.b_stop_recording).setVisibility(View.VISIBLE);
        final MediaProjectionManager manager
                = (MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        final Intent permissionIntent = manager.createScreenCaptureIntent();
        startActivityForResult(permissionIntent, REQUEST_CODE_SCREEN_CAPTURE);
    }

    public void stopRecording(View view) {
//        if (mScreenRecorder != null) {
//            mScreenRecorder.stopRecording();
//        }
        findViewById(R.id.b_start_recording).setVisibility(View.VISIBLE);
        findViewById(R.id.b_stop_recording).setVisibility(View.GONE);

        final Intent intent = new Intent(this, ScreenRecorderService.class);
        intent.setAction(ScreenRecorderService.ACTION_STOP);
        startService(intent);
    }

    protected void queryRecordingStatus() {
        if (DEBUG) Log.v(TAG, "queryRecording:");
        final Intent intent = new Intent(this, ScreenRecorderService.class);
        intent.setAction(ScreenRecorderService.ACTION_QUERY_STATUS);
        startService(intent);
    }

    protected void startScreenRecorder(final int resultCode, final Intent data) {
        final Intent intent = new Intent(this, ScreenRecorderService.class);
        intent.setAction(ScreenRecorderService.ACTION_START);
        intent.putExtra(ScreenRecorderService.EXTRA_RESULT_CODE, resultCode);
        intent.putExtras(data);
        startService(intent);
    }

    protected static final class MyBroadcastReceiver extends BroadcastReceiver {
        private final WeakReference<WiFiAugmentedRealityActivity> mWeakParent;
        public MyBroadcastReceiver(final WiFiAugmentedRealityActivity parent) {
            mWeakParent = new WeakReference<>(parent);
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
