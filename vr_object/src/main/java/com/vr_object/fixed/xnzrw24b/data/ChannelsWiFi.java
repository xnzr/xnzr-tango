package com.vr_object.fixed.xnzrw24b.data;

import com.vr_object.fixed.BuildConfig;

/**
 * Created by Michael Lukin on 11.11.2017.
 */

public class ChannelsWiFi {
    public static final int FIRST_CHANNEL_24 = 1;
    public static final int LAST_CHANNEL_24 = 13;
    public static final float FIRST_CHANNEL_CENTRAL_FREQ_24_GHz = 2.412f;
    public static final float CHANNEL_DELTA_24_GHz = 0.005f;
    public static boolean isChannelValid24(int channel) {
        return (channel >= FIRST_CHANNEL_24) && (channel <= LAST_CHANNEL_24);
    }
    public static float channelFreq24GHz(int channel) throws IllegalArgumentException {
        if (BuildConfig.DEBUG) {
            if (!isChannelValid24(channel)) {
                throw new IllegalArgumentException("Invalid channel number");
            }
        } else {
            if (channel < FIRST_CHANNEL_24) {
                channel = FIRST_CHANNEL_24;
            }
            if (channel > LAST_CHANNEL_24) {
                channel = LAST_CHANNEL_24;
            }
        }
        return FIRST_CHANNEL_CENTRAL_FREQ_24_GHz + CHANNEL_DELTA_24_GHz * (channel - 1);
    }
}
