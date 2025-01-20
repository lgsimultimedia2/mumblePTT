package com.jio.jiotalkie.network;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;

import java.util.ArrayList;

import com.application.customservice.CustomService;
import com.application.customservice.dataManagment.models.Server;
import com.jio.jiotalkie.JioTalkieSettings;
import com.jio.jiotalkie.interfaces.JioTalkieServiceInterface;
import com.jio.jiotalkie.service.JioTalkieService;
import com.jio.jiotalkie.dispatch.R;

public class CustomServerConnectTask extends AsyncTask<Server, Void, Bundle> {
    private final byte[] certificate;
    private Context context;
    private JioTalkieSettings mJioTalkieSettings;
    private JioTalkieServiceInterface jioTalkieInterface;

    public CustomServerConnectTask(Context context, byte[] certificate , JioTalkieServiceInterface jioTalkieInterface) {
        this.jioTalkieInterface = jioTalkieInterface;
        this.context = context;
        mJioTalkieSettings = JioTalkieSettings.getInstance(context);
        this.certificate = certificate;
    }

    @Override
    protected Bundle doInBackground(Server... params) {
        Server server = params[0];

        int inputMethod = mJioTalkieSettings.getJioTalkieAudioMode();

        int audioSource = mJioTalkieSettings.isHandheldMode() ?
                MediaRecorder.AudioSource.DEFAULT : MediaRecorder.AudioSource.MIC;
        int audioStream = mJioTalkieSettings.isHandheldMode() ?
                AudioManager.STREAM_VOICE_CALL : AudioManager.STREAM_MUSIC;

        String applicationVersion = "";
        try {
            applicationVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        Bundle bundle = new Bundle();
//        Intent connectIntent = new Intent(mContext, JioTalkieService.class);
        bundle.putParcelable(CustomService.EXTRAS_SERVER_INFO, server);
        bundle.putString(CustomService.EXTRAS_CLIENT_NAME_VALUE, context.getString(R.string.app_name)+" "+applicationVersion);
        bundle.putFloat(CustomService.EXTRAS_TRANSMIT_MODE_TYPE, inputMethod);
        bundle.putFloat(CustomService.EXTRAS_DETECTION_THRESHOLD_VALUE, mJioTalkieSettings.getSensitivityLevel());
        bundle.putFloat(CustomService.EXTRAS_AMPLITUDE_BOOST_VALUE, mJioTalkieSettings.getSoundAmplitudeBoost());
        bundle.putBoolean(CustomService.EXTRAS_AUTO_RECONNECT_ENABLED, mJioTalkieSettings.isReconnectAutomaticallyEnabled());
        bundle.putInt(CustomService.EXTRAS_AUTO_RECONNECT_DELAY_TIME, JioTalkieService.RECONNECT_DELAY);
        bundle.putBoolean(CustomService.EXTRAS_USE_OPUS_CODEC, !mJioTalkieSettings.isCodecDisabled());
        bundle.putInt(CustomService.EXTRAS_INPUT_FREQUENCY, mJioTalkieSettings.getAudioSampleRate());
        bundle.putInt(CustomService.EXTRAS_INPUT_QUALITY_LEVEL, mJioTalkieSettings.getAudioBitQuality());
        bundle.putBoolean(CustomService.EXTRAS_FORCE_TCP_CONNECTION, mJioTalkieSettings.isTcpConnectionForced());
        bundle.putBoolean(CustomService.EXTRAS_USE_TOR_NETWORK, mJioTalkieSettings.isTorNetworkEnabled());
        //TODO if proper token is required then, get token value from DB
        ArrayList<String> token = new ArrayList<>();
        bundle.putStringArrayList(CustomService.EXTRAS_ACCESS_TOKENS_LIST, token);
        bundle.putInt(CustomService.EXTRAS_AUDIO_SOURCE_TYPE, audioSource);
        bundle.putInt(CustomService.EXTRAS_AUDIO_STREAM_TYPE, audioStream);
        bundle.putInt(CustomService.EXTRAS_FRAMES_PER_PACKET_COUNT, mJioTalkieSettings.getPacketFrames());
//        bundle.putString(CustomService.EXTRAS_TRUST_STORE_PATH, JioTalkieStore.getTrustStoreFilPath(context));
//        bundle.putString(CustomService.EXTRAS_TRUST_STORE_PASSWORD_VALUE, JioTalkieStore.getTrustPassword());
//        bundle.putString(CustomService.EXTRAS_TRUST_STORE_FORMAT_TYPE, JioTalkieStore.getCustomTrustStoreFormat());
        bundle.putBoolean(CustomService.EXTRAS_HALF_DUPLEX_MODE, mJioTalkieSettings.isSimplexMode());
        bundle.putBoolean(CustomService.EXTRAS_ENABLE_PREPROCESSOR_FLAG, mJioTalkieSettings.isAudioProcessingEnabled());
        bundle.putString(CustomService.EXTRAS_CHANNEL_NAME_VALUE, mJioTalkieSettings.getCommunicationChannel());
        /*if (server.isSaved()) {
            ArrayList<Integer> muteHistory = (ArrayList<Integer>) mDatabase.getLocalMutedUsers(server.getId());
            ArrayList<Integer> ignoreHistory = (ArrayList<Integer>) mDatabase.getLocalIgnoredUsers(server.getId());
            connectIntent.putExtra(CustomService.EXTRAS_LOCAL_MUTE_HISTORY_LIST, muteHistory);
            connectIntent.putExtra(CustomService.EXTRAS_LOCAL_IGNORE_HISTORY_LIST, ignoreHistory);
        }*/
        if (mJioTalkieSettings.hasCertificate()) {
            if (certificate != null)
                bundle.putByteArray(CustomService.EXTRAS_CERT_DATA, certificate);
        }
        return bundle;
    }

    @Override
    protected void onPostExecute(Bundle bundle) {
        super.onPostExecute(bundle);
        jioTalkieInterface.startPttService(bundle);
    }
}
