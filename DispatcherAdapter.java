package com.jio.jiotalkie.adapter;


import static com.jio.jiotalkie.util.Constants.LATITUDE;
import static com.jio.jiotalkie.util.Constants.USER_GEOFENCE_PREFERENCE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Outline;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.jio.jiotalkie.activity.DashboardActivity;
import com.jio.jiotalkie.adapter.provider.UserAdapterProvider;
import com.jio.jiotalkie.dispatch.BuildConfig;
import com.jio.jiotalkie.dispatch.R;
import com.jio.jiotalkie.model.DispatcherItemModel;
import com.jio.jiotalkie.util.BitmapUtils;
import com.jio.jiotalkie.util.CommonUtils;
import com.jio.jiotalkie.util.EnumConstant;
import com.jio.jiotalkie.util.UserPinnedStateHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.application.customservice.Mumble;


public class DispatcherAdapter extends RecyclerView.Adapter<DispatcherAdapter.ItemViewHolder> {
    private List<DispatcherItemModel> mItemModelList;

    private List<DispatcherItemModel> mBaseItemModeList;
    private Context mContext;

    private int mTotalRegUsers;

    private int mOnlineRegUsers;

    private UserAdapterProvider mUserAdapterProvider;

    private String mChannelName;
    private int mChannelId;
    private PopupMenu mUserPopupMenu;
    private PopupMenu mChannelPopupMenu;

    private boolean isSubChannelAvailable;

    private DashboardActivity mActivity;

