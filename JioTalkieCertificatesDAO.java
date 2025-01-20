package com.jio.jiotalkie.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.jio.jiotalkie.model.JioTalkieCertificates;

import java.util.List;

@Dao
public interface JioTalkieCertificatesDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addJioTalkieCertificates(JioTalkieCertificates jioTalkieCertificates);

    @Update
    void updateJioTalkieCertificates(JioTalkieCertificates jioTalkieCertificates);

    @Query("SELECT * FROM jio_talkie_certificates_table")
    LiveData<List<JioTalkieCertificates>> getJioTalkieCertificates();

    @Delete
    void removeJioTalkieCertificates(JioTalkieCertificates jioTalkieCertificates);
}
