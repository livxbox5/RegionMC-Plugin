@echo off
cls
chcp 65001 >nul

set "PLUGIN_NAME=RegionMC"
set "PLUGIN_VERSION=1.0-SNAPSHOT"
set "MINECRAFT_VERSION=1.21.1-26.1.2"
set "JAR_DIR=%CD%\build\libs"
set "PAPER_REPO=https://repo.papermc.io/repository/maven-public/"

echo ================================
echo    🛠️  RegionMC Plugin Builder
echo    📦 Version: %PLUGIN_VERSION%
echo    🎯 Target: Minecraft %MINECRAFT_VERSION%
echo    💾 Output: %JAR_DIR%
echo    👤 Author: KillerYT
echo ================================

:menu
echo.
echo Выберите действие:
echo 1 - Очистка и сборка (clean build)
echo 2 - Быстрая сборка (jar)
echo 3 - Только компиляция (compileJava)
echo 4 - Запуск тестового сервера (runServer)
echo 5 - Показать задачи Gradle (tasks)
echo 6 - Проверка версии Java
echo 7 - Очистка кеша Gradle
echo 8 - Удалить файлы в папке build
echo 9 - Проверить зависимости
echo 10 - Настройки плагина
echo 11 - Выход
echo.

set /p choice="Введите номер [1-11]: "

if "%choice%"=="1" goto clean_build
if "%choice%"=="2" goto fast_build
if "%choice%"=="3" goto compile
if "%choice%"=="4" goto run_server
if "%choice%"=="5" goto tasks
if "%choice%"=="6" goto java_version
if "%choice%"=="7" goto clean_cache
if "%choice%"=="8" goto delete_build_files
if "%choice%"=="9" goto check_dependencies
if "%choice%"=="10" goto settings
if "%choice%"=="11" goto exit

echo Неверный выбор! Попробуйте снова.
goto menu

:clean_build
echo.
echo 🧹 Очистка и сборка RegionMC...
call gradlew clean build --warning-mode=none
if %ERRORLEVEL% equ 0 (
    echo ✅ Сборка успешна!
    if exist "%JAR_DIR%\*.jar" (
        echo 📦 Созданные JAR файлы:
        dir "%JAR_DIR%\*.jar" /b
    )
) else (
    echo ❌ СБОРКА ПРОВАЛИЛАСЬ!
)
echo.
pause
goto menu

:fast_build
echo.
echo ⚡ Быстрая сборка...
call gradlew jar --warning-mode=none
if %ERRORLEVEL% equ 0 (
    echo ✅ Быстрая сборка успешна!
    if exist "%JAR_DIR%\*.jar" (
        dir "%JAR_DIR%\*.jar" /b
    )
) else (
    echo ❌ Ошибка быстрой сборки
)
echo.
pause
goto menu

:compile
echo.
echo 🛠️ Компиляция Java...
call gradlew compileJava --warning-mode=none
echo.
pause
goto menu

:run_server
echo.
echo 🎮 Запуск тестового сервера...
echo ⚠️  Это может занять некоторое время при первом запуске
call gradlew runServer
echo.
pause
goto menu

:tasks
echo.
echo 📋 Список задач Gradle...
call gradlew tasks --warning-mode=none
echo.
pause
goto menu

:java_version
echo.
echo ☕ Проверка версии Java...
java -version
javac -version
echo.
echo 📍 Требуется Java 21
echo.
pause
goto menu

:clean_cache
echo.
echo 🧹 Очистка кеша Gradle...
call gradlew clean --refresh-dependencies --warning-mode=none
echo ✅ Кеш очищен
echo.
pause
goto menu

:delete_build_files
echo.
echo 🗑️ Удаление файлов в папке build...
if exist "build" (
    echo 📁 Содержимое папки build перед удалением:
    dir build /b 2>nul

    echo 🔧 Удаление файлов...
    if exist "build\classes" rmdir /s /q "build\classes"
    if exist "build\generated" rmdir /s /q "build\generated"
    if exist "build\reports" rmdir /s /q "build\reports"
    if exist "build\resources" rmdir /s /q "build\resources"
    if exist "build\tmp" rmdir /s /q "build\tmp"

    if exist "build\libs" (
        echo 📦 Очистка папки build\libs...
        del /q "build\libs\*" 2>nul
        echo ✅ Файлы в build\libs удалены
    )

    del /q "build\*" 2>nul
    echo ✅ Все файлы в папке build удалены
    echo 📁 Содержимое папки build после удаления:
    dir build /b 2>nul || echo 📁 Папка build пуста
) else (
    echo ⚠️ Папка build не найдена
)
echo.
pause
goto menu

:check_dependencies
echo.
echo 📚 Проверка зависимостей...
call gradlew dependencies --configuration compileClasspath --warning-mode=none
echo.
pause
goto menu

:settings
echo.
echo === НАСТРОЙКИ REGIONMC ===
echo Текущее название: %PLUGIN_NAME%
echo Текущая версия: %PLUGIN_VERSION%
echo Версия Minecraft: %MINECRAFT_VERSION%
echo Путь для JAR: %JAR_DIR%
echo.
set /p "new_plugin_name=Введите название плагина [Enter для пропуска]: "
if not "%new_plugin_name%"=="" set "PLUGIN_NAME=%new_plugin_name%"

set /p "new_plugin_version=Введите версию плагина [Enter для пропуска]: "
if not "%new_plugin_version%"=="" set "PLUGIN_VERSION=%new_plugin_version%"

set /p "new_mc_version=Введите версию Minecraft [Enter для пропуска]: "
if not "%new_mc_version%"=="" set "MINECRAFT_VERSION=%new_mc_version%"

set /p "new_jar_dir=Введите новый путь для JAR [Enter для пропуска]: "
if not "%new_jar_dir%"=="" set "JAR_DIR=%new_jar_dir%"

echo.
echo ✅ Новые настройки:
echo 🏷️  Название: %PLUGIN_NAME%
echo 📦 Версия: %PLUGIN_VERSION%
echo 🎯 Minecraft: %MINECRAFT_VERSION%
echo 💾 Путь для JAR: %JAR_DIR%
echo.
pause
goto menu

:exit
echo.
echo 👋 До свидания! RegionMC Builder завершен.
timeout /t 2 >nul
exit