package com.jio.jiotalkie;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.jio.jiotalkie.db.JioTalkieDatabaseRepository;



import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.HashSet;
import java.util.Set;

import com.application.customservice.wrapper.Constants;

import com.jio.jiotalkie.db.CertificateEntity;
import com.jio.jiotalkie.dispatch.BuildConfig;
import com.jio.jiotalkie.dispatch.R;
import com.jio.jiotalkie.util.ServerConstant;

public class JioTalkieSettings {
    private static final String TAG = JioTalkieSettings.class.getName();

    public static final String PREFERENCE_AUDIO_MODE = "audioModeSelection";
    public static final Set<String> AUDIO_MODES;
    /**
     * Voice activity transmits depending on the amplitude of user input.
     */
    public static final String AUDIO_MODE_SPEECH_TRIGGER = "speechTrigger";
    /**
     * Push to talk transmits on command.
     */
    public static final String AUDIO_MODE_PUSH_TO_TALK = "pushToTalk";
    /**
     * Continuous transmits always.
     */
    public static final String AUDIO_MODE_ALWAYS_ACTIVE = "alwaysActive";

    // Update the default values in settings_PAGE.xml when changing DEFAULTs.

    public static final String PREF_SENSITIVITY_LEVEL = "sensitivityLevel";
    public static final int DEFAULT_SENSITIVITY_LEVEL = 50;

    public static final String PREFERENCE_PUSH_TO_TALK_KEY = "pttKey";
    public static final Integer DEFAULT_PUSH_TO_TALK_KEY = -1;

    public static final String PREF_QUICK_ACCESS_ZONE = "quickAccessZone";
    public static final String QUICK_ACCESS_ZONE_DISABLED = "none";

    public static final String DEFAULT_QUICK_ACCESS_ZONE = QUICK_ACCESS_ZONE_DISABLED;

    public static final String PREFERENCE_TOGGLE_MODE = "toggleModeSwitch";
    public static final Boolean DEFAULT_TOGGLE_MODE = false;

    public static final String PREFERENCE_AUDIO_SAMPLE_RATE = "audioSampleFrequency";
    public static final String DEFAULT_AUDIO_SAMPLE_RATE = "48000";

    public static final String PREFERENCE_AUDIO_BIT_QUALITY = "audioBitQuality";
    public static final int DEFAULT_AUDIO_BIT_QUALITY = 40000;

    public static final String PREFERENCE_SOUND_AMPLIFICATION = "soundLevel";
    public static final Integer DEFAULT_AMPLITUDE_BOOST = 100;

    public static final String PREFERENCE_RECONNECT_AUTOMATICALLY = "autoReconnectEnabled";
    public static final Boolean DEFAULT_RECONNECT_AUTOMATICALLY = true;
    public static final String UPDATE_AVAILABLE_CHECK = "update_available_check";
    public static final String APK_UPDATE_TIME_EXPIRED = "apk_update_time_Expired";

    /**
     * @deprecated use {@link #PREFERENCE_CERTIFICATE_IDENTIFIER }
     */
    public static final String PREFERENCE_OLD_CERT_PATH = "deprecatedCertPath";
    /**
     * @deprecated use {@link #PREFERENCE_CERTIFICATE_IDENTIFIER }
     */
    public static final String PREFERENCE_OLD_CERT_PASSWORD = "deprecatedCertPassword";

    /**
     * The DB identifier for the default certificate.
     *
     * @see CertificateEntity
     */
    public static final String PREFERENCE_CERTIFICATE_IDENTIFIER = "certIdentifier";

    public static final String PREFERENCE_USER_NAME = "userNameDefault";
    public static final String DEFAULT_USER_NAME = "Talkie_User";

    public static final String PREFERENCE_USE_TCP_PROTOCOL = "enableTcpConnection";
    public static final Boolean DEFAULT_USE_TCP_PROTOCOL = false;

