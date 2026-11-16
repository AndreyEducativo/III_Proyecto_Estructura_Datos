@echo off
mvn clean package
if errorlevel 1 exit /b %errorlevel%
java -ea -jar target\social-una-1.0.0.jar
