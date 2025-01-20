package com.jio.jiotalkie.model;

public class DispatcherItemModel {
    public enum ItemType {
        CHANNEL,
        ADD_SUB_CHANNEL,
        USER,
        ADD_USER
    }

    private String name;
    private ItemType type;
    private boolean hasPttIcon;
    private boolean isPinned;
    private boolean hasOnline;

    private String lastSeenMsg;
    private boolean isDispatcherUser;
    private int userId;
    private int seqNo;
    private boolean isMute;
    private int sessionId;
    private boolean isDeafen;

    private byte[] textureData;

    private int mUserRole;


    public DispatcherItemModel(ItemType type, String name) {
        this.name = name;
        this.type = type;
    }

    public DispatcherItemModel(ItemType type) {
        this.type = type;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public boolean isDispatcherUser() {
        return isDispatcherUser;
    }

    public void setDispatcherUser(boolean dispatcherUser) {
        isDispatcherUser = dispatcherUser;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTexture(byte[] textureData) {
        this.textureData = textureData;
    }
    public byte[] getTexture() {
        return textureData;
    }
    public ItemType getType() {
        return type;
    }

    public void setType(ItemType type) {
        this.type = type;
    }

    public boolean isHasPttIcon() {
        return hasPttIcon;
    }

    public void setHasPttIcon(boolean hasPttIcon) {
        this.hasPttIcon = hasPttIcon;
    }

    public boolean isPinned() {
        return isPinned;
    }

    public void setPinned(boolean isPinned) {
        this.isPinned = isPinned;
    }

    public boolean isHasOnline() {
        return hasOnline;
    }

    public void setHasOnline(boolean hasOnline) {
        this.hasOnline = hasOnline;
    }

    public String getLastSeenMsg() {
        return lastSeenMsg;
    }

    public void setLastSeenMsg(String lastSeenMsg) {
        this.lastSeenMsg = lastSeenMsg;
    }

    public int getSeqNo() {
        if (getType() == ItemType.CHANNEL) {
            return 0;
        } else if (getType() == ItemType.ADD_SUB_CHANNEL) {
            return 1;
        } else if (getType() == ItemType.ADD_USER) {
            return 2;
        } else if (getType() == ItemType.USER && isDispatcherUser()) {
            return 3;
        } else if (getType() == ItemType.USER && !isDispatcherUser() && isPinned()) {
            return 4;
        }
        // return default number for all users
        return 10;
    }

    public boolean isMute() {
        return isMute;
    }

    public void setMute(boolean mute) {
        isMute = mute;
    }

    public int getSessionId() {
        return sessionId;
    }

    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }

    public boolean isDeafen() {
        return isDeafen;
    }

    public void setDeafen(boolean deafen) {
        isDeafen = deafen;
    }

    public int getUserRole() {
        return mUserRole;
    }

    public void setUserRole(int userRole) {
        mUserRole = userRole;
    }
}
