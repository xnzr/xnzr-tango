package com.vr_object.fixed;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.View;

import com.vr_object.fixed.xnzrw24b.ChannelInfoFragment;
import com.vr_object.fixed.xnzrw24b.NetworkInfoFragment;
import com.vr_object.fixed.xnzrw24b.data.ChannelInfo;
import com.vr_object.fixed.xnzrw24b.data.NetworkInfo;

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
}
