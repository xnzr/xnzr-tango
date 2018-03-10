package com.vr_object.fixed;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraManager;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TabHost;
import android.widget.TextView;

import com.vr_object.firebase.DatabaseProxy;
import com.vr_object.fixed.xnzrw24b.AimView;
import com.vr_object.fixed.xnzrw24b.ChannelInfoFragment;
import com.vr_object.fixed.xnzrw24b.ItsPacketCreator;
import com.vr_object.fixed.xnzrw24b.LevelCalculator;
import com.vr_object.fixed.xnzrw24b.NetworkInfoFragment;
import com.vr_object.fixed.xnzrw24b.UsbSerialPortTi;
import com.vr_object.fixed.xnzrw24b.WFPacketCreator;
import com.vr_object.fixed.xnzrw24b.data.ChannelInfo;
import com.vr_object.fixed.xnzrw24b.data.NetworkInfo;
import com.vr_object.screencast.ScreenRecorderService;

import java.lang.ref.WeakReference;

/**
 * Created by Michael Lukin on 16.12.2017.
 */

public class BaseFinderActivity extends Activity
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


    @Override
    public void onListFragmentInteraction(NetworkInfo item) {

    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return false;
    }

    @Override
    public void onListFragmentInteraction(ChannelInfo item) {

    }

    protected void updateRecording(final boolean isRecording, final boolean isPausing) {
        if (DEBUG) Log.v(TAG, "updateRecording:isRecording=" + isRecording + ",isPausing=" + isPausing);
        //TODO: update buttons
    }

    protected static final class MyBroadcastReceiver extends BroadcastReceiver {
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
