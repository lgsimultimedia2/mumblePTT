package com.jio.jiotalkie.dataclass;

import com.application.customservice.wrapper.IMediaMessage;

public class ChatMessageState {
    private IMediaMessage mMessage;

    public ChatMessageState(IMediaMessage message) {
        this.mMessage = message;
    }

    public IMediaMessage getMessage() {
        return mMessage;
    }

    public void setMessage(IMediaMessage mMessage) {
        this.mMessage = mMessage;
    }
}
