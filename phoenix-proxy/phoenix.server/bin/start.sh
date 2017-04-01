#!/usr/bin/env bash

export PHOENIX_HOME="`pwd`/../"

JAVA_OPTS=" -server -Xms4096m -Xmx4096m  -Xss256K -XX:PermSize=512M -XX:MaxPermSize=512M  -XX:+UseConcMarkSweepGC -XX:+DisableExplicitGC  -XX:+UseParNewGC -XX:+CMSClassUnloadingEnabled -XX:+CMSParallelRemarkEnabled -XX:+UseCMSCompactAtFullCollection -XX:+UseFastAccessorMethods -XX:+UseCMSInitiatingOccupancyOnly -XX:+UseCompressedOops -XX:CMSInitiatingOccupancyFraction=70 -XX:+HeapDumpOnOutOfMemoryError -XX:SurvivorRatio=8 "

java  ${JAVA_DEBUG_OPTS} ${JAVA_OPTS} -DPHOENX_HOME=${PHOENIX_HOME} -Djava.ext.dirs=../target:../libs -cp ../conf com.fangcloud.phoenix.server.PhoenixStarter >>  nohup.log 2>&1 &
