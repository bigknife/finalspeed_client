#!/bin/sh

function stop_fs(){
    echo "Stopping finalspeed client ... "
    ps -ef|grep client.jar|grep -v grep|awk '{print $2}'|xargs kill -9
}

function start_fs(){
    echo "Starting finalspeed client ... "
    cd ~/fs
    java -jar client.jar >/dev/null 2>&1 &
}

case $1 in
    start)
    start_fs
    ;;
    stop)
    stop_fs
    ;;
esac
exit 0