package com.jio.jiotalkie.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;


import com.jio.jiotalkie.model.JioTalkieServer;

import java.util.List;


@Dao
public interface JioTalkieServerDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addJioTalkieServer(JioTalkieServer jioTalkieServer);

    @Update
    void updateJioTalkieServer(JioTalkieServer jioTalkieServer);

    @Query("SELECT * FROM jio_talkie_server_table")
    LiveData<List<JioTalkieServer>> getJioTalkieServers();

    @Delete
    void removeJioTalkieServer(JioTalkieServer jioTalkieServer);
    @Query("DELETE FROM jio_talkie_server_table")
    void deleteAll();
}
