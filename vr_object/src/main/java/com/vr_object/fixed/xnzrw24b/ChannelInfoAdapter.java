package com.vr_object.fixed.xnzrw24b;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.vr_object.fixed.R;
import com.vr_object.fixed.xnzrw24b.ChannelInfoFragment.OnListFragmentInteractionListener;
import com.vr_object.fixed.xnzrw24b.data.ChannelInfo;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link ChannelInfo} and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class ChannelInfoAdapter extends RecyclerView.Adapter<ChannelInfoAdapter.ViewHolder> {

    private final List<ChannelInfo> mValues;
    private final OnListFragmentInteractionListener mListener;
    private RecyclerView mRecyclerView;
    private int mSelectedPosition = -1;

    public void clearSelection() {
        mSelectedPosition = -1;
    }

    public ChannelInfoAdapter(RecyclerView recyclerView, List<ChannelInfo> items, OnListFragmentInteractionListener listener) {
        mRecyclerView = recyclerView;
        mValues = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_channelinfo, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.mChannel.setText("Channel=" + Integer.toString(mValues.get(position).Channel, 10));
        holder.mRssi1.setText(String.format("Rssi1=%.2f", mValues.get(position).AvgRssi1()));
        holder.mRssi2.setText(String.format("Rssi2=%.2f", mValues.get(position).AvgRssi2()));

        if(mSelectedPosition == position) {
            // Here I am just highlighting the background
            holder.itemView.setBackgroundColor(Color.GREEN);
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
        public final TextView mChannel;
        public final TextView mRssi1;
        public final TextView mRssi2;
        public ChannelInfo mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mChannel = (TextView) view.findViewById(R.id.row_channel);
            mRssi1 = (TextView) view.findViewById(R.id.row_rssi1);
            mRssi2 = (TextView) view.findViewById(R.id.row_rssi2);

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
            return super.toString() + " '" + mChannel.getText() + " " + mRssi1.getText() + ":" + mRssi2.getText() + "'";
        }
    }
}
