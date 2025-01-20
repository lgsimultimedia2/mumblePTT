package com.jio.jiotalkie.dataclass;

public class PttCallUserState {

    private String mCallId;
    private boolean isGroupCall;

    public PttCallUserState(String mCallId, boolean isGroupCall) {
        this.mCallId = mCallId;
        this.isGroupCall = isGroupCall;
    }

    public String getmCallId() {
        return mCallId;
    }

    public void setmCallId(String mCallId) {
        this.mCallId = mCallId;
    }

    public boolean isGroupCall() {
        return isGroupCall;
    }

    public void setGroupCall(boolean groupCall) {
        isGroupCall = groupCall;
    }
}
