package com.jio.jiotalkie.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.List;

@Entity(tableName = "jio_talkie_chats_table",indices = {@Index(value = {"msg_id"},unique = true)})
public class JioTalkieChats {
    @PrimaryKey(autoGenerate = true)
    private int _id;
    @NonNull
    @ColumnInfo(name = "user_session_id")
    private int user_session_id;
    @NonNull
    @ColumnInfo(name = "user_name")
    private String user_name;
    @ColumnInfo(name = "channels_name")
    private String channels_name;
    @NonNull
    @ColumnInfo(name = "trees_name")
    private String trees_name;
    @ColumnInfo(name = "target_user")
    private String  target_user;
    @NonNull
    @ColumnInfo(name = "message")
    private String message;
    @ColumnInfo(name = "msgStatus")
    private int msgStatus;
    @ColumnInfo(name = "receiver_displayed")
    private List<String> receiver_displayed;
    @ColumnInfo(name = "receiver_delivered")
    private List<String> receiver_delivered;
    @ColumnInfo(name = "file_upload_status")
    private String file_upload_status;
    @NonNull
    @ColumnInfo(name = "received_time")
    private long received_time;
    @ColumnInfo(name = "msg_id")
    private String msg_id;
    @NonNull
    @ColumnInfo(name = "message_type")
    private String message_type;
    @ColumnInfo(name = "mime_type")
    private String mime_type;
    @NonNull
    @ColumnInfo(name = "media_path")
    private String media_path;
    @ColumnInfo(name = "isSos")
    private Boolean isSos;
    @NonNull
    @ColumnInfo(name = "is_self_chat")
    private Boolean is_self_chat;
    @NonNull
    @ColumnInfo(name = "is_group_chat")
    private Boolean is_group_chat;
    @ColumnInfo(name = "latitude")
    private String latitude;
    @ColumnInfo(name = "longitude")
    private String longitude;
    @ColumnInfo(name = "battery_info")
    private int battery_info;
    @ColumnInfo(name = "size")
    private long size;

    public JioTalkieChats(int user_session_id, @NonNull String user_name, String channels_name, @NonNull String trees_name, String target_user, @NonNull String message, int msgStatus, long received_time, String msg_id, @NonNull String message_type, String mime_type, @NonNull String media_path, Boolean isSos, @NonNull Boolean is_self_chat, @NonNull Boolean is_group_chat, String latitude, String longitude, int battery_info, long size,List<String> receiver_displayed,List<String> receiver_delivered, String file_upload_status) {
        this.user_session_id = user_session_id;
        this.user_name = user_name;
        this.channels_name = channels_name;
        this.trees_name = trees_name;
        this.target_user = target_user;
        this.message = message;
        this.msgStatus = msgStatus;
        this.received_time = received_time;
        this.msg_id = msg_id;
        this.message_type = message_type;
        this.mime_type = mime_type;
        this.media_path = media_path;
        this.isSos = isSos;
        this.is_self_chat = is_self_chat;
        this.is_group_chat = is_group_chat;
        this.latitude = latitude;
        this.longitude = longitude;
        this.battery_info = battery_info;
        this.size = size;
        this.receiver_displayed = receiver_displayed;
        this.receiver_delivered = receiver_delivered;
        this.file_upload_status = file_upload_status;
    }

    public int get_id() {
        return _id;
    }

    public void set_id(int _id) {
        this._id = _id;
    }

    public int getUser_session_id() {
        return user_session_id;
    }

    public void setUser_session_id(int user_session_id) {
        this.user_session_id = user_session_id;
    }

    @NonNull
    public String getUser_name() {
        return user_name;
    }

    public void setUser_name(@NonNull String user_name) {
        this.user_name = user_name;
    }

    public String getChannels_name() {
        return channels_name;
    }

    public void setChannels_name(String channels_name) {
        this.channels_name = channels_name;
    }

    @NonNull
    public String getTrees_name() {
        return trees_name;
    }

    public void setTrees_name(@NonNull String trees_name) {
        this.trees_name = trees_name;
    }

    public String getTarget_user() {
        return target_user;
    }

    public void setTarget_user(String target_user) {
        this.target_user = target_user;
    }

    @NonNull
    public String getMessage() {
        return message;
    }

    public void setMessage(@NonNull String message) {
        this.message = message;
    }
    @NonNull
    public long getReceived_time() {
        return received_time;
    }

    public void setReceived_time(@NonNull long received_time) {
        this.received_time = received_time;
    }

    @NonNull
    public String getMessage_type() {
        return message_type;
    }

    public void setMessage_type(@NonNull String message_type) {
        this.message_type = message_type;
    }

    @NonNull
    public String getMedia_path() {
        return media_path;
    }

    public void setMedia_path(@NonNull String media_path) {
        this.media_path = media_path;
    }

    @NonNull
    public Boolean getIs_self_chat() {
        return is_self_chat;
    }

    public void setIs_self_chat(@NonNull Boolean is_self_chat) {
        this.is_self_chat = is_self_chat;
    }

    @NonNull
    public Boolean getIs_group_chat() {
        return is_group_chat;
    }

    public void setIs_group_chat(@NonNull Boolean is_group_chat) {
        this.is_group_chat = is_group_chat;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public int getBattery_info() {
        return battery_info;
    }

    public void setBattery_info(int battery_info) {
        this.battery_info = battery_info;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public int getMsgStatus() {
        return msgStatus;
    }

    public void setMsgStatus(int msgStatus) {
        this.msgStatus = msgStatus;
    }

    public String getMsg_id() {
        return msg_id;
    }

    public void setMsg_id(String msg_id) {
        this.msg_id = msg_id;
    }

    public String getMime_type() {
        return mime_type;
    }

    public void setMime_type(String mime_type) {
        this.mime_type = mime_type;
    }

    public Boolean getSos() {
        return isSos;
    }

    public void setSos(Boolean sos) {
        isSos = sos;
    }

    public List<String> getReceiver_displayed() {
        return receiver_displayed;
    }

    public void setReceiver_displayed(List<String> receiver_displayed) {
        this.receiver_displayed = receiver_displayed;
    }

    public List<String> getReceiver_delivered() {
        return receiver_delivered;
    }

    public void setReceiver_delivered(List<String> receiver_delivered) {
        this.receiver_delivered = receiver_delivered;
    }

    public String getFile_upload_status() {
        return file_upload_status;
    }

    public void setFile_upload_status(String file_upload_status) {
        this.file_upload_status = file_upload_status;
    }
}
