# easycallcenter365

![easycallcenter365](logo.jpg) 

基于FreeSWITCH和大模型的智能电话客服系统。

### 功能列表

* 支持对接大模型，实时流式语音合成
* 支持acd话务排队
* 支持AI通话无缝转接人工坐席
* 支持电话工具条
* 支持IMS视频通话/语音通话转视频

### 技术交流 && 商业咨询

   bug反馈或者咨询问题请在gitee/github上，新建 Issue，并贴上日志。

  ![联系方式](wetchat.png) 

### 是否有一键安装包，可以快速体验该产品?

是的，一键安装体验包的地址在百度网盘。
链接: https://pan.baidu.com/s/1ZnQ64KIJWn1p-iJr-b9f4A 提取码: z2qn 
一键安装包内置了FreeSWITCH-1.10.11、funasr-0.1.9、easycallcenter365.jar、mysql-8。
下载到本地后，按照目录中的"使用说明.txt" 导入虚拟机并启动，最后调整相关参数即可体验测试。


### 运行环境

   该项目目前仅在 debian-12.5 环境下编译测试通过。其他操作系统环境尚未测试。

### 如何编译 easycallcenter365

参考  [Build.md](Build.md)

### 如何配置并运行 easycallcenter365

* 创建数据库 easycallcenter365

  导入sql文件： sql\easycallcenter365.sql

* 修改参数表 cc_params 的参数
   model-api-key、 model-faq-dir、 robot-asr-type(可选) 

* 拷贝 docs\kb\下的文件到  model-faq-dir

* 启动 nohup java -jar easycallcenter365.jar > /dev/null 2>&1 &

* 查看日志

  tail -f /home/call-center/log/easycallcenter365.log 

* 注意事项

  启动 easycallcenter365.jar 之前，请确保FreeSWITCH已经启动。
  
  如果修改了 easycallcenter365.jar 的 server.port，
  请同步修改 FreeSWITCH拨号计划 public.xml 中的引用。
  

### 编译FreeSWITCH模块

   参考 https://gitee.com/easycallcenter365/free-switch-modules-libs 
   
### 目前支持哪些语音识别方式?   

  目前支持 websocket、mrcp 语音识别方式。目前 mod_funasr 支持 websocket 方式对接funasr语音识别。 
  mrcp 语音识别方式，支持阿里云语音识别， 可以参考阿里云官网关于sdm-mrcp-server配置阿里云asr的文档。  
  
### 如何设置转人工

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

  
  