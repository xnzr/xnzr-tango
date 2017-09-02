package com.airtago.xnzrw24b;

import android.util.Log;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.UUID;


public class ItsPacketCreator {
    private static final String TAG = ItsPacketCreator.class.getSimpleName();

    public static final int PACK_LEN = 30;

    static public ItsPacket createFromBytes(byte[] data) throws ItsPacketParseException, BadRssiException {
        // uint8 preambula;  0
        // uint8 size;       1
        // uint8 opcode;     2
        // uint8 uuid[16];   3
        // uint16 minor;     19
        // uint16 major;     21
        // int8 ch0rssi_37;  23
        // int8 ch0rssi_38;  24
        // int8 ch0rssi_39;  25
        // int8 ch1rssi_37;  26
        // int8 ch1rssi_38;  27
        // int8 ch1rssi_39;  28
        // uint8 crc;        29

        if ( data.length < PACK_LEN ) {
            Log.e(TAG, "small data.len = " + data.length);
            throw new ItsPacketParseException();
        }
        ItsPacket packet = new ItsPacket();

        packet.preambula = data[0];
        if (packet.preambula != 0x55) {
            Log.e(TAG, String.format( "Preambula error: 0x%02x", packet.preambula ));
            throw new ItsPacketParseException();
        }

        packet.size = data[1];
        if (packet.size != PACK_LEN) {
            Log.e(TAG, String.format( "Size error: 0x%02x", packet.size ));
            throw new ItsPacketParseException();
        }

        packet.opcode = data[2];
        if (packet.opcode != ItsPacket.OPCODE_RCV) {
            if (packet.opcode == ItsPacket.OPCODE_ERROR) {
                packet.error_code = data[3];
                Log.w(TAG, String.format("dev error: 0x%02x", packet.error_code));
                return packet;
            } else {
                Log.e(TAG, String.format("Opcode error: 0x%02x", packet.opcode));
                throw new ItsPacketParseException();
            }
        }

        packet.crc = data[29];

        packet.timeStamp = new Timestamp(System.currentTimeMillis());


        packet.uuid = new UUID(
            new BigInteger( Arrays.copyOfRange(data,
                                UUID_START_POS,
                                UUID_START_POS + UUID_HALF_LEN)).longValue(),
            new BigInteger(Arrays.copyOfRange(data,
                                UUID_START_POS + UUID_HALF_LEN,
                                UUID_START_POS + UUID_HALF_LEN + UUID_HALF_LEN)).longValue()
        );

        packet.major = (int)data[19]*256 + ((int)data[20]);
        packet.minor = (int)data[21]*256 + ((int)data[22]);

        packet.ch0rssi_37 = (double)data[23];
        packet.ch0rssi_38 = (double)data[24];
        packet.ch0rssi_39 = (double)data[25];

        packet.ch1rssi_37 = (double)data[26];
        packet.ch1rssi_38 = (double)data[27];
        packet.ch1rssi_39 = (double)data[28];

        packet.diff = CalcDiff(packet);

        Log.w(TAG, packet.toStringLong());

        return packet;
    }

    static private double CalcDiff(ItsPacket p) throws BadRssiException {
        return CalcDiff_ThrowGreat(p);
        //return CalcDiff_37_Only(p);
    }

    static private double CalcDiff_37_Only(ItsPacket p) throws BadRssiException {
        if ( p.ch0rssi_37 == 0 || p.ch1rssi_37 == 0 ) {
            throw new BadRssiException();
        }
        return makeDiff2chan(p.ch1rssi_37, p.ch0rssi_37);
    }

    static private double CalcDiff_All_Avg(ItsPacket p) throws BadRssiException {
        double d = 0.0;
        int cnt = 0;
        if ( p.ch0rssi_37 != 0 && p.ch1rssi_37 != 0 ) {
            d += p.ch1rssi_37 - p.ch0rssi_37;
            cnt++;
        }
        if ( p.ch0rssi_38 != 0 && p.ch1rssi_38 != 0 ) {
            d += p.ch1rssi_38 - p.ch0rssi_38;
            cnt++;
        }
        if ( p.ch0rssi_39 != 0 && p.ch1rssi_39 != 0 ) {
            d += p.ch1rssi_39 - p.ch0rssi_39;
            cnt++;
        }
        if ( cnt == 0 ) {
            throw new BadRssiException();
        }
        return makeDiff(d / cnt);
    }

    static private double CalcDiff_ThrowGreat(ItsPacket p) throws BadRssiException {
        double d7, d8, d9;
        if ( p.ch0rssi_37 == 0 || p.ch1rssi_37 == 0 ||
             p.ch0rssi_38 == 0 || p.ch1rssi_38 == 0 ||
             p.ch0rssi_39 == 0 || p.ch1rssi_39 == 0 )
        {
            return CalcDiff_All_Avg(p);
        }

        d7 = p.ch1rssi_37 - p.ch0rssi_37;
        d8 = p.ch1rssi_38 - p.ch0rssi_38;
        d9 = p.ch1rssi_39 - p.ch0rssi_39;

        double d78 = Math.abs(d7-d8);
        double d79 = Math.abs(d7-d9);
        double d89 = Math.abs(d8-d9);

        double dmin = Math.min(d78, Math.min(d79, d89));

        double diff = 0.0;
        if ( dmin == d78 ) {
            diff = (d7+d8)/2;
            //Log.d(TAG,"no 39");
        } else
        if ( dmin == d79 ) {
            diff = (d7+d9)/2;
            //Log.d(TAG,"no 38");
        } else
        if ( dmin == d89 ) {
            diff = (d8+d9)/2;
            //Log.d(TAG,"no 37");
        }
        return makeDiff( diff );
    }

    static private double makeDiff2chan( double a, double b ) {
        return makeDiff(a - b);
    }

    static private double makeDiff( double x ) {
        return Math.pow(10.0, (x * 0.1 + 4) );
    }


    static final int UUID_START_POS = 3;
    static final int UUID_HALF_LEN  = 8;
}
