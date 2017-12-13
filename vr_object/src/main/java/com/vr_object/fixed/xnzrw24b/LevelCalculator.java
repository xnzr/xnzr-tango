package com.vr_object.fixed.xnzrw24b;

import android.util.Log;

import com.vr_object.fixed.xnzrw24b.data.GlobalSettings;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Created by alexe on 12.01.2017.
 */

public final class LevelCalculator {
    private String ssid;
    private String mac;
    private final int AVG_COUNT_WIFI = 15;
    private final int AVG_COUNT_BLE = 5;
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

    public boolean handleInfo(PacketFromDevice data)
    {
        if (data.apName.equals(ssid) && data.mac.equals(mac)) {
            int idx = data.antIdx;
            if (0 <= idx && idx <= 1) {
                int avg_count = AVG_COUNT_WIFI;
                switch (GlobalSettings.getMode()) {
                    case WIFI:
                        avg_count = AVG_COUNT_WIFI;
                        break;
                    case BLE:
                        avg_count = AVG_COUNT_BLE;
                        break;
                }

                mLevels.get(idx).addLast(data.power);
                while (mLevels.get(idx).size() > avg_count) {
                    mLevels.get(idx).removeFirst();
                }
                needRecalc = true;
                print();
            } else {
                Log.d(TAG, "LevelCalculator.HandleInfo() Bad rcvIdx: " + data.antIdx);
            }
        } else {
            //Console.WriteLine("LevelCalculator.HandleInfo() skip ssid " + data.ssid);
            return false;
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
            avgDiff = Math.pow(10.0, (avgLevels[1] - avgLevels[0]) * 0.1 + 2.5);
            needRecalc = false;
        }
        return avgDiff;
    }

    public double GetCurrent() {
        return !mLevels.get(0).isEmpty() ? mLevels.get(0).peekLast() : -100;
    }
}
