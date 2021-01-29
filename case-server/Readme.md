#### 简介

AgileTC 是一套敏捷的线上测试用例管理平台，支持测试用例集管理、用例分级管理、任务管理、进度计算、多人实施协同等通用能力。适用于软件测试/开发人员用例编写、用例评审、测试任务管理等场景。

#### 功能

 - 用例集服务
   -  复杂搜索
   -  导入导出
   -  需求关联
   -  用例集增删改
 - 任务服务
   -  需求绑定
   -  圈选用例
   -  任务执行
   -  进度计算
   -  任务增删改
 - 用例服务
   - 多人实时协同
   - 定时巡检探活
   - 历史记录
 - 文件夹服务
   - 用例集分类管理

#### 启动方式

 - 环境依赖
    - linux/mac/windows
    - java 1.8
    - mysql

 - 准备
    - 若您使用master分支，若您在此分支上有过二次开发，请先拉子分支保存当前内容，再拉取远端master
    - 创建依赖数据库，application-dev.properties中配置数据库名称为case_manager    create database case_manager
    - 利用sql中的脚本配置对应表。创建脚本路径：case-server/sql/case-server.sql
        * 请注意，如果您是2020年12月15日之前就有使用本平台并且创建数据库的，请执行case-server.sql中create table Biz {}后面的所有语句
        * 如果您是新用户，还未使用过本平台，请将case-server.sql中的语句全部执行一次
    - 修改properties中spring.datasource的配置

 - 运行
    - mvn spring-boot:run 
    - 浏览器打开 http://localhost:8094/case/caseList/1