    public DispatcherAdapter(DashboardActivity activity, UserAdapterProvider userAdapterProvider, List<DispatcherItemModel> list, boolean isSubChannelAvailable) {
        mItemModelList = list;
        mBaseItemModeList = new ArrayList<>(mItemModelList);
        mUserAdapterProvider = userAdapterProvider;
        this.isSubChannelAvailable=isSubChannelAvailable;
        mActivity = activity;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.dispatcher_row_item, parent, false);
        mContext = parent.getContext();
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        DispatcherItemModel model = mItemModelList.get(position);
        holder.bind(mContext, model, mUserAdapterProvider);
        updateCategory(holder, model);
    }

    @Override
    public int getItemCount() {
        return mItemModelList.size();
    }

    public void clear() {
        mBaseItemModeList.clear();
        mItemModelList.clear();
    }

    public void add(DispatcherItemModel item) {
        mBaseItemModeList.add(item);
        mItemModelList.add(item);
    }

    public void setTotalRegUsers(int count) {
        this.mTotalRegUsers = count;
    }

    public void setOnlineRegUsers(int count) {
        this.mOnlineRegUsers = count;
    }

    public void setChannel(String channelName, int channelId) {
        mChannelName = channelName;
        mChannelId = channelId;
    }
    private void showChannelPopUpMenu(View view) {
        Context wrapper = new ContextThemeWrapper(mContext, R.style.CustomPopupMenu);
        mChannelPopupMenu = new PopupMenu(wrapper, view);
        SharedPreferences sharedPreferences = mActivity.getSharedPreferences(USER_GEOFENCE_PREFERENCE, Context.MODE_PRIVATE);
        MenuInflater inflater = mChannelPopupMenu.getMenuInflater();
        inflater.inflate(R.menu.dispatcher_channel_menu, mChannelPopupMenu.getMenu());
        MenuItem menuItem = mChannelPopupMenu.getMenu().findItem(R.id.sub_channel_list);
        mChannelPopupMenu.getMenu().findItem(R.id.geo_fences).setVisible(true);
        if (sharedPreferences.contains(LATITUDE)) {
            mChannelPopupMenu.getMenu().findItem(R.id.geo_fences).setTitle(R.string.update_geo_fence);
        } else {
            mChannelPopupMenu.getMenu().findItem(R.id.geo_fences).setTitle(R.string.set_geo_fence);
        }
        if (BuildConfig.BUILD_TYPE.contains("devpreprod")) {
            mChannelPopupMenu.getMenu().findItem(R.id.mark_attendance).setVisible(true);
        } else {
            mChannelPopupMenu.getMenu().findItem(R.id.mark_attendance).setVisible(false);
        }

        menuItem.setVisible(isSubChannelAvailable);
        // if Dispatcher user belongs to ROOT channel i.e ChannelModel id = 0, then Add user context menu hide.
        mChannelPopupMenu.getMenu().findItem(R.id.add_users).setVisible(mChannelId != EnumConstant.ROOT_CHANNEL_ID);

        mChannelPopupMenu.setForceShowIcon(true);
        mChannelPopupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.add_users) {
                mUserAdapterProvider.onAddUsers();
                return true;
            }else if(item.getItemId() == R.id.add_sub_channel){
                mActivity.loadInnerFragment(EnumConstant.getSupportedFragment.DISPATCHER_SUB_CHANNEL_FRAGMENT.ordinal(), null);
                return true;
            }else if(item.getItemId() == R.id.sub_channel_list){
                mActivity.loadInnerFragment(EnumConstant.getSupportedFragment.DISPATCHER_SUB_CHANNEL_LIST.ordinal(),null);
                return true;
            } else if (item.getItemId() == R.id.message_multiple_users) {
                mActivity.loadInnerFragment(EnumConstant.getSupportedFragment.MULTIPLE_MESSAGE_FRAGMENT.ordinal(), null);
            } else if (item.getItemId() == R.id.geo_fences) {
                mActivity.loadInnerFragment(EnumConstant.getSupportedFragment.GEO_FENCE_FRAGMENT.ordinal(), null);
                return true;
            } else if (item.getItemId() == R.id.mark_attendance) {
                if (sharedPreferences.contains(LATITUDE)) {
                    mActivity.loadInnerFragment(EnumConstant.getSupportedFragment.MARK_ATTENDANCE.ordinal(), null);
                } else {
                    Toast.makeText(mActivity, "Please contact to administrator to set boundary for attendance marking", Toast.LENGTH_SHORT).show();
                }
            }
            return false;
        });
        mChannelPopupMenu.setOnDismissListener(menu -> mUserAdapterProvider.onUserContextMenu(false));
        mChannelPopupMenu.show();
        mUserAdapterProvider.onUserContextMenu(true);
    }

    private void showUserPopUpMenu(View view, DispatcherItemModel model) {
        Context wrapper = new ContextThemeWrapper(mContext, R.style.CustomPopupMenu);
        mUserPopupMenu = new PopupMenu(wrapper, view);
        MenuInflater inflater = mUserPopupMenu.getMenuInflater();
        inflater.inflate(R.menu.dispatcher_user_menu, mUserPopupMenu.getMenu());
        mUserPopupMenu.setForceShowIcon(true);
        mUserPopupMenu.getMenu().findItem(R.id.mute_menu).setVisible(false);
        mUserPopupMenu.getMenu().findItem(R.id.deafen_menu).setVisible(false);
        mUserPopupMenu.getMenu().findItem(R.id.kick_menu).setVisible(false);
        if (model.isMute()) {
            mUserPopupMenu.getMenu().findItem(R.id.mute_menu).setTitle(mContext.getString(R.string.audio_unmute));
            mUserPopupMenu.getMenu().findItem(R.id.mute_menu).setIcon(R.drawable.ic_unmute_menu);
        } else {
            mUserPopupMenu.getMenu().findItem(R.id.mute_menu).setTitle(mContext.getString(R.string.audio_mute));
            mUserPopupMenu.getMenu().findItem(R.id.mute_menu).setIcon(R.drawable.ic_mute_menu);
        }
        if (model.isDeafen()) {
            mUserPopupMenu.getMenu().findItem(R.id.mute_menu).setTitle(mContext.getString(R.string.audio_unmute));
            mUserPopupMenu.getMenu().findItem(R.id.mute_menu).setIcon(R.drawable.ic_unmute_menu);
            mUserPopupMenu.getMenu().findItem(R.id.deafen_menu).setTitle(mContext.getString(R.string.audio_undeafen));
            mUserPopupMenu.getMenu().findItem(R.id.deafen_menu).setIcon(R.drawable.ic_undeafen_menu);
        } else {
            mUserPopupMenu.getMenu().findItem(R.id.deafen_menu).setTitle(mContext.getString(R.string.audio_deafen));
            mUserPopupMenu.getMenu().findItem(R.id.deafen_menu).setIcon(R.drawable.ic_deafen_menu);
        }
        if (model.isPinned()) {
            mUserPopupMenu.getMenu().findItem(R.id.pin_menu).setTitle(mContext.getString(R.string.unpin));
            mUserPopupMenu.getMenu().findItem(R.id.pin_menu).setIcon(R.drawable.ic_unpin_menu);
        } else {
            mUserPopupMenu.getMenu().findItem(R.id.pin_menu).setTitle(mContext.getString(R.string.pin));
            mUserPopupMenu.getMenu().findItem(R.id.pin_menu).setIcon(R.drawable.ic_pin_menu);
        }
        // Context menu : Mute, Deafen and Kick are only applicable for Normal user.
        if (model.getUserRole() == Mumble.UserState.UserRole.Normal.getNumber()) {
            // Below menu shown only for online users
            mUserPopupMenu.getMenu().findItem(R.id.mute_menu).setVisible(model.isHasOnline());
            mUserPopupMenu.getMenu().findItem(R.id.deafen_menu).setVisible(model.isHasOnline());
            mUserPopupMenu.getMenu().findItem(R.id.kick_menu).setVisible(true);

        } else {
            mUserPopupMenu.getMenu().findItem(R.id.mute_menu).setVisible(false);
            mUserPopupMenu.getMenu().findItem(R.id.deafen_menu).setVisible(false);
            mUserPopupMenu.getMenu().findItem(R.id.kick_menu).setVisible(false);
        }
        // Context menu : "Locate" option only applicable for Normal user and dispatcher user.
        mUserPopupMenu.getMenu().findItem(R.id.locate_menu).
                setVisible(model.getUserRole() != Mumble.UserState.UserRole.CompanyAdmin.getNumber());
        // Context menu : "Push to Talk" option only applicable for online users.
        mUserPopupMenu.getMenu().findItem(R.id.push_to_talk).setVisible(model.isHasOnline());

        mUserPopupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.locate_menu:
                    mUserAdapterProvider.onLocateUser(model);
                    return true;
                case R.id.push_to_talk:
                    mUserAdapterProvider.onPushToTalk(model.getName(), model.getUserId());
                    return true;
                case R.id.mute_menu:
                    mUserAdapterProvider.onMute(model);
                    return true;
                case R.id.deafen_menu:
                    mUserAdapterProvider.onDeafen(model);
                    return true;
                case R.id.kick_menu:
                    mUserAdapterProvider.onKick(model);
                    return true;
                case R.id.pin_menu:
                    setPinned(model, !model.isPinned());
                    return true;
                default:
                    return false;
            }
        });
        mUserPopupMenu.setOnDismissListener(menu -> mUserAdapterProvider.onUserContextMenu(false));
        mUserPopupMenu.show();
        mUserAdapterProvider.onUserContextMenu(true);
    }
    private void setPinned(DispatcherItemModel model, boolean isPinned){
        UserPinnedStateHandler.getInstance().setPinned(model.getUserId(), isPinned);
        model.setPinned(isPinned);
        refreshUserList();
        String pinMsg = isPinned ? mContext.getResources().getString(R.string.user_pinned, model.getName())
                : mContext.getResources().getString(R.string.user_unpinned, model.getName());
        Toast.makeText(mContext, pinMsg, Toast.LENGTH_SHORT).show();
    }

    public void dismissUserPopupMenu() {
        if (mUserPopupMenu != null) {
            mUserPopupMenu.dismiss();
        }
        if (mChannelPopupMenu != null) {
            mChannelPopupMenu.dismiss();
        }
    }

    private void updateCategory(ItemViewHolder holder, DispatcherItemModel model) {
        if (model.getType() == DispatcherItemModel.ItemType.CHANNEL) {
            holder.userIconTv.setVisibility(View.GONE);
            holder.userImageTv.setVisibility(View.GONE);
            holder.categoryIcon.setVisibility(View.VISIBLE);
            holder.menuIcon.setImageResource(R.drawable.menu_icon);
            holder.menuIcon.setVisibility(View.VISIBLE);
            holder.menuIcon.setOnClickListener(view -> showChannelPopUpMenu(view));
            holder.title.setText(model.getName());
            holder.title.setTextColor(mContext.getColor(R.color.blue));
            holder.statusIcon.setImageResource(R.drawable.online_icon);
            holder.statusIcon.setVisibility(View.VISIBLE);
            holder.pttIcon.setVisibility(View.VISIBLE);
            holder.statusInfo.setText(mContext.getResources().getString(R.string.channels_user_info, mOnlineRegUsers, mTotalRegUsers));
            holder.categoryIcon.setImageResource(R.drawable.channel_icon);
            holder.muteIcon.setVisibility(View.GONE);
            holder.deafenIcon.setVisibility(View.GONE);
            holder.pinIcon.setVisibility(View.GONE);
        }
        if (model.getType() == DispatcherItemModel.ItemType.ADD_SUB_CHANNEL) {
            holder.userIconTv.setVisibility(View.GONE);
            holder.userImageTv.setVisibility(View.GONE);
            holder.categoryIcon.setVisibility(View.VISIBLE);
            holder.menuIcon.setImageResource(R.drawable.plus_icon);
            holder.categoryIcon.setImageResource(R.drawable.add_channel);
            holder.title.setTextColor(mContext.getColor(R.color.black));
            holder.title.setText(model.getName());
            holder.statusInfo.setText(mContext.getResources().getString(R.string.dispatcher_create_another_channel));
            holder.statusIcon.setVisibility(View.GONE);
            holder.pttIcon.setVisibility(View.GONE);
            holder.pinIcon.setVisibility(View.GONE);
            holder.muteIcon.setVisibility(View.GONE);
            holder.menuIcon.setVisibility(View.GONE);
            holder.deafenIcon.setVisibility(View.GONE);
        }
        if (model.getType() == DispatcherItemModel.ItemType.USER) {
            holder.categoryIcon.setVisibility(View.GONE);
            holder.title.setText(model.getName());
            holder.title.setTextColor(mContext.getColor(R.color.black));

            if (model.getTexture() != null) {
                holder.userIconTv.setVisibility(View.GONE);
                holder.userImageTv.setVisibility(View.VISIBLE);
                byte[] textureData = model.getTexture();
                if (textureData != null) {
                    Bitmap profilepic = BitmapFactory.decodeByteArray(textureData, 0, textureData.length);
                    Bitmap oldimage = getBitmapFromImageView(holder.userImageTv);
                    if (!areBitmapsEqual(oldimage, profilepic)) {
                        holder.userImageTv.setImageBitmap(BitmapUtils.getCircularBitmap(profilepic));
                    }
                }
            } else {
                holder.userIconTv.setVisibility(View.VISIBLE);
                holder.userImageTv.setVisibility(View.GONE);
                holder.userIconTv.setText(String.valueOf(model.getName().charAt(0)));
                BitmapUtils.setRandomBgColor(mContext, holder.userIconTv, model.isHasOnline());
            }

            if (model.isHasOnline()) {
                holder.statusIcon.setImageResource(R.drawable.online_icon);
                holder.statusInfo.setText(mContext.getResources().getString(R.string.status_online));
                if (model.isMute()) {
                    holder.muteIcon.setImageResource(R.drawable.ic_unmute);
                } else {
                    holder.muteIcon.setImageResource(R.drawable.ic_mute);
                }
                if (model.isDeafen()) {
                    holder.deafenIcon.setImageResource(R.drawable.ic_undeafen);
                    holder.muteIcon.setImageResource(R.drawable.ic_unmute);
                } else {
                    holder.deafenIcon.setImageResource(R.drawable.ic_deafen);
                }
                holder.muteIcon.setVisibility(View.VISIBLE);
                holder.pttIcon.setVisibility(View.VISIBLE);
                holder.deafenIcon.setVisibility(View.VISIBLE);
            } else {
                holder.statusIcon.setImageResource(R.drawable.offline_icon);
                holder.muteIcon.setVisibility(View.GONE);
                holder.deafenIcon.setVisibility(View.GONE);
                holder.pttIcon.setVisibility(View.GONE);
                if (model.getLastSeenMsg() == null || model.getLastSeenMsg().isEmpty()) {
                    holder.statusInfo.setText(mContext.getResources().getString(R.string.status_offline));
                } else {
                    holder.statusInfo.setText(mContext.getResources().getString(R.string.user_last_seen_info, CommonUtils.getFormattedLastSeen(model.getLastSeenMsg())));
                }
            }
            holder.statusIcon.setVisibility(View.VISIBLE);
            holder.menuIcon.setVisibility(View.VISIBLE);
            if (model.isDispatcherUser()) {
                holder.menuIcon.setImageResource(R.drawable.current_user_icon);
                setDispatcherBgColor(holder.userIconTv);
                // Dispatcher user show only online status icon a per Figma.
                holder.muteIcon.setVisibility(View.GONE);
                holder.deafenIcon.setVisibility(View.GONE);
                holder.pttIcon.setVisibility(View.GONE);
                holder.pinIcon.setVisibility(View.GONE);
                if (model.isMute()) {
                    holder.muteIcon.setImageResource(R.drawable.ic_unmute);
                    holder.muteIcon.setVisibility(View.VISIBLE);
                }
                if (model.isDeafen()) {
                    holder.deafenIcon.setImageResource(R.drawable.ic_undeafen);
                    holder.deafenIcon.setVisibility(View.VISIBLE);
                }
                holder.menuIcon.setOnClickListener(null);

            } else {
                BitmapUtils.setRandomBgColor(mContext, holder.userIconTv, model.isHasOnline());
                holder.menuIcon.setImageResource(R.drawable.menu_icon);
                holder.menuIcon.setOnClickListener(view -> showUserPopUpMenu(view, model));
                // Visible icon of pinned user
                if (model.isPinned()) {
                    holder.pinIcon.setVisibility(View.VISIBLE);
                } else {
                    holder.pinIcon.setVisibility(View.GONE);
                }
            }

        }
        if (model.getType() == DispatcherItemModel.ItemType.ADD_USER) {
            holder.userIconTv.setVisibility(View.GONE);
            holder.userImageTv.setVisibility(View.GONE);
            holder.categoryIcon.setVisibility(View.VISIBLE);
            holder.categoryIcon.setImageResource(R.drawable.add_user_icon);
            holder.title.setText(model.getName());
            holder.title.setTextColor(mContext.getColor(R.color.black));
            holder.statusInfo.setText(mContext.getResources().getString(R.string.dispatcher_add_user_info, mChannelName));
            holder.statusIcon.setVisibility(View.GONE);
            holder.pttIcon.setVisibility(View.GONE);
            holder.pinIcon.setVisibility(View.GONE);
            holder.muteIcon.setVisibility(View.GONE);
            holder.deafenIcon.setVisibility(View.GONE);
            holder.menuIcon.setVisibility(View.GONE);
        }
    }

    public Bitmap getBitmapFromImageView(ImageView imageView) {
        Drawable drawable = imageView.getDrawable();
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        return null; // or handle the case where the drawable is not a BitmapDrawable
    }

    public boolean areBitmapsEqual(Bitmap bitmap1, Bitmap bitmap2) {
        if (bitmap1 == null || bitmap2 == null) {
            return bitmap1 == bitmap2; // true if both are null, false otherwise
        }
        if (bitmap1.getWidth() != bitmap2.getWidth() || bitmap1.getHeight() != bitmap2.getHeight()) {
            Log.d("Tarun", "not same.........................");
            return false;
        }
        int width = bitmap1.getWidth();
        int height = bitmap1.getHeight();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (bitmap1.getPixel(x, y) != bitmap2.getPixel(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void setDispatcherBgColor(TextView userIconBg) {
        Drawable background = userIconBg.getBackground();
        if (background instanceof GradientDrawable gradientDrawable) {
            gradientDrawable.setColor(ContextCompat.getColor(mContext, R.color.dispatcher_user_bg));
        }
    }

    public void customSort() {
        mItemModelList.sort((user1, user2) -> {
            if (user1.getSeqNo() != user2.getSeqNo()) {
                // sort by category of items
                return user1.getSeqNo() - user2.getSeqNo();
            }
            if (user1.isHasOnline() == user2.isHasOnline()) {
                // sort by name if status is same
                return user1.getName().compareToIgnoreCase(user2.getName());
            } else {
                // sort by status
                return Boolean.compare(!user1.isHasOnline(), !user2.isHasOnline());
            }
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    public void refreshUserList() {
        customSort();
        notifyDataSetChanged();
    }

    public void applyFilter(String filterStr) {
        if (TextUtils.isEmpty(filterStr)) {
            mItemModelList = new ArrayList<>(mBaseItemModeList);
        } else {
            mItemModelList = mBaseItemModeList.stream()
                    .filter(user -> user.getName().toLowerCase().contains(filterStr.toLowerCase())
                            && user.getType() == DispatcherItemModel.ItemType.USER)
                    .collect(Collectors.toList());
        }
        refreshUserList();
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView userIconTv;

        ImageView userImageTv;
        ImageView categoryIcon;
        TextView title;
        ImageView pinIcon;
        ImageView pttIcon;
        ImageView muteIcon;
        ImageView deafenIcon;

        ImageView statusIcon;
        TextView statusInfo;
        ImageView menuIcon;

        public ItemViewHolder(View itemView) {
            super(itemView);
            userIconTv = itemView.findViewById(R.id.user_icon);
            userImageTv = itemView.findViewById(R.id.user_image);

            categoryIcon = itemView.findViewById(R.id.category_icon);
            title = itemView.findViewById(R.id.item_name);
            pttIcon = itemView.findViewById(R.id.ptt_icon);
            pinIcon = itemView.findViewById(R.id.pin_icon);
            statusIcon = itemView.findViewById(R.id.status_icon);
            statusInfo = itemView.findViewById(R.id.status_info);
            menuIcon = itemView.findViewById(R.id.menu_icon);
            muteIcon = itemView.findViewById(R.id.mute_icon);
            deafenIcon = itemView.findViewById(R.id.deafen_icon);
            userImageTv.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    int width = view.getWidth();
                    int height = view.getHeight();
                    int radius = Math.min(width, height) / 2;
                    outline.setRoundRect(0, 0, width, height, radius);
                }
            });
            userImageTv.setClipToOutline(true);

        }

        public void bind(final Context context, final DispatcherItemModel model, final UserAdapterProvider userAdapterProvider) {
            itemView.setOnClickListener(v -> {
                if (model.getType() == DispatcherItemModel.ItemType.USER) {
                    if (model.isDispatcherUser()) {
                        Toast.makeText(context, context.getResources().getString(R.string.self_chat_msg), Toast.LENGTH_LONG).show();
                    } else {
                        userAdapterProvider.onUserSelect(model.getName(), model.getUserId());
                    }
                } else if (model.getType() == DispatcherItemModel.ItemType.ADD_USER) {
                    userAdapterProvider.onAddUsers();
                } else if (model.getType() == DispatcherItemModel.ItemType.ADD_SUB_CHANNEL) {
                    userAdapterProvider.onSubChannel();
                } else {
                    Toast.makeText(context, "Feature coming soon !!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
