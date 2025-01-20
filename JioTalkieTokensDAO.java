package com.jio.jiotalkie.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.jio.jiotalkie.model.JioTalkieTokens;

import java.util.List;

@Dao
public interface JioTalkieTokensDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addJioTalkieTokens(JioTalkieTokens jioTalkieTokens);

    @Update
    void updateJioTalkieTokens(JioTalkieTokens jioTalkieTokens);

    @Query("SELECT * FROM jio_talkie_tokens_table")
    List<JioTalkieTokens> getJioTalkieTokens();

    @Delete
    void removeJioTalkieTokens(JioTalkieTokens jioTalkieTokens);
}
