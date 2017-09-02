package com.vr_object.fixed;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * An OpenGL renderer that renders the Tango RGB camera texture on a full-screen background
 * and two spheres representing the earth and the moon in Augmented Reality.
 */
public class WiFiAugmentedRealityRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = WiFiAugmentedRealityRenderer.class.getSimpleName();

    private float[] mObjectTransform = null;
    private boolean mObjectPoseUpdated = false;

    public void updateObjectPose(float[] planeFitTransform) {
        mObjectTransform = planeFitTransform;
        mObjectPoseUpdated = true;
    }


    /**
     * A small callback to allow the caller to introduce application-specific code to be executed
     * in the OpenGL thread.
     */
    public interface RenderCallback {
        void preRender();
    }

    private RenderCallback mRenderCallback;
    private OpenGlCameraPreview mOpenGlCameraPreview;
    private OpenGlSphere mEarthSphere;
    private WiFiOpenGLLine mLine;
    private Context mContext;

    public WiFiAugmentedRealityRenderer(Context context, RenderCallback callback) {
        mContext = context;
        mRenderCallback = callback;
        mOpenGlCameraPreview = new OpenGlCameraPreview();
        mEarthSphere = new OpenGlSphere(0.15f, 20, 20);

        mLine = new WiFiOpenGLLine();
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        // Enable depth test to discard fragments that are behind of another fragment.
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        // Enable face culling to discard back facing triangles.
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);
        GLES20.glClearColor(1.0f, 1.0f, 0.0f, 1.0f);
        mOpenGlCameraPreview.setUpProgramAndBuffers();
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap earthBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable
                .earth, options);
        mEarthSphere.setUpProgramAndBuffers(earthBitmap);

        mLine.setUpProgramsAndBuffers();
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Call application-specific code that needs to run on the OpenGL thread.
        mRenderCallback.preRender();
        // Don't write depth buffer because we want to draw the camera as background.
        GLES20.glDepthMask(false);
        mOpenGlCameraPreview.drawAsBackground();
        // Enable depth buffer again for AR.
        GLES20.glDepthMask(true);
        GLES20.glCullFace(GLES20.GL_BACK);
        //mEarthSphere.drawSphere();

        mLine.draw();
    }

    /**
     * It returns the ID currently assigned to the texture where the Tango color camera contents
     * should be rendered.
     * NOTE: This must be called from the OpenGL render thread - it is not thread safe.
     */
    public int getTextureId() {
        return mOpenGlCameraPreview == null ? -1 : mOpenGlCameraPreview.getTextureId();
    }

    /**
     * Set the Projection matrix matching the Tango RGB camera in order to be able to do
     * Augmented Reality.
     */
    public void setProjectionMatrix(float[] matrixFloats) {
        mEarthSphere.setProjectionMatrix(matrixFloats);
        mLine.setProjectionMatrix(matrixFloats);
    }

    /**
     * Update the View matrix matching the pose of the Tango RGB camera.
     *
     * @param ssTcamera The transform from RGB camera to Start of Service.
     */
    public void updateViewMatrix(float[] ssTcamera) {
        float[] viewMatrix = new float[16];
        Matrix.invertM(viewMatrix, 0, ssTcamera, 0);
        mEarthSphere.setViewMatrix(viewMatrix);

        mLine.setViewMatrix(viewMatrix);
    }

    public float[] getViewMatrix() {
        return mLine.getViewMatrix();
    }

    public void setEarthTransform(float[] worldTEarth) {
        mEarthSphere.setModelMatrix(worldTEarth);
        //mLine.setModelMatrix(worldTEarth);
    }

    public void setLineTransform(float[] worldTLine) {
        mLine.setModelMatrix(worldTLine);
    }

    public void setLine(float[] ssTcamera, float[] worldTEarth)
    {
        float[] matrix = new float[16];
        Matrix.invertM(matrix, 0, ssTcamera, 0);

        float[] zero = new float[4];
        zero[0] = 0.0f;
        zero[1] = 0.0f;
        zero[2] = 0.0f;
        zero[3] = 0.0f;

        float[] one = new float[4];
        one[0] = 20.0f;
        one[1] = 20.0f;
        one[2] = 20.0f;
        one[3] = 0.0f;


        float[] start = new float[4];
        Matrix.multiplyMV(start, 0, matrix, 0, zero, 0);

        Matrix.invertM(matrix, 0, worldTEarth, 0);
        float[] end = new float[4];
        Matrix.multiplyMV(end, 0, matrix, 0, one, 0);

        //mLine.setPosition(zero, one);
        mLine.AddLine(zero, one);
    }

    public void setLinePos(float start[], float end[]) {
        //mLine.setPosition(start, end);
        mLine.AddLine(start, end);
    }


}
