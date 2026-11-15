@echo off
setlocal
set APP_HOME=%~dp0
set GRADLE_VERSION=8.9
set DIST=%USERPROFILE%\.gradle\universal-wrapper\gradle-%GRADLE_VERSION%
if exist "%DIST%\bin\gradle.bat" goto runGradle
where gradle >nul 2>nul
if %ERRORLEVEL% EQU 0 (gradle -p "%APP_HOME%" %* & exit /b %ERRORLEVEL%)
set WRAPPER_DIR=%USERPROFILE%\.gradle\universal-wrapper
set ARCHIVE=%WRAPPER_DIR%\gradle-%GRADLE_VERSION%-bin.zip
if not exist "%WRAPPER_DIR%" mkdir "%WRAPPER_DIR%"
if not exist "%ARCHIVE%" powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%ARCHIVE%'"
if not exist "%ARCHIVE%" exit /b 1
powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -LiteralPath '%ARCHIVE%' -DestinationPath '%WRAPPER_DIR%' -Force"
:runGradle
call "%DIST%\bin\gradle.bat" -p "%APP_HOME%" %*
exit /b %ERRORLEVEL%
