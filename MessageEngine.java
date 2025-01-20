package com.jio.jiotalkie.util;

import com.application.customservice.wrapper.IMediaMessage;
import com.jio.jiotalkie.dataclass.RegisteredUser;
import com.jio.jiotalkie.viewmodel.DashboardViewModel;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.application.customservice.wrapper.Constants;
import com.application.customservice.wrapper.IJioPttSession;
import com.application.customservice.dataManagment.imodels.IUserModel;
import com.application.customservice.Mumble;

public class MessageEngine {

    private final Pattern LINK_PATTERN = Pattern.compile("(https?://\\S+)");
    private static MessageEngine mInstance;
    private DashboardViewModel mViewModel;

    private MessageEngine() {

    }

    public static MessageEngine getInstance() {
        synchronized (MessageEngine.class) {
            if (mInstance == null) {
                mInstance = new MessageEngine();
            }
            return mInstance;
        }
    }

    /**
     * Init once time when app lunch and init useful members.
     *
     * @param viewModel : for send message to server.
     */
    public void init(DashboardViewModel viewModel) {
        mViewModel = viewModel;
    }

    private String formattedMessage(String message) {
        String formattedBody = message;
        Matcher matcher = LINK_PATTERN.matcher(formattedBody);
        formattedBody = matcher.replaceAll("<a href=\"$1\">$1</a>")
                .replaceAll("\n", "<br>");
        return formattedBody;
    }

    public boolean msgToChannel(String message) {
        if (!mViewModel.isJioTalkieServiceActive()) {
            return false;
        }
        String msgId = MessageIdUtils.generateUUID();
        String finalMessage = formattedMessage(message);
        Mumble.TextMessage.MsgType messageType = Mumble.TextMessage.MsgType.TextMessageType;
        IJioPttSession session = mViewModel.getJioTalkieService().getJioPttSession();
        IMediaMessage responseMessage = session.sendTextMsgToPttChannel(session.fetchSessionPttChannel().getChannelID(),
                finalMessage, false, messageType, msgId, "", false);
        // Add message to Group chat database
        mViewModel.storeMessageDataInDB(responseMessage, true, true,
                EnumConstant.MessageType.TEXT.name(), "", EnumConstant.MsgStatus.Undelivered.ordinal());
        // Event send to ADC for Group Message.
        ADCInfoUtils.calculateTextSize(message, true, mViewModel.getUserId(), mViewModel.getChannelId(), "Group chat", -1);
        return true;
    }

    public boolean msgToOnlineUser(int userId, int userSession, String message) {
        if (!mViewModel.isJioTalkieServiceActive()) {
            return false;
        }
        String msgId = MessageIdUtils.generateUUID();
        String finalMessage = formattedMessage(message);
        Mumble.TextMessage.MsgType messageType = Mumble.TextMessage.MsgType.TextMessageType;
        IJioPttSession session = mViewModel.getJioTalkieService().getJioPttSession();
        IMediaMessage responseMessage = session.sendTextMsgToPttUser(userSession, finalMessage, messageType, msgId, "", false);
        // Add message to One-One chat database
        mViewModel.storeMessageDataInDB(responseMessage, true, false,
                EnumConstant.MessageType.TEXT.name(), "", EnumConstant.MsgStatus.Undelivered.ordinal());
        // Event send to ADC for One-One Message.
        ADCInfoUtils.calculateTextSize(message, true, mViewModel.getUserId(), mViewModel.getChannelId(), "OneToOne", userId);
        return true;
    }

    public boolean msgToOfflineUser(int userId, String userName, String message) {
        if (!mViewModel.isJioTalkieServiceActive()) {
            return false;
        }
        String msgId = MessageIdUtils.generateUUID();
        String finalMessage = formattedMessage(message);
        Mumble.TextMessage.MsgType messageType = Mumble.TextMessage.MsgType.TextMessageType;
        IJioPttSession session = mViewModel.getJioTalkieService().getJioPttSession();
        IMediaMessage responseMessage = session.sendTextMsgToPttUser(Constants.OFFLINE_USER_SESSION_ID, userId, userName,
                finalMessage, messageType, msgId, "", false);

        // Event send to ADC for One-One Message.
        mViewModel.storeMessageDataInDB(responseMessage, true, false,
                EnumConstant.MessageType.TEXT.name(), "", EnumConstant.MsgStatus.Undelivered.ordinal());
        // Event send to ADC for track.
        ADCInfoUtils.calculateTextSize(message, true, mViewModel.getUserId(), mViewModel.getChannelId(), "OneToOne", userId);
        return true;
    }

    public void msgToCompanyAdmin(String message) {
        RegisteredUser companyAdmin = getCompanyAdmin();
        if (companyAdmin != null) {
            IJioPttSession session = mViewModel.getJioTalkieService().getJioPttSession();
            List<? extends IUserModel> mOnlineUserList = session.fetchSessionPttChannel().getUserList();
            int index = CommonUtils.getSearchItemIndex(mOnlineUserList, companyAdmin.getName());
            //Send Message to company admin
            if (index != -1) {
                MessageEngine.getInstance().msgToOnlineUser(mOnlineUserList.get(index).getUserID(),
                        mOnlineUserList.get(index).getSessionID(), message);
            } else {
                MessageEngine.getInstance().msgToOfflineUser(companyAdmin.getUserId(),
                        companyAdmin.getName(), message);
            }
        }
    }

    private RegisteredUser getCompanyAdmin() {
        if (mViewModel.getJioTalkieService().isBoundToPttServer() && mViewModel.getJioTalkieService().isPttConnectionActive() &&
                mViewModel.getJioTalkieService().getJioPttSession() != null) {
            for(RegisteredUser user : mViewModel.RegUserList){
                if(user.getUserRole() == Mumble.UserState.UserRole.CompanyAdmin_VALUE)
                    return user;
            }
        }
        return null;
    }
}
