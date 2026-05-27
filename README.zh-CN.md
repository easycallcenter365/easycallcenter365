# easycallcenter365

![easycallcenter365](logo.jpg) 

基于FreeSWITCH和大模型的智能电话客服系统。

### 功能列表

* 支持对接大模型/ `coze` 智能体 / Dify /MaxKB
* 支持AI客服说话时被打断/打断关键词设置
* 提供网页管理系统，支持在线配置
* 实时流式语音合成/FunAsr开源语音识别/阿里云语音识别
* 支持豆包声音复刻和语音合成 [支持v3接口]
* 支持IVR外呼
* 支持AI通话无缝转接人工坐席
* 支持电话工具条/支持acd话务排队
* 支持多个AI呼入客服/独立配置呼入号码及大模型底座
* 支持创建AI语音外呼任务
* 压力测试: 商业版支持200外呼或呼入并发
* 支持多方通话/电话会议
* 实现对大模型的知识库的支持 
* 支持海外语音引擎 amazon aws/deepgram
* 支持deepseek v4
 
更新日期: 2026/05/02

**修复了已发现的bug，本次更新重点是：支持海外语音引擎(亚马逊aws和美国本土的语音厂商deepgram)。**

亚马逊aws，语音识别支持全球100中语言，语音合成支持42种语言。助力中国中小企业出海业务。

**已经下载老版本测试体验的用户，需要立即更新到最新版本，老版本已过期且无法使用。**

### 商业咨询

 本项目当前仅对商业用户提供技术支持服务，这不是一个免费软件， 商业使用需付费获得授权。

![联系方式](/docs/images/wechat2.png) 
  
### 系统功能演示视频

