package com.vr_object.fixed;


import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static android.opengl.GLES10.glDisableClientState;
import static android.opengl.GLES10.glEnableClientState;
import static javax.microedition.khronos.opengles.GL10.GL_COLOR_ARRAY;
import static javax.microedition.khronos.opengles.GL10.GL_VERTEX_ARRAY;

public class WiFiOpenGLLineEx {

    private final String mVss =
            "uniform mat4 u_MvpMatrix; \n" +
                    "attribute vec3 a_Position;\n" +
                    "void main() {\n" +
                    "  gl_Position = u_MvpMatrix * vec4(a_Position.x, a_Position.y, a_Position.z, 1.0);\n" +
                    "}";

    private final String mFss =
            "precision mediump float;\n" +
                    "uniform vec4 u_Color;\n" +
                    "void main() {\n" +
                    "  gl_FragColor = u_Color;\n" +
                    "}";

    private int mProgram;

    private float[] mModelMatrix = new float[16];
    private float[] mViewMatrix = new float[16];
    private float[] mProjectionMatrix = new float[16];

    private FloatBuffer mVertex;
    private int mVertexCoordNumber;

    private int mNumLines;
    private int mNumPoints;

    public WiFiOpenGLLineEx() {

        mNumLines = 0;
        mNumPoints = 2;

        mVertexCoordNumber = 3;
        int capacity = Float.SIZE / 8 * (mVertexCoordNumber * mNumPoints);
        mVertex = ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mVertex.put(0.0f);
        mVertex.put(0.0f);
        mVertex.put(0.0f);
        mVertex.put(2.0f);
        mVertex.put(2.0f);
        mVertex.put(2.0f);
        mVertex.position(0);
    }

    private void setPosition(float[] start, float[] end)
    {
//        mVertex.put(0.0f);
//        mVertex.put(0.0f);
//        mVertex.put(0.0f);

        mVertex.put(start[0]);
        mVertex.put(start[1]);
        mVertex.put(start[2]);

        mVertex.put(end[0]);
        mVertex.put(end[1]);
        mVertex.put(end[2]);

        mVertex.position(0);
    }

    public void AddLine(float[] start, float[] end)
    {
        if (mNumLines == 0)
        {
            mNumLines = 1;
            mNumPoints = 2;
            setPosition(start, end);
            return;
        }

        float[] buffer = new float[mVertexCoordNumber * mNumPoints];
        mVertex.get(buffer, 0, buffer.length);

        mNumLines += 1;
        mNumPoints += 2;

        int capacity = Float.SIZE / 8 * (mVertexCoordNumber * mNumPoints);
        mVertex = ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder()).asFloatBuffer();

        mVertex.put(buffer, 0, buffer.length);

        mVertex.put(start[0]);
        mVertex.put(start[1]);
        mVertex.put(start[2]);

        mVertex.put(end[0]);
        mVertex.put(end[1]);
        mVertex.put(end[2]);

        mVertex.position(0);
    }

    public void setUpProgramsAndBuffers()
    {
        mProgram = OpenGlHelper.createProgram(mVss, mFss);
    }

    public void draw() {
        GLES20.glUseProgram(mProgram);

        int colorLocation = GLES20.glGetUniformLocation(mProgram, "u_Color");
        GLES20.glUniform4f(colorLocation, 1.0f, 0.0f, 0.0f, 1.0f);

        int posLocation = GLES20.glGetAttribLocation(mProgram, "a_Position");
        int mvpMatrixLocation = GLES20.glGetUniformLocation(mProgram, "u_MvpMatrix");

        float[] mvMatrix = new float[16];
        Matrix.multiplyMM(mvMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        float[] mvpMatrix = new float[16];
        Matrix.multiplyMM(mvpMatrix, 0, mProjectionMatrix, 0, mvMatrix, 0);

        Matrix.multiplyMM(mvpMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);

        GLES20.glUniformMatrix4fv(mvpMatrixLocation, 1, false, mvpMatrix, 0);

        GLES20.glGetError();

        /// draw lines
        GLES20.glEnableVertexAttribArray(posLocation);
        int stride = Float.SIZE / 8 * mVertexCoordNumber;
        GLES20.glVertexAttribPointer(
                posLocation,
                mVertexCoordNumber,
                GLES20.GL_FLOAT,
                false,
                stride,
                mVertex);

        GLES20.glEnableVertexAttribArray(posLocation);

        //glEnableClientState(GL_VERTEX_ARRAY);
        //GLES20.glEnableClientState(GL_COLOR_ARRAY);

        //GLES20.glLineWidth(5.0f);

        GLES20.glDrawArrays(GLES20.GL_LINES, 0, mNumPoints);

        //glDisableClientState(GL_COLOR_ARRAY);
        //glDisableClientState(GL_VERTEX_ARRAY);
    }

    public void setModelMatrix(float[] modelMatrix) {
        System.arraycopy(modelMatrix, 0, mModelMatrix, 0, 16);
    }

    public void setProjectionMatrix(float[] projectionMatrix) {
        System.arraycopy(projectionMatrix, 0, mProjectionMatrix, 0, 16);
    }

    public void setViewMatrix(float[] viewMatrix) {
        System.arraycopy(viewMatrix, 0, mViewMatrix, 0, 16);
    }

    public float[] getViewMatrix() {
        return mViewMatrix;
    }

}

