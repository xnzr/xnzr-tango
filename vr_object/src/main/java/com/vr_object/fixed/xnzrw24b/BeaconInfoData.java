package com.vr_object.fixed.xnzrw24b;

import com.vr_object.fixed.xnzrw24b.data.NamesBLE;

/**
 * Created by alexe on 10.01.2017.
 */

public final class BeaconInfoData {
    public int rcvIdx;
    public byte wifiChan;
    public String mac;
    public long time;
    public double level;
    public String ssid;
    public String bleName = "";

    @Override
    public String toString() {
        return "ant " + rcvIdx + "  " +
               "chan " + wifiChan + "  " +
               "mac " + mac + "  " +
               "time " + time + "  " +
               "lev " + level + "  " +
               "ssid " + ssid;
    }

    static final int MIN_COUNT = 6;

    public static BeaconInfoData FromString(String str) {
        String[] subStrings = str.split(" ");
        if (subStrings.length < MIN_COUNT) {
            //System.Diagnostics.Debug.WriteLine("           parseString() error:'" + str + "'");
            return null;
        }

        //2 06 9027E45EA88D 12E69160 -079 WiFi Alexander
        BeaconInfoData info = new BeaconInfoData();
        try {
            info.rcvIdx = Integer.parseInt(subStrings[0], 10) - 1;
            info.wifiChan = Byte.parseByte(subStrings[1], 10);
            info.mac = subStrings[2];
            info.time = Long.parseLong(subStrings[3], 16);
            info.level = (double)Integer.parseInt(subStrings[4], 10);
            info.ssid = subStrings[5];
            for (int i = 6; i < subStrings.length; i++)
            {
                info.ssid += " ";
                info.ssid += subStrings[i];
            }

            if (NamesBLE.getData().containsKey(info.mac)) {
                info.bleName = NamesBLE.getData().get(info.mac);
            }
        }
        catch (Exception e) {
            //System.Diagnostics.Debug.WriteLine("Parse Error: " + e.StackTrace);
            return null;
        }
        //info.print();
        return info;
    }
}
