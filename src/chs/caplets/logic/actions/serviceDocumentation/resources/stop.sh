#!/bin/sh

RESULTOUTDIR=$1

PID=`cat $RESULTOUTDIR/jstd.pid`
kill $PID

#rm -f $RESULTOUTDIR/jstd.pid


PID=`cat $RESULTOUTDIR/phantomjs.pid`
kill $PID

#rm -f $RESULTOUTDIR/phantomjs.pid

grep '^Total' $RESULTOUTDIR\\log.txt
echo please check $RESULTOUTDIR/log.txt for result.
