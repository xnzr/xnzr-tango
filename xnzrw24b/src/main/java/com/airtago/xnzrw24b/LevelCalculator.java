package com.airtago.xnzrw24b;

import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Created by alexe on 12.01.2017.
 */

public final class LevelCalculator {
    private String ssid;
    private String mac;
    private int avgCount = 15;
    private double[] avgLevels = new double[2];
    private double avgDiff = 0.0;
    private boolean needRecalc = true;
    private ArrayList<LinkedList<Double>> mLevels = new ArrayList<>();

    private final String TAG = LevelCalculator.class.getSimpleName();

    public LevelCalculator(String ssid, String mac) {
        this.ssid = ssid;
        this.mac = mac;
        mLevels.add(new LinkedList<Double>());
        mLevels.add(new LinkedList<Double>());
    }

    public boolean handleInfo(WFPacket data)
    {
        if (data.apName.equals(ssid) && data.mac.equals(mac)) {
            int idx = data.antIdx;
            if (0 <= idx && idx <= 1) {
                mLevels.get(idx).addLast(data.power);
                while (mLevels.get(idx).size() > avgCount) {
                    mLevels.get(idx).removeFirst();
                }
                needRecalc = true;
                print();
            } else {
                Log.d(TAG, "LevelCalculator.HandleInfo() Bad rcvIdx: " + data.antIdx);
            }
        } else {
            //Console.WriteLine("LevelCalculator.HandleInfo() skip ssid " + data.ssid);
        }
        return needRecalc;
    }

    public void print() {
        double diff = getAvg();
        Log.d(TAG, String.format("\r                                                          \rLEVEL: %5.2f       rssi: %6.2f  %6.2f", diff, avgLevels[0], avgLevels[1]));
    }

    public double getAvg() {
        if (needRecalc) {
            for (int idx = 0; idx < 2; idx++) {
                double sum = 0d;
                for (Double x : mLevels.get(idx)) {
                    sum += x;
                }
                int count = mLevels.get(idx).size();
                if (count != 0) {
                    sum /= count;
                }
                avgLevels[idx] = sum;
            }
            //copy-pasted from beacon radar
            //tfDiff = Math.Pow(10.0, ((double)(ch1rssi - ch0rssi) * 0.1 + 4) * Services.SettingsServices.SettingsService.Instance.PowerAmplifier);
            //
            //avgDiff = Math.Abs(avgLevels[0] - avgLevels[1]);
            //avgDiff = 100 - 4*(avgLevels[0] - avgLevels[1]);
            avgDiff = Math.pow(10.0, ((double)(avgLevels[1] - avgLevels[0]) * 0.1 + 2.5) * 1);
            //avgDiff = 2000 / 100 * avgDiff;
            needRecalc = false;
        }
        return avgDiff;
    }

    public double GetCurrent() {
        return !mLevels.get(0).isEmpty() ? mLevels.get(0).peekLast() : -100;
    }
}
