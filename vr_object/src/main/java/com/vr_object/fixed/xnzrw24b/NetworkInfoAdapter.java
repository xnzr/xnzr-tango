package com.vr_object.fixed.xnzrw24b;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.vr_object.fixed.R;
import com.vr_object.fixed.xnzrw24b.NetworkInfoFragment.OnListFragmentInteractionListener;
import com.vr_object.fixed.xnzrw24b.data.NetworkInfo;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link NetworkInfo} and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class NetworkInfoAdapter extends RecyclerView.Adapter<NetworkInfoAdapter.ViewHolder> {

    private final List<NetworkInfo> mValues;
    private final OnListFragmentInteractionListener mListener;
    private RecyclerView mRecyclerView;
    private int mSelectedPosition = -1;

    public NetworkInfoAdapter(RecyclerView recyclerView, List<NetworkInfo> items, OnListFragmentInteractionListener listener) {
        mRecyclerView = recyclerView;
        mValues = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_networkinfo, parent,  false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.mSsidView.setText(mValues.get(position).Ssid);
        holder.mMacView.setText(mValues.get(position).Mac);

        if(mSelectedPosition == position) {
            // Here I am just highlighting the background
            holder.itemView.setBackgroundColor(Color.argb(80, 0, 200, 0));
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mSsidView;
        public final TextView mMacView;
        public NetworkInfo mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mSsidView = (TextView) view.findViewById(R.id.row_ssid);
            mMacView = (TextView) view.findViewById(R.id.row_mac);
            mMacView.setTextColor(mMacView.getTextColors().withAlpha(100));

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Redraw the old selection and the new
                    notifyItemChanged(mSelectedPosition);
                    mSelectedPosition = getLayoutPosition();
                    notifyItemChanged(mSelectedPosition);

                    if (null != mListener) {
                        // Notify the active callbacks interface (the activity, if the
                        // fragment is attached to one) that an item has been selected.
                        mListener.onListFragmentInteraction(mItem);
                    }
                }
            });
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mSsidView.getText() + ":" + mMacView.getText() + "'";
        }
    }
}
