package com.jio.jiotalkie.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.jio.jiotalkie.adapter.provider.CalendarUpdateProvider;
import com.jio.jiotalkie.dispatch.R;
import com.jio.jiotalkie.model.CalendarItem;

import java.util.ArrayList;
import java.util.List;

public class CalenderListAdapter extends RecyclerView.Adapter<CalenderListAdapter.CalenderListViewHolder>{

    private Context mContext;

    List<CalendarItem> mCalendarList = new ArrayList<>();

    List<CalendarItem> mMonthCalendarList = new ArrayList<>();

    List<CalendarItem> mYearCalendarList = new ArrayList<>();

    private int selectedItemPosition;

    private boolean isYearListSet = false;
    private CalendarUpdateProvider mCalendarUpdateProvider;

    public CalenderListAdapter(){

    }

    public CalenderListAdapter(List<CalendarItem> calendarList, boolean isYearList){
        mCalendarList=calendarList;
        if(isYearList){
            mYearCalendarList = calendarList;
        }else{
            mMonthCalendarList = calendarList;
        }
    }

    public void clear(){
        mCalendarList.clear();
        mMonthCalendarList.clear();
        mYearCalendarList.clear();
    }

    public List<CalendarItem> getMonthCalendarList() {
        return mMonthCalendarList;
    }

    public List<CalendarItem> getYearCalendarList() {
        return mYearCalendarList;
    }

    public List<CalendarItem> getCalendarList() {
        return mCalendarList;
    }

    @NonNull
    @Override
    public CalenderListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.calendar_list_item, parent, false);
        mContext = parent.getContext();
        return new CalenderListAdapter.CalenderListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CalenderListViewHolder holder, int position) {
        holder.listItem.setText(mCalendarList.get(position).getName());
        if(mCalendarList.get(position).isSelected()){
            selectedItemPosition=position;
            Drawable drawable = ContextCompat.getDrawable(mContext, R.drawable.bt_rounded_corner_blue);
            holder.listItem.setBackgroundDrawable(drawable);
            holder.listItem.setTextColor(Color.WHITE);
        }else{
            holder.listItem.setBackgroundDrawable(null);
            holder.listItem.setTextColor(mContext.getResources().getColor(R.color.dialog_text_1));
        }

        holder.listItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCalendarList.get(selectedItemPosition).setSelected(false);
                notifyItemChanged(selectedItemPosition);
                mCalendarList.get(position).setSelected(true);
                notifyItemChanged(position);
                selectedItemPosition=position;
                if(mCalendarUpdateProvider!=null){
                    mCalendarUpdateProvider.update(getSelectedItem());
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mCalendarList.size();
    }

    public int getSelectedItemPosition(){
        return selectedItemPosition;
    }

    public String getSelectedItem() {
        return mCalendarList.get(selectedItemPosition).getName();
    }

    public void setYearList(List<CalendarItem> list) {
        isYearListSet=true;
        mCalendarList=list;
        mYearCalendarList = list;

    }

    public void setMonthList(List<CalendarItem> list) {
        isYearListSet=false;
        mCalendarList=list;
        mMonthCalendarList = list;

    }

    public boolean isYearListSet(){
        return isYearListSet;
    }

    public void setCalendarUpdateProvider(CalendarUpdateProvider calendarUpdateProvider) {
        mCalendarUpdateProvider = calendarUpdateProvider;
    }

    public static class CalenderListViewHolder extends RecyclerView.ViewHolder {

        TextView listItem;

        public CalenderListViewHolder(@NonNull View itemView) {
            super(itemView);
            listItem = itemView.findViewById(R.id.calendarItem);
        }
    }
}
