## easycallcenter365 build guides

### 安装第三方依赖 esl-lib:  

   mvn install:install-file -Dfile=thirdparty\freeswitch-esl-1.3.release.jar -DgroupId=link.thingscloud -DartifactId=freeswitch-esl  -Dversion=1.3.release  -Dpackaging=jar
   
   thirdparty\freeswitch-esl-1.3.release.jar 文件在当前项目的源代码根目录下。
   
   freeswitch-esl-1.3的开源地址是： https://gitee.com/easycallcenter365/freeswitch-esl-conn-pool 。
   
### 设置数据库参数:
 
   set mysql connection info, update src\main\resources\application-uat.properties,
   
   spring.datasource.username=root
   
   spring.datasource.password=123456


### 打包项目:

   run build.bat
   
   After the build is complete, you will see the easycallcenter365.jar file in the target directory.
   