    public static final String PREFERENCE_ENABLE_TOR = "torNetworkEnabled";
    public static final Boolean DEFAULT_ENABLE_TOR = false;

    public static final String PREFERENCE_DISABLE_CODEC = "disableCodecUsage";
    public static final Boolean DEFAULT_DISABLE_CODEC = false;

    public static final String PREFERENCE_MICROPHONE_MUTED = "microphoneMuted";
    public static final Boolean DEFAULT_MICROPHONE_MUTED = false;

    public static final String PREFERENCE_AUDIO_DISABLED = "audioDisabled";
    public static final Boolean DEFAULT_AUDIO_DISABLED = false;
    public static final String PREFERENCE_FIRST_LAUNCH = "isFirstLaunch";
    public static final Boolean DEFAULT_FIRST_LAUNCH = true;

    public static final String PREFERENCE_PACKET_INTERVAL = "packetSendInterval";
    public static final String DEFAULT_PACKET_INTERVAL = "2";

    public static final String PREFERENCE_SIMPLEX_MODE = "simplexTransmissionMode";
    public static final boolean DEFAULT_SIMPLEX_MODE = false;

    public static final String PREFERENCE_HANDHELD_MODE = "handheldDeviceMode";
    public static final boolean DEFAULT_HANDHELD_MODE = false;

    public static final String PREFERENCE_SPEAKER_ALERT = "speakerAlertTone";
    public static final boolean DEFAULT_SPEAKER_ALERT = false;

    public static final String PREFERENCE_AUDIO_PROCESSING_ENABLED = "audioProcessingActive";
    public static final boolean DEFAULT_PREPROCESSOR_ENABLED = true;
    private final String DURATIONTILL = "Duration_Till";
    private final String DURATIONFROM = "Duration_From";

    public static final String PREFERENCE_COMM_CHANNEL = "communicationChannelName";
    public static final String DEFAULT_COMM_CHANNEL = "Talkie_User_Default";

    public static final String JTOKEN = "jtoken";
    public static final String SSOTOKEN = "ssotoken";
    public static final String SSOTOKEN_EXPIRY = "ssotoken_expiry";
    public static final String MSISDN = "MSISDN";
    public static final String SERVER_IP = "server_ip";
    public static final String SERVER_PORT = "server_port";
    public static final String SERVER_SNI = "server_sni";
    public static final String ENCRYPTED_MSISDN = "encryptedMsisdn";

    private static final String PREF_USER_AGREEMENT_ACCEPT = "userAgreement";
    private static final String LAST_VERSION_CHECK_DATE_TIME = "last_version_check_date_time";

    private static final String BATTERY_STATUS_SENT = "batteryStatusSent";

    private static final String LTE_SIGNAL_STATUS_SENT = "lteSignalStatusSent";

    private static final String POWER_SAVER_ENABLE = "powerSaverEnable";

    private static final String WIFI_STATUS_SENT = "wifiStatusSent";

    private static final String IMAGE_COMPRESS_ENABLE = "image_compress";

    private static final String IMAGE_EDIT_ENABLE = "image_edit";

    static {
        AUDIO_MODES = new HashSet<String>();
        AUDIO_MODES.add(AUDIO_MODE_SPEECH_TRIGGER);
        AUDIO_MODES.add(AUDIO_MODE_PUSH_TO_TALK);
        AUDIO_MODES.add(AUDIO_MODE_ALWAYS_ACTIVE);
    }

    private final SharedPreferences sharedPreferences;

    public static JioTalkieSettings getInstance(Context context) {
        return new JioTalkieSettings(context);
    }

