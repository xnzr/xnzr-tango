package com.vr_object.fixed;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

/**
 * A sphere that is renderer in AR using OpenGL.
 */
public class OpenGlCylinder implements OpenGlObject {

    private final String mVss =
            "attribute vec3 a_Position;\n" +
                    "attribute vec2 a_TexCoord;\n" +
                    "uniform mat4 u_MvpMatrix; \n" +
                    "varying vec2 v_TexCoord;\n" +
                    "void main() {\n" +
                    "  v_TexCoord = a_TexCoord;\n" +
                    "  gl_Position = u_MvpMatrix * vec4(a_Position.x, a_Position.y, a_Position.z," +
                    " 1.0);\n" +
                    "}";

    private final String mFss =
            "precision mediump float;\n" +
                    "uniform sampler2D u_Texture;\n" +
                    "varying vec2 v_TexCoord;\n" +
                    "void main() {\n" +
                    "  gl_FragColor = texture2D(u_Texture,v_TexCoord);\n" +
                    "}";

    private OpenGlMesh mMesh;
    private int[] mTextures;
    private int mProgram;

    private float[] mModelMatrix = new float[16];
    private float[] mViewMatrix = new float[16];
    private float[] mProjectionMatrix = new float[16];

    public OpenGlCylinder(float radius, float height, int n) {
        float[] vertices = new float[3*2*n];
        // Generate position grid.
        float angle = (float) ((2 * Math.PI) / n);
        for (int i = 0; i < n; i++) {
            vertices[3*i] =     (float) (radius * Math.cos(angle*i));
            vertices[3*i + 1] = (float) (radius * Math.sin(angle*i));
            vertices[3*i + 2] = 0.01f; //Zero distance to camera means bottom culling. We want to avoid it.

            vertices[3*n + 3*i + 0] = (float) (radius * Math.cos(angle*i));
            vertices[3*n + 3*i + 1] = (float) (radius * Math.sin(angle*i));
            vertices[3*n + 3*i + 2] = height;
        }


        // Create texture grid.
        float[] textureGrid = new float[2*2*n];
        for (int i = 0; i < n; i++) {
            textureGrid[2*i] = (float) 0;
            textureGrid[2*i + 1] = (float) (1/n);

            textureGrid[2*n + 2*i] = (float) 1;
            textureGrid[2*n + 2*i + 1] = (float) (1/n);
        }

        // Create triangle indices.
        List<Short> idx = new ArrayList<>();
        //bottom
        for (short i = 1; i < n - 1; i++) {
            idx.add((short) 0);
            idx.add(i);
            idx.add((short) (i+1));
        }

        //side
        for (short i = 0; i < n; i++) {
            idx.add(i);
            idx.add((short) (n+i));
            idx.add((short) ((i+1)%n));
            idx.add((short) (n+i));
            idx.add((short) (n + (i+1)%n));
            idx.add((short) ((i+1)%n));
        }

        //top
        for (short i = 1; i < n - 1; i++) {
            idx.add((short) n);
            idx.add((short) (n+i+1));
            idx.add((short) (n+i));
        }
        short[] triangles = new short[idx.size()];
        for (int i = 0; i < idx.size(); i++) {
            triangles[i] = idx.get(i);
        }

        mMesh = new OpenGlMesh(vertices, 3, textureGrid, 2, triangles);
    }

    @Override
    public void setUpProgramsAndBuffers(Bitmap texture) {
        mMesh.createVbos();
        createTexture(texture);
        mProgram = OpenGlHelper.createProgram(mVss, mFss);
    }

    private void createTexture(Bitmap texture) {
        mTextures = new int[1];
        GLES20.glGenTextures(1, mTextures, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_NEAREST);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, texture, 0);
//        texture.recycle();
    }

    @Override
    public void draw(GL10 gl) {
        GLES20.glUseProgram(mProgram);
        // Enable depth write for AR.
        int sph = GLES20.glGetAttribLocation(mProgram, "a_Position");
        int sth = GLES20.glGetAttribLocation(mProgram, "a_TexCoord");

        int um = GLES20.glGetUniformLocation(mProgram, "u_MvpMatrix");
        int ut = GLES20.glGetUniformLocation(mProgram, "u_Texture");

        gl.glColor4f(0, 1, 0, 0.3f);

        float[] mvMatrix = new float[16];
        Matrix.multiplyMM(mvMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        float[] mvpMatrix = new float[16];
        Matrix.multiplyMM(mvpMatrix, 0, mProjectionMatrix, 0, mvMatrix, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[0]);
        GLES20.glUniform1i(ut, 0);
        GLES20.glUniformMatrix4fv(um, 1, false, mvpMatrix, 0);

        mMesh.drawMesh(sph, sth);
    }

    @Override
    public void setModelMatrix(float[] modelMatrix) {
        System.arraycopy(modelMatrix, 0, mModelMatrix, 0, 16);
    }

    @Override
    public void setProjectionMatrix(float[] projectionMatrix) {
        System.arraycopy(projectionMatrix, 0, mProjectionMatrix, 0, 16);
    }

    @Override
    public void setViewMatrix(float[] viewMatrix) {
        System.arraycopy(viewMatrix, 0, mViewMatrix, 0, 16);
    }
}
