package com.jio.jiotalkie.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jio.jiotalkie.dataclass.UserLocationTimelineData;
import com.jio.jiotalkie.dispatch.R;

import java.util.ArrayList;

public class LocationTimelineAdapter extends RecyclerView.Adapter<LocationTimelineAdapter.LocationHistoryViewHolder>{

    private ArrayList<UserLocationTimelineData> mLocationTimelineList;
    private Context mContext;
    public LocationTimelineAdapter(Context context, ArrayList<UserLocationTimelineData> locationTimelineList) {
        this.mContext = context;
        this.mLocationTimelineList = locationTimelineList;
    }

    @NonNull
    @Override
    public LocationTimelineAdapter.LocationHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.location_timeline_row, parent, false);
        return new LocationTimelineAdapter.LocationHistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LocationTimelineAdapter.LocationHistoryViewHolder holder, int position) {
        UserLocationTimelineData userLocationTimelineData =  mLocationTimelineList.get(position);
        if (userLocationTimelineData.getBatteryLevel() != -1 && userLocationTimelineData.getBatteryLevel() <= 15) {
            holder.locationCount.setBackground(mContext.getDrawable(R.drawable.location_timeline_count_battery_low_background));
            holder.locationCount.setTextColor(mContext.getColor(R.color.white));
            holder.batteryLowIcon.setVisibility(View.VISIBLE);
            holder.locationReceiveTime.setTextColor(mContext.getColor(R.color.low_battery_level));
            holder.batteryCapacity.setVisibility(View.VISIBLE);
            holder.batteryCapacity.setText(mContext.getString(R.string.low_battery_capacity, userLocationTimelineData.getBatteryLevel()));
        } else {
            holder.locationCount.setBackground(mContext.getDrawable(R.drawable.location_timeline_count_background));
            holder.locationCount.setTextColor(mContext.getColor(R.color.black));
            holder.batteryLowIcon.setVisibility(View.GONE);
            holder.locationReceiveTime.setTextColor(mContext.getColor(R.color.black));
            holder.batteryCapacity.setVisibility(View.GONE);
        }

        holder.locationCount.setText(String.valueOf(position +1));
        holder.locationReceiveTime.setText(userLocationTimelineData.getReceivedTime().toString());
        holder.address.setText(userLocationTimelineData.getAddress());
    }

    @Override
    public int getItemCount() {
        return mLocationTimelineList.size();
    }

    public static class LocationHistoryViewHolder extends RecyclerView.ViewHolder {

        TextView locationReceiveTime;
        TextView locationCount, batteryCapacity;
        TextView address;
        ImageView batteryLowIcon;

        public LocationHistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            locationReceiveTime = itemView.findViewById(R.id.location_receive_time);
            locationCount = itemView.findViewById(R.id.location_count);
            batteryCapacity = itemView.findViewById(R.id.battery_capacity);
            address = itemView.findViewById(R.id.address);
            batteryLowIcon = itemView.findViewById(R.id.battery_low_icon);
        }
    }
}
