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

#### 启动方式

 - 环境依赖
    - linux/mac/windows
    - java 1.8
    - mysql

 - 准备
    - 创建依赖数据库，application-dev.properties中配置数据库名称为case_manager。create database case_manager
    - 利用sql中的脚本配置对应表。创建脚本路径：case-server/sql/case-server.sql
    - 修改properties中spring.datasource的配置
    - 安装xmind jar包。 mvn install:install-file -Dfile=org.xmind.core_3.5.2.201505201101.jar -DgroupId=com.xmind -DartifactId=sdk-Java -Dversion=201505201101 -Dpackaging=jar

 - 运行
    - mvn spring-boot:run 
    - 浏览器打开 http://localhost:8094/case/caseList/1


