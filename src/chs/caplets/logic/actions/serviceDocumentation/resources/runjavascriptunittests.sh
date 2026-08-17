#!/bin/sh

JSTestDriverConf=$1
JSTestDriverJar=$2
JGENHTMLDIR=$3
PHONTOMJS=$4
RESULTOUTDIR=$5
PORT=$6

echo $PORT

echo "Starting JSTD Server"

java -jar $JSTestDriverJar/JsTestDriver.jar  --port $PORT &
echo $! > $RESULTOUTDIR/jstd.pid

sleep 5s

echo "Starting PhantomJS"
$PHONTOMJS/phantomjs $PHONTOMJS/phantomjs-jstd.js &

echo $! > $RESULTOUTDIR/phantomjs.pid

echo "Starting JS unit test run"
java -jar $JSTestDriverJar/JsTestDriver.jar --browserTimeout 60000 --config $JSTestDriverConf/jsTestDriver.conf --tests all --testOutput $RESULTOUTDIR --reset

#echo "Generate JS unit test coverage"
#java -jar $JGENHTMLDIR/jgenhtml.jar $RESULTOUTDIR/jsTestDriver.conf-coverage.dat --output-directory $RESULTOUTDIR
