package com.jio.jiotalkie.dataclass;

import com.jio.jiotalkie.util.EnumConstant;

public class UserTalkState {
    private EnumConstant.userTalkState userTalkState;
    private String receiverUserTalking;

    private boolean isSelfUser;

    public UserTalkState(EnumConstant.userTalkState state, String receiverName, boolean isSelf) {
        this.userTalkState = state;
        this.receiverUserTalking = receiverName;
        this.isSelfUser = isSelf;
    }

    public EnumConstant.userTalkState getUserTalkState() {
        return userTalkState;
    }

    public void setUserTalkState(EnumConstant.userTalkState userTalkState) {
        this.userTalkState = userTalkState;
    }

    public String getReceiverUserTalking() {
        return receiverUserTalking;
    }

    public void setReceiverUserTalking(String receiverUserTalking) {
        this.receiverUserTalking = receiverUserTalking;
    }

    public boolean isSelfUser() {
        return isSelfUser;
    }

    public void setSelfUser(boolean selfUser) {
        isSelfUser = selfUser;
    }
}
