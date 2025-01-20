package com.jio.jiotalkie.dataclass;


import com.application.customservice.dataManagment.imodels.IChannelModel;
import com.application.customservice.exception.JioTalkieException;

public class SubChannelCreationData {
    private boolean isSuccess ;
    private IChannelModel subChannel;
    private JioTalkieException jioTalkieException;


    public SubChannelCreationData(boolean isSuccess , IChannelModel subChannel , JioTalkieException jioTalkieException){
        this.isSuccess = isSuccess;
        this.subChannel = subChannel;
        this.jioTalkieException = jioTalkieException;
    }

    public IChannelModel getSubChannel() {
        return subChannel;
    }

    public JioTalkieException getJioTalkieException() {
        return jioTalkieException;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

}
