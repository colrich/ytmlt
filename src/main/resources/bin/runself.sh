#!/bin/sh

/app/.java-buildpack/open_jdk_jre/bin/java -cp /app:/app/BOOT-INF/lib/*:/app/BOOT-INF/classes/ io.gridbug.ytu.ytutility.YtUtilityApplication $1 $2 $3 $4 $5 $6
