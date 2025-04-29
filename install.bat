@echo off
setlocal enabledelayedexpansion

echo ================================
echo Step 1: Cleaning and compiling with Maven...
echo ================================
mvn clean install
if %errorlevel% neq 0 (
    echo Maven build failed! Exiting...
    pause
    exit /b %errorlevel%
)
pause

echo ================================
echo Step 2: Creating custom runtime with jlink...
echo ================================
rmdir /s /q target\runtime

"C:\Program Files\Java\jdk-24\bin\jlink.exe" ^
  --module-path "C:/Program Files/Java/jdk-24/jmods;C:/Program Files/javafx-sdk-24.0.1/lib;target/classes" ^
  --add-modules com.example.mantracount,java.base,java.desktop,java.logging,jdk.unsupported,javafx.controls,javafx.fxml,javafx.graphics,javafx.base,javafx.swing,javafx.media ^
  --launcher mantracount=com.example.mantracount/com.example.mantracount.App ^
  --output target/runtime

if %errorlevel% neq 0 (
    echo Jlink failed! Exiting...
    pause
    exit /b %errorlevel%
)
pause

echo ================================
echo Step 3: Copying JavaFX DLLs into runtime/bin...
echo ================================
xcopy /Y /S "C:\Program Files\javafx-sdk-24.0.1\bin\*.dll" "target\runtime\bin\"

if %errorlevel% neq 0 (
    echo Copying DLLs failed! Exiting...
    pause
    exit /b %errorlevel%
)
pause

echo ================================
echo Step 4: Packaging application with jpackage...
echo ================================
rmdir /s /q target\output

jpackage --type exe ^
  --input target/ ^
  --dest target/ ^
  --name MantraCount ^
  --main-jar MantraCount.jar ^
  --main-class com.example.mantracount.App ^
  --runtime-image target/runtime ^
  --icon "C:/Users/tashi.TASHI-LENOVO/OneDrive/Desktop/Darma/Mantras/MantraCountUI/src/main/resources/icons/BUDA.ico" ^
  --win-shortcut ^
  --win-menu ^
  --win-console ^
  --app-version 3.0 ^
  --vendor "Tashi Rabten" ^
  --java-options "--enable-native-access=javafx.graphics -Dprism.order=sw,j2d -Djavafx.verbose=true"

if %errorlevel% neq 0 (
    echo Jpackage failed! Exiting...
    pause
    exit /b %errorlevel%
)

echo ================================
echo 🎉 Build complete! MantraCount.exe created!
echo ================================
pause
