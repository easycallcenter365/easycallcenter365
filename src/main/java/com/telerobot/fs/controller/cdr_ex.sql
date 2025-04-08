/*
Navicat MySQL Data Transfer

Source Server         : 172.200.115.102-人机协作-测试
Source Server Version : 50719
Source Host           : 172.200.115.102:3306
Source Database       : db_robot_ipcc_strategy

Target Server Type    : MYSQL
Target Server Version : 50719
File Encoding         : 65001

Date: 2023-02-12 14:01:23
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for cdr_ex
-- ----------------------------
DROP TABLE IF EXISTS `cdr_ex`;
CREATE TABLE `cdr_ex` (
  `id` varchar(30) NOT NULL,
  `record_type` varchar(10) NOT NULL DEFAULT '.wav' COMMENT '录音类型',
  `uuid` varchar(50) NOT NULL DEFAULT '' COMMENT '通话唯一编号,用作录音文件名称',
  `opNum` varchar(50) DEFAULT '' COMMENT '工号',
  `extNum` varchar(20) NOT NULL DEFAULT '' COMMENT '分机号码',
  `caller` varchar(20) NOT NULL DEFAULT '' COMMENT '通话的主叫号码',
  `callee` varchar(50) NOT NULL DEFAULT '' COMMENT '通话的被叫号码',
  `start_time` datetime NOT NULL DEFAULT '1980-01-01 00:00:00' COMMENT '通话开始时间',
  `end_time` datetime NOT NULL DEFAULT '1980-01-01 00:00:00' COMMENT '通话结束时间',
  `answer_time` datetime NOT NULL DEFAULT '1980-01-01 00:00:00' COMMENT '通话应答时间',
  `hangup_cause` varchar(50) NOT NULL DEFAULT '' COMMENT '挂机原因',
  `ValidTimeLenMills` int(11) DEFAULT NULL COMMENT '通话时长;毫秒数',
  `ValidTimeLen` int(11) NOT NULL DEFAULT '0' COMMENT '有效通话时长,秒',
  `TimeLen` int(11) NOT NULL DEFAULT '0' COMMENT '通话时长,秒',
  `projectId` varchar(20) DEFAULT '' COMMENT '项目名称',
  `savedCdr` int(11) DEFAULT '0' COMMENT '话单是否已经推送到远程业务系统; 0否，1是',
  `full_record_filename` varchar(255) DEFAULT '' COMMENT '录音文件路径;',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
