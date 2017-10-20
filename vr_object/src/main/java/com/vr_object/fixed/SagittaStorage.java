package com.vr_object.fixed;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by 811243 on 30.09.2017.
 *
 * Sagitta (lat) - arrow
 */

public class SagittaStorage implements OpenGlObject {
    OpenGlObject mSagittaObject;
    private CopyOnWriteArrayList<float[]> mModelMatrixList = new CopyOnWriteArrayList<>();

    private float[] mProjectionMatrix;

    public void setObject(OpenGlObject o) {
        mSagittaObject = o;
    }

    public void clearSagittae() {
        mModelMatrixList.clear();
    }

    @Override
    public void draw(GL10 gl) {
        for (float[] mm : mModelMatrixList) {
            mSagittaObject.setModelMatrix(mm);
            mSagittaObject.draw(gl);
        }
    }

    @Override
    public void setUpProgramsAndBuffers(Bitmap texture) {
        mSagittaObject.setUpProgramsAndBuffers(texture);
    }

    public void addModelMatrix(float[] modelMatrix) {
        mModelMatrixList.add(modelMatrix);
    }

    @Override
    public void setModelMatrix(float[] modelMatrix) {

    }

    @Override
    public void setProjectionMatrix(float[] projectionMatrix) {
        mSagittaObject.setProjectionMatrix(projectionMatrix);
        mProjectionMatrix = new float[projectionMatrix.length];
        System.arraycopy(projectionMatrix, 0, mProjectionMatrix, 0, projectionMatrix.length);
    }

    @Override
    public void setViewMatrix(float[] viewMatrix) {
        mSagittaObject.setViewMatrix(viewMatrix);
    }
}
