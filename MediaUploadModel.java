package com.jio.jiotalkie.model.api;

import com.google.gson.annotations.SerializedName;

public class MediaUploadModel {
    @SerializedName("filetype")
    private Integer filetype;
    @SerializedName("mimetype")
    private String mimetype;
    @SerializedName("msg_id")
    private String msgId;

    public MediaUploadModel(Integer filetype, String mimetype, String msgId) {
        super();
        this.filetype = filetype;
        this.mimetype = mimetype;
        this.msgId = msgId;
    }

}
