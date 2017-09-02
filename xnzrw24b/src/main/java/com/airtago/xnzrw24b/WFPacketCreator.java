package com.airtago.xnzrw24b;

import java.util.ArrayList;

public class WFPacketCreator {
    private static final String TAG = WFPacketCreator.class.getSimpleName();

    private String buffer = "";

    public void putData(byte[] data, int len) {
        buffer += new String(data, 0, len);// "ASCII").substring(0, len);
    }

    private final byte terminator1 = 13;
    private final byte terminator2 = 10;

    public ArrayList<WFPacket> getPackets() {

        ArrayList<WFPacket> packets = new ArrayList<>();

        int termPos1 = 0, termPos2 = 0;

        do {
            termPos1 = buffer.indexOf(terminator1);
            termPos2 = buffer.indexOf(terminator2);

            if (termPos1 > -1 && termPos2 > -1) {
                String packetString = buffer.substring(0, termPos2).trim();
                buffer = buffer.substring(termPos2 + 1);
                try {
                    packets.add(new WFPacket(packetString));
                } catch (WFParseException e) {
                    e.printStackTrace();
                }
            }
        } while (termPos1 > -1 && termPos2 > -1);

        return packets;
    }

}
