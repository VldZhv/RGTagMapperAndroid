#!/usr/bin/env bash
set -euo pipefail

USER_HOME="/home/vscode"
SDK_DIR="${ANDROID_SDK_ROOT:-$USER_HOME/android-sdk}"
PROJECT_DIR="/workspace/RGTagMapperAndroid"   # если путь иной — поправьте

mkdir -p "$SDK_DIR/cmdline-tools"
cd /tmp

# Скачиваем commandline-tools (Linux)
echo "Downloading Android commandline-tools..."
curl -L -o cmdline-tools.zip "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
unzip -q cmdline-tools.zip -d "$SDK_DIR/cmdline-tools"
mv "$SDK_DIR/cmdline-tools/cmdline-tools" "$SDK_DIR/cmdline-tools/latest"

# Лицензии и базовые компоненты
yes | sdkmanager --licenses
sdkmanager "platform-tools" "cmdline-tools;latest"

# Узнаём compileSdk из gradle (по умолчанию 34, если не нашли)
COMPILE_SDK=$(grep -R "compileSdk" "$PROJECT_DIR/app/build.gradle.kts" | head -n1 | sed -E 's/[^0-9]*([0-9]+).*/\1/' || true)
if [[ -z "$COMPILE_SDK" ]]; then COMPILE_SDK=34; fi

echo "Installing platforms;android-${COMPILE_SDK} and build-tools..."
sdkmanager "platforms;android-${COMPILE_SDK}" || true
# Подберите актуальную версию build-tools под ваш compileSdk
# На 34 обычно 34.0.0:
sdkmanager "build-tools;34.0.0" || true

# Пишем local.properties с корректным SDK
echo "sdk.dir=${SDK_DIR}" > "${PROJECT_DIR}/local.properties"
chown vscode:vscode "${PROJECT_DIR}/local.properties"

# Небольшая проверка
echo "java -version:"
java -version
echo "sdkmanager location:"
which sdkmanager || true
