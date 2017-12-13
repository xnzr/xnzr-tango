package com.vr_object.fixed.xnzrw24b.data;

import com.vr_object.fixed.xnzrw24b.BeaconInfoData;
import com.vr_object.fixed.xnzrw24b.PacketFromDevice;

import java.util.ArrayList;

/**
 * Created by alexe on 10.01.2017.
 */

public final class NetworkInfo {
    public NetworkInfo(BeaconInfoData info) {
        Ssid = info.ssid;
        Mac = info.mac;
        BleName = info.bleName;
    }

    public NetworkInfo(PacketFromDevice info) {
        Ssid = info.apName;
        Mac = info.mac;
        BleName = info.bleName;
    }

    public String Ssid;
    public String Mac;
    public String BleName = "";

    public ArrayList<ChannelInfo> Channels = new ArrayList<ChannelInfo>();

    public boolean addChannel(PacketFromDevice packet) {
        boolean result = false;
        ChannelInfo chan = null;

        for (ChannelInfo ch: Channels) {
            if (ch.Channel == packet.wifiCh) {
                chan = ch;
                break;
            }
        }
        if (chan == null) {
            chan = new ChannelInfo(packet.wifiCh);
            Channels.add(chan);
            result = true;
        }
        if (packet.antIdx == 0)
            chan.AddRssi1(packet.power);
        else if (packet.antIdx == 1)
            chan.AddRssi2(packet.power);
        return result;
    }
}
