package com.jio.jiotalkie.adapter;

import static android.graphics.Color.rgb;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.jio.jiotalkie.dispatch.R;
import com.jio.jiotalkie.dataclass.RegisteredUser;

public class ChannelUserAdapter extends RecyclerView.Adapter<ChannelUserAdapter.ViewHolder> {

    // Sort the list by name
    public void sortByName() {
        Collections.sort(mRegUserList, (registeredUser, t1)
                -> registeredUser.getName().compareToIgnoreCase(t1.getName()));
    }

    // Sort list by status first and then by name
    public void sortByStatus() {
        Collections.sort(mRegUserList, new Comparator<RegisteredUser>() {
            @Override
            public int compare(RegisteredUser registeredUser, RegisteredUser t1) {
                if (registeredUser.getOnlineStatus() == t1.getOnlineStatus()) {
                    // sort by name if status is same
                    return registeredUser.getName().compareToIgnoreCase(t1.getName());
                } else {
                    // sort by status
                    return Boolean.compare(!registeredUser.getOnlineStatus(), !t1.getOnlineStatus());
                }
            }
        });
    }

    public void clear() {
        mRegUserList.clear();
    }

    public interface UserAdapterProvider {
        void onMessageOptionClick(RegisteredUser user);
        void onLocateOptionClick();
        void onLocateUser(RegisteredUser user);
    }

    private static final String TAG = ChannelUserAdapter.class.getName();
    private List<RegisteredUser> mRegUserList;
    private String mSessionUser;
    private UserAdapterProvider muserAdapterProvider;

    public ChannelUserAdapter(UserAdapterProvider userAdapterProvider, String sessionUser) {
        muserAdapterProvider = userAdapterProvider;
        mSessionUser = sessionUser;
        mRegUserList = new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View userViewList = inflater.inflate(R.layout.list_item_user_channel, parent, false);
        ViewHolder viewHolder = new ViewHolder(userViewList);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        RegisteredUser user = mRegUserList.get(position);
        String name = user.getName();
        holder.mUserName.setText(name);
        String firstName = name.substring(0, 1).toUpperCase();
        holder.mUserNameHighlight.setText(firstName);

        // Disable 3 dot option menu for self
        if (name.equals(mSessionUser)) {
            holder.mOptionsButton.setEnabled(false);
            holder.mOptionsButton.setColorFilter(rgb(128,128,128));
        } else {
            holder.mOptionsButton.setColorFilter(rgb(0,0,0));
        }

        // set online status / last seen
        if (user.getOnlineStatus()) {
            holder.mOnlineIcon.setVisibility(View.VISIBLE);
            holder.mOnlineStatus.setVisibility(View.VISIBLE);
            holder.mOfflineIcon.setVisibility(View.GONE);
            holder.mLastSeen.setVisibility(View.GONE);
            settingLayoutMargin(holder,-20);
        } else {
            holder.mOnlineIcon.setVisibility(View.GONE);
            holder.mOnlineStatus.setVisibility(View.GONE);
            holder.mOfflineIcon.setVisibility(View.VISIBLE);
            if (user.getLastSeen() != null && !((user.getLastSeen()).isEmpty())) {
                String str = "Last seen" + " - " + getFormattedLastSeen(user.getLastSeen()) + " ago";
                holder.mLastSeen.setText(str);
            } else {
                holder.mLastSeen.setText(R.string.status_offline);
            }
            holder.mLastSeen.setVisibility(View.VISIBLE);
            settingLayoutMargin(holder,25);
        }

        holder.mOptionsButton.setOnClickListener(view -> {
            muserAdapterProvider.onLocateUser(user);
            PopupMenu popupMenu = new PopupMenu(view.getContext(), holder.mOptionsButton);
            popupMenu.getMenuInflater().inflate(R.menu.user_menu, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.message_button:
                        muserAdapterProvider.onMessageOptionClick(user);
                        return true;
                    case R.id.locate_button:
                        muserAdapterProvider.onLocateOptionClick();
                        return true;
                    default:
                        return false;
                }
            });
            popupMenu.show();
        });
    }

    private String getFormattedLastSeen(String date) {
        String oldDate = date.replaceAll("[TZ]", "");
        String currentDate = new SimpleDateFormat("yyyy-MM-ddHH:mm:ss")
                .format(Calendar.getInstance().getTime());
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-ddHH:mm:ss");
        Date date1 = null;
        Date date2 = null;
        try {
            date1 = format.parse(oldDate);
            date2 = format.parse(currentDate);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        long diff = date2.getTime() - date1.getTime();
        long months = diff / (24 * 60 * 60 * 1000 * 31);
        long weeks = diff / (24 * 60 * 60 * 1000 * 7);
        long days = diff / (24 * 60 * 60 * 1000);
        long hours = diff / (60 * 60 * 1000) % 24;
        long minutes = diff / (60 * 1000) % 60;
        long seconds = diff / 1000 % 60;

        if (months != 0) {
            if (months > 1) {
                return months + " months";
            } else {
                return months + " month";
            }
        } else if (weeks != 0) {
            if (weeks > 1) {
                return weeks + " weeks";
            } else {
                return weeks + " week";
            }
        } else if (days != 0) {
            if (days > 1) {
                return days + " days";
            } else {
                return days + " day";
            }
        } else if (hours != 0) {
            if (hours > 1) {
                return hours + " hours";
            } else {
                return hours + " hour";
            }
        } else if (minutes != 0) {
            if (minutes > 1) {
                return minutes + " mins";
            } else {
                return minutes + " min";
            }
        } else if (seconds != 0) {
            if (seconds > 1) {
                return seconds + " seconds";
            } else {
                return seconds + " second";
            }
        }

        return "";
    }

    private void settingLayoutMargin(@NonNull ViewHolder holder, int margin) {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.mUserName
                .getLayoutParams();
        params.setMarginStart(margin);
        holder.mUserName.setLayoutParams(params);
    }

    @Override
    public int getItemCount() {
        return mRegUserList != null ? mRegUserList.size() : 0;
    }

    public void add(RegisteredUser regUser) {
        mRegUserList.add(regUser);
    }

    public List<RegisteredUser> getCurrentRegUserList() {
        return mRegUserList;
    }

    public void remove(int index) {
        mRegUserList.remove(index);
    }

    public void updateUserList(List<RegisteredUser> list) {
        this.mRegUserList = list;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public RelativeLayout mUserHolder;
        public TextView mUserName;
        public TextView mUserNameHighlight;
        public ImageView mOptionsButton;
        public TextView mLastSeen;
        public TextView mOnlineStatus;
        public ImageView mOfflineIcon;
        public ImageView mOnlineIcon;

        public ViewHolder(View itemView) {
            super(itemView);

            mUserHolder = (RelativeLayout) itemView.findViewById(R.id.text_user_row_title);
            mUserNameHighlight = (TextView) itemView.findViewById(R.id.user_row_name_highlight);
            mUserName = (TextView) itemView.findViewById(R.id.user_row_name);
            mOptionsButton = (ImageView) itemView.findViewById(R.id.user_options_icon);
            mLastSeen = (TextView) itemView.findViewById(R.id.tv_last_seen);
            mOnlineStatus = (TextView) itemView.findViewById(R.id.tv_online_status);
            mOfflineIcon = (ImageView) itemView.findViewById(R.id.iv_offline_status);
            mOnlineIcon = (ImageView) itemView.findViewById(R.id.iv_online_status);
        }
    }
}