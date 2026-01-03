
create table user
(
    id            bigint auto_increment comment 'id'
        primary key,
    userAccount   varchar(256)                           not null comment '账号',
    userPassword  varchar(512)                           not null comment '密码',
    userName      varchar(256)                           null comment '用户昵称',
    userAvatar    varchar(1024)                          null comment '用户头像',
    userProfile   varchar(512)                           null comment '用户简介',
    userRole      varchar(256) default 'user'            not null comment '用户角色：user/admin',
    vipExpireTime datetime                               null comment '会员过期时间',
    vipCode       varchar(128)                           null comment '会员兑换码',
    vipNumber     bigint                                 null comment '会员编号',
    editTime      datetime     default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime    datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime    datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete      tinyint      default 0                 not null comment '是否删除',
    credits       bigint       default 10                not null comment '积分余额',
    constraint uk_userAccount
        unique (userAccount)
)
    comment '用户' collate = utf8mb4_unicode_ci;

create index idx_userName
    on user (userName);

-- auto-generated definition
create table space_user
(
    id         bigint auto_increment comment 'id'
        primary key,
    spaceId    bigint                                 not null comment '空间id',
    userId     bigint                                 not null comment '用户id',
    spaceRole  varchar(128) default 'viewer'          null comment '空间角色：viewer/editor/admin',
    createTime datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_space_user
        unique (spaceId, userId)
)
    comment '空间成员表';

-- auto-generated definition
create table space
(
    id         bigint auto_increment comment 'id'
        primary key,
    spaceName  varchar(128)                       null comment '空间名称',
    spaceLevel int      default 0                 not null comment '空间级别：0-普通版 1-专业版 2-旗舰版',
    maxSize    bigint   default 0                 null comment '空间最大容量',
    maxCount   bigint   default 0                 null comment '空间最大数量',
    totalSize  bigint   default 0                 null comment '当前已用容量',
    totalCount bigint   default 0                 null comment '当前已用数量',
    userId     bigint                             not null comment '创建用户id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除',
    type       int      default 0                 not null comment '空间类型：0-私有 1-团队'
)
    comment '空间表';

create index idx_space_type
    on space (type);

-- auto-generated definition
create table picture_task
(
    id         bigint                                not null comment '主键 ID'
        primary key,
    userId     bigint                                not null comment '创建用户 ID',
    prompt     varchar(1024)                         not null comment 'AI 提示词',
    outputUrl  varchar(1024)                         null comment '生成结果 URL (阿里云的临时地址)',
    pictureId  bigint                                null comment '转存后的图片 ID (对应 picture 表)',
    status     varchar(32) default 'PENDING'         not null comment '任务状态: PENDING-生成中, SUCCESS-成功, FAILED-失败',
    createTime datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint     default 0                 not null comment '是否删除'
)
    comment 'AI 绘图任务表';

create index idx_userId
    on picture_task (userId);

-- auto-generated definition
create table picture
(
    id            bigint auto_increment comment 'id'
        primary key,
    url           varchar(512)                       not null comment '图片 url',
    name          varchar(128)                       not null comment '图片名称',
    introduction  varchar(512)                       null comment '简介',
    category      varchar(64)                        null comment '分类',
    tags          varchar(512)                       null comment '标签（JSON 数组）',
    picSize       bigint                             null comment '图片体积',
    picWidth      int                                null comment '图片宽度',
    picHeight     int                                null comment '图片高度',
    picScale      double                             null comment '图片宽高比例',
    picFormat     varchar(32)                        null comment '图片格式',
    userId        bigint                             not null comment '创建用户 id',
    spaceId       bigint                             null comment '空间 id（为空表示公共空间）',
    picColor      varchar(16)                        null comment '图片主色调',
    reviewStatus  int      default 0                 not null comment '审核状态：0-待审核; 1-通过; 2-拒绝',
    reviewMessage varchar(512)                       null comment '审核信息',
    reviewerId    bigint                             null comment '审核人 ID',
    reviewTime    datetime                           null comment '审核时间',
    thumbnailUrl  varchar(512)                       null comment '缩略图 url',
    createTime    datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    editTime      datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    updateTime    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete      tinyint  default 0                 not null comment '是否删除'
)
    comment '图片' collate = utf8mb4_unicode_ci;

create index idx_category
    on picture (category);

create index idx_introduction
    on picture (introduction);

create index idx_name
    on picture (name);

create index idx_reviewStatus
    on picture (reviewStatus);

create index idx_spaceId
    on picture (spaceId);

create index idx_tags
    on picture (tags);

create index idx_userId
    on picture (userId);

use `ooohyg-picture`;
ALTER TABLE picture ADD COLUMN version INT DEFAULT 1 COMMENT '乐观锁版本号';
-- auto-generated definition
create table credit_log
(
    id         bigint auto_increment comment 'id'
        primary key,
    userId     bigint                             not null comment '用户id',
    amount     bigint                             not null comment '变动金额(正数为加，负数为减)',
    type       int      default 0                 not null comment '类型：0-签到 1-画图扣费',
    remark     varchar(256)                       null comment '备注',
    createTime datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 null comment '是否删除'
)
    comment '积分流水表';

