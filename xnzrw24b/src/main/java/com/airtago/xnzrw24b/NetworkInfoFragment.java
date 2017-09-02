package com.airtago.xnzrw24b;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

//import com.airtago.R;
import com.airtago.xnzrw24b.data.NetworkInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class NetworkInfoFragment extends Fragment {

    private OnListFragmentInteractionListener mListener;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public NetworkInfoFragment() {
    }

    private List<NetworkInfo> networks = new ArrayList<>();
    private NetworkInfoAdapter mAdapter;

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static NetworkInfoFragment newInstance(OnListFragmentInteractionListener listener) {
        NetworkInfoFragment fragment = new NetworkInfoFragment();
        fragment.mListener = listener;
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public void addInfo(WFPacket packet) {
        NetworkInfo networkInfo = null;
        for (NetworkInfo netInfo: networks) {
            if (netInfo.Ssid.equals(packet.apName) && netInfo.Mac.equals(packet.mac)) {
                networkInfo = netInfo;
            }
        }

        if (networkInfo == null) {
            networkInfo = new NetworkInfo(packet);
            networks.add(networkInfo);
            mAdapter.notifyDataSetChanged();
        }
        networkInfo.addChannel(packet);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            //mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_networkinfo_list, container, false);

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) view;
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            mAdapter = new NetworkInfoAdapter(recyclerView, networks, mListener);
            recyclerView.setAdapter(mAdapter);
        }
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        void onListFragmentInteraction(NetworkInfo item);
    }
}
