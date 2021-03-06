package com.vr_object.map_editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import com.vr_object.fixed.R

/**
* Created by Michael Lukin on 21.02.2018.
*/

class MapView(context: Context?, private val mapInfo: MapInfo) : View(context) {
    private var mapBitmap: Bitmap = BitmapFactory.decodeByteArray(mapInfo.map, 0, mapInfo.map.size)
    private val radioBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.icons8_ibeacon_96)
    private var radioWidth = resources.getDimension(R.dimen.radio_size)
    private var radioHeight = resources.getDimension(R.dimen.radio_size)
    private var mapRectF = RectF(0f, 0f, 100f, 100f) //some default value to achieve consistency while View size is unknown

    private enum class State {Idle, StartCreateRadio, CreateRadio, StartDeleteRadio, DeleteRadio}
    private var state = State.Idle

    private data class Vector2(val x: Float, val y: Float)

    interface OnMapChanged {
        fun onMapChanged()
    }
    var mapListener: OnMapChanged? = null

    init {
        listenClicks()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mapRectF = calcProperRect(w.toFloat(), h.toFloat())
    }

    private fun calcProperRect(viewWidth: Float, viewHeight: Float): RectF {
        val viewRatio = viewWidth/viewHeight
        val mapRatio = mapInfo.width.toFloat()/mapInfo.height.toFloat()

        return if (mapRatio >= viewRatio) { //map is wider
            RectF(0f, 0f, viewWidth, viewWidth/mapRatio)
        } else { //view is wider
            RectF(0f, 0f, viewHeight*mapRatio, viewHeight)
        }
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.drawBitmap(mapBitmap, null, mapRectF, null)

        mapInfo.radioSources?.let {
            for (src in it) {
                val x = mapRectF.width() * src.x
                val y = mapRectF.height() * src.y
                val rect = RectF((x - radioWidth /2f).toFloat(), (y - radioHeight /2).toFloat(), (x + radioWidth /2f).toFloat(), (y + radioHeight /2f).toFloat())
                canvas?.drawBitmap(radioBitmap, null, rect, null)
            }
        }
    }

    private fun listenClicks() {
        setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> onTouch(event)
                MotionEvent.ACTION_UP -> onTouchUp(event)
                else -> Unit
            }
            true
        }
    }

    private fun onTouch(event: MotionEvent) {
        when (state) {
            State.Idle -> Unit
            State.StartCreateRadio -> beginPlaceRadio()
            State.StartDeleteRadio -> beginDeleteRadio()
            else -> Unit
        }
    }

    private fun onTouchUp(event: MotionEvent) {
        when (state) {
            State.CreateRadio -> placeRadio(event.x, event.y)
            State.DeleteRadio -> deleteRadio(event.x, event.y)
            else -> Unit
        }
    }

    private fun beginPlaceRadio() {
        state = State.CreateRadio
    }

    private fun beginDeleteRadio() {
        state = State.DeleteRadio
    }

    private fun calcDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return Math.sqrt(((x1-x2)*(x1-x2) + (y1-y2)*(y1-y2)).toDouble()).toFloat()
    }

    private fun deleteRadio(x: Float, y: Float) {
        mapInfo.radioSources?.let {
            for (r in mapInfo.radioSources) {
                val rxUnscaled = (r.x*mapRectF.width()).toFloat()
                val ryUnscaled = (r.y*mapRectF.height()).toFloat()
                val dst = calcDistance(x, y, rxUnscaled, ryUnscaled)

                if (dst < resources.getDimension(R.dimen.radio_size)) {
                    mapInfo.radioSources.remove(r)
                    state = State.Idle
                    invalidate()
                    break
                }
            }
        }
    }

    private fun placeRadio(x: Float, y: Float) {
        val xScaled = x/mapRectF.width()
        val yScaled = y/mapRectF.height()
        mapInfo.addRadioSource(RadioSource(xScaled.toDouble(), yScaled.toDouble()))
        state = State.Idle
        invalidate()
        mapListener?.onMapChanged()
    }

    fun onCreateRadioClick() {
        state = State.StartCreateRadio
    }

    fun onDeleteRadioClick() {
        state = State.StartDeleteRadio
    }

    fun moveRadio() {

    }
}
