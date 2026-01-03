package com.h.ooohygpicture.annotation;


import com.h.ooohygpicture.model.enums.SpaceRoleEnum;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SaSpaceCheckPermission {

    /**
     * 需要校验的角色权限 (默认需要是管理员)
     */
    SpaceRoleEnum mustRole() default SpaceRoleEnum.ADMIN;
}
