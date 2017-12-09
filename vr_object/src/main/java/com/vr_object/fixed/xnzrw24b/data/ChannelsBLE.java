package com.vr_object.fixed.xnzrw24b.data;

/**
 * Created by Michael Lukin on 05.12.2017.
 */

public class ChannelsBLE {
    public static final int FIRST_CHANNEL_ADV = 37;
//    public static final int LAST_CHANNEL_ADV = 39;
    public static final int LAST_CHANNEL_ADV = 37;
    public static boolean isChannelValid24(int channel) { return true; } //we do not want to hop between channels

    public static float channelFreq24GHz(int channel) {
        return 2.402f;
    }

}
