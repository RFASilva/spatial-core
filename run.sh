#!/bin/sh
export MAVEN_OPTS='-Xms512M -Xmx1024M'
mvn compile
mvn exec:java 2>&1 | tee logs/status.log