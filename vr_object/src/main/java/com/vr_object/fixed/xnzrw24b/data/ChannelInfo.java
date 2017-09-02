package com.vr_object.fixed.xnzrw24b.data;

import java.util.ArrayList;

/**
 * Created by alexe on 10.01.2017.
 */

public final class ChannelInfo {

    public ChannelInfo(int channel) {
        Channel = channel;
    }

    public int Channel;
    public int Rssi;

    private final int MAX_HISTORY = 20;

    private ArrayList<Double> mHistory1 = new ArrayList<>();
    private ArrayList<Double> mHistory2 = new ArrayList<>();
    private double mRssi;
    private double mRssiDiff;

    public void AddRssi1(double rssi) {
        mHistory1.add(rssi);
        if (mHistory1.size() > MAX_HISTORY)
            mHistory1.remove(0);
        //OnPropertyChanged(nameof(AvgRssi1));
    }

    public void AddRssi2(double rssi) {
        mHistory2.add(rssi);
        if (mHistory2.size() > MAX_HISTORY)
            mHistory2.remove(0);
        //OnPropertyChanged(nameof(AvgRssi2));
    }

    public double AvgRssi1() {
        double r = 0;
        for (double d: mHistory1) {
            r += d;
        }
        return mHistory1.size() > 0 ? r / mHistory1.size() : -255d;
    }

    public double AvgRssi2() {
        double r = 0;
        for (double d: mHistory2) {
            r += d;
        }
        return mHistory2.size() > 0 ? r / mHistory2.size() : -0d;
    }
}
