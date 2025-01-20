package com.jio.jiotalkie.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class UserPinnedStateHandler {

    final String USER_PINNED_PREFERENCE = "user_pinned_preference";
    final String USER_ID_KEY = "user_id_key";
    private static UserPinnedStateHandler mInstance;
    private Set<String> mPinnedUserIds = new HashSet<>();
    private SharedPreferences.Editor mPreferencesEditor;

    private UserPinnedStateHandler() {

    }

    public static UserPinnedStateHandler getInstance() {
        synchronized (UserPinnedStateHandler.class) {
            if (mInstance == null) {
                mInstance = new UserPinnedStateHandler();
            }
            return mInstance;
        }
    }

    /**
     *  Init once time when app lunch and init useful members.
     * @param context : for shared Preferences object init.
     */
    public void init(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(USER_PINNED_PREFERENCE, Context.MODE_PRIVATE);
        // Load saved pinned details
        mPinnedUserIds = sharedPreferences.getStringSet(USER_ID_KEY, new HashSet<>());
        mPreferencesEditor = sharedPreferences.edit();
    }

    /**
     *
     * @param userId to be save in shared preference
     * @param isPinned state of user
     */
    public void setPinned(final int userId, final boolean isPinned) {
        String user = String.valueOf(userId);
        if (isPinned) {
            mPinnedUserIds.add(user);
        } else {
            mPinnedUserIds.remove(user);
        }
        updatePinnedInfo();
    }

    private void updatePinnedInfo() {
        mPreferencesEditor.putStringSet(USER_ID_KEY, mPinnedUserIds);
        mPreferencesEditor.apply();
    }

    /**
     *
     * @param userId to check user pinned or not
     * @return : return pinned status from shared preference
     */
    public boolean isPinned(final int userId) {
        return mPinnedUserIds.contains(String.valueOf(userId));
    }
}
