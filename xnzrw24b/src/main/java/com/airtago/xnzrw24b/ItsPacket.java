package com.airtago.xnzrw24b;

import java.sql.Timestamp;
import java.util.UUID;



public class ItsPacket {

    public String toString() {
        return String.format( "%16s %3.1f %d %d %s",
                uuid.toString(), diff, major, minor, timeStamp.toString() );
    }

    public String toStringLong() {
        return String.format( "[%2d] %6.1f %d %d   #37: %3.0f %3.0f (%3.0f)   #38: %3.0f %3.0f (%3.0f)   #39: %3.0f %3.0f (%3.0f)",
                uuid.getLeastSignificantBits(), diff, major, minor,
                ch0rssi_37, ch1rssi_37, ch0rssi_37 - ch1rssi_37,
                ch0rssi_38, ch1rssi_38, ch0rssi_38 - ch1rssi_38,
                ch0rssi_39, ch1rssi_39, ch0rssi_39 - ch1rssi_39
                );
    }

    public boolean isOk() {
        return opcode == OPCODE_RCV;
    }

    public int size;
    public int preambula;
    public int opcode;
    public int crc;
    public int error_code;

    public UUID uuid;
    public int major = 0;
    public int minor = 0;
    public double diff = 0.0;
    public Timestamp timeStamp;

    public double ch0rssi_37 = 0.0;
    public double ch0rssi_38 = 0.0;
    public double ch0rssi_39 = 0.0;

    public double ch1rssi_37 = 0.0;
    public double ch1rssi_38 = 0.0;
    public double ch1rssi_39 = 0.0;

    static public final int OPCODE_RCV    = 0;
    static public final int OPCODE_ON_OFF = 1;
    static public final int OPCODE_UUID   = 2;
    static public final int OPCODE_SCAN   = 3;
    static public final int OPCODE_ERROR  = 4;

    static public final int DEV_ERR_CRC  = 1;
    static public final int DEV_ERR_UUID = 2;
    static public final int DEV_ERR_MAJ_MIN_MISMATCH = 3;
    static public final int DEV_ERR_READ_ERR = 4;

}
