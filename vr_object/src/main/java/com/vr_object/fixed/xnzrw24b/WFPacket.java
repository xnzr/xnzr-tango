package com.vr_object.fixed.xnzrw24b;

public class WFPacket {

    public int antIdx = 0;
    public int wifiCh = 0;
    public double power = -100.0;
    public String apName = "";
    public String mac = "";
    public long time = 0;
    public String raw;

    private static final String TAG = WFPacket.class.getSimpleName();

    private final String delimeters = "[ ]+";


    public WFPacket( String str ) throws WFParseException {
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

        raw = str;
    }

    public String toString() {
        return String.format( "%16s %2d   %1d %5.1f",
                apName, wifiCh, antIdx, power );
    }

}
