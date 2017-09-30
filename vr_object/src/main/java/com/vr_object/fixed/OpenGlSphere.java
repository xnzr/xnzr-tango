package com.vr_object.fixed;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import javax.microedition.khronos.opengles.GL10;

/**
 * A sphere that is renderer in AR using OpenGL.
 */
public class OpenGlSphere implements OpenGlObject {

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

    public OpenGlSphere(float radius, int rows, int columns) {
        float[] vtmp = new float[rows * columns * 3];
        // Generate position grid.
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                float theta = i * (float) Math.PI / (rows - 1);
                float phi = j * 2 * (float) Math.PI / (columns - 1);
                float x = (float) (radius * Math.sin(theta) * Math.cos(phi));
                float y = (float) (radius * Math.cos(theta));
                float z = (float) -(radius * Math.sin(theta) * Math.sin(phi));
                int index = i * columns + j;
                vtmp[3 * index] = x;
                vtmp[3 * index + 1] = y;
                vtmp[3 * index + 2] = z;
            }
        }

        // Create texture grid.
        float[] ttmp = new float[rows * columns * 2];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                int index = i * columns + j;
                ttmp[index * 2] = (float) j / (columns - 1);
                ttmp[index * 2 + 1] = (float) i / (rows - 1);
            }
        }

        // Create indices.
        int numIndices = 2 * (rows - 1) * columns;
        short[] itmp = new short[numIndices];
        short index = 0;
        for (int i = 0; i < rows - 1; i++) {
            if ((i & 1) == 0) {
                for (int j = 0; j < columns; j++) {
                    itmp[index++] = (short) (i * columns + j);
                    itmp[index++] = (short) ((i + 1) * columns + j);
                }
            } else {
                for (int j = columns - 1; j >= 0; j--) {
                    itmp[index++] = (short) ((i + 1) * columns + j);
                    itmp[index++] = (short) (i * columns + j);
                }
            }
        }

        mMesh = new OpenGlMesh(vtmp, 3, ttmp, 2, itmp);
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
        //texture.recycle();
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
