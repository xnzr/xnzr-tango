package com.vr_object.fixed;

/**
 * Created by Michael Lukin on 26.10.2017.
 */

public class Vector3D {
    private static final int x = 0;
    private static final int y = 1;
    private static final int z = 2;
    private static final int w = 3;

    public static final float Epsilon = 0.0001f;

    public static float[] vectorMultiply(float[] lhs, float[] rhs) {
        float[] result = new float[3];

        result[x] = lhs[y]*rhs[z] - lhs[z]*rhs[y];
        result[y] = lhs[z]*rhs[x] - lhs[x]*rhs[z];
        result[z] = lhs[x]*rhs[y] - lhs[y]*rhs[z];

        return result;
    }

    public static float scalarMultiply(float[] lhs, float[] rhs) {
        return lhs[x]*rhs[x] + lhs[y]*rhs[y] + lhs[z]*rhs[z];
    }

    /**
     * Computes the distance between two vertices.
     * @param left x, y, z coordinates of vertex.
     * @param right x, y, z coordinates of another vertex.
     * @return the distance between two vertices.
     */
    public static float distanceVertices(float[] left, float[] right) {
        float sum = 0;
        for (int i = 0; i < 3; i++) {
            sum += (left[i] - right[i])*(left[i] - right[i]);
        }
        return (float) Math.sqrt(sum);
    }

    /**
     * Computes the length of a vector.
     * @param vector x, y, z coordinates of vector.
     * @return the length of a vector.
     */
    public static float vectorLength(float[] vector) {
        return (float) Math.sqrt(vector[x]*vector[x] + vector[y]*vector[y] + vector[z]*vector[z]);
    }

    /**
     * Creates a 3D vector with 4-th element == 0 for usage in OpenGL matrices.
     * @param start 3D coordinates of vector start.
     * @param end 3D coordinates of vector end.
     * @return An array of size 4: x, y, z, w components of 4D OpenGL vector. w == 0.
     */
    public static float[] createVector(float[] start, float[] end) {
        float[] res = new float[4];
        for (int i = 0; i < 3; i++) {
            res[i] = end[i] - start[i];
        }
        res[w] = 0;

        return res;
    }

    /**
     * Calculate distance between dot and line defined by two vertices.
     * @param dot - 3D coordinates of dot.
     * @param lineA - 3D coordinates of one of the line vertex.
     * @param lineB - 3D coordinates of another line vertex.
     * @return the distance between dot and line segment.
     */
    public static float distanceDotLine(float[] dot, float[] lineA, float[] lineB) {
        float[] v_l = createVector(lineA, lineB);
        float[] w = createVector(lineA, dot);

        float[] mul = vectorMultiply(v_l, w);
        return vectorLength(mul);
    }

    /**
     * Calculate distance between dot and line segment.
     * @param dot - 3D coordinates of dot.
     * @param segmentStart - 3D coordinates of one of the segment vertex.
     * @param segmentEnd - 3D coordinates of another segment vertex.
     * @return the distance between dot and line segment.
     */
    public static float distanceDotSegment(float[] dot, float[] segmentStart, float[] segmentEnd) {
        float[] vecSegment = createVector(segmentStart, segmentEnd);
        float[] vecDot = createVector(segmentStart, dot);
        float scalarMul = scalarMultiply(vecSegment, vecDot);

        if ((Math.abs(scalarMul) < Epsilon) || (scalarMul < 0)) {
            //perpendicular is coincides with segmentStart or is outside of segment from the start side.
            return distanceVertices(dot, segmentStart);
        }

        vecSegment = createVector(segmentEnd, segmentStart);
        vecDot = createVector(segmentEnd, dot);
        scalarMul = scalarMultiply(vecSegment, vecDot);
        if ((Math.abs(scalarMul) < Epsilon) || (scalarMul < 0)) {
            //perpendicular is coincides with segmentStart or is outside of segment from the end side.
            return distanceVertices(dot, segmentEnd);
        }

        //perpendicular is inside the segment
        return distanceDotLine(dot, segmentStart, segmentEnd);
    }
}
