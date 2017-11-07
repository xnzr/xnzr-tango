package com.vr_object.glutils;

import java.io.IOException;

/**
 *
 * Created by saki t_saki@serenegiant.com
 * Edited by Michael Lukin
 *
 */
public interface ITexture {
	void release();

	void bind();
	void unbind();

	int getTexTarget();
	int getTexture();

	float[] getTexMatrix();
	void getTexMatrix(float[] matrix, int offset);

	int getTexWidth();
	int getTexHeight();

	void loadTexture(String filePath) throws NullPointerException, IOException;
}
