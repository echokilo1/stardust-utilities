#!/bin/bash

mkdir -p traces
mkdir -p data
mkdir -p dots
rm -rf traces/*
rm -rf data/*
rm -rf dots/*

java -cp lib/xtrace-2.2-11_2011.jar:lib/hbase-0.94.4.jar:lib/hadoop-core-1.0.4.jar:lib/commons-logging-1.1.1.jar:lib/log4j-1.2.16.jar:lib/slf4j-log4j12-1.5.8.jar:lib/slf4j-api-1.5.8.jar:lib/commons-configuration-1.6.jar:lib/commons-lang-2.5.jar:lib/zookeeper-3.4.5.jar:lib/protobuf-java-2.4.0a.jar:. HBaseToFile report.out
cp report.out traces/

./collect.sh traces
rm -rf data/derby.log
rm -rf data/tasks

./generate.sh data

for f in `ls -1 dots`;
do
    dot -Tpdf dots/$f -o dots/$f.pdf
done