package com.vr_object.fixed;

import android.graphics.Bitmap;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by 811243 on 30.09.2017.
 */

public interface OpenGlObject {
    public void draw(GL10 gl);

    public void setUpProgramsAndBuffers(Bitmap texture);


    public void setModelMatrix(float[] modelMatrix);

    public void setProjectionMatrix(float[] projectionMatrix);

    public void setViewMatrix(float[] viewMatrix);
}
