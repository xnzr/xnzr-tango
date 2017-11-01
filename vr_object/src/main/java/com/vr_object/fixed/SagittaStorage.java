package com.vr_object.fixed;

import android.graphics.Bitmap;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoPoseData;
import com.projecttango.tangosupport.TangoSupport;
import com.vr_object.fixed.xnzrw24b.OpenGlSagitta;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by 811243 on 30.09.2017.
 *
 * Sagitta (lat) - arrow
 */

class SagittaStorage implements OpenGlObject {
    private OpenGlSagitta mSagittaObject;
    private CopyOnWriteArrayList<float[]> mModelMatrixList = new CopyOnWriteArrayList<>();

    private Bitmap mTexture;
    private float[] mProjectionMatrix;
    private float[] mViewMatrix;
    private float[] mCurPose;
    private Tango mTango;

    void setSagittaLength(float length) {
        if (mSagittaObject != null) {
            mSagittaObject.setLength(length);
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
        mTexture = texture;
        mSagittaObject.setUpProgramsAndBuffers(texture);
    }

    void putPose(float[] pose) {
        //mCurPose = pose;
    }

    public void setTangoService(Tango t) {
        mTango = t;
    }
    void getPose() {

//        if (mIsFrameAvailableTangoThread.compareAndSet(true, false)) {
//            // {@code mRgbTimestampGlThread} contains the exact timestamp at
//            // which the rendered RGB frame was acquired.
//            double mRgbTimestampGlThread =
//                    mTango.updateTexture(TangoCameraIntrinsics.
//                            TANGO_CAMERA_COLOR);
//
//            TangoPoseData startPose = TangoSupport.getPoseAtTime(
//                    mRgbTimestampGlThread,
//                    TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
//                    TangoPoseData.COORDINATE_FRAME_CAMERA_COLOR,
//                    TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
//                    0);
//
//            if (startPose.statusCode == TangoPoseData.POSE_VALID) {
//
//                float[] end = new float[4];
//                float[] start = new float[4];
//
//                start[0] = (float) startPose.translation[0];
//                start[1] = (float) startPose.translation[1];
//                start[2] = (float) startPose.translation[2];
//                start[3] = 0.0f;//(float)pose.translation[2];
//            }
//        }
    }

    void addModelMatrix(float[] modelMatrix) {
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
        mViewMatrix = viewMatrix;
        mSagittaObject.setViewMatrix(viewMatrix);
    }
}
