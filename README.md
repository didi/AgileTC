#### AgileTC
![image](https://dpubstatic.udache.com/static/dpubimg/RQnYIFAwEd/logo.png)
#### 简介

>AgileTC是一套敏捷的测试用例管理平台，支持测试用例管理、执行计划管理、进度计算、多人实时协同等能力，方便测试人员对用例进行管理和沉淀。产品以脑图方式编辑可快速上手，用例关联需求形成流程闭环，并支持组件化引用，可在各个平台嵌入使用，是测试人员的贴心助手！

#### 功能描述

##### 测试用例集管理
>AgileTC能够将用例集与需求关联，支持xmind/xmind zen的导入/导出，具备丰富的搜索能力，如根据用例集名称、创建人、管理需求和创建时间进行搜索。

##### 用例编辑
>AgileTC支持多人实时协同编辑用例集，其中一个人的修改，会实时同步到打开相同用例的其他客户端，实现更加高效的测试集编写和测试执行协同。支持用例优先级和自定义标签标记。服务端和客户端增加了定时巡检探活机制保障连接稳定性。支持连接异常（如浏览器异常退出或系统故障等）场景下，自动保存用例集。

##### 测试任务管理
>用户可以根据用例中的优先级和标签圈选测试用例，组合成自身需要的测试任务。用户可以在测试任务中标记用例测试状态，并查看测试任务的整体进展。

##### 如何使用
##### 环境依赖
- mac/linux/windows 
- java 1.8 
- mysql 服务端 

##### 下载
```
git clone https://github.com/didi/AgileTC.git 
或者 直接Download ZIP
```

##### 准备
- 创建依赖数据库，application-dev.properties中配置数据库名称为case_manager create database case_manager 
- 利用sql中的脚本配置对应表。创建脚本路径：case-server/sql/case-server.sql 
- 修改application-dev.properties中spring.datasource的配置。默认数据库端口号为3306 
- 安装xmind jar包。 mvn install:install-file -Dfile=org.xmind.core_3.5.2.201505201101.jar -DgroupId=com.xmind -DartifactId=sdk-Java -Dversion=201505201101 -Dpackaging=jar

##### 运行
- mvn spring-boot:run 
- 浏览器打开 http://localhost:8094/case/caseList/1

#### 整体架构
![整体架构](https://dpubstatic.udache.com/static/dpubimg/f1f36dbd-d85a-452e-85d6-47738aa3f459.png)

#### 联系我们
群成员超200，需要扫描二维码添加客服，邀请入群

![image](https://dpubstatic.udache.com/static/dpubimg/1caac875-675a-4078-a946-6680f30553ef.png)
