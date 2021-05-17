CREATE TABLE `case_backup` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `case_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '用例集id',
  `title` varchar(64) NOT NULL DEFAULT '' COMMENT '用例名称',
  `creator` varchar(20) NOT NULL DEFAULT '' COMMENT '用例保存人',
  `gmt_created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '用例保存时间',
  `case_content` longtext CHARACTER SET utf8mb4,
  `record_content` longtext COMMENT '任务执行内容',
  `extra` varchar(256) NOT NULL DEFAULT '' COMMENT '扩展字段',
  `is_delete` int(11) NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_caseId` (`case_id`)
) ENGINE=InnoDB AUTO_INCREMENT=677 DEFAULT CHARSET=utf8 COMMENT='测试备份';

CREATE TABLE `exec_record` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `case_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '执行的用例id',
  `title` varchar(64) NOT NULL DEFAULT '' COMMENT '用例名称',
  `env` int(10) NOT NULL DEFAULT '0' COMMENT '执行环境： 0、测试环境 1、预发环境 2.线上环境 3.冒烟qa 4.冒烟rd',
  `case_content` longtext COMMENT '任务执行内容',
  `is_delete` int(10) NOT NULL DEFAULT '0' COMMENT '用例状态 0-正常 1-删除',
  `pass_count` int(10) NOT NULL DEFAULT '0' COMMENT '执行个数',
  `total_count` int(10) NOT NULL DEFAULT '0' COMMENT '需执行总个数',
  `success_count` int(10) NOT NULL DEFAULT '0' COMMENT '成功个数',
  `creator` varchar(20) NOT NULL DEFAULT '' COMMENT '用例创建人',
  `modifier` varchar(20) NOT NULL DEFAULT '' COMMENT '用例修改人',
  `executors` varchar(200) NOT NULL DEFAULT '' COMMENT '执行人',
  `description` varchar(1000) NOT NULL DEFAULT '' COMMENT '描述',
  `choose_content` varchar(200) NOT NULL DEFAULT '' COMMENT '圈选用例内容',
  `gmt_created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  `gmt_modified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录修改时间',
  `expect_start_time` timestamp NOT NULL DEFAULT '1971-01-01 00:00:00' COMMENT '预计开始时间',
  `expect_end_time` timestamp NOT NULL DEFAULT '1971-01-01 00:00:00' COMMENT '预计结束时间',
  `actual_start_time` timestamp NOT NULL DEFAULT '1971-01-01 00:00:00' COMMENT '实际开始时间',
  `actual_end_time` timestamp NOT NULL DEFAULT '1971-01-01 00:00:00' COMMENT '实际结束时间',
  `owner` varchar(200) NOT NULL DEFAULT '' COMMENT '负责人',
  PRIMARY KEY (`id`),
  KEY `idx_caseId_isdelete` (`case_id`,`is_delete`)
) ENGINE=InnoDB AUTO_INCREMENT=898 DEFAULT CHARSET=utf8 COMMENT='用例执行记录';

CREATE TABLE `test_case` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `group_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '用例集id',
  `title` varchar(64) NOT NULL DEFAULT 'testcase' COMMENT '用例名称',
  `description` varchar(512) NOT NULL DEFAULT '' COMMENT '用例描述',
  `is_delete` int(11) NOT NULL DEFAULT '0' COMMENT '用例状态 0-正常 1-删除',
  `creator` varchar(20) NOT NULL DEFAULT '' COMMENT '用例创建人',
  `modifier` varchar(1000) NOT NULL DEFAULT '' COMMENT '用例修改人',
  `case_content` longtext CHARACTER SET utf8mb4,
  `gmt_created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  `gmt_modified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `extra` varchar(256) NOT NULL DEFAULT '' COMMENT '扩展字段',
  `product_line_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '业务线id 默认0',
  `case_type` int(11) NOT NULL DEFAULT '0' COMMENT '0-需求用例，1-核心用例，2-冒烟用例',
  `module_node_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '模块节点id',
  `requirement_id` varchar(1000) NOT NULL DEFAULT '0' COMMENT '需求id',
  `smk_case_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '冒烟case的id',
  `channel` int(11) NOT NULL DEFAULT '0' COMMENT '渠道标志 现默认1',
  PRIMARY KEY (`id`),
  KEY `idx_productline_isdelete` (`product_line_id`,`is_delete`),
  KEY `idx_requirement_id` (`requirement_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2207 DEFAULT CHARSET=utf8 COMMENT='测试用例';

## 请执行以下SQL，新建了一张文件夹表，同时给test_case增加文件夹字段，exec_record增加执行个数统计的兜底字段
# 增加文件夹表
create table biz
(
    id           bigint auto_increment comment '文件夹主键'
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

# 增加用户信息表
CREATE TABLE `user` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `username` varchar(255) NOT NULL DEFAULT '' COMMENT '用户名',
  `password` varchar(1023) NOT NULL DEFAULT '' COMMENT '密码',
  `salt` varchar(1023) NOT NULL DEFAULT '' COMMENT '盐',
  `is_delete` int(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  `channel` int(1) NOT NULL DEFAULT '0' COMMENT '渠道',
  `product_line_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '业务线',
  `gmt_created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `gmt_updated` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=677 DEFAULT CHARSET=utf8 COMMENT='用户信息';

alter table user add column authority_name varchar(63) default '' after salt;

# 增加权限信息表
CREATE TABLE `authority` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `authority_name` varchar(63) NOT NULL DEFAULT ''COMMENT '权限名称，ROLE_开头，全大写',
  `authority_desc` varchar(255) NOT NULL DEFAULT ''COMMENT '权限描述',
  `authority_content` varchar(1023) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '权限内容，可访问的url，多个时用,隔开',
  `gmt_created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_updated` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COMMENT='权限信息';

INSERT INTO `authority` (id,authority_name,authority_desc,authority_content) VALUES (1, 'ROLE_USER', '普通用户', '/api/dir/list,/api/record/list,/api/record/getRecordInfo,/api/user/**,/api/case/list*');
INSERT INTO `authority` (id,authority_name,authority_desc,authority_content) VALUES (2, 'ROLE_ADMIN', '管理员', '/api/dir/list,/api/backup/**,/api/record/**,/api/file/**,/api/user/**,/api/case/**');
INSERT INTO `authority` (id,authority_name,authority_desc,authority_content) VALUES (3, 'ROLE_SA', '超级管理员','/api/**');

