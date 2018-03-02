package com.vr_object.map_editor

import android.graphics.BitmapFactory
import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.io.xml.DomDriver
import java.io.FileInputStream
import java.io.IOException
import java.io.PrintWriter
import java.nio.BufferOverflowException
import java.util.concurrent.CopyOnWriteArrayList


class MapInfo internal constructor() {


    var radioSources: CopyOnWriteArrayList<RadioSource>? = null

    var map = ByteArray(0)
    var width: Int = 0
    var height: Int = 0

    init {
        radioSources = CopyOnWriteArrayList()
    }

    internal fun addRadioSource(source: RadioSource) {
        radioSources!!.add(source)
    }

    companion object {
        private val ROOT_NODE = "MapInfo"
        private val RADIO_SOURCE_NODE = "RadioSource"
        @Throws(IOException::class)
        internal fun saveToXml(dstPath: String, mapInfo: MapInfo) {
            val xstream = XStream(DomDriver())
            xstream.alias(ROOT_NODE, MapInfo::class.java)
            xstream.alias(RADIO_SOURCE_NODE, RadioSource::class.java)
            val xml = xstream.toXML(mapInfo)

            PrintWriter(dstPath).use { out -> out.println(xml) }
        }

        @Throws(IOException::class)
        internal fun loadFromXml(path: String): MapInfo {
            val xstream = XStream(DomDriver())
            xstream.alias(ROOT_NODE, MapInfo::class.java)
            xstream.alias(RADIO_SOURCE_NODE, RadioSource::class.java)

            val fileInputStream = FileInputStream(path)
            return xstream.fromXML(fileInputStream) as MapInfo
        }

        @Throws(IOException::class)
        internal fun loadFromImage(path: String): MapInfo {
            val res = MapInfo()
            val fileInputStream = FileInputStream(path)
            val size = fileInputStream.channel.size()
            if (size > Int.MAX_VALUE) {
                throw BufferOverflowException()
            }
            val isize = size.toInt()
            val b = ByteArray(isize)
            fileInputStream.read(b)
            res.map = b

            val pic = BitmapFactory.decodeByteArray(b, 0, isize)
            res.width = pic.width
            res.height = pic.height

            return res
        }
    }
}
