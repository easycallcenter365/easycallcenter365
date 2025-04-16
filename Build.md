## easycallcenter365 build guides

### install third-party esl-lib:  

   mvn install:install-file -Dfile=thirdparty\freeswitch-esl-1.3.release.jar -DgroupId=link.thingscloud -DartifactId=freeswitch-esl  -Dversion=1.3.release  -Dpackaging=jar
   
   
### set up database info:
 
   set mysql connection info, update src\main\resources\application-uat.properties,
   
   spring.datasource.username=root
   
   spring.datasource.password=123456


### build project:

   run build.bat
   
   After the build is complete, you will see the easycallcenter365.jar file in the target directory.
   