package com.airtago.xnzrw24b.data;

import com.airtago.xnzrw24b.BeaconInfoData;
import com.airtago.xnzrw24b.WFPacket;

import java.util.ArrayList;

/**
 * Created by alexe on 10.01.2017.
 */

public final class NetworkInfo {
    public NetworkInfo(BeaconInfoData info) {
        Ssid = info.ssid;
        Mac = info.mac;
    }

    public NetworkInfo(WFPacket info) {
        Ssid = info.apName;
        Mac = info.mac;
    }

    public String Ssid;
    public String Mac;

    public ArrayList<ChannelInfo> Channels = new ArrayList<ChannelInfo>();

    public boolean addChannel(WFPacket packet) {
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
