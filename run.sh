#!/bin/bash
set -e
mvn clean package
java -ea -jar target/social-una-1.0.0.jar
