package com.jio.jiotalkie.model.api;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class MessageResponseModel {
    @SerializedName("isLast")
    private Boolean isLast;
    @SerializedName("next_offset")
    private Integer nextOffset;
    @SerializedName("previous_offset")
    private Integer previousOffset;
    @SerializedName("total_records")
    private Integer totalRecords;
    @SerializedName("data")
    List<MessageListResponseModel> data;

    public Boolean getLast() {
        return isLast;
    }

    public Integer getNextOffset() {
        return nextOffset;
    }

    public Integer getPreviousOffset() {
        return previousOffset;
    }

    public Integer getTotalRecords() {
        return totalRecords;
    }

    public List<MessageListResponseModel> getData() {
        return data;
    }
}
