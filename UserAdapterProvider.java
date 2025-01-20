package com.jio.jiotalkie.adapter.provider;

import com.jio.jiotalkie.model.DispatcherItemModel;

public interface UserAdapterProvider {
    void onUserSelect(String userName, int userId);
    void onLocateUser(DispatcherItemModel model);
    void onAddUsers();
    void onMute(DispatcherItemModel model);
    void onDeafen(DispatcherItemModel model);
    void onPushToTalk(String userName, int userId);
    void onKick(DispatcherItemModel model);
    void onSubChannel();
    void onUserContextMenu(boolean isShow);
}
