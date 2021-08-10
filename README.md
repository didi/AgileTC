English | [简体中文](./README_zh-CN.md)

## AgileTC

![image](https://dpubstatic.udache.com/static/dpubimg/RQnYIFAwEd/logo.png)

[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)[![GitHub issues](https://img.shields.io/github/issues/didi/AgileTC.svg)](https://github.com/didi/AgileTC/issues)

### Introduction

AgileTC - A Test case management platform with ability of multi real-time collaboration base on mind map.

### Project Description:

With the rapid business iteration, the efficiency requirements for each link of the project process are getting higher and higher. Many QAs have begun to choose to use offline brain maps to write and execute use cases for testing, but they are also facing more and more problems:
1. Confusion and no precipitation: There are many kinds of brain map software, and the format is not uniform; each module responsible person maintains it independently, which is prone to use case redundancy and missing, and there is no global perspective use case;
2. Low collaboration efficiency: unable to perceive test progress and results; use case changes need to be communicated verbally.

The industry's more reliable use case management platforms, such as test-link, QC, Zen Tao, etc., all adopt traditional use case management methods, similar to excel operating experience, and the test case writing process is more cumbersome, which is similar to the mind map management method commonly used in current business It does not match, nor does it meet the demands of current business rapid iteration. Therefore, we need a complete test case management system to meet daily test requirements.

AgileTC is an agile online test case management platform that supports general capabilities such as test case collection management, use case hierarchical management, task management, schedule calculation, and multi-person implementation collaboration. It is suitable for software testing/developer use case writing, use case review, test task management and other scenarios.

### Function

#### Test case set management

AgileTC can associate use case sets with requirements, supports the import/export of xmind/xmind zen, and has rich search capabilities, such as searching based on use case set name, creator, management requirements, and creation time

#### Use case edit

AgileTC supports real-time collaborative editing of use case sets by multiple people. One person's modification will be synchronized to other clients that open the same use case in real time, realizing more efficient test set writing and test execution collaboration. Support use case priority and custom labeling. The server and client have added a regular inspection and detection mechanism to ensure the stability of the connection. Support the automatic saving of the use case set in the scenario of abnormal connection (such as abnormal browser exit or system failure, etc.).

#### Test task management

Users can circle the test cases according to their priority and tags, and combine them into the test tasks they need. The user can mark the test status of the use case in the test task and view the overall progress of the test task.

#### Use Case Set Service

* Complex search
* Import and Export
* Demand correlation
* Addition, deletion and modification of use case set

#### Mission service

* Demand binding
* Circle selection example
* Task execution
* Schedule calculation
* Task addition, deletion and modification

#### Use case service

* Multi-person real-time collaboration
* Regular inspections
* history record

#### Folder service

* Use case set classification management

### Start method

#### Environmental dependence

- linux/mac/windows
- java 1.8
- mysql

#### Download

* git clone https://github.com/didi/AgileTC.git 
  Or download ZIP directly

#### Prepare

* If you use the master branch, if you have done secondary development on this branch, please first pull the sub-branch to save the current content, and then pull the remote master
* Create a dependent database, the configuration database name in application-dev.properties is case_manager create database case_manager
* Use the script in sql to configure the corresponding table. Create script path: case-server/sql/case-server.sql
  * Please note that if you have used this platform and created a database before December 15, 2020, please execute all the statements following create table Biz {} in case-server.sql
  * If you are a new user and have not used this platform, please execute all the statements in case-server.sql once
* Modify the configuration of spring.datasource in properties

#### Run

* mvn spring-boot:run
* Browser open http://localhost:8094/case/caseList/1

#### Contact us

Group members exceed 200, you need to scan the QR code to add customer service and invite to join the group

![image](https://dpubstatic.udache.com/static/dpubimg/1caac875-675a-4078-a946-6680f30553ef.png)









