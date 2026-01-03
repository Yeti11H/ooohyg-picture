package com.h.ooohygpicture.model.dto.space;

import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceUserAddRequest implements Serializable {
    private Long spaceId;
    private Long userId;
    private String spaceRole; // "editor", "viewer"
}
