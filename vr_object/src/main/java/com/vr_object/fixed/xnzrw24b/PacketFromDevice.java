package com.vr_object.fixed.xnzrw24b;

import com.vr_object.fixed.xnzrw24b.data.GlobalSettings;
import com.vr_object.fixed.xnzrw24b.data.NamesBLE;

public class PacketFromDevice {

    public int antIdx = 0;
    public int wifiCh = 0;
    public double power = -100.0;
    public String apName = "";
    public String mac = "";
    public long time = 0;
    public String raw;
    public String bleName = "";

    private static final String TAG = PacketFromDevice.class.getSimpleName();

    private final String delimeters = "[ ]+";


    public PacketFromDevice(String str ) throws WFParseException {
        String[] tokens = str.split(delimeters);

        if ( tokens.length < 6 ) {
            throw new WFParseException( "Have " + tokens.length + " tokens in string '" + str + "'" );
        }

        antIdx = Integer.parseInt(tokens[0], 10) - 1;
        wifiCh = Integer.parseInt(tokens[1], 10);
        mac = tokens[2];
        time = Long.parseLong(tokens[3], 16);
        power  = Double.parseDouble(tokens[4]);

        for ( int i = 5; i < tokens.length; i++ ) {
            apName += tokens[i];
            if ( i != tokens.length - 1 ) {
                apName += " ";
            }
        }

        if (GlobalSettings.getMode() == GlobalSettings.WorkMode.BLE) {
            if (NamesBLE.getData().containsKey(mac)) {
                bleName = NamesBLE.getData().get(mac);
            }
        }

        raw = str;
    }

    public String toString() {
        return String.format( "%16s %2d   %1d %5.1f",
                apName, wifiCh, antIdx, power );
    }

}
