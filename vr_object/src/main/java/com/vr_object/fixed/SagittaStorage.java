package com.vr_object.fixed;

import android.graphics.Bitmap;
import android.opengl.Matrix;

import com.google.atap.tangoservice.Tango;
import com.vr_object.fixed.xnzrw24b.OpenGlSagitta;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by 811243 on 30.09.2017.
 *
 * Sagitta (lat) - arrow
 */

class SagittaStorage implements OpenGlObject {
    private OpenGlSagitta mSagittaObject;
    private OpenGlObject mIntersectObject;
    private CopyOnWriteArrayList<float[]> mModelMatrixList = new CopyOnWriteArrayList<>();

    private Bitmap mTexture;
    private float[] mProjectionMatrix;
    private float[] mViewMatrix;
    private float[] mCurPose;
    private Tango mTango;
    private Intersector intersector;

    public SagittaStorage(float scale) {
        intersector = new Intersector(scale, 5);
    }

    void setSagittaLength(float length) {
        if (mSagittaObject != null) {
            mSagittaObject.setLength(length);
        }
    }

    void setIntersectObject(OpenGlObject o) {
        mIntersectObject = o;
        if (mTexture != null) {
            mIntersectObject.setUpProgramsAndBuffers(mTexture);
        }
        if (mProjectionMatrix != null) {
            mIntersectObject.setProjectionMatrix(mProjectionMatrix);
        }
        if (mViewMatrix != null) {
            mIntersectObject.setViewMatrix(mViewMatrix);
        }
    }

    void setObject(OpenGlSagitta o) {
        mSagittaObject = o;
        if (mTexture != null) {
            mSagittaObject.setUpProgramsAndBuffers(mTexture);
        }
        if (mProjectionMatrix != null) {
            mSagittaObject.setProjectionMatrix(mProjectionMatrix);
        }
        if (mViewMatrix != null) {
            mSagittaObject.setViewMatrix(mViewMatrix);
        }
    }

    void clearSagittae() {
        mModelMatrixList.clear();
        intersector.clear();
    }

    @Override
    public void draw(GL10 gl) {
        for (float[] mm : mModelMatrixList) {
            mSagittaObject.setModelMatrix(mm);
            mSagittaObject.draw(gl);
        }

        ArrayList<Float[]> cubeCenters = intersector.getIntersection();
        for (Float[] c: cubeCenters) {
            drawCube(c, gl);
        }
    }

    private void drawCube(Float[] cubeCenterPos, GL10 gl) {
        float[] modelMatrix = new float[16];
        Matrix.setIdentityM(modelMatrix, 0);
        modelMatrix[12] = cubeCenterPos[0];
        modelMatrix[13] = cubeCenterPos[1];
        modelMatrix[14] = cubeCenterPos[2];

        mIntersectObject.setModelMatrix(modelMatrix);
        mIntersectObject.draw(gl);
    }

    @Override
    public void setUpProgramsAndBuffers(Bitmap texture) {
        mTexture = texture;
        mSagittaObject.setUpProgramsAndBuffers(texture);
        mIntersectObject.setUpProgramsAndBuffers(texture);
    }

    void putPose(float[] pose) {
        //mCurPose = pose;
    }

    public void setTangoService(Tango t) {
        mTango = t;
    }

    void addSagittaModelMatrix(float[] modelMatrix) {
        mModelMatrixList.add(modelMatrix);
    }

    void intersectSagitta(float[] start, float[] end) {
        Float[] s = new Float[start.length];
        for (int i = 0; i < 3; i++) {
            s[i] = start[i];
        }
        Float[] e = new Float[start.length];
        for (int i = 0; i < 3; i++) {
            e[i] = end[i];
        }
        intersector.addSagitta(s, e);
    }

    @Override
    public void setModelMatrix(float[] modelMatrix) {

    }

    @Override
    public void setProjectionMatrix(float[] projectionMatrix) {
        mSagittaObject.setProjectionMatrix(projectionMatrix);
        mIntersectObject.setProjectionMatrix(projectionMatrix);
        mProjectionMatrix = new float[projectionMatrix.length];
        System.arraycopy(projectionMatrix, 0, mProjectionMatrix, 0, projectionMatrix.length);
    }

    @Override
    public void setViewMatrix(float[] viewMatrix) {
        mViewMatrix = viewMatrix;
        mSagittaObject.setViewMatrix(viewMatrix);
        mIntersectObject.setViewMatrix(viewMatrix);
    }
}
