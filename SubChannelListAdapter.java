package com.jio.jiotalkie.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.application.customservice.dataManagment.imodels.IChannelModel;
import com.jio.jiotalkie.dispatch.R;

import java.util.List;


public class SubChannelListAdapter extends RecyclerView.Adapter<SubChannelListAdapter.ItemViewHolder>{

    List<IChannelModel> mSubChannelList;

    private Context mContext;

    public SubChannelListAdapter(List<IChannelModel> subChannelList){
        mSubChannelList = subChannelList;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.subchannel_list_item, parent, false);
        mContext = parent.getContext();
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        holder.subChannelName.setText(mSubChannelList.get(position).getChannelName());
    }

    @Override
    public int getItemCount() {
        return mSubChannelList.size();
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {

        TextView subChannelName;
        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            subChannelName = itemView.findViewById(R.id.subChannel_Name);
        }
    }
}
