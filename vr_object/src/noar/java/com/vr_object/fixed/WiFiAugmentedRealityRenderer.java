package com.vr_object.fixed;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


/**
 * An OpenGL renderer that renders the Tango RGB camera texture on a full-screen background
 * and two spheres representing the earth and the moon in Augmented Reality.
 */
class WiFiAugmentedRealityRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = WiFiAugmentedRealityRenderer.class.getSimpleName();
    public static final float BEARING_LENGTH = 100f;

    private float[] mObjectTransform = null;
    private boolean mObjectPoseUpdated = false;

    private boolean shouldDrawSphere = false;

    void updateObjectPose(float[] planeFitTransform) {
        mObjectTransform = planeFitTransform;
        mObjectPoseUpdated = true;
    }

    void setShouldDrawSphere(boolean shouldDrawSphere) {
        this.shouldDrawSphere = shouldDrawSphere;
    }


    /**
     * A small callback to allow the caller to introduce application-specific code to be executed
     * in the OpenGL thread.
     */
    interface RenderCallback {
        void preRender();
    }

    private RenderCallback mRenderCallback;
    private OpenGlCameraPreview mOpenGlCameraPreview;
    private OpenGlSphere mSphere;
    private WiFiOpenGLLine mLine;
    private SagittaStorage mSagittae;
    private Context mContext;
    private OptionsHolder optionsHolder;
    private float mScale = 0.4f;

    WiFiAugmentedRealityRenderer(Context context, RenderCallback callback) {
        mContext = context;
        mRenderCallback = callback;
        mOpenGlCameraPreview = new OpenGlCameraPreview();
        mSphere = new OpenGlSphere(0.15f, 20, 20);
        mSagittae = new SagittaStorage(mScale);
        optionsHolder = new OptionsHolder(mContext);

        mLine = new WiFiOpenGLLine();
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        // Enable depth test to discard fragments that are behind of another fragment.
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        // Enable face culling to discard back facing triangles.
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glCullFace(GLES20.GL_BACK);
        //GLES20.glClearColor(1.0f, 1.0f, 0.0f, 1.0f);
        GLES20.glClearColor(1.0f, 1.0f, 0.0f, 0.5f);
        mOpenGlCameraPreview.setUpProgramAndBuffers();
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(),
                R.drawable.red, options);
        bitmap.setHasAlpha(true);

        int sagittaLength = optionsHolder.loadSagittaLength();
        initSagitta(0.001f, sagittaLength);

        mSphere.setUpProgramsAndBuffers(bitmap);
        mSagittae.setUpProgramsAndBuffers(bitmap);

        mLine.setUpProgramsAndBuffers();
        GLES20.glDisable(GLES20.GL_BLEND);
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


//        mSagittae.draw(gl10);
        mLine.draw();
    }


    private void initSagitta(float width, float length) {
        OpenGlCylinder cylinder = new OpenGlCylinder(width, length, 8);
        mSagittae.setObject(cylinder);

        float cubeDiagonalLength = 1.732f*mScale;
//        OpenGlCube cube = new OpenGlCube(0.1f);
        OpenGlSphere sphere = new OpenGlSphere(cubeDiagonalLength/2, 20, 20);
        mSagittae.setIntersectObject(sphere);
    }

    void setSagittaLength(float length) {
        mSagittae.setSagittaLength(length);
    }

    void clearPelengs() {
        mSagittae.clearSagittae();
    }

}
