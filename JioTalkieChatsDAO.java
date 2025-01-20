package com.jio.jiotalkie.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.jio.jiotalkie.model.JioTalkieChats;

import java.util.List;

@Dao
public interface JioTalkieChatsDAO {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long addJioTalkieChats(JioTalkieChats jioTalkieChats);

    @Query("UPDATE jio_talkie_chats_table SET media_path = :media_path WHERE msg_id =:msg_id")
    void updateJioTalkieChats(String media_path, String msg_id);

    @Query("UPDATE jio_talkie_chats_table SET file_upload_status = :file_Upload_Status, message = :message WHERE msg_id =:msg_id")
    void updateJioTalkieChats(String file_Upload_Status, String msg_id, String message);

    @Query("SELECT * FROM jio_talkie_chats_table")
    LiveData<List<JioTalkieChats>> getJioTalkieChats();

    @Query("UPDATE jio_talkie_chats_table SET msgStatus = :msgStatus, receiver_displayed =:receiverDisplayedList, receiver_delivered =:receiverDeliveredList WHERE msg_id =:msg_id")
    void updateJioTalkieChatStatus(int msgStatus, String msg_id, String receiverDisplayedList, String receiverDeliveredList);

    @Query("SELECT * FROM jio_talkie_chats_table WHERE is_group_chat = 1 ORDER BY received_time ASC")
    LiveData<List<JioTalkieChats>> getJioTalkieGroupChats();

    @Query("SELECT * FROM jio_talkie_chats_table WHERE msg_id = :msgId")
    LiveData<JioTalkieChats> getJioTalkieChatByMsgId(String msgId);

    @Query("SELECT * FROM jio_talkie_chats_table WHERE is_group_chat = 1 AND (received_time >= :receivedTime) ORDER BY received_time ASC")
    LiveData<List<JioTalkieChats>> getFilteredJioTalkieGroupChats(long receivedTime);

    @Query("SELECT * FROM jio_talkie_chats_table WHERE is_group_chat = 0 AND (target_user = :targetUser OR user_name = :targetUser) ORDER BY received_time ASC")
    LiveData<List<JioTalkieChats>> getJioTalkiePersonalChats(String targetUser);

    @Query("SELECT * FROM jio_talkie_chats_table WHERE is_group_chat = 0 AND (target_user = :targetUser OR user_name = :targetUser) AND (received_time >= :receivedTime) ")
    LiveData<List<JioTalkieChats>> getFilteredJioTalkiePersonalChats(String targetUser,long receivedTime);

    @Query("SELECT * FROM jio_talkie_chats_table WHERE message_type = 'SOS_AUDIO' AND user_name = :targetUser")
    LiveData<List<JioTalkieChats>> getJioTalkieSOSChats(String targetUser);

    @Delete
    void removeJioTalkieChats(JioTalkieChats jioTalkieChats);

    @Query("DELETE FROM jio_talkie_chats_table WHERE msg_id =:msg_id")
    int deleteJioTalkieByMsgId(String msg_id);

    @Query("UPDATE jio_talkie_chats_table SET size = :imageSize WHERE msg_id =:msg_id")
    int updateImageSizeByMsgId(String msg_id,long imageSize);

    @Query("SELECT * FROM jio_talkie_chats_table WHERE is_group_chat = 1 AND (received_time >= :receivedTime) ORDER BY received_time ASC")
    List<JioTalkieChats> getFilteredJioTalkieGroupChatList(long receivedTime);

    @Query("SELECT * FROM jio_talkie_chats_table WHERE is_group_chat = 1 ORDER BY received_time ASC")
    List<JioTalkieChats> getPaginationGroupChats();

    @Query("SELECT * FROM jio_talkie_chats_table WHERE is_group_chat = 0 AND (target_user = :targetUser OR user_name = :targetUser) ORDER BY received_time ASC")
    List<JioTalkieChats> getPaginatedPersonalChat(String targetUser);
}
