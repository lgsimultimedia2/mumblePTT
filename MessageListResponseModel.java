package com.jio.jiotalkie.model.api;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class MessageListResponseModel {
    @SerializedName("actor")
    private int actor;
    @SerializedName("receiver")
    private int receiver;
    @SerializedName("session_id")
    private int sessionId;
    @SerializedName("channel_id")
    private int channelId;
    @SerializedName("tree_id")
    private int treeId;
    @SerializedName("message")
    private String message;
    @SerializedName("msg_type")
    private String messageType;
    @SerializedName("is_sos")
    private int isSos;
    @SerializedName("has_location")
    private int hasLocation;
    @SerializedName("location")
    private String location;
    @SerializedName("has_comment")
    private int hasComment;
    @SerializedName("comment")
    private String comment;
    @SerializedName("has_battery_strength")
    private int hasBatteryStrength;
    @SerializedName("battery_strength")
    private String batteryStrength;
    @SerializedName("has_network_strength")
    private int hasNetworkStrength;
    @SerializedName("network_strength")
    private String networkStrength;
    @SerializedName("received_time")
    private String receivedTime;
    @SerializedName("msg_id")
    private String msgId;
    @SerializedName("minio_bucket")
    private String minioBucketName;
    @SerializedName("mime_type")
    private String mime_type;
    @SerializedName("server_id")
    private String serverId;
    @SerializedName("original_file_name")
    private String originalFileName;
    @SerializedName("message_delivery_status")
    private int messageDeliveryStatus;
    @SerializedName("message_deliveryupdate_timestamp")
    private String  messageDeliveryUpdateTimestamp;
    @SerializedName("is_rest_server_file")
    private String isRestServerFile;

    @SerializedName("displayed_receiver_list")
    private List<String> displayReceiverList;

    @SerializedName("delivered_receiver_list")
    private List<String> deliveredReceiverList;


    public int getActor() {
        return actor;
    }

    public int getReceiver() {
        return receiver;
    }

    public int getSessionId() {
        return sessionId;
    }

    public int getChannelId() {
        return channelId;
    }

    public int getTreeId() {
        return treeId;
    }

    public String getMessage() {
        return message;
    }

    public String getMessageType() {
        return messageType;
    }

    public int getIsSos() {
        return isSos;
    }

    public int getHasLocation() {
        return hasLocation;
    }

    public String getLocation() {
        return location;
    }

    public int getHasComment() {
        return hasComment;
    }

    public String getComment() {
        return comment;
    }

    public int getHasBatteryStrength() {
        return hasBatteryStrength;
    }

    public String getBatteryStrength() {
        return batteryStrength;
    }

    public int getHasNetworkStrength() {
        return hasNetworkStrength;
    }

    public String getNetworkStrength() {
        return networkStrength;
    }

    public String getReceivedTime() {
        return receivedTime;
    }

    public String getMsgId() {
        return msgId;
    }

    public String getMinioBucketName() {
        return minioBucketName;
    }

    public String getMime_type() {
        return mime_type;
    }

    public String getServerId() {
        return serverId;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public int getMessageDeliveryStatus() {
        return messageDeliveryStatus;
    }

    public String getMessageDeliveryUpdateTimestamp() {
        return messageDeliveryUpdateTimestamp;
    }

    public String getIsRestServerFile() {
        return isRestServerFile;
    }

    public List<String> getDisplayReceiverList() {
        return displayReceiverList;
    }

    public List<String> getDeliveredReceiverList() {
        return deliveredReceiverList;
    }
}
