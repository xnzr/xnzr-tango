package com.vr_object.fixed.xnzrw24b;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.vr_object.fixed.OptionsHolder;
import com.vr_object.fixed.R;
import com.vr_object.fixed.xnzrw24b.NetworkInfoFragment.OnListFragmentInteractionListener;
import com.vr_object.fixed.xnzrw24b.data.GlobalSettings;
import com.vr_object.fixed.xnzrw24b.data.NetworkInfo;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import static android.content.Context.MODE_PRIVATE;

public class NetworkInfoAdapter extends RecyclerView.Adapter<NetworkInfoAdapter.ViewHolder> {

    private final List<NetworkInfo> mValues;
    private final OnListFragmentInteractionListener mListener;
    private RecyclerView mRecyclerView;
    private int mSelectedPosition = -1;
    private CopyOnWriteArrayList<ViewHolder> mViewHolders = new CopyOnWriteArrayList<>();

    NetworkInfoAdapter(RecyclerView recyclerView, List<NetworkInfo> items, OnListFragmentInteractionListener listener) {
        mRecyclerView = recyclerView;
        mValues = items;
        mListener = listener;
    }

    void updateNameBLE(String mac, String name) {
        if (GlobalSettings.getMode() != GlobalSettings.WorkMode.BLE) {
            return;
        }

        if (name == null) {
            return;
        }

        if (name.trim().equals("")) {
            return;
        }

        for (ViewHolder wh: mViewHolders) {
            if (wh.mSsidView.getText().equals(mac)) { //This means that BLE is unnamed
                wh.mSsidView.setText(name);
                wh.mMacView.setText(mac);
            }
        }
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_networkinfo, parent,  false);

        ViewHolder wh = new ViewHolder(view);
        mViewHolders.add(wh);
        return wh;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);

        switch (GlobalSettings.getMode()) {
            case WIFI:
                holder.mSsidView.setText(mValues.get(position).Ssid);
                holder.mMacView.setText(mValues.get(position).Mac);
                break;
            case BLE:
                //TODO: rename mSsidView and mMacView
                if (Objects.equals(holder.mItem.BleName, "") || holder.mItem.BleName == null) {
                    holder.mSsidView.setText(holder.mItem.Mac);
                    holder.mMacView.setText(holder.mItem.Ssid);
                } else {
                    holder.mSsidView.setText(holder.mItem.BleName);
                    holder.mMacView.setText(holder.mItem.Mac);
                }
                break;
        }


        if(mSelectedPosition == position) {
            // Here I am just highlighting the background
            switch (GlobalSettings.getMode()) {
                case WIFI:
                    holder.itemView.setBackgroundColor(Color.argb(80, 0, 200, 0));
                    break;
                case BLE:
                    holder.itemView.setBackgroundColor(Color.argb(80, 0x1D, 0xB3, 0xE7));
                    break;
            }
        } else {
//            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
            holder.itemView.setBackgroundColor(Color.argb(70, 200, 200, 200));
        }
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    void tryLoadLastSelected() {
        SharedPreferences mPref = mRecyclerView.getContext().getSharedPreferences(OptionsHolder.OPTIONS_NAME, MODE_PRIVATE);
        boolean save = mPref.getBoolean(OptionsHolder.SAVE_SELECTED_ID_KEY, OptionsHolder.DEFAULT_SAVE_SELECTED_ID);

        //we want load last selection if and only if saving is on and signal source did not selected.
        if (!save || mSelectedPosition != -1) {
            return;
        }

        String name = mPref.getString(OptionsHolder.SAVED_ID_KEY, OptionsHolder.DEFAULT_SAVED_ID);
        if (!Objects.equals(name, OptionsHolder.DEFAULT_SAVED_ID)) {
            for (int i = 0; i < mValues.size(); i++) {
                NetworkInfo ni = mValues.get(i);
                if (ni.Mac.equals(name) || ni.Ssid.equals(name)) {
                    mSelectedPosition = i;
                    notifyItemChanged(mSelectedPosition);
                    if (null != mListener) {
                        mListener.onListFragmentInteraction(ni);
                    }
                    break;
                }
            }
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final View mView;
        final TextView mSsidView;
        final TextView mMacView;
        NetworkInfo mItem;

        ViewHolder(View view) {
            super(view);
            mView = view;
            mSsidView = (TextView) view.findViewById(R.id.row_ssid);
            mMacView = (TextView) view.findViewById(R.id.row_mac);
            //mMacView.setTextColor(mMacView.getTextColors().withAlpha(100));

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Redraw the old selection and the new
                    notifyItemChanged(mSelectedPosition);
                    mSelectedPosition = getLayoutPosition();    
                    notifyItemChanged(mSelectedPosition);
                    saveSelectedId();

                    if (null != mListener) {
                        // Notify the active callbacks interface (the activity, if the
                        // fragment is attached to one) that an item has been selected.
                        mListener.onListFragmentInteraction(mItem);
                    }
                }
            });
        }



        private void saveSelectedId() {
            if (mSelectedPosition < 0 || mSelectedPosition >= mValues.size()) {
                return;
            }
            SharedPreferences mPref = mRecyclerView.getContext().getSharedPreferences(OptionsHolder.OPTIONS_NAME, MODE_PRIVATE);
            boolean save = mPref.getBoolean(OptionsHolder.SAVE_SELECTED_ID_KEY, OptionsHolder.DEFAULT_SAVE_SELECTED_ID);
            if (save) {
                NetworkInfo info = mValues.get(mSelectedPosition);
                SharedPreferences.Editor e = mPref.edit();
                e.putString(OptionsHolder.SAVED_ID_KEY, info.getId());
                e.apply();
            }
        }



        @Override
        public String toString() {
            return super.toString() + " '" + mSsidView.getText() + ":" + mMacView.getText() + "'";
        }
    }
}
