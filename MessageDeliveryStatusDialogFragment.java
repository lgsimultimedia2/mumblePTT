package com.jio.jiotalkie.fragment;

import static com.jio.jiotalkie.util.EnumConstant.MESSAGE_RECEIPT_STATUS_LIST;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.jio.jiotalkie.dataclass.MessageReceiptStatus;
import com.jio.jiotalkie.dispatch.R;
import com.jio.jiotalkie.util.EnumConstant;

import java.util.HashMap;
import java.util.List;

public class MessageDeliveryStatusDialogFragment extends DialogFragment {

    TextView readUserList, deliveredUserList;

    public static MessageDeliveryStatusDialogFragment newInstance(HashMap<Integer, MessageReceiptStatus> messageReceiptStatusList) {
        MessageDeliveryStatusDialogFragment fragment = new MessageDeliveryStatusDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(MESSAGE_RECEIPT_STATUS_LIST, messageReceiptStatusList);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        HashMap<Integer,MessageReceiptStatus> messageReceiptStatusList = (HashMap<Integer,MessageReceiptStatus>) getArguments().getSerializable(MESSAGE_RECEIPT_STATUS_LIST);
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.message_delivery_status_dialog_fragment, null);

        builder.setView(view)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (getVisibleFragment() instanceof GroupChatFragment) {
                            ((GroupChatFragment) getVisibleFragment()).onItemDeselected();
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (getVisibleFragment() instanceof GroupChatFragment) {
                            ((GroupChatFragment) getVisibleFragment()).onItemDeselected();
                        }
                    }
                });
        StringBuffer receiverDisplay = new StringBuffer();
        StringBuffer receiverDelivered = new StringBuffer();
        for (MessageReceiptStatus value : messageReceiptStatusList.values()) {
            if (value.getStatus() == EnumConstant.MsgStatus.Read) {
                receiverDisplay.append(value.getUserName()).append(", ");
            } else if (value.getStatus() == EnumConstant.MsgStatus.DeliveredToClient) {
                receiverDelivered.append(value.getUserName()).append(", ");
            }
        }
        if (receiverDisplay.length() > 1 && receiverDisplay.charAt(receiverDisplay.length() - 2) == ',') {
            receiverDisplay.setLength(receiverDisplay.length() - 2);
        }

        if (receiverDelivered.length() > 1  && receiverDelivered.charAt(receiverDelivered.length() - 2) == ',') {
            receiverDelivered.setLength(receiverDelivered.length() - 2);
        }
        readUserList  = view.findViewById(R.id.message_read_user_list);
        deliveredUserList = view.findViewById(R.id.message_delivered_user_list);
        if (!TextUtils.isEmpty(receiverDisplay)) {
            readUserList.setText(receiverDisplay);
        } else {
            readUserList.setText("No one has seen this message");
        }

        if (!TextUtils.isEmpty(receiverDelivered) && !TextUtils.isEmpty(receiverDisplay)) {
            deliveredUserList.setText(receiverDisplay.append(", ").append(receiverDelivered));
        } else if (!TextUtils.isEmpty(receiverDelivered)) {
            deliveredUserList.setText(receiverDelivered);
        } else if (!TextUtils.isEmpty(receiverDisplay)) {
            deliveredUserList.setText(receiverDisplay);
        } else if (TextUtils.isEmpty(receiverDelivered)) {
            deliveredUserList.setText("No one has delivered this message");
        }
        return builder.create();
    }

    public Fragment getVisibleFragment() {
        FragmentManager fragmentManager = getParentFragmentManager();
        List<Fragment> fragments = fragmentManager.getFragments();
        for (Fragment fragment : fragments) {
            if (fragment != null && fragment.isVisible()) {
                return fragment;
            }
        }
        return null; // No visible fragment found
    }

    public void dismissDialog(){
        if (getVisibleFragment() instanceof GroupChatFragment) {
            ((GroupChatFragment) getVisibleFragment()).onItemDeselected();
        }
        dismiss();
    }
}