    private JioTalkieSettings(Context ctx) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);
        migrateLegacyCertificate(ctx);
    }

    private void migrateLegacyCertificate(Context ctx) {
        if (sharedPreferences.contains(PREFERENCE_OLD_CERT_PATH)) {
            Toast.makeText(ctx, R.string.cert_migration_start, Toast.LENGTH_LONG).show();
            String certPath = sharedPreferences.getString(PREFERENCE_OLD_CERT_PATH, "");
            String certPassword = sharedPreferences.getString(PREFERENCE_OLD_CERT_PASSWORD, "");

            Log.d(TAG, "Migrating certificate from " + certPath);
            try {
                migrateCertificate(ctx, certPath, certPassword);
                Toast.makeText(ctx, R.string.cert_migration_completed, Toast.LENGTH_LONG).show();
            } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
                handleMigrationException(e);
            } finally {
                removeLegacyCertificateEntries();
            }
        }
    }

    private void migrateCertificate(Context ctx, String certPath, String certPassword) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        File certFile = new File(certPath);
        FileInputStream certInput = new FileInputStream(certFile);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//        KeyStore oldStore = KeyStore.getInstance("PKCS12", new BouncyCastleProvider());
//        oldStore.load(certInput, certPassword.toCharArray());
//        oldStore.store(outputStream, new char[0]);

        JioTalkieDatabaseRepository database = new JioTalkieDatabaseRepository((Application) ctx);
        CertificateEntity certificate = database.addCertificate(certFile.getName(), null);
        applyDefaultCertificateIdentifier(certificate.getCertificateId());
    }

    private void handleMigrationException(Exception e) {
        if (e instanceof FileNotFoundException) {
            // We can safely ignore this; the only case in which we might still want to recover
            // would be if the user's external storage is removed.
        } else if (e instanceof CertificateException) {
            // Likely caused due to stored password being incorrect.
        } else {
            e.printStackTrace();
        }
    }

    private void removeLegacyCertificateEntries() {
        sharedPreferences.edit()
                .remove(PREFERENCE_OLD_CERT_PATH)
                .remove(PREFERENCE_OLD_CERT_PASSWORD)
                .apply();
    }

    public String getAudioMode() {
        String mode = sharedPreferences.getString(PREFERENCE_AUDIO_MODE, AUDIO_MODE_SPEECH_TRIGGER);
        if (isInvalidAudioMode(mode)) {
            mode = AUDIO_MODE_SPEECH_TRIGGER;
        }
        return mode;
    }

    private boolean isInvalidAudioMode(String method) {
        return !AUDIO_MODES.contains(method);
    }

    /**
     * Converts the preference input method value to the one used for server connection.
     *
     * @return An input method value for a jioTalkie custom service.
     */
    public int getJioTalkieAudioMode() {
        String inputMethod = getAudioMode();
        return convertToJioTalkieAudioMode(inputMethod);
    }

    private int convertToJioTalkieAudioMode(String mode) {
        if (AUDIO_MODE_SPEECH_TRIGGER.equals(mode)) {
            return Constants.TRANSMIT_VOICE_ACTIVITY;
        } else if (AUDIO_MODE_PUSH_TO_TALK.equals(mode)) {
            return Constants.TRANSMIT_PUSH_TO_TALK;
        } else if (AUDIO_MODE_ALWAYS_ACTIVE.equals(mode)) {
            return Constants.TRANSMIT_CONTINUOUS;
        }
        throw new RuntimeException("Invalid audio mode: '" + mode + "'. Could not convert to a jioTalkie audio mode id.");
    }

    public void setAudioMode(String mode) {
        if (AUDIO_MODE_SPEECH_TRIGGER.equals(mode) ||
                AUDIO_MODE_PUSH_TO_TALK.equals(mode) ||
                AUDIO_MODE_ALWAYS_ACTIVE.equals(mode)) {
            sharedPreferences.edit().putString(PREFERENCE_AUDIO_MODE, mode).apply();
        } else {
            throw new RuntimeException("Invalid audio mode " + mode);
        }
    }

    public String getCommunicationChannel() {
        return sharedPreferences.getString(PREFERENCE_COMM_CHANNEL, DEFAULT_COMM_CHANNEL);
    }

    public void setCommunicationChannel(String channelName) {
        sharedPreferences.edit().putString(PREFERENCE_COMM_CHANNEL, channelName).apply();
    }

    public void setPowerSaverEnable(boolean enable) {
        sharedPreferences.edit().putBoolean(POWER_SAVER_ENABLE, enable).apply();
    }

    public boolean isPowerSaverEnable() {
        return sharedPreferences.getBoolean(POWER_SAVER_ENABLE, false);
    }

    public void setBatteryStatusSent(boolean sent) {
        sharedPreferences.edit().putBoolean(BATTERY_STATUS_SENT, sent).apply();
    }

    public boolean getBatteryStatusSent() {
        return sharedPreferences.getBoolean(BATTERY_STATUS_SENT, false);
    }

    public void setLTESignalStatusSent(boolean sent) {
        sharedPreferences.edit().putBoolean(LTE_SIGNAL_STATUS_SENT, sent).apply();
    }

    public boolean getLTESignalStatusSent() {
        return sharedPreferences.getBoolean(LTE_SIGNAL_STATUS_SENT, false);
    }

    public void setWifiStatusSent(boolean sent) {
        sharedPreferences.edit().putBoolean(WIFI_STATUS_SENT, sent).apply();
    }

    public boolean getWifiStatusSent() {
        return sharedPreferences.getBoolean(WIFI_STATUS_SENT, false);
    }

    public boolean isUserAgreementAccepted() {
        return sharedPreferences.getBoolean(PREF_USER_AGREEMENT_ACCEPT, false);
    }

    public void setUserAgreementAccept(boolean accept) {
        sharedPreferences.edit().putBoolean(PREF_USER_AGREEMENT_ACCEPT, accept).apply();
    }

    public int getAudioSampleRate() {
        return Integer.parseInt(sharedPreferences.getString(JioTalkieSettings.PREFERENCE_AUDIO_SAMPLE_RATE, DEFAULT_AUDIO_SAMPLE_RATE));
    }

    public int getAudioBitQuality() {
        return sharedPreferences.getInt(JioTalkieSettings.PREFERENCE_AUDIO_BIT_QUALITY, DEFAULT_AUDIO_BIT_QUALITY);
    }

    public float getSoundAmplitudeBoost() {
        return (float) sharedPreferences.getInt(JioTalkieSettings.PREFERENCE_SOUND_AMPLIFICATION, DEFAULT_AMPLITUDE_BOOST) / 100;
    }

    public float getSensitivityLevel() {
        return (float) sharedPreferences.getInt(PREF_SENSITIVITY_LEVEL, DEFAULT_SENSITIVITY_LEVEL) / 100;
    }

    public int getPTTKey() {
        return sharedPreferences.getInt(PREFERENCE_PUSH_TO_TALK_KEY, DEFAULT_PUSH_TO_TALK_KEY);
    }

    public String getQuickAccessZone() {
        return sharedPreferences.getString(PREF_QUICK_ACCESS_ZONE, DEFAULT_QUICK_ACCESS_ZONE);
    }

    /**
     * Returns whether or not the hot corner is enabled.
     *
     * @return true if a hot corner should be shown.
     */
    public boolean isQuickAccessZoneEnabled() {
        return !QUICK_ACCESS_ZONE_DISABLED.equals(sharedPreferences.getString(PREF_QUICK_ACCESS_ZONE, DEFAULT_QUICK_ACCESS_ZONE));
    }

    /**
     * Returns a database identifier for the default certificate, or a negative number if there is
     * no default certificate set.
     *
     * @return The default certificate's ID, or a negative integer if not set.
     */
    public long getCertificateIdentifier() {
        return sharedPreferences.getLong(PREFERENCE_CERTIFICATE_IDENTIFIER, -1);
    }

    public String fetchDefaultUsername() {
        return sharedPreferences.getString(PREFERENCE_USER_NAME, DEFAULT_USER_NAME);
    }

    public boolean getToggleMode() {
        return sharedPreferences.getBoolean(PREFERENCE_TOGGLE_MODE, DEFAULT_TOGGLE_MODE);
    }

    public boolean isReconnectAutomaticallyEnabled() {
        return sharedPreferences.getBoolean(PREFERENCE_RECONNECT_AUTOMATICALLY, DEFAULT_RECONNECT_AUTOMATICALLY);
    }

    public boolean isTcpConnectionForced() {
        return sharedPreferences.getBoolean(PREFERENCE_USE_TCP_PROTOCOL, DEFAULT_USE_TCP_PROTOCOL);
    }

    public boolean isCodecDisabled() {
        return sharedPreferences.getBoolean(PREFERENCE_DISABLE_CODEC, DEFAULT_DISABLE_CODEC);
    }

    public boolean isTorNetworkEnabled() {
        return sharedPreferences.getBoolean(PREFERENCE_ENABLE_TOR, DEFAULT_ENABLE_TOR);
    }

    public void disableTorNetwork() {
        sharedPreferences.edit().putBoolean(PREFERENCE_ENABLE_TOR, false).apply();
    }

    public boolean isMicrophoneMuted() {
        return sharedPreferences.getBoolean(PREFERENCE_MICROPHONE_MUTED, DEFAULT_MICROPHONE_MUTED);
    }

    public boolean isAudioDisabled() {
        return sharedPreferences.getBoolean(PREFERENCE_AUDIO_DISABLED, DEFAULT_AUDIO_DISABLED);
    }


    public boolean isFirstLaunch() {
        return sharedPreferences.getBoolean(PREFERENCE_FIRST_LAUNCH, DEFAULT_FIRST_LAUNCH);
    }

    public void setAudioMuteAndDeafen(boolean muted, boolean deafened) {
        Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREFERENCE_MICROPHONE_MUTED, muted || deafened);
        editor.putBoolean(PREFERENCE_AUDIO_DISABLED, deafened);
        editor.apply();
    }

    public void setFirstLaunch(boolean run) {
        sharedPreferences.edit().putBoolean(PREFERENCE_FIRST_LAUNCH, run).apply();
    }

    public int getPacketFrames() {
        return Integer.parseInt(sharedPreferences.getString(PREFERENCE_PACKET_INTERVAL, DEFAULT_PACKET_INTERVAL));
    }

    public boolean isSimplexMode() {
        return sharedPreferences.getBoolean(PREFERENCE_SIMPLEX_MODE, DEFAULT_SIMPLEX_MODE);
    }

    public boolean isHandheldMode() {
        return sharedPreferences.getBoolean(PREFERENCE_HANDHELD_MODE, DEFAULT_HANDHELD_MODE);
    }

    public boolean isSpeakerAlertEnabled() {
        return sharedPreferences.getBoolean(PREFERENCE_SPEAKER_ALERT, DEFAULT_SPEAKER_ALERT);
    }

    public boolean isAudioProcessingEnabled() {
        return sharedPreferences.getBoolean(PREFERENCE_AUDIO_PROCESSING_ENABLED, DEFAULT_PREPROCESSOR_ENABLED);
    }

    public void applyDefaultCertificateIdentifier(long defaultCertificateId) {
        sharedPreferences.edit().putLong(PREFERENCE_CERTIFICATE_IDENTIFIER, defaultCertificateId).apply();
    }

    public boolean hasCertificate() {
        return getCertificateIdentifier() >= 0;
    }

    /**
     * @return true if the user count should be shown next to channels.
     */

    public void setDurationTill(String durationTill) {
        sharedPreferences.edit().putString(DURATIONTILL, durationTill).commit();
    }

    public String getDurationTill() {
        return sharedPreferences.getString(DURATIONTILL, " ");
    }

    public void setDurationFrom(String durationFrom) {
        sharedPreferences.edit().putString(DURATIONFROM, durationFrom).commit();
    }

    public String getDurationFrom() {
        return sharedPreferences.getString(DURATIONFROM, " ");
    }

    public String getJToken() {
        return sharedPreferences.getString(JTOKEN, "");
    }

    public void setJToken(String token) {
        sharedPreferences.edit().putString(JTOKEN, token).apply();
    }

    public String getSsoToken() {
        return sharedPreferences.getString(SSOTOKEN, "");
    }

    public void setSsoToken(String token) {
        sharedPreferences.edit().putString(SSOTOKEN, token).apply();
    }

    public void setSsotokenExpiry(long milliseconds) {
        sharedPreferences.edit().putLong(SSOTOKEN_EXPIRY, milliseconds).apply();
    }

    public long getSsotokenExpiry() {
        return sharedPreferences.getLong(SSOTOKEN_EXPIRY, 0);
    }

    public void setUpdateAvailableCheck(long milliSeconds) {
        sharedPreferences.edit().putLong(UPDATE_AVAILABLE_CHECK, milliSeconds).apply();
    }

    public void setApkUpdateTimeExpired(boolean apkUpdateTimerIsExpired) {
        sharedPreferences.edit().putBoolean(APK_UPDATE_TIME_EXPIRED, apkUpdateTimerIsExpired).apply();
    }

    public boolean getApkUpdateTimeExpired() {
        return sharedPreferences.getBoolean(APK_UPDATE_TIME_EXPIRED, false);
    }

    public long getUpdateAvailableCheck() {
        return sharedPreferences.getLong(UPDATE_AVAILABLE_CHECK, 0);
    }

    public String getServerIp() {
        return sharedPreferences.getString(SERVER_IP, "");
    }

    public int getServerPort() {
        // Runtime port number feature only for SIT build
        if (BuildConfig.BUILD_TYPE.equals("sit")) {
            int port = sharedPreferences.getInt(SERVER_PORT, -1);
            // if port number not fetch from SNI than set hard code port number
            if (port == -1) {
                return ServerConstant.getMumblePort();
            }
            return port;
        }
        return ServerConstant.getMumblePort();
    }

    public void setServerPort(int port) {
        sharedPreferences.edit().putInt(SERVER_PORT, port).apply();
    }

    public void setServerSni(String sni) {
        sharedPreferences.edit().putString(SERVER_SNI, sni).apply();
    }

    public String getServerSni() {
        return sharedPreferences.getString(SERVER_SNI, "");
    }


    public String getMSISDN() {
        return sharedPreferences.getString(MSISDN, "");
    }

    public void setMSISDN(String token) {
        sharedPreferences.edit().putString(MSISDN, token).apply();
    }

    public String getEncryptedMsisdn() {
        return sharedPreferences.getString(ENCRYPTED_MSISDN, "");
    }

    public void setEncryptedMsisdn(String encryptedMsisdn) {
        sharedPreferences.edit().putString(ENCRYPTED_MSISDN, encryptedMsisdn).apply();
    }

    public String getLastVersionCheckDateTime() {
        return sharedPreferences.getString(LAST_VERSION_CHECK_DATE_TIME, "");
    }

    public void setLastVersionCheckDateTime(String dateTime) {
        sharedPreferences.edit().putString(LAST_VERSION_CHECK_DATE_TIME, dateTime).apply();
    }

    public void setImageCompressEnable(boolean enable) {
        sharedPreferences.edit().putBoolean(IMAGE_COMPRESS_ENABLE, enable).apply();
    }

    public boolean isImageCompressEnable() {
        return sharedPreferences.getBoolean(IMAGE_COMPRESS_ENABLE, true);
    }

    public void setImageEditEnable(boolean enable) {
        sharedPreferences.edit().putBoolean(IMAGE_EDIT_ENABLE, enable).apply();
    }

    public boolean isImageEditEnable() {
        return sharedPreferences.getBoolean(IMAGE_EDIT_ENABLE, true);
    }
}
