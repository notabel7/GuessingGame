@echo off
echo Compiling Number Guessing Game...
echo.

if not exist "lib\svgSalamander.jar" (
    echo ERROR: lib\svgSalamander.jar not found.
    echo Please copy svgSalamander.jar into the lib\ folder.
    pause
    exit /b 1
)

if not exist "out" mkdir out

echo Copying icons...
if exist "resources\icons" (
    if not exist "out\icons" mkdir out\icons
    xcopy /s /y "resources\icons\*" "out\icons\" >nul 2>&1
)

echo Copying audio...
if exist "resources\audio" (
    if not exist "out\audio" mkdir out\audio
    xcopy /s /y "resources\audio\*" "out\audio\" >nul 2>&1
)

echo Compiling sources...
javac -g -cp "lib\svgSalamander.jar" -d out -sourcepath src src\Main.java

if %errorlevel% equ 0 (
    echo.
    echo  Compilation successful! Run run.bat to start the game.
) else (
    echo.
    echo  Compilation failed. Check the errors above.
    pause
)
