package com.vr_object.fixed;

import android.graphics.Bitmap;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by 811243 on 30.09.2017.
 */

public interface OpenGlObject {
    void draw(GL10 gl);

    void setUpProgramsAndBuffers(Bitmap texture);

    void setModelMatrix(float[] modelMatrix);

    void setProjectionMatrix(float[] projectionMatrix);

    void setViewMatrix(float[] viewMatrix);
}
