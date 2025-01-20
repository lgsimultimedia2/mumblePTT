package com.jio.jiotalkie.util;

public class EnumConstant {

    public static final String USER_STATE_DEFAULT = "USER_STATE_DEFAULT";
    public static final String USER_STATE_SPEAKER = "USER_STATE_SPEAKER";
    public static final String USER_STATE_RECEIVER = "USER_STATE_RECEIVER";


    public static final String MIME_TYPE_IMAGE_JPEG = "image/jpeg";
    public static final String MIME_TYPE_IMAGE_JPG = "image/jpg";
    public static final String MIME_TYPE_IMAGE_PNG = "image/png";
    public static final String MIME_TYPE_AUDIO_OGG = "audio/ogg";
    public static final String MIME_TYPE_VIDEO_MP4 = "video/mp4";
    public static final String TEXT_FILE_EXTENSION = "text/txt";
    public static final String PDF_FILE_EXTENSION = "application/pdf";
    public static final String MIME_TYPE_TEXT_PLAIN_DOC = "text/plain";

    // Document File MIME Types
    public static final String MIME_TYPE_TXT = "text/plain";
    public static final String MIME_TYPE_DOC = "application/msword";
    public static final String MIME_TYPE_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    public static final String MIME_TYPE_PPT = "application/vnd.ms-powerpoint";
    public static final String MIME_TYPE_PPTX = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
    public static final String MIME_TYPE_XLS = "application/vnd.ms-excel";
    public static final String MIME_TYPE_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    public static final String MIME_TYPE_PDF = "application/pdf";

    public static final String TXT_TEXT = "TXT";
    public static final String PDF_TEXT = "PDF";

    public static final String DOC_TEXT = "DOC";
    public static final String DOCX_TEXT = "DOCX";

    public static final String PPT_TEXT = "PPT";
    public static final String PPTX_TEXT = "PPTX";

    public static final String XLS_TEXT = "XLS";
    public static final String XLSX_TEXT = "XLSX";

    public static final String FILE_TEXT = "FILE";

    public static final String AUDIO = "audio";
    public static final String VIDEO = "video";
    public static final String CHANNEL_ID = "jiotalkie_notification_channel";
    public static final String TARGET_USER_NAME = "target_user_name";
    public static final String TARGET_USER_ID = "target_user_id";
    public static final String AUTHORITY_PROVIDER_NAME = "com.jio.jiotalkie.dispatch.provider";
    public static final String UPDATE_APK_FOLDER_NAME = "update";

    public static final String MESSAGE_TYPE_IMAGE = "data:image/";
    public static final String UPDATE_APK_URI_TYPE = "application/vnd.android.package-archive";

    public static final String MESSAGE_TYPE_LOCATION = "data:location/";
    public static final int FILE_TYPE_IMAGE = 2;
    public static final int FILE_TYPE_AUDIO = 3;
    public static final int FILE_TYPE_VIDEO = 4;
    public static final int FILE_TYPE_DOC = 5;

    public static final int IMAGE_DOC_MAX_SIZE_MB = 10;
    public static final int VIDEO_MAX_SIZE_MB = 100;

    public static final String IMAGE_MSG = "IMAGE_MSG";
    public static final String AUDIO_MSG = "AUDIO_MSG";
    public static final String VIDEO_MSG = "VIDEO_MSG";
    public static final String MEDIA_PATH = "mediaPath";
    public static final String IS_VIDEO = "isVideo";

    public static final int LOGOUT = 1;

    public static final int UPDATE = 2;
    public static final int JIO_CARRIER_ID = 2018;

    public static final int ROOT_CHANNEL_ID = 0;

    public static final String MESSAGE_RECEIPT_STATUS_LIST = "message_receipt_status_list";
    public static final long SSO_TOKEN_REFRESH_TIME = (23 * 60 * 60 * 1000); // 23 hours

    public static final long SET_UPDATE_AVAILABLE_API_TIME = (24 * 60 * 60 * 1000); // 24 hours
    public static final int DEFAULT_TARGET_USER_ID = -1;
    public enum getSupportedFragment {
        GROUP_CHAT_FRAGMENT,
        PERSONAL_CHAT_FRAGMENT,
        SOS_FRAGMENT,
        LOGIN_FRAGMENT,
        PHONE_LOGIN_FRAGMENT,
        SETUP_ACCOUNT_FRAGMENT,
        SETTINGS_FRAGMENT,
        HELP_FRAGMENT,
        PRIVACY_POLICY_FRAGMENT,
        DISPATCHER_HOME_FRAGMENT,
        DISPATCHER_ACTIVE_CHANNEL_FRAGMENT,
        DISPATCHER_LOCATION_FRAGMENT,
        DISPATCHER_STATUS_FRAGMENT,
        DISPATCHER_USER_MAP_FRAGMENT,
        DISPATCHER_ADD_USER_FRAGMENT,
        PROFILE_FRAGMENT,
        BILLING_FRAGMENT,
        DISPATCHER_ROLE_FRAGMENT,
        DISPATCHER_ABOUT_APP_FRAGMENT,
        DISPATCHER_SUB_CHANNEL_FRAGMENT,
        MEDIAPLAYER_FRAGMENT,
        LOCATION_HISTORY_FRAGMENT,
        LOCATION_TIMELINE_FRAGMENT,

        DISPATCHER_SUB_CHANNEL_LIST,
        MULTIPLE_MESSAGE_FRAGMENT,
        GEO_FENCE_FRAGMENT,
        MARK_ATTENDANCE,
        IMAGE_EDIT_FRAGMENT

    }

    public enum connectionState {
        SERVER_CONNECTED,
        SERVER_CONNECTING,
        SERVER_DISCONNECTED,
        CONNECT_TO_SERVER,
        PERMISSION_DENY

    }

    public enum ServerMessageType {
        Undefined,
        // The message is text message
        TextMessageType,
        // The message is image message
        ImageMessageType,
        // The message is voice message
        VoiceMessageType,
        // The message is video message.
        VideoMessageType,
        // The message is document message.
        DocMessageType
    }

    public enum userState {
        USER_CONNECTED,
        USER_JOINED,
        USER_REMOVED
    }

    public enum userTalkState {
        TALKING,
        PASSIVE

    }

    public enum pttAnimationState {
        USER_STATE_DEFAULT,
        USER_STATE_SPEAKER,
        USER_STATE_RECEIVER

    }

    public enum MessageType {
        TEXT,
        AUDIO,
        IMAGE,
        VIDEO,
        SOS,
        SOS_AUDIO,
        LOCATION,
        DOCUMENT
    }

    public enum MsgStatus {
        Undelivered,
        DeliveredToServer,
        DeliveredToClient,
        Read
    }

    public enum LoginState {
        DUMMY,
        AUTHTOKEN_VERIFY_SUCCESS,
        AUTHTOKEN_VERIFY_FAILURE,
        ZLA_SUCCESS,
        ZLA_FAILURE,
        USER_SNI_SUCCESS,
        USER_SNI_FAILURE
    }

    public enum sosState {
        SENDER,
        RECEIVER,
        DEFAULT
    }

    public enum filter {
        ALL,
        OFFLINE,
        ONLINE,
        SEARCH
    }

    public enum Notification {
        CHAT_FRAGMENT,
        CHAT_TYPE,
        USER_NAME,
        USER_SESSION

    }
    public enum ADCMessageType {
        UNDEFINED,
        TEXT,
        VOICE,
        IMAGE,
        VIDEO,
        SOS
    }
    public enum ADCMessageCategory {
        GROUP,
        ONE_TO_ONE
    }
}
