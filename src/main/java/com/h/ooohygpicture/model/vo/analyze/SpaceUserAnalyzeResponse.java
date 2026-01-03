package com.h.ooohygpicture.model.vo.analyze;



import lombok.Data;
import java.io.Serializable;

@Data
public class SpaceUserAnalyzeResponse implements Serializable {
    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 上传数量
     */
    private Long count;

    private static final long serialVersionUID = 1L;
}
