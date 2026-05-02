#!/bin/bash
JAR="$(dirname "$0")/target/erc20-deploy-app-1.0.0-jar-with-dependencies.jar"

if [ ! -f "$JAR" ]; then
    echo "JAR not found. Building..."
    cd "$(dirname "$0")" && mvn clean package -q
fi

java -jar "$JAR"
