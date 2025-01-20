package com.jio.jiotalkie.dataclass;

import com.application.customservice.dataManagment.imodels.IUserModel;
import com.application.customservice.Mumble;

public class RegisteredUser {
    private int mSession;

    private int mUserId;
    private String mUserName;
    private int mLastChannel;
    private String mLastSeen;
    private boolean mOnlineStatus;
    private int mUserRole;
    private String msisdn;


    public RegisteredUser(Mumble.UserList.User user) {
        mUserId = user.getUserId();
        mUserName = user.getName();
        mLastChannel = user.getLastChannel();
        mLastSeen = user.getLastSeen();
        mUserRole = user.getUserRole();
        mOnlineStatus = false;
        msisdn = user.getMsisdn();
    }

    public RegisteredUser(IUserModel user) {
        mUserId = user.getUserID();
        mUserName = user.getUserName();
        // channel could be null if user state is REMOVED
        if (user.getUserChannel() != null) {
            mLastChannel = user.getUserChannel().getChannelID();
        }
        mSession = user.getSessionID();
        mLastSeen = null;
        mOnlineStatus = false;
        mUserRole = user.getUserRole().ordinal();
    }
    public void setOnlineStatus(boolean onlineStatus) { mOnlineStatus = onlineStatus; }
    public void setSession(int session) {
        mSession = session;
    }
    public void setLastSeen(String lastSeen) { mLastSeen = lastSeen; }
    public boolean getOnlineStatus(){
        return mOnlineStatus;
    }
    public int getSession() {
        return mSession;
    }
    public String getName() {
        return mUserName;
    }
    public String getLastSeen() { return mLastSeen; }
    public int getUserId() { return  mUserId; }

    public int getUserRole() { return  mUserRole; }
    public String getMsisdn() {return msisdn; }
    public void setMsisdn(String msisdn) { this.msisdn = msisdn; }

}
