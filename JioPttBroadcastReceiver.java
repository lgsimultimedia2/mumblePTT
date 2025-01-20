package com.jio.jiotalkie.service.ipc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import java.util.Optional;

import com.application.customservice.wrapper.IJioPttService;
import com.application.customservice.wrapper.IJioPttSession;

public class JioPttBroadcastReceiver extends BroadcastReceiver {
    public static final String ACTION_TRIGGER_COMMUNICATION = "com.jio.jiotalkie.action.TRIGGER_COMMUNICATION";
    public static final String EXTRA_COMMUNICATION_STATE = "communication_state";
    public static final String COMMUNICATION_STATE_ACTIVE = "active";
    public static final String COMMUNICATION_STATE_INACTIVE = "inactive";
    public static final String COMMUNICATION_STATE_SWITCH = "switch";

    private static final String LOG_TAG = JioPttBroadcastReceiver.class.getName();


    private IJioPttService mService;

    public JioPttBroadcastReceiver(IJioPttService service) {
        mService = service;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ACTION_TRIGGER_COMMUNICATION.equals(intent.getAction())) {
            throw new IllegalArgumentException("Invalid action: " + intent.getAction());
        }

        if (!mService.isPttConnectionActive()) {
            return;
        }

        IJioPttSession session = mService.getJioPttSession();
        String state = intent.getStringExtra(EXTRA_COMMUNICATION_STATE);


        state = Optional.ofNullable(state).orElse(COMMUNICATION_STATE_SWITCH);

        boolean isTalking = switch (state) {
            case COMMUNICATION_STATE_ACTIVE -> true;
            case COMMUNICATION_STATE_INACTIVE -> false;
            case COMMUNICATION_STATE_SWITCH -> !session.isPttUserTalking();
            default -> throw new IllegalStateException("Unexpected value: " + state);
        };

        session.updatePttUserTalkingState(isTalking);
    }
}
