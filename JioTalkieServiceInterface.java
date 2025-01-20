package com.jio.jiotalkie.interfaces;

import android.app.Activity;
import android.os.Bundle;

import com.jio.jiotalkie.service.JioTalkieNotification;

import java.util.List;

import com.application.customservice.wrapper.IJioPttService;

public interface JioTalkieServiceInterface extends IJioPttService {

    void startPttService(Bundle extras);    //Start foreground service
    void bindToPttServer();                // bind to service
    void unBindToPttServer();               // unbinding from service
    boolean isBoundToPttServer();           // to check if service is bound , earlier we used mService != null
    void stopPttService();                  //  stop foreground service.

    void clearChatNotifications();
    boolean isErrorShown();
    void onTalkKeyDown();

    void onTalkKeyUp();

    void setSuppressNotifications(boolean suppressNotifications);


    boolean isConnectionSynchronized();


    public void setBoundActivity(Activity activity);

    public void muteStreamAudio();

    public void resetStreamAudio();
    JioTalkieNotification getJioTalkieNotification();
}

