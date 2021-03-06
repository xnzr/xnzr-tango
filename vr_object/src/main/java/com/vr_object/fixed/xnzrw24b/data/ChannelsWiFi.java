package com.vr_object.fixed.xnzrw24b.data;

/**
 * Created by Michael Lukin on 11.11.2017.
 */

public class ChannelsWiFi {
    private static final int FIRST_CHANNEL_24 = 1;
    private static final int LAST_CHANNEL_24 = 13;
    private static final float FIRST_CHANNEL_CENTRAL_FREQ_24_GHz = 2.412f;
    private static final float CHANNEL_DELTA_24_GHz = 0.005f;
    private static boolean isChannelValid24(int channel) {
        return (channel >= FIRST_CHANNEL_24) && (channel <= LAST_CHANNEL_24);
    }
    public static float channelFreq24GHz(int channel) throws IllegalArgumentException {
//        if (BuildConfig.DEBUG) {
//            if (!isChannelValid24(channel)) {
//                throw new IllegalArgumentException(String.format(Locale.ENGLISH, "Invalid channel number: %d", channel));
//            }
//        } else {
            if (channel < FIRST_CHANNEL_24) {
                channel = FIRST_CHANNEL_24;
            }
            if (channel > LAST_CHANNEL_24) {
                channel = LAST_CHANNEL_24;
            }
//        }
        return FIRST_CHANNEL_CENTRAL_FREQ_24_GHz + CHANNEL_DELTA_24_GHz * (channel - 1);
    }
}
