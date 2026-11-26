@echo off
cd /d "%~dp0"
mvn exec:java
if errorlevel 1 pause
