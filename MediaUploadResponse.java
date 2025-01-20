package com.jio.jiotalkie.model.api;

import com.google.gson.annotations.SerializedName;

public class MediaUploadResponse {
    @SerializedName("msg_id")
    private String msgId;
    @SerializedName("uploadStatus")
    private String uploadStatus;

    private Boolean isFileUploadSuccess;


    private String mimeType;
    private int fileType;
    private String mediaPath;

    public MediaUploadResponse(String msgId, String uploadStatus, Boolean isFileUploadSuccess, String mimeType, int fileType, String mediaPath) {
        super();
        this.msgId = msgId;
        this.uploadStatus = uploadStatus;
        this.isFileUploadSuccess=isFileUploadSuccess;
        this.mimeType = mimeType;
        this.fileType = fileType;
        this.mediaPath = mediaPath;
    }

    public String getMsgId() {
        return msgId;
    }

    public String getUploadStatus() {
        return uploadStatus;
    }


    public Boolean getFileUploadSuccess() {
        return isFileUploadSuccess;
    }
    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public void setFileUploadSuccess(Boolean fileUploadSuccess) {
        isFileUploadSuccess = fileUploadSuccess;
    }

    public int getFileType() {
        return fileType;
    }

    public void setFileType(int fileType) {
        this.fileType = fileType;
    }
    public String getMediaPath() {
        return mediaPath;
    }

    public void setMediaPath(String mediaPath) {
        this.mediaPath = mediaPath;
    }
}
