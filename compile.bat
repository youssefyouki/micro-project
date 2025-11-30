@echo off
REM Check if JavaFX SDK exists
if not exist "C:\javafx-sdk-21" (
    echo JavaFX SDK not found at C:\javafx-sdk-21
    echo.
    echo Please download it from: https://gluonhq.com/products/javafx/
    echo Then extract to: C:\javafx-sdk-21
    echo.
    pause
    exit /b 1
)

echo Compiling project with JavaFX...
cd /d "%~dp0"
javac -cp ".;C:\javafx-sdk-21\lib\*" *.java Componenets/*.java Register.java

if %ERRORLEVEL% equ 0 (
    echo.
    echo Compilation successful!
    echo.
    echo To run the simulator, execute:
    echo java -cp ".;C:\javafx-sdk-21\lib\*" --add-modules javafx.controls,javafx.fxml view.MainUI
    pause
) else (
    echo Compilation failed!
    pause
)
