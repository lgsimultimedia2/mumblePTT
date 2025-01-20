package com.jio.jiotalkie.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "jio_talkie_certificates_table")
public class JioTalkieCertificates {
    @PrimaryKey(autoGenerate = true)
    private int _id;
    @NonNull
    @ColumnInfo(name = "data")
    private byte[] data;
    @NonNull
    @ColumnInfo(name = "name")
    private String name;

    public JioTalkieCertificates(@NonNull byte[] data, @NonNull String name) {
        this.data = data;
        this.name = name;
    }

    public int get_id() {
        return _id;
    }

    public void set_id(int _id) {
        this._id = _id;
    }

    @NonNull
    public byte[] getData() {
        return data;
    }

    public void setData(@NonNull byte[] data) {
        this.data = data;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }
}
