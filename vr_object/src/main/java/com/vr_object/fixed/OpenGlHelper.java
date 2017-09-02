package com.vr_object.fixed;

import android.opengl.GLES20;
import android.util.Log;

/**
 * Utility methods to compile and link GLSL programs.
 */
public class OpenGlHelper {

    private static final String TAG = OpenGlHelper.class.getSimpleName();

    public static int createProgram(String vertexShader, String fragmentShader) {
        int program = GLES20.glCreateProgram();
        if (program == 0) {
            return 0;
        }
        int vShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShader);
        int fShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);
        GLES20.glAttachShader(program, vShader);
        GLES20.glAttachShader(program, fShader);
        GLES20.glLinkProgram(program);
        int[] linked = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linked, 0);
        if (linked[0] == 0) {
            Log.e(TAG, "Could not link program");
            Log.v(TAG, "Could not link program:" +
                    GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            return 0;
        }
        return program;
    }

    private static int loadShader(int type, String shaderSrc) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderSrc);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Could not compile shader");
            Log.v(TAG, "Could not compile shader:" +
                    GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

}
