#!/bin/sh
cpath="."
if [ -d "$1" ]
then
  for file in $1/*
  do
    for f in `ls lib/*.jar` 
    do
      cpath=$cpath:$f
    done
    java -Xmx512m -Dlog4j.configuration=log4j.properties -cp $cpath edu.berkeley.xtrace.server.XTraceCollector $file data
  done
fi
