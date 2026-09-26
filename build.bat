@echo off
setlocal
set ROOT=%~dp0
set BUILD=%ROOT%build
set CLASSES=%BUILD%\classes
if exist "%BUILD%" rmdir /s /q "%BUILD%"
mkdir "%CLASSES%"
if not exist "%ROOT%dist" mkdir "%ROOT%dist"
for /r "%ROOT%src\main\java" %%f in (*.java) do echo "%%f">>"%BUILD%\sources.txt"
javac --release 11 -encoding UTF-8 -d "%CLASSES%" @"%BUILD%\sources.txt"
if errorlevel 1 exit /b 1
copy /y "%ROOT%src\main\resources\*.b64" "%CLASSES%\" >nul
jar cfm "%ROOT%dist\DXMD-Archive-Editor-Pro-v0.6.12.jar" "%ROOT%src\main\resources\MANIFEST.MF" -C "%CLASSES%" .
if errorlevel 1 exit /b 1
echo Built: %ROOT%dist\DXMD-Archive-Editor-Pro-v0.6.12.jar
