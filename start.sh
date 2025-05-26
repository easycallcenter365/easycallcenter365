echo "The service is being launched..."

echo "This will probably take 35 to 60 seconds. "

echo "Try to start mysql-server..."
docker start  mysql

echo "Try to start funasr..."
docker start  funasr-online-2710
sleep 15

echo "Try to start freeswitch-docker..."
docker start  freeswitch-debian12
sleep 35

echo "Try to start easycallcenter365.jar "
nohup /usr/local/jdk1.8.0_391/bin/java  -Dfile.encoding=UTF-8  -jar  easycallcenter365.jar > /dev/null 2>&1 &
sleep 15

echo "Try to start callcenter-manager.jar"
nohup /usr/local/jdk1.8.0_391/bin/java  -Dfile.encoding=UTF-8  -jar  easycallcenter365-gui.jar > /dev/null 2>&1 &

echo " "
echo "Done."
echo " "
