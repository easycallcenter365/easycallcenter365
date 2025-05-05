# easycallcenter365

![easycallcenter365](logo.jpg) 

基于FreeSWITCH和大模型的智能电话客服系统。

### 功能列表

* 支持对接大模型
* 实时流式语音合成
* 支持acd话务排队
* 支持AI通话无缝转接人工坐席
* 支持电话工具条
* 支持IMS视频通话/语音通话转视频

* 支持对接 `coze` 智能体
* 支持对接 `maxKB` 开源知识库
* 支持AI客服说话时被打断
* 提供网页管理系统，支持在线配置

更新日期: 2025/05/05

### 技术交流 && 商业咨询

   bug反馈或者咨询问题请在gitee/github上，新建 Issue，并贴上日志。

  ![联系方式](wetchat.png) 

### 是否有一键安装包，可以快速体验该产品?

是的，一键安装体验包的地址在百度网盘。**如需源码编译，请参考后面的编译说明。**
链接: https://pan.baidu.com/s/1ZnQ64KIJWn1p-iJr-b9f4A 提取码: z2qn 
一键安装包内置了FreeSWITCH-1.10.11、funasr-0.1.9、easycallcenter365.jar、mysql-8。
下载到本地后，按照目录中的"使用说明.txt" 导入虚拟机并启动，最后调整相关参数即可体验测试。


### 呼入电话的处理流程

   ![呼入电话的处理流程](docs/images/process-flow.png) 
   
* 客户来电时，电话一般进入 public context 的拨号计划，然后在拨号计划中调用 curl 指令，
curl 请求 easycallcenter365 的一个 api 接口，把通话uuid、主叫被叫等信息发送过去。通话被 easycallcenter365 接管。

* easycallcenter365 启动录音/录像。easycallcenter365 把 faq.txt 作为上下文，附带电话接听的场景提示词，发送给 DeepSeek v3。
* DeepSeek 以流式http响应的返回开场白，easycallcenter365 一边接收文本，一边调用 speak 指令发送文本进行语音合成。
* FreeSWITCH 的 mod_aliyun_tts 收到语音合成指令后，提取参数中的文本，然后连接语音合成服务器，发送语音合成请求。
* 由于整个过程中，文本是不断产生的，mod_aliyun_tts 一边发送语音合成文本，一边接收合成后的语音流数据，同时进行解码语音并播放。
* 播放完毕后，FreeSWITCH 启动语音识别的检测。通过 mod_funasr 或者 unimrcp 模块实现语音流的发送及语音识别结果文本的接收。
* 通过 event socket 消息，FreeSWITCH 把收到的语音识别结果文本，发送给 easycallcenter365。
* easycallcenter365 把语音识别结果文本，附带前面交互的消息，一起打包，发送给  DeepSeek 。
* 接下来继续循环，直到电话通话结束。

### 运行环境

   该项目目前仅在 debian-12.5 环境下编译测试通过。其他操作系统环境尚未测试。

### 如何编译 easycallcenter365

参考  [Build.md](Build.md)

### 设置Debian12的中文支持

解决乱码问题： vim ~/.profile  追加配置：

```bash
LANG=zh_CN.UTF-8
LANGUAGE=zh_CN.UTF-8    
```	

如果不设置，会导致语音合成异常。让配置立即生效：

```bash
source ~/.profile
```	

### 编译FreeSWITCH模块
 
   这里主要是指 流式语音合成 以及 语音识别 模块。
   参考 https://gitee.com/easycallcenter365/freeswitch-modules-libs 
   
### 配置FreeSWITCH

* 配置拨号计划

在public.xml中加入下面的xml代码，确保插入到文件最前的位置。
```xml
<extension name="inbound_call"> 
	  <condition field="destination_number"  expression="^(\d{7,13})$" >
		  <action application="set" data="inherit_codec=true"/>
		  <action application="answer" />
		  <action application="start_dtmf"/> 
		  <action application="log" data="INFO inbound call:  ${uuid}  caller=${caller_id_number}, callee=$1 " />
		  <action application="set" data="continue_on_fail=true"/>
		  <action application="set" data="hangup_after_bridge=false"/>
		  <action application="set" data="groupId=1"/>
		  <action application="set" data="send_silence_when_idle=-1"/>
		  <action application="curl" data="http://127.0.0.1:8880/call-center/inboundProcessor?remote_video_port=${remote_video_port}&amp;local-media-port=${local_media_port}&amp;uuid=${uuid}&amp;caller=${caller_id_number}&amp;callee=$1&amp;load-test-uuid=${uuid}&amp;group-id=${groupId}"/>
		  <action application="park" />
	  </condition>
</extension>
```   

* 配置测试话机

这里使用分机来模拟呼入电话，如果使用的是项目  https://gitee.com/easycallcenter365/freeswitch-modules-libs  中的配置文件，默认的测试注册端口是5079。
完整的话机注册信息如下:
   
  分机号： 你的手机号，比如 13800138000
  
  密码: 123456
  
  注册地址：虚拟机IP:5079 
  
  如果注册超时，请检查FreeSWITCH是否启动。

### 如何配置并运行 easycallcenter365

* 创建数据库 easycallcenter365

  导入sql文件： sql\easycallcenter365.sql

* 修改参数表 cc_params 的参数
   model-api-key、 model-faq-dir、 robot-asr-type(可选) 

* 拷贝 docs\kb\下的文件到  model-faq-dir

* 启动 nohup java -Dfile.encoding=UTF-8  -jar  easyCallcenter365.jar > /dev/null 2>&1 &

* 查看日志

  tail -f /home/call-center/log/easycallcenter365.log 
  
* 拨打测试电话    

  使用上一步注册的话机拨号: 1234567
  
  如果一切正常就可以听到语音播报，如果异常请查看 easycallcenter365.log 以及 FreeSWITCH 日志。

* 注意事项

  启动 easycallcenter365.jar 之前，请确保FreeSWITCH已经启动。
  
  如果修改了 easycallcenter365.jar 的 server.port，
  请同步修改 FreeSWITCH拨号计划 public.xml 中的引用。

   
### 目前支持哪些语音识别方式?   

  目前支持 websocket、mrcp 语音识别方式。目前 mod_funasr 支持 websocket 方式对接funasr语音识别。 
  mrcp 语音识别方式，支持阿里云语音识别， 可以参考阿里云官网关于sdm-mrcp-server配置阿里云asr的文档。  

### 如何设置转接到外部网关  

在AI通话中，如果用户明确表达了转人工的诉求，系统会自动转人工坐席。

* 参数表 `cc_params` 的参数 `transfer-to-agent-type` 的值设置为 `gateway`。

* 参数表 `cc_params` 的参数 `transfer-to-agent-gateway-number` 的值设置为转人工的号码，比如 15005600327 或者一个固话号码。

* 参数表 `cc_params` 的参数 `transfer-to-agent-gateway-info` 设置转人工网关的参数。 比如：
```txt
gatewayAddr=192.168.14.252:5090&caller=64901409&profile=external&calleePrefix=
```
这里解释下，`gatewayAddr` 是网关地址及端口，`caller` 是主叫号码，`profile` 是外呼时的路由profile，`calleePrefix` 是被叫前缀。
  
### 如何设置转内置人工坐席

  在AI通话中，如果用户明确表达了转人工的诉求，系统会自动转人工坐席。
  
  请把参数表 `cc_params` 的参数 `transfer-to-agent-type` 的值设置为 `acd`。

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
   

  
  