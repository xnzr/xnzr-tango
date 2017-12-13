package com.vr_object.fixed.xnzrw24b;

import android.content.Context;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.vr_object.fixed.R;
import com.vr_object.fixed.xnzrw24b.data.ChannelInfo;
import com.vr_object.fixed.xnzrw24b.data.NetworkInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class ChannelInfoFragment extends Fragment {

    // TODO: Customize parameters
    private OnListFragmentInteractionListener mListener;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ChannelInfoFragment() {
    }

    private List<ChannelInfo> mChannels = new ArrayList<>();
    private ChannelInfoAdapter mAdapter;

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static ChannelInfoFragment newInstance(OnListFragmentInteractionListener listener) {
        ChannelInfoFragment fragment = new ChannelInfoFragment();
        fragment.mListener = listener;
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public void setNetwork(NetworkInfo info) {
        mAdapter.clearSelection();
        mChannels.clear();
        for (ChannelInfo chan: info.Channels) {
            mChannels.add(chan);
        }
        mAdapter.notifyDataSetChanged();
    }

    public void addInfo(PacketFromDevice packet) {
        for (int i = 0; i < mChannels.size(); ++i) {
            if (mChannels.get(i).Channel == packet.wifiCh) {
                mAdapter.notifyItemChanged(i);
                break;
            }
        }
    }

    public void clearList() {
        mChannels.clear();
        mAdapter.notifyDataSetChanged();
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
        View view = inflater.inflate(R.layout.fragment_channelinfo_list, container, false);

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) view;
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            mAdapter = new ChannelInfoAdapter(recyclerView, mChannels, mListener);
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
        void onListFragmentInteraction(ChannelInfo item);
    }
}
