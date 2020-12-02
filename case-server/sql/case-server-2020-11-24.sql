# 增加文件夹表
create table biz
(
    id           bigint auto_increment comment '子业务线主键'
        primary key,
    product_line_id      bigint    default 0                 not null comment '业务线名称',
    content              mediumtext                          not null comment '文件数内容',
    channel              int(1)    default 0                 not null comment '渠道',
    is_delete            int(1)    default 0                 not null comment '逻辑删除',
    gmt_created          timestamp default CURRENT_TIMESTAMP not null comment '创建时间',
    gmt_modified         timestamp default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '文件夹';

alter table test_case add column biz_id varchar(500) default '-1' not null comment '关联的文件夹id';

# 操作记录字段增加失败、阻塞、忽略字段
alter table exec_record
    add column fail_count int(10) default 0 not null comment '失败个数' after success_count,
    add column block_count int(10) default 0 not null comment '阻塞个数' after success_count,
    add column ignore_count int(10) default 0 not null comment '不执行个数' after success_count;