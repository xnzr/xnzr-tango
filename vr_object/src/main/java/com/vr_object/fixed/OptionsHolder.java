package com.vr_object.fixed;

import android.content.Context;
import android.content.SharedPreferences;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by 811243 on 22.10.2017.
 */

public class OptionsHolder {
    private final String THRESHOLD_KEY = "threshold";
    private final String SAGITTAE_LENGTH_KEY = "sagittae_length";
    private final String SHOW_REMOVE_SAGITTAE_BUTTON = "show_remove_sagittae_button";

    private final String OPTIONS_NAME = "WiFiAugmentedRealityActivity";

    private final int DEFAULT_THRESHOLD = 3000;
    private final int DEFAULT_SAGGITAE_LENGTH = 5;

    private SharedPreferences mPref;

    Context mActivity;
    OptionsHolder(Context activity) {
        mActivity = activity;
    }

    private void loadPreferences() {
        if (mPref == null) {
            mPref = mActivity.getSharedPreferences(OPTIONS_NAME, MODE_PRIVATE);
        }
    }

    int loadThreshold() {
        loadPreferences();
        return mPref.getInt(THRESHOLD_KEY, DEFAULT_THRESHOLD);
    }

    void saveThreshold(int value) {
        loadPreferences();

        SharedPreferences.Editor e = mPref.edit();
        e.putInt(THRESHOLD_KEY, value);
        e.apply();
    }

    int loadSagittaLength() {
        loadPreferences();
        return mPref.getInt(SAGITTAE_LENGTH_KEY, DEFAULT_SAGGITAE_LENGTH);
    }

    void saveSagittaLength(int value) {
        loadPreferences();

        SharedPreferences.Editor e = mPref.edit();
        e.putInt(SAGITTAE_LENGTH_KEY, value);
        e.apply();
    }

    boolean loadShowClearSagittaeButton() {
        loadPreferences();

        return mPref.getBoolean(SHOW_REMOVE_SAGITTAE_BUTTON, true);
    }

    void saveShowClearSagittaeButton(boolean value) {
        loadPreferences();

        SharedPreferences.Editor e = mPref.edit();
        e.putBoolean(SHOW_REMOVE_SAGITTAE_BUTTON, value);
        e.apply();
    }
}
