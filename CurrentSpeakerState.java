package com.jio.jiotalkie.dataclass;

public class CurrentSpeakerState {

    private boolean isSelfCurrentSpeaker;
    private boolean isSelfCurrentSpeakerSOS;

    private int currentSpeakerId;

    private int currentSpeakerOneToOne;
    private int currentListenerOneToOne;
    private boolean isOneToOneTargetBusy;

    private String callId;

    public CurrentSpeakerState(boolean isSelfCurrentSpeaker, boolean isSelfCurrentSpeakerSOS, int currentSpeakerId,
                               int currentSpeakerOneToOne, int currentListenerOneToOne, boolean isOneToOneTargetBusy, String callId) {
        this.isSelfCurrentSpeaker = isSelfCurrentSpeaker;
        this.isSelfCurrentSpeakerSOS = isSelfCurrentSpeakerSOS;
        this.currentSpeakerId = currentSpeakerId;
        this.currentSpeakerOneToOne = currentSpeakerOneToOne;
        this.currentListenerOneToOne = currentListenerOneToOne;
        this.isOneToOneTargetBusy = isOneToOneTargetBusy;
        this.callId = callId;
    }

    public boolean isSelfCurrentSpeaker() {
        return isSelfCurrentSpeaker;
    }

    public void setSelfCurrentSpeaker(boolean selfCurrentSpeaker) {
        isSelfCurrentSpeaker = selfCurrentSpeaker;
    }

    public boolean isSelfCurrentSpeakerSOS() {
        return isSelfCurrentSpeakerSOS;
    }

    public void setSelfCurrentSpeakerSOS(boolean selfCurrentSpeakerSOS) {
        isSelfCurrentSpeakerSOS = selfCurrentSpeakerSOS;
    }

    public int getCurrentSpeakerId() {
        return currentSpeakerId;
    }

    public void setCurrentSpeakerId(int currentSpeakerId) {
        this.currentSpeakerId = currentSpeakerId;
    }

    public int getCurrentSpeakerOneToOne() {
        return currentSpeakerOneToOne;
    }

    public int getCurrentListenerOneToOne() {
        return currentListenerOneToOne;
    }

    public boolean isOneToOneTargetBusy() {
        return isOneToOneTargetBusy;
    }

    public String getCallId() {
        return callId;
    }
}
