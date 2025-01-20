package com.jio.jiotalkie.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.jio.jiotalkie.adapter.provider.ProfileAdapterProvider;
import com.jio.jiotalkie.dispatch.R;
import com.jio.jiotalkie.util.EnumConstant;

import java.util.List;

public class ProfileAdapter extends RecyclerView.Adapter<ProfileAdapter.ProfileViewHolder>{

    private static String TAG = "ProfileAdapter";

    private Context mContext;

    private List<String> profileList;

    private ProfileAdapterProvider mProfileProvider;

    public ProfileAdapter(List<String> list, ProfileAdapterProvider provider) {
        profileList = list;
        mProfileProvider=provider;
    }

    @NonNull
    @Override
    public ProfileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.profile_row_item, parent, false);
        mContext = parent.getContext();
        return new ProfileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProfileViewHolder holder, int position) {
        Log.d(TAG,"onBindViewHolder list "+profileList.toString());
        holder.profileItemName.setText(profileList.get(position));
        if(profileList.get(position).equals(mContext.getResources().getString(R.string.logout))||profileList.get(position).equals(mContext.getResources().getString(R.string.update)) ){
            holder.profileItemName.setCompoundDrawablesWithIntrinsicBounds(0,0,0,0);
        }
        holder.profileItemLayout.setOnClickListener(view -> {
            if(profileList.get(position).equals(mContext.getResources().getString(R.string.billing))){
                mProfileProvider.onItemClick(EnumConstant.getSupportedFragment.BILLING_FRAGMENT.ordinal());
            }else if(profileList.get(position).equals(mContext.getResources().getString(R.string.dispatcher_role))){
                mProfileProvider.onItemClick(EnumConstant.getSupportedFragment.DISPATCHER_ROLE_FRAGMENT.ordinal());
            }else if(profileList.get(position).equals(mContext.getResources().getString(R.string.about_app))){
                mProfileProvider.onItemClick(EnumConstant.getSupportedFragment.DISPATCHER_ABOUT_APP_FRAGMENT.ordinal());
            }else if(profileList.get(position).equals(mContext.getResources().getString(R.string.help))){
                mProfileProvider.onItemClick(EnumConstant.getSupportedFragment.HELP_FRAGMENT.ordinal());
            }else if(profileList.get(position).equals(mContext.getResources().getString(R.string.privacy_policy))){
                mProfileProvider.onItemClick(EnumConstant.getSupportedFragment.PRIVACY_POLICY_FRAGMENT.ordinal());
            }else if(profileList.get(position).equals(mContext.getResources().getString(R.string.update))){
                mProfileProvider.onItemClick(EnumConstant.UPDATE);
            }else if(profileList.get(position).equals(mContext.getResources().getString(R.string.logout))){
                mProfileProvider.onItemClick(EnumConstant.LOGOUT);
            }

        });
    }

    @Override
    public int getItemCount() {
        Log.d(TAG,"getItemCount list "+profileList.size()+" list "+profileList);
        return profileList.size();
    }

    public static class ProfileViewHolder extends RecyclerView.ViewHolder {

        TextView profileItemName;

        CardView profileItemLayout;

        public ProfileViewHolder(@NonNull View itemView) {
            super(itemView);
            profileItemName = itemView.findViewById(R.id.profile_item);
            profileItemLayout = itemView.findViewById(R.id.dispatcher_profile_item);
        }
    }
}
