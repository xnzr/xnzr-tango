package com.vr_object.map_editor

import android.Manifest
import android.app.Fragment
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import com.vr_object.fixed.OpenFileDialog
import com.vr_object.fixed.R
import kotlinx.android.synthetic.main.fragment_map.*

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [MapFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [MapFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MapFragment : Fragment(), MapView.OnMapChanged {
    override fun onMapChanged() {
        if (savingStatus == SavingStatus.Saved) {
            savingStatus = SavingStatus.Unsaved
        }
    }

    // TODO: Rename and change types of parameters
    private var mParam1: String? = null
    private var mParam2: String? = null

    private lateinit var mapInfo: MapInfo
    private var mapView: MapView? = null

    private var mListener: OnFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            mParam1 = arguments.getString(ARG_PARAM1)
            mParam2 = arguments.getString(ARG_PARAM2)
        }
    }

    private enum class SavingStatus {Saved, NewFile, Unsaved}
    private var savingStatus = SavingStatus.Saved
    private var lastSavedFile = ""

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater!!.inflate(R.layout.fragment_map, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        b_open_map.setOnClickListener({
            pickMap()
        })

        b_create_radio.setOnClickListener {
            mapView?.onCreateRadioClick()
        }

        b_remove_radio.setOnClickListener {
            mapView?.onDeleteRadioClick()
        }

        b_save_map.setOnClickListener {
            saveMap()
        }

        b_save_map_as.setOnClickListener {
            saveMapAs()
        }
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {
        if (mListener != null) {
            mListener!!.onFragmentInteraction(uri)
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException(context!!.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html) for more information.
     */
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        // TODO: Rename parameter arguments, choose names that match
        // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
        private val ARG_PARAM1 = "param1"
        private val ARG_PARAM2 = "param2"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment MapFragment.
         */
        // TODO: Rename and change types and number of parameters
        fun newInstance(param1: String, param2: String): MapFragment {
            val fragment = MapFragment()
            val args = Bundle()
            args.putString(ARG_PARAM1, param1)
            args.putString(ARG_PARAM2, param2)
            fragment.arguments = args
            return fragment
        }
    }

    private fun pickMap() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 0);
        }

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            return
        }

        val fileDialog = OpenFileDialog(activity)
        fileDialog.setFilter(".*((.mxn)|(.jpg)|(.png))$")
        fileDialog.setOpenDialogListener {
            loadMap(it)
        }
        fileDialog.show()
    }

    private fun loadMap(path: String) {


        try {
            val ext = getExtension(path).toLowerCase()
            when (ext) {
                ".mxn" -> loadFromXml(path)
                ".jpg" -> loadFromImage(path)
                ".jpeg" -> loadFromImage(path)
                ".png" -> loadFromImage(path)
                else -> return
            }

            showMapView()
        } catch (e: Exception) {
            activity.runOnUiThread({
                Toast.makeText(activity, resources.getString(R.string.could_not_open_file), Toast.LENGTH_LONG).show()
            })
            Log.d(this.javaClass.name, e.message)
            e.printStackTrace()
        }
    }

    private fun loadFromImage(path: String) {
        mapInfo = MapInfo.loadFromImage(path)
        savingStatus = SavingStatus.NewFile
    }

    private fun loadFromXml(path: String) {
        mapInfo = MapInfo.loadFromXml(path)
        savingStatus = SavingStatus.Saved
    }

    private fun getExtension(file: String): String {
        return file.substring(file.lastIndexOf('.'))
    }

    private fun saveMap() {
        when (savingStatus) {
            MapFragment.SavingStatus.Saved -> Unit
            MapFragment.SavingStatus.NewFile -> saveMapAs()
            MapFragment.SavingStatus.Unsaved -> {
                MapInfo.saveToXml(lastSavedFile, mapInfo)
                savingStatus = SavingStatus.Saved
            }
        }
    }

    private fun saveMapAs() {
        if (!::mapInfo.isInitialized) {
            return
        }
        val fileDialog = OpenFileDialog(activity)
        fileDialog.dialogType = OpenFileDialog.DialogType.NewFiles
        fileDialog.setFilter(".*\\.mxn")
        fileDialog.setOpenDialogListener {
            MapInfo.saveToXml(it, mapInfo)
            lastSavedFile = it
            savingStatus = SavingStatus.Saved
        }
        fileDialog.show()
    }

    private fun showMapView() {
        mapView?.let {
            container.removeView(it)
        }

        mapView = MapView(activity, mapInfo)
        mapView?.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        container.addView(mapView)
    }
}// Required empty public constructor
