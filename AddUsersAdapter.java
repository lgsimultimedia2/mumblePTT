package com.jio.jiotalkie.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jio.jiotalkie.dispatch.R;
import com.jio.jiotalkie.model.AddUserModel;
import com.jio.jiotalkie.util.BitmapUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


public class AddUsersAdapter extends RecyclerView.Adapter<AddUsersAdapter.ItemViewHolder> {

    public interface AdapterInterface {
        void refreshSelected(int totalCount, int selected, List<AddUserModel> selectedUserList);
    }
    private Context mContext;
    private final List<AddUserModel> mRootUsers;

    private  List<AddUserModel> mFilterUsers;

    private final AdapterInterface mAdapterInterface;

    public AddUsersAdapter(List<AddUserModel> addUserModelList , AdapterInterface adapterInterface) {
        mAdapterInterface = adapterInterface;
        mRootUsers = addUserModelList;
        mFilterUsers = addUserModelList;
        mAdapterInterface.refreshSelected(mRootUsers.size(),0, Collections.emptyList());
        sort();
    }
    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.add_user_row_item, parent, false);
        mContext = parent.getContext();
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        AddUserModel user = mFilterUsers.get(position);
        holder.userIcon.setText(String.valueOf(user.getUserName().charAt(0)));
        BitmapUtils.setRandomBgColor(mContext, holder.userIcon, user.isOnline());
        holder.title.setText(user.getUserName());
        holder.checkBox.setOnCheckedChangeListener((compoundButton, selected) -> {
            user.setSelected(selected);
            mAdapterInterface.refreshSelected(mRootUsers.size(),getSelectedUsers().size(), getSelectedUsers());
        });
        holder.checkBox.setChecked(user.isSelected());
        if (user.isOnline()) {
            holder.status.setText(mContext.getString(R.string.status_online));
            holder.status.setCompoundDrawablesWithIntrinsicBounds(R.drawable.online_icon, 0, 0, 0);
        } else {
            holder.status.setText(mContext.getString(R.string.status_offline));
            holder.status.setCompoundDrawablesWithIntrinsicBounds(R.drawable.offline_icon, 0, 0, 0);
        }
    }

    @Override
    public int getItemCount() {
        return mFilterUsers.size();
    }

    public List<AddUserModel> getSelectedUsers() {
        return mRootUsers.stream()
                .filter(AddUserModel::isSelected)
                .collect(Collectors.toList());
    }
    public void clearSelection() {
        for(int i=0 ;i < mRootUsers.size(); i++){
            mRootUsers.get(i).setSelected(false);
        }
        mFilterUsers = mRootUsers;
        mAdapterInterface.refreshSelected(mRootUsers.size(),0, Collections.emptyList());
        refreshList();
    }
    private void refreshList (){
        sort();
        notifyDataSetChanged();
    }

    public void applyFilter(String filterStr) {
        if (TextUtils.isEmpty(filterStr)) {
            mFilterUsers = mRootUsers;
        } else {
            mFilterUsers = mRootUsers.stream()
                    .filter(user -> user.getUserName().toLowerCase().contains(filterStr.toLowerCase()))
                    .collect(Collectors.toList());
        }
        refreshList();
    }

    public void sort() {
        mFilterUsers.sort((user1, user2) -> {
            if (user1.isOnline() == user2.isOnline()) {
                // sort by name if status is same
                return user1.getUserName().compareToIgnoreCase(user2.getUserName());
            } else {
                // sort by status
                return Boolean.compare(!user1.isOnline(), !user2.isOnline());
            }
        });
    }
    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView userIcon;
        TextView title;
        CheckBox checkBox;
        TextView status;
        public ItemViewHolder(View itemView) {
            super(itemView);
            userIcon = itemView.findViewById(R.id.user_icon);
            title = itemView.findViewById(R.id.item_name);
            checkBox = itemView.findViewById(R.id.check_icon);
            status = itemView.findViewById(R.id.status_info);
        }

    }

}
