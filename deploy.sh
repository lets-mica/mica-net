#!/bin/sh

## 1. java version
java -version
printf "\n"

## 2. mvn version
mvn -version
printf "\n"

## 3. 环境
if [ -z $1 ]; then
    profile="release"
else
    profile="$1"
fi

## 4. deploy
mvn clean package
mvn deploy -P$profile -pl mica-net-utils,mica-net-core,mica-net-http
