echo "The service is being shutdown..."

echo "This will probably take 30 seconds. "

echo "Try to stop mysql-server..."
docker stop  mysql

echo "Try to stop funasr..."
docker stop  funasr-online-2710
sleep 3

echo "Try to stop freeswitch-docker..."
docker stop  freeswitch-debian12
sleep 3

echo "Try to stop easycallcenter365.jar "
echo "Try to stop callcenter-manager.jar"
pkill -9 java

echo " "
echo "Done."
echo " "
