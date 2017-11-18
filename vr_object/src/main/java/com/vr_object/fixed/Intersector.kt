package com.vr_object.fixed

/**
 * Created by Michael Lukin on 14.11.2017.
 */

class Intersector {
    data class Coordinate(val x: Int, val y: Int, val z: Int) {
        constructor(array: Array<Int>) : this(array[0], array[1], array[2])
        constructor(array: Array<Float>) : this(Math.round(array[0]), Math.round(array[1]), Math.round(array[2]))

        fun toCoordArray(): Array<Float> = arrayOf(x.toFloat(), y.toFloat(), z.toFloat())
    }

    val space = hashMapOf<Coordinate, Int>()

    fun addSagitta(start: Array<Float>, end: Array<Float>) {
        val cStart = Coordinate(start)
        val cEnd = Coordinate(end)

        val d = arrayOf(Math.abs(cStart.x - cEnd.x), Math.abs(cStart.y - cEnd.y), Math.abs(cStart.z - cEnd.z))

        val lc = when {
            (d[0] > d[1] && d[0] > d[2]) -> LengthCoord.x
            (d[1] > d[0] && d[1] > d[2]) -> LengthCoord.y
            else -> LengthCoord.z
        }
        markCubes(cStart, cEnd, lc)
    }

    enum class LengthCoord {x, y, z}

    private fun markCubes(start: Coordinate, end: Coordinate, lengthCoord: LengthCoord) {
        fun getX(vertex: Coordinate) = when(lengthCoord) {
            LengthCoord.x -> vertex.x
            LengthCoord.y -> vertex.y
            LengthCoord.z -> vertex.z
        }
        fun getY(vertex: Coordinate) = when(lengthCoord) {
            LengthCoord.x -> vertex.y
            LengthCoord.y -> vertex.x
            LengthCoord.z -> vertex.y
        }
        fun getZ(vertex: Coordinate) = when(lengthCoord) {
            LengthCoord.x -> vertex.z
            LengthCoord.y -> vertex.z
            LengthCoord.z -> vertex.x
        }
        fun setXYZ(x: Int, y: Int, z: Int, lengthCoord: LengthCoord): Coordinate =
                when(lengthCoord) {
                    LengthCoord.x -> Coordinate(x, y, z)
                    LengthCoord.y -> Coordinate(y, x, z)
                    LengthCoord.z -> Coordinate(z, y, x)
                }

        val dx = when {
            getX(end) > getX(start) -> 1
            getX(end) < getX(start) -> -1
            else -> 0
        }

        val dy = when {
            getY(end) > getY(start) -> 1
            getY(end) < getY(start) -> -1
            else -> 0
        }

        val dz = when {
            getZ(end) > getZ(start) -> 1
            getZ(end) < getZ(start) -> -1
            else -> 0
        }

        val d = arrayOf(Math.abs(getX(start) - getX(end)), Math.abs(getY(start) - getY(end)), Math.abs(getZ(start) - getZ(end)))
        var x = getX(start)
        var y = getY(start)
        var z = getZ(start)

        var errY = d[0] / 2
        var errZ = d[0] / 2
        while (x != getX(end)) {
            addPoint(setXYZ(x, y, z, lengthCoord))
            x += dx
            errY -= d[1]
            errZ -= d[2]
            if (errY < 0) {
                y += dy
                errY += d[0]
            }
            if (errZ < 0) {
                z += dz
                errZ += d[0]
            }
        }
        addPoint(setXYZ(x, y, z, lengthCoord))
    }


    private fun addPoint(coord: Coordinate) {
        var v = space[coord]
        if (v != null) {
            v++
            space[coord] = v.toInt()
        } else {
            space[coord] = 1
        }
    }

    fun clear() {
        space.clear()
    }
}