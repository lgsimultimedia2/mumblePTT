package com.jio.jiotalkie.dataclass;

import com.jio.jiotalkie.util.EnumConstant;

import com.application.customservice.dataManagment.imodels.IUserModel;

public class UserConnectionState {
    private EnumConstant.userState userState;
    private IUserModel user;

    public UserConnectionState(EnumConstant.userState userState, IUserModel user) {
        this.userState = userState;
        this.user = user;
    }

    public EnumConstant.userState getUserState() {
        return userState;
    }

    public void setUserState(EnumConstant.userState userState) {
        this.userState = userState;
    }

    public IUserModel getUser() {
        return user;
    }

    public void setUser(IUserModel user) {
        this.user = user;
    }
}
