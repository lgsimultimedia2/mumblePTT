package com.jio.jiotalkie.dataclass;

import com.jio.jiotalkie.util.EnumConstant;

public class MessageReceiptStatus {

    int userId;
    String userName;
    EnumConstant.MsgStatus status;

    public MessageReceiptStatus(int userId, String userName, EnumConstant.MsgStatus status) {
        this.userId = userId;
        this.userName = userName;
        this.status = status;
    }

    public int getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public EnumConstant.MsgStatus getStatus() {
        return status;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setStatus(EnumConstant.MsgStatus status) {
        this.status = status;
    }

}
