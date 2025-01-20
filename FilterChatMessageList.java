package com.jio.jiotalkie.model;

import java.util.List;

public class FilterChatMessageList {

    List<JioTalkieChats> jioTalkieChats;
    String durationFrom;

    public String getDurationFrom() {
        return durationFrom;
    }

    public void setDurationFrom(String durationFrom) {
        this.durationFrom = durationFrom;
    }

    public List<JioTalkieChats> getJioTalkieChats() {
        return jioTalkieChats;
    }

    public void setJioTalkieChats(List<JioTalkieChats> jioTalkieChats) {
        this.jioTalkieChats = jioTalkieChats;
    }
}
