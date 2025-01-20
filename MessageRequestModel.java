package com.jio.jiotalkie.model.api;

public class MessageRequestModel {
    private Integer userIdSend;
    private Integer userIdRecv;
    private Boolean isOneToOneChat;
    private Integer channelId;
    private String durationFrom;
    private String durationTill;
    private Boolean isPagination;
    private Integer limit;
    private Integer offset;

    public MessageRequestModel(Integer userId_send, Integer userId_recv, Boolean isOneToOneChat, Integer channelId, String durationFrom, String durationTill, Boolean isPagination, Integer limit, Integer offset) {
        this.userIdSend = userId_send;
        this.userIdRecv = userId_recv;
        this.isOneToOneChat = isOneToOneChat;
        this.channelId = channelId;
        this.durationFrom = durationFrom;
        this.durationTill = durationTill;
        this.isPagination = isPagination;
        this.limit = limit;
        this.offset = offset;
    }

    public MessageRequestModel(Integer userId_send, Integer userId_recv, Boolean isOneToOneChat, String durationFrom, String durationTill) {
        this.userIdSend = userId_send;
        this.userIdRecv = userId_recv;
        this.isOneToOneChat = isOneToOneChat;
        this.durationFrom = durationFrom;
        this.durationTill = durationTill;
    }

    public Boolean getOneToOneChat() {
        return isOneToOneChat;
    }

    public Boolean getPagination() {
        return isPagination;
    }

    public Integer getUserIdRecv() {
        return userIdRecv;
    }

    public Integer getUserIdSend() {
        return userIdSend;
    }

}
