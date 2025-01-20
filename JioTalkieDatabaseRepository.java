package com.jio.jiotalkie.db;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.jio.jiotalkie.model.JioTalkieCertificates;
import com.jio.jiotalkie.model.JioTalkieChats;
import com.jio.jiotalkie.model.JioTalkieServer;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.application.customservice.dataManagment.models.Server;


public class JioTalkieDatabaseRepository {
    JioTalkieRoomDatabase jioTalkieRoomDatabase;
    JioTalkieServerDAO jioTalkieServerDAO;
    JioTalkieChatsDAO jioTalkieChatsDAO;
    JioTalkieTokensDAO jioTalkieTokensDAO;
    JioTalkieCertificatesDAO jioTalkieCertificatesDAO;

    public JioTalkieDatabaseRepository(Context application) {
        jioTalkieRoomDatabase = JioTalkieRoomDatabase.getDatabase(application);
        jioTalkieServerDAO = jioTalkieRoomDatabase.jioTalkieServerDAO();
        jioTalkieChatsDAO = jioTalkieRoomDatabase.jioTalkieChatsDAO();
        jioTalkieTokensDAO = jioTalkieRoomDatabase.jioTalkieTokensDAO();
        jioTalkieCertificatesDAO = jioTalkieRoomDatabase.jioTalkieCertificatesDAO();
    }

    public void addServer(Server server) {
        JioTalkieServer jioTalkieServer = new JioTalkieServer(server.getName(), server.getHost(), server.getPort(), server.getUsername(), server.getPassword(), "", "");
        addJioTalkieServer(jioTalkieServer);
    }
    public void addJioTalkieServer(JioTalkieServer jioTalkieServer) {
        JioTalkieRoomDatabase.databaseWriteExecutor.execute(() -> jioTalkieServerDAO.addJioTalkieServer(jioTalkieServer));
    }

    public LiveData<List<JioTalkieServer>> getJioTalkieServers() {
        return jioTalkieServerDAO.getJioTalkieServers();
    }

    public void removeServer(Server server) {
        JioTalkieServer jioTalkieServer = new JioTalkieServer(server.getName(), server.getHost(), server.getPort(), server.getUsername(), server.getPassword(), "", "");
        removeJioTalkieServer(jioTalkieServer);
    }

    public void removeJioTalkieServer(JioTalkieServer jioTalkieServer) {
        JioTalkieRoomDatabase.databaseWriteExecutor.execute(() -> jioTalkieServerDAO.removeJioTalkieServer(jioTalkieServer));
    }
    public void clearJioTalkieServer() {
        JioTalkieRoomDatabase.databaseWriteExecutor.execute(() -> jioTalkieServerDAO.deleteAll());
    }

