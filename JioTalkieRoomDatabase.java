package com.jio.jiotalkie.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.jio.jiotalkie.model.JioTalkieCertificates;
import com.jio.jiotalkie.model.JioTalkieChats;
import com.jio.jiotalkie.model.JioTalkieServer;
import com.jio.jiotalkie.model.JioTalkieTokens;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {JioTalkieServer.class, JioTalkieChats.class, JioTalkieTokens.class, JioTalkieCertificates.class}, version = 1, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class JioTalkieRoomDatabase extends RoomDatabase {
    public abstract JioTalkieServerDAO jioTalkieServerDAO();
    public abstract  JioTalkieCertificatesDAO jioTalkieCertificatesDAO();
    public abstract  JioTalkieTokensDAO jioTalkieTokensDAO();
    public abstract  JioTalkieChatsDAO jioTalkieChatsDAO();

    private static volatile JioTalkieRoomDatabase jioTalkieRoomDatabase;
    private static final int NUMBER_OF_THREADS = 4;
    static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    static JioTalkieRoomDatabase getDatabase(final Context context) {
        if (jioTalkieRoomDatabase == null) {
            synchronized (JioTalkieRoomDatabase.class) {
                if (jioTalkieRoomDatabase == null) {
                    jioTalkieRoomDatabase = Room.databaseBuilder(context.getApplicationContext(),
                                    JioTalkieRoomDatabase.class, "jioTalkie_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return jioTalkieRoomDatabase;
    }

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase supportSQLiteDatabase) {
            supportSQLiteDatabase.execSQL("ALTER TABLE jio_talkie_chats_table "
                    + " ADD COLUMN msgStatus TEXT");
            supportSQLiteDatabase.execSQL("ALTER TABLE jio_talkie_chats_table "
                    + " ADD COLUMN msg_id TEXT");
            supportSQLiteDatabase.execSQL("ALTER TABLE jio_talkie_chats_table "
                    + " ADD COLUMN mime_type TEXT");
            supportSQLiteDatabase.execSQL("ALTER TABLE jio_talkie_chats_table "
                    + " ADD COLUMN isSos INTEGER");
        }
    };

    static final Migration MIGRATION_2_3 = new Migration(2,3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase supportSQLiteDatabase) {
            supportSQLiteDatabase.execSQL("ALTER TABLE jio_talkie_chats_table "
                    + " ADD COLUMN receiver_displayed TEXT");
        }
    };
}
