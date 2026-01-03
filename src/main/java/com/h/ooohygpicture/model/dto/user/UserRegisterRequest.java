package com.h.ooohygpicture.model.dto.user;

import lombok.Getter;

import java.io.Serializable;

@Getter
public class UserRegisterRequest implements Serializable {
    private static final long serialVersionUID = -4765086932205200711L;

    private String userAccount;

    private String userPassword;

    private String checkPassword;
}
