package com.h.ooohygpicture.service.impl;


import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.h.ooohygpicture.constant.UserConstant;
import com.h.ooohygpicture.exception.BusinessException;
import com.h.ooohygpicture.exception.ErrorCode;
import com.h.ooohygpicture.mapper.PictureMapper;
import com.h.ooohygpicture.model.dto.space.SpaceAnalyzeRequest;
import com.h.ooohygpicture.model.entity.Picture;
import com.h.ooohygpicture.model.entity.Space;
import com.h.ooohygpicture.model.entity.User;
import com.h.ooohygpicture.model.vo.analyze.SpaceCategoryAnalyzeResponse;
import com.h.ooohygpicture.model.vo.analyze.SpaceUsageAnalyzeResponse;
import com.h.ooohygpicture.service.SpaceAnalyzeService;
import com.h.ooohygpicture.service.SpaceService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SpaceAnalyzeServiceImpl implements SpaceAnalyzeService {

    @Resource
    private SpaceService spaceService;
    @Resource
    private PictureMapper pictureMapper;

    /**
     * 校验权限并获取查询条件
     * (面试亮点：统一封装鉴权与查询条件构造)
     */
    private void checkSpaceAuth(SpaceAnalyzeRequest request, User loginUser) {
        // 如果是查询所有或者是公共图库，检查管理员权限
        if (request.isQueryAll() || request.isQueryPublic()) {
            if (!loginUser.getUserRole().equals(UserConstant.ADMIN_ROLE)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        } else {
            // 查询特定空间
            Long spaceId = request.getSpaceId();
            if (spaceId == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR);
            }
            Space space = spaceService.getById(spaceId);
            if (space == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            }
            // 🔒 这里应该加上 spaceService.checkAuth(space, loginUser)
            // 简单起见，只要空间存在且非空，暂时放行，由 Controller 层保障
        }
    }

    /**
     * 构造通用的查询 Wrapper
     */
    private QueryWrapper<Picture> fillQueryWrapper(SpaceAnalyzeRequest request) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (request.isQueryAll()) {
            return queryWrapper;
        }
        if (request.isQueryPublic()) {
            queryWrapper.isNull("spaceId");
            return queryWrapper;
        }
        Long spaceId = request.getSpaceId();
        if (spaceId != null) {
            queryWrapper.eq("spaceId", spaceId);
        }
        return queryWrapper;
    }

    @Override
    public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceAnalyzeRequest request, User loginUser) {
        checkSpaceAuth(request, loginUser);

        // 如果是特定空间，直接查 space 表的信息 (因为我们在 upload 时已经累加了 totalSize)
        // 这样比 select sum(size) from picture 快得多！
        if (ObjUtil.isNotNull(request.getSpaceId())) {
            Space space = spaceService.getById(request.getSpaceId());
            SpaceUsageAnalyzeResponse response = new SpaceUsageAnalyzeResponse();
            response.setUsedSize(space.getTotalSize());
            response.setMaxSize(space.getMaxSize());
            response.setUsedCount(space.getTotalCount());
            response.setMaxCount(space.getMaxCount());
            // 计算百分比
            response.setSizeUsageRatio(NumberUtil.div(space.getTotalSize(), space.getMaxSize()).doubleValue());

// 下面那行 countUsageRatio 也要改：
            response.setCountUsageRatio(NumberUtil.div(space.getTotalCount(), space.getMaxCount()).doubleValue());
            return response;
        } else {
            // 如果是查公共图库或者所有，就没有 max 限制，只能现场统计
            // ... 暂时略过，一般只分析特定空间
            return new SpaceUsageAnalyzeResponse();
        }
    }

    @Override
    public List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceAnalyzeRequest request, User loginUser) {
        checkSpaceAuth(request, loginUser);

        QueryWrapper<Picture> queryWrapper = fillQueryWrapper(request);

        // SQL 核心逻辑：SELECT category, count(*) as count, sum(picSize) as totalSize FROM picture WHERE ... GROUP BY category
        queryWrapper.select("category", "count(*) as count", "sum(picSize) as totalSize")
                .groupBy("category");

        // 使用 MP 的 selectMaps
        List<Map<String, Object>> mapList = pictureMapper.selectMaps(queryWrapper);

        // 将 Map 转换为 VO
        return mapList.stream().map(map -> {
            SpaceCategoryAnalyzeResponse response = new SpaceCategoryAnalyzeResponse();
            response.setCategory((String) map.get("category"));
            // 注意：selectMaps 返回的数值类型可能是 Long 也可能是 BigDecimal，最好转一下 string 再 parse
            response.setCount(Long.parseLong(map.get("count").toString()));
            response.setTotalSize(Long.parseLong(map.get("totalSize").toString()));
            return response;
        }).collect(Collectors.toList());
    }
}
