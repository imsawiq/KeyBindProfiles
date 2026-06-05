@echo off
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\build-version.ps1" %*
exit /b %ERRORLEVEL%
