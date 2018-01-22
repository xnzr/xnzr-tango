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
    private final String SHOW_REMOVE_SAGITTAE_BUTTON_KEY = "show_remove_sagittae_button";
    private final String CIRCLE_VISIBILITY_KEY = "circle_visibility";
    private final String SOUND_ON_OFF_KEY = "sound_on_off";
    private final String SWITCH_WIFI_BLE_KEY = "switch_wifi_ble";
    public static final String SAVE_SELECTED_ID_KEY = "save_selected_id";
    public static final String SAVED_ID_KEY = "saved_id";

    public static final String OPTIONS_NAME = "WiFiAugmentedRealityActivity";

    private final int DEFAULT_THRESHOLD = 3000;
    private final int DEFAULT_SAGGITAE_LENGTH = 5;
    private final boolean DEFAULT_SAGITTAE_BUTTON_VISIBILITY = true;
    private final boolean DEFAULT_CIRCLE_VISIBILITY = true;
    private final boolean DEFAULT_SOUND_STATE = true;
    private final boolean DEFAULT_WIFI_BLE_STATE = true; //true is wi-fi, false is ble; this is a feature of switch control
    public static final boolean DEFAULT_SAVE_SELECTED_ID = true;
    public static final String DEFAULT_SAVED_ID = "";

    private SharedPreferences mPref;

    private Context mActivity;
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

        return mPref.getBoolean(SHOW_REMOVE_SAGITTAE_BUTTON_KEY, DEFAULT_SAGITTAE_BUTTON_VISIBILITY);
    }

    void saveShowClearSagittaeButton(boolean value) {
        loadPreferences();

        SharedPreferences.Editor e = mPref.edit();
        e.putBoolean(SHOW_REMOVE_SAGITTAE_BUTTON_KEY, value);
        e.apply();
    }

    boolean loadCircleVisibility() {
        loadPreferences();

        return mPref.getBoolean(CIRCLE_VISIBILITY_KEY, DEFAULT_CIRCLE_VISIBILITY);
    }

    void saveCircleVisibility(boolean value) {
        loadPreferences();

        SharedPreferences.Editor e = mPref.edit();
        e.putBoolean(CIRCLE_VISIBILITY_KEY, value);
        e.apply();
    }

    boolean loadSoundState() {
        loadPreferences();

        return mPref.getBoolean(SOUND_ON_OFF_KEY, DEFAULT_SOUND_STATE);
    }

    void saveSoundState(boolean value) {
        loadPreferences();

        SharedPreferences.Editor e = mPref.edit();
        e.putBoolean(SOUND_ON_OFF_KEY, value);
        e.apply();
    }

    boolean loadWifiBleState() {
        loadPreferences();

        return mPref.getBoolean(SWITCH_WIFI_BLE_KEY, DEFAULT_WIFI_BLE_STATE);
    }

    void saveWifiBleState(boolean value) {
        loadPreferences();

        SharedPreferences.Editor e = mPref.edit();
        e.putBoolean(SWITCH_WIFI_BLE_KEY, value);
        e.apply();
    }
}