[系统使用及配置演示视频](https://mp.weixin.qq.com/s/05GIFKWtHt6EB0mxgL0BuA)

此外我们还编写了 [图文手册](/docs/manual/manual.md) 供参考。

[常见问题手册](docs/manual/faq.md) 提供了对常见问题的排查的指导。

### 一键安装包

一键安装体验包的地址在百度网盘。
链接: https://pan.baidu.com/s/1ZnQ64KIJWn1p-iJr-b9f4A 提取码: z2qn 
一键安装包内置了FreeSWITCH-1.10.11、funasr-0.1.9、easycallcenter365.jar、easycallcenter365-gui.jar、mysql-8。
下载到本地后，按照目录中的"使用说明.txt" 导入VmWare虚拟机并启动，最后调整相关参数即可体验测试。
该部署方案可以省去从源代码编译的繁琐步骤，适合用户快速体验产品特性。

### 一键安装脚本

该安装方案适合空白主机的安装及初始化。 下载地址： https://pan.baidu.com/s/19MyZXg5HagRgDtiSVKQ3Ow 提取码: rgde 。
安装包下载到服务器目录后，执行以下指令即可自动安装，整个过程会自动安装docker及mysql等。

```bash
chmod +x ./install.sh  && ./install.sh
```

### 手动安装包

我们提供了预编译的二进制文件。 下载地址: https://pan.baidu.com/s/1xFgMPCu0VKHKnG69QhyTlA 提取码: etv5
部署文档参考目录下的文件 "部署文档.txt"。 该部署方案可以省去从源代码编译的繁琐步骤。

### 系统截图

![系统截图](/docs/shot/webgui-1.png)
![系统截图](/docs/shot/webgui-2.png)
![系统截图](/docs/shot/webgui-3.png)
![IVR功能](/docs/shot/ivr.png)
![豆包声音复刻](/docs/shot/doubao_vcl.png)



### 呼入电话的处理流程

   ![呼入电话的处理流程](docs/images/process-flow.png) 
   
* 客户来电时，电话一般进入 public context 的拨号计划，然后在拨号计划中调用 curl 指令，
curl 请求 easycallcenter365 的一个 api 接口，把通话uuid、主叫被叫等信息发送过去。通话被 easycallcenter365 接管。

* `easycallcenter365` 启动录音/录像。`easycallcenter365` 尝试和`机器人大脑`(deepseek/Coze智能体/MaxKB) 建立连接 。
* `机器人大脑` 以流式http响应的返回开场白，easycallcenter365 一边接收文本，一边调用 speak 指令发送文本进行语音合成。
* FreeSWITCH 的 mod_aliyun_tts 收到语音合成指令后，提取参数中的文本，然后连接语音合成服务器，发送语音合成请求。
* 由于整个过程中，文本是不断产生的，mod_aliyun_tts 一边发送语音合成文本，一边接收合成后的语音流数据，同时进行解码语音并播放。
* 播放完毕后，FreeSWITCH 启动语音识别的检测。通过 mod_funasr 或者 unimrcp 模块实现语音流的发送及语音识别结果文本的接收。
* 通过 event socket 消息，FreeSWITCH 把收到的语音识别结果文本，发送给 easycallcenter365。
* easycallcenter365 把语音识别结果文本，附带前面交互的消息，一起打包，发送给  `机器人大脑` 。
* 接下来继续循环，直到电话通话结束。

### 运行环境

   该项目目前仅在 debian-12 环境下编译测试通过。其他操作系统环境尚未测试。 


### 设置Debian12的中文支持

* 解决乱码问题

 vim ~/.profile  追加配置：

```bash
LANG=zh_CN.UTF-8
LANGUAGE=zh_CN.UTF-8    
```	

如果不设置，可能会导致语音合成异常。让配置立即生效：

```bash
source ~/.profile
```	

* 后台菜单乱码问题

  请使用`navicat`或者`SQLYog`管理工具创建数据库，并指定数据库的编码为utf8mb4，最后使用工具导入数据库脚本。
  注意：如果使用source命令，需要特别注意，请参考下面的命令。
  
```sql
CREATE DATABASE `easycallcenter365` 
  CHARACTER SET utf8mb4 
  COLLATE utf8mb4_general_ci;  
  
SET NAMES utf8mb4;

USE easycallcenter365;

SOURCE /home/easycallcenter365.sql;
```  
   
### 详细部署及配置

参考文件 [Deploy.txt](https://github.com/easycallcenter365/easycallcenter365/blob/master/Deploy.txt)。
   
### 目前支持哪些语音识别方式?   

目前支持 websocket、mrcp 语音识别方式。目前 mod_funasr 支持 websocket 方式对接funasr语音识别。 
mrcp 语音识别方式，支持阿里云语音识别。  
注意：目前mrcp语音识别方式，无法实现对机器人语音的打断功能。  

  
### 如何设置转内置人工坐席

  在AI通话中，如果用户明确表达了转人工的诉求，系统会自动转人工坐席。

  转人工坐席的流程是，先自动排队，然后转接给空闲坐席处理，坐席需要通过电话工具条登录。  
  
  ![电话工具条](docs/images/phone-bar.png) 
  
  坐席接听测试方法：请用记事本打开 docs\phone-bar.html 文件，
  修改 scriptServer 的地址为 easycallcenter365 所在服务器的IP地址，保存后重新使用浏览器打开 phone-bar.html 文件。
  点击 "签入" 按钮，登录上线。 如果无法登录，请检查 easycallcenter365.jar 是否启动。
  然后点击 "置闲" 按钮，同时注册软电话分机 1018 ， 最后等待电话接入。

### 如何设置电话工具条的分机号及工号

  用记事本打开 docs\phone-bar.html 文件。
  
  找到   
```java     
var scriptServer = "192.168.14.218";		
var  extnum = '1018'; //分机号		
var opnum = '8001'; //工号		
var skillLevel = 9; //技能等级		
var groupId = 1; // 业务组id  
```		
		
  按照提示修改即可。
  
### 传统电话接入后的网络结构图

![传统电话接入后的网络结构图](docs/images/network-tra.png) 
  
### 呼叫中心接入后的网络结构图

![传统电话接入后的网络结构图](docs/images/network-callcenter.png) 

  
  