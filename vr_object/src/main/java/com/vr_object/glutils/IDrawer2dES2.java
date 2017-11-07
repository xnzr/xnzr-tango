package com.vr_object.glutils;

/**
 *
 * Created by saki t_saki@serenegiant.com
 * Edited by Michael Lukin
 *
 */
public interface IDrawer2dES2 extends IDrawer2D {
	public int glGetAttribLocation(final String name);
	public int glGetUniformLocation(final String name);
	public void glUseProgram();
}
