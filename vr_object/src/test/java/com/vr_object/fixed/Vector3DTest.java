package com.vr_object.fixed;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by Michael Lukin on 01.11.2017.
 */
public class Vector3DTest {
    public static final float Epsilon = 0.001f;

    public static boolean equal(float left, float right) {
        return (Math.abs(left - right) < Epsilon);
    }
    public static boolean vectorEqual(float[] left, float[] right) {
        for (int i = 0; i < 3; i++) {
            if (Math.abs(left[i] - right[i]) >= Epsilon) {
                return false;
            }
        }
        return true;
    }

    @Test
    public void vectorMultiply() throws Exception {
        float vec1[] = {1, 0, 0};
        float vec2[] = {0, 1, 0};
        float res[] = Vector3D.vectorMultiply(vec1, vec2);
        assertTrue(vectorEqual(res, new float[] {0, 0, 1}));
        res = Vector3D.vectorMultiply(vec2, vec1);
        assertTrue(vectorEqual(res, new float[] {0, 0, -1}));

        float[] vec3 = {1, 2, 3};
        float[] vec4 = {4, 5, 6};
        res = Vector3D.vectorMultiply(vec3, vec4);
        assertTrue(vectorEqual(res, new float[] {-3, 6, -3}));
        res = Vector3D.vectorMultiply(vec4, vec3);
        assertTrue(vectorEqual(res, new float[] {3, -6, 3}));

        res = Vector3D.vectorMultiply(vec4, vec4);
        assertTrue(vectorEqual(res, new float[] {0, 0, 0}));
    }

    @Test
    public void scalarMultiply() throws Exception {
        float vec1[] = {1, 0, 0};
        float vec2[] = {0, 1, 0};
        float res = Vector3D.scalarMultiply(vec1, vec2);
        assertTrue(equal(res, 0));

        float v3[] = {5, 0, 0};
        float v4[] = {3, -1, 0};
        res = Vector3D.scalarMultiply(v3, v4);
        assertTrue(equal(res, 15));
    }

    @Test
    public void distanceVertices() throws Exception {
        float[] start = {0, 1, 0, 1};
        float[] end = {5, 1, 0, 1};
        float[] vertex = {3, 0, 0, 1};

        float res = Vector3D.distanceVertices(start, end);
        assertTrue(equal(res, 5));

        res = Vector3D.distanceVertices(start, vertex);
        assertTrue(equal(res, 3.1623f));
    }

    @Test
    public void vectorLength() throws Exception {

    }

    @Test
    public void createVector() throws Exception {

    }

    @Test
    public void distanceDotLine() throws Exception {

    }

    @Test
    public void distanceVertexSegment() throws Exception {
        float[] start = {0, 1, 0, 1};
        float[] end = {5, 1, 0, 1};
        float[] vertex = {3, 0, 0, 1};
        float res = Vector3D.distancevertexSegment(vertex, start, end);
        assertTrue(equal(res, 1));

        float[] vertex2 = {0, 0, 0};
        res = Vector3D.distancevertexSegment(vertex2, start, end);
        assertTrue(equal(res, 1));

        float[] vertex3 = {-1, 0, 0};
        res = Vector3D.distancevertexSegment(vertex3, start, end);
        assertTrue(equal(res, 1.4142f));

        float[] start2 = {-3f, 2, -5f};
        float[] end2 = {3, 2, 5};
        res = Vector3D.distancevertexSegment(vertex2, start2, end2);
        assertTrue(equal(res, 2));
    }

}