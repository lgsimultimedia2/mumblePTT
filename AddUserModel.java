package com.jio.jiotalkie.model;

public class AddUserModel {
    private String mUserName;
    private int mUserId;
    private boolean isSelected;
    private int mSession;
    private boolean isOnline;

    private int mUserRole = -1; // default value.

    public AddUserModel(String userName, int userId, int session) {
        mUserName = userName;
        mUserId = userId;
        mSession = session;
    }

    public AddUserModel(String userName, int userId, int session, boolean online) {
        mUserName = userName;
        mUserId = userId;
        mSession = session;
        isOnline = online;
    }

    public String getUserName() {
        return mUserName;
    }

    public void setUserName(String userName) {
        mUserName = userName;
    }

    public int getUserId() {
        return mUserId;
    }

    public void setUserId(int mUserId) {
        this.mUserId = mUserId;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public int getSession() {
        return mSession;
    }

    public void setSession(int session) {
        mSession = session;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public int getUserRole() {
        return mUserRole;
    }

    public void setUserRole(int userRole) {
        mUserRole = userRole;
    }
}