    public long addChat(JioTalkieChats jioTalkieChats) {
        return addJioTalkieChats(jioTalkieChats);
    }
    public long addJioTalkieChats(JioTalkieChats jioTalkieChats) {
        Future<Long> future = jioTalkieRoomDatabase.databaseWriteExecutor.submit(new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                return jioTalkieChatsDAO.addJioTalkieChats(jioTalkieChats);
            }
        });
        try {
            return future.get();
        } catch (ExecutionException e) {
            e.printStackTrace();
            return -1;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public LiveData<List<JioTalkieChats>> getJioTalkieChats() {
        return jioTalkieChatsDAO.getJioTalkieChats();
    }

    public void updateJioTalkieChatStatus(int msgStatus, String msg_id, String receiverDisplayedList, String receiverDeliveredList) {
        JioTalkieRoomDatabase.databaseWriteExecutor.execute(() -> jioTalkieChatsDAO.updateJioTalkieChatStatus(msgStatus, msg_id, receiverDisplayedList, receiverDeliveredList));
    }

    public LiveData<List<JioTalkieChats>> getJioTalkieGroupChats() {
        return jioTalkieChatsDAO.getJioTalkieGroupChats();
    }

    public LiveData<List<JioTalkieChats>> getFilteredJioTalkieGroupChats(long receivedTime) {
        return jioTalkieChatsDAO.getFilteredJioTalkieGroupChats(receivedTime);
    }

    public LiveData<List<JioTalkieChats>> getJioTalkiePersonalChats(String targetUser) {
        return jioTalkieChatsDAO.getJioTalkiePersonalChats(targetUser);
    }

    public LiveData<List<JioTalkieChats>> getFilteredJioTalkiePersonalChats(String targetUser,long receivedTime) {
        return jioTalkieChatsDAO.getFilteredJioTalkiePersonalChats(targetUser,receivedTime);
    }

    public LiveData<List<JioTalkieChats>> getJioTalkieSOSChats(String targetUser) {
        return jioTalkieChatsDAO.getJioTalkieSOSChats(targetUser);
    }

    public LiveData<JioTalkieChats> getJioTalkieChatByMsgId(String msgId) {
        return jioTalkieChatsDAO.getJioTalkieChatByMsgId(msgId);
    }

    public void removeJioTalkieChats(JioTalkieChats jioTalkieChats) {
        JioTalkieRoomDatabase.databaseWriteExecutor.execute(() -> jioTalkieChatsDAO.removeJioTalkieChats(jioTalkieChats));
    }

    public CertificateEntity addCertificate(String name, byte[] certificate) {
       JioTalkieCertificates jioTalkieCertificates = new JioTalkieCertificates(certificate, name);
       return addJioTalkieCertificate(jioTalkieCertificates);
    }
    public CertificateEntity addJioTalkieCertificate(JioTalkieCertificates jioTalkieCertificates) {
        JioTalkieRoomDatabase.databaseWriteExecutor.execute(() -> jioTalkieCertificatesDAO.addJioTalkieCertificates(jioTalkieCertificates));
        return new CertificateEntity(jioTalkieCertificates.get_id(), jioTalkieCertificates.getName());
    }

    public void updateJioTalkieChats(String media_path, String msg_id) {
        JioTalkieRoomDatabase.databaseWriteExecutor.execute(() -> jioTalkieChatsDAO.updateJioTalkieChats(media_path, msg_id));
    }

    public void updateJioTalkieChats(String file_Upload_Status, String msg_id, String message) {
        JioTalkieRoomDatabase.databaseWriteExecutor.execute(() -> jioTalkieChatsDAO.updateJioTalkieChats(file_Upload_Status, msg_id, message));
    }

    public LiveData<List<JioTalkieCertificates>> getJioTalkieCertificates() {
        return jioTalkieCertificatesDAO.getJioTalkieCertificates();
    }

    public void removeJioTalkieCertificates(JioTalkieCertificates jioTalkieCertificates) {
        JioTalkieRoomDatabase.databaseWriteExecutor.execute(() -> jioTalkieCertificatesDAO.removeJioTalkieCertificates(jioTalkieCertificates));
    }

    public int deleteJioTalkieByMsgId(String msgId) {
        Future<Integer> future = jioTalkieRoomDatabase.databaseWriteExecutor.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return jioTalkieChatsDAO.deleteJioTalkieByMsgId(msgId);
            }
        });
        try {
            return future.get();
        } catch (ExecutionException e) {
            e.printStackTrace();
            return -1;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return -1;
        }
    }
    public void updateImageSizeByMsgId(String msgId, long size) {
        JioTalkieRoomDatabase.databaseWriteExecutor.execute(() -> jioTalkieChatsDAO.updateImageSizeByMsgId(msgId, size));
    }

    public List<JioTalkieChats> getFilteredJioTalkieGroupChatList(long receivedTime) {
        return jioTalkieChatsDAO.getFilteredJioTalkieGroupChatList(receivedTime);
    }

    public List<JioTalkieChats> getPaginationGroupChats() {
        return jioTalkieChatsDAO.getPaginationGroupChats();
    }

    public List<JioTalkieChats> getPaginatedPersonalChat(String targetUser){
        return jioTalkieChatsDAO.getPaginatedPersonalChat(targetUser);
    }
}
