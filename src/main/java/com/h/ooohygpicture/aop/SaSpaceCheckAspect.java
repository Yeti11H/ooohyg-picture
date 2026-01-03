package com.h.ooohygpicture.aop;

import cn.hutool.core.bean.BeanUtil;
import com.h.ooohygpicture.annotation.SaSpaceCheckPermission;
import com.h.ooohygpicture.exception.BusinessException;
import com.h.ooohygpicture.exception.ErrorCode;
import com.h.ooohygpicture.model.entity.Space;
import com.h.ooohygpicture.model.entity.SpaceUser;
import com.h.ooohygpicture.model.entity.User;
import com.h.ooohygpicture.model.enums.SpaceRoleEnum;
import com.h.ooohygpicture.model.enums.SpaceTypeEnum;
import com.h.ooohygpicture.service.SpaceService;
import com.h.ooohygpicture.service.SpaceUserService;
import com.h.ooohygpicture.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
@Slf4j
public class SaSpaceCheckAspect {

    @Resource
    private UserService userService;
    @Resource
    private SpaceService spaceService;
    @Resource
    private SpaceUserService spaceUserService;

    @Around("@annotation(saSpaceCheckPermission)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, SaSpaceCheckPermission saSpaceCheckPermission) throws Throwable {
        // 1. 获取请求
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }
        HttpServletRequest request = attributes.getRequest();

        // 获取当前登录用户 (如果没登录，userService 应该会抛异常或者返回 null)
        User loginUser = userService.getLoginUser(request);

        // 2. 获取请求参数中的 spaceId
        Object[] args = joinPoint.getArgs();
        Long spaceId = null;

        // 自动解析参数：支持 Request 对象中包含 spaceId，或者直接传 Long spaceId
        for (Object arg : args) {
            // 方式1：参数是 Long 类型，且参数名是 spaceId (简单判断值)
            // 但 AOP 拿参数名比较麻烦，这里我们用更通用的方式：反射读对象的 getSpaceId 方法
            if (BeanUtil.getPropertyDescriptor(arg.getClass(), "spaceId") != null) {
                Object value = BeanUtil.getProperty(arg, "spaceId");
                if (value != null && value instanceof Long) {
                    spaceId = (Long) value;
                    break;
                }
            }
            // 方式2：如果你有特定的 Request 类（如 SpaceUserAddRequest），也可以用 instanceof 判断
        }

        // 还有一种情况：参数在 @RequestBody 没解析前（JSON格式），AOP 拿到的已经是对象了，所以上面的 BeanUtil 是可行的。
        // 如果是从 PathVariable 取 (比如 /delete/{id})，参数里可能直接就是 Long id
        if (spaceId == null) {
            // 尝试直接找 Long 类型的参数 (这招有点暴力，假设第一个 Long 就是 spaceId)
            for (Object arg : args) {
                if (arg instanceof Long) {
                    spaceId = (Long) arg;
                    break;
                }
            }
        }

        if (spaceId == null || spaceId <= 0) {
            // 如果没找到 spaceId，为了安全起见，报错或者跳过校验（建议报错）
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "缺少 spaceId 参数，无法鉴权");
        }

        // 3. 校验空间是否存在
        Space space = spaceService.getById(spaceId);
        if (space == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }

        // 4. 权限校验核心逻辑

        // 4.1 如果是“个人私有空间” (type = 0)
        // 只有创建者本人（或者系统管理员）能操作
        if (SpaceTypeEnum.PRIVATE.getValue() == space.getType()) {
            if (!space.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作该私有空间");
            }
        }
        // 4.2 如果是“团队空间” (type = 1)
        else {
            // 先去查 space_user 表，看我有没有在这个团队里
            SpaceUser spaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceId)
                    .eq(SpaceUser::getUserId, loginUser.getId())
                    .one();

            if (spaceUser == null) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "您不是该团队成员");
            }

            // 校验角色等级
            SpaceRoleEnum mustRole = saSpaceCheckPermission.mustRole();
            SpaceRoleEnum myRole = SpaceRoleEnum.getEnumByValue(spaceUser.getSpaceRole());

            // 如果注解要求 ADMIN，但我只是 EDITOR 或 VIEWER -> 报错
            if (SpaceRoleEnum.ADMIN.equals(mustRole)) {
                if (!SpaceRoleEnum.ADMIN.equals(myRole)) {
                    throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "需要管理员权限");
                }
            }
            // 如果注解要求 EDITOR，但我只是 VIEWER -> 报错
            else if (SpaceRoleEnum.EDITOR.equals(mustRole)) {
                if (SpaceRoleEnum.VIEWER.equals(myRole)) {
                    throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "需要编辑权限");
                }
            }
            // 如果注解要求 VIEWER，那只要进来了就行，不报错
        }

        // 5. 放行
        return joinPoint.proceed();
    }
}
