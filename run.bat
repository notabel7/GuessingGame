@echo off
echo Starting Number Guessing Game...
java -Dsun.java2d.uiScale.enabled=true -Dawt.useSystemAAFontSettings=on -Dswing.aatext=true -cp "out;lib\svgSalamander.jar" Main
if %errorlevel% neq 0 (
    echo.
    echo Application exited with error. Run compile.bat first if you haven't already.
    pause
)
