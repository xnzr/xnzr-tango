package com.vr_object.map_editor

import android.util.Base64

import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.io.xml.DomDriver

import java.io.FileInputStream
import java.io.IOException
import java.io.PrintWriter
import java.util.concurrent.CopyOnWriteArrayList


class MapInfo internal constructor() {


    var radioSources: CopyOnWriteArrayList<RadioSource>? = null

    var map64: String? = null

    internal//        Base64.Encoder e = Base64.getEncoder();
            //        map64 = e.encodeToString(map);
    var map: ByteArray
        get() = Base64.decode(map64, Base64.DEFAULT)
        set(map) {
            map64 = Base64.encodeToString(map, Base64.DEFAULT)
        }


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
    }
}
