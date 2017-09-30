package com.vr_object.fixed.xnzrw24b;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.atap.tangoservice.TangoPoseData;
import com.vr_object.fixed.OpenGlSphere;
import com.vr_object.fixed.R;

/**
 * Created by Michael Lukin on 04.09.2017.
 */

public class SpatialIntersect {
    private OpenGlSphere [][][] gizmo;

    private int [][][] cubes;

    private double unit;
    private int horizSize;
    private int vertSize;
    private final int gizmoSize = 4;
    private final float gizmoRadius = 0.05f;
    private Context mContext;
    private boolean hasMaximum = false;

    private TangoPoseData initPose;

    public SpatialIntersect() {
        unit = 0.2;
        horizSize = 100;
        vertSize = 20;
        cubes = new int[horizSize][horizSize][vertSize];

        gizmo = new OpenGlSphere[gizmoSize][gizmoSize][gizmoSize];

    }

    public void addDot(double [] coord) {
        int [] u = coord2Units(coord, new int[]{horizSize, horizSize, vertSize});
        cubes[u[0]][u[1]][u[2]]++;
    }

    public double [] getMaximum() {
        int max = 3;
        int [] maxCube = new int[3];
        hasMaximum = false;

        for (int i = 0; i < horizSize; i++) {
            for (int j = 0; j < horizSize; j++) {
                for (int k = 0; k < vertSize; k++) {
                    if (cubes[i][j][k] > max) {
                        hasMaximum = true;
                        maxCube[0] = i;
                        maxCube[1] = j;
                        maxCube[2] = k;
                        max = cubes[i][j][k];
                    }
                }
            }
        }

        double [] res = units2Coord(maxCube, new int[]{horizSize, horizSize, vertSize});
        return res;
    }

    public void initGizmo(Context context) {
        mContext = context;
        final BitmapFactory.Options options = new BitmapFactory.Options();
        Bitmap bmp = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.red, options);
        for (int i = 0; i < gizmoSize; i++) {
            for (int j = 0; j < gizmoSize; j++) {
                for (int k = 0; k < gizmoSize; k++) {
                    gizmo[i][j][k] = new OpenGlSphere(gizmoRadius, 5, 5);
                    gizmo[i][j][k].setUpProgramsAndBuffers(bmp);
                }
            }
        }
    }

    public void placeGizmo() {
        for (int i = 0; i < gizmoSize; i++) {
            for (int j = 0; j < gizmoSize; j++) {
                for (int k = 0; k < gizmoSize; k++) {
                    double [] c = units2Coord(new int [] {i, j, k}, new int [] {gizmoSize, gizmoSize, gizmoSize});
//                    gizmo[i][j][k].setModelMatrix();
                }
            }
        }
    }

    public int [] coord2Units(double [] coords, int [] dimensionSize) {
        int [] res = new int[3];
        for (int i = 0; i < 3; i++) {
            res[i] = (int) ((coords[i] - initPose.translation[i]) / unit) + dimensionSize[i] / 2;
            if (res[i] < 0) {
                res[i] = 0;
            }
            if (res[i] > dimensionSize[i] - 1) {
                res[i] = dimensionSize[i] - 1;
            }
        }
        return res;
    }

    public double [] units2Coord(int [] units, int [] dimensionSize) {
        double [] res = new double[3];
        for (int i = 0; i < 3; i++) {
            res[i] = (units[i] - dimensionSize[i]/2) * unit + initPose.translation[i];
        }
        return res;
    }

    public double getUnit() {
        return unit;
    }

    public void setUnit(double unit) {
        this.unit = unit;
    }

    public int getHorizSize() {
        return horizSize;
    }

    public int getVertSize() {
        return vertSize;
    }

    public TangoPoseData getInitPose() {
        return initPose;
    }

    public void setInitPose(TangoPoseData initPose) {
        this.initPose = initPose;
    }

    public void setmContext(Context mContext) {
        this.mContext = mContext;
    }

    public boolean isHasMaximum() {
        return hasMaximum;
    }
}
