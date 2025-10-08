#!/usr/bin/env bash
set -euo pipefail

SDK_DIR="/home/vscode/android-sdk"
PROJECT_DIR="/workspace/RGTagMapperAndroid"

# 1) (Если gradlew оставили и успели починить права — хорошо; если удалили — этот шаг просто пропустится)
if [ -f "${PROJECT_DIR}/gradlew" ]; then
  chmod +x "${PROJECT_DIR}/gradlew" || true
fi

# 2) Установка commandline-tools
mkdir -p "$SDK_DIR/cmdline-tools"
cd /tmp
curl -fsSL -o cmdline-tools.zip "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
unzip -q cmdline-tools.zip -d "$SDK_DIR/cmdline-tools"
mv "$SDK_DIR/cmdline-tools/cmdline-tools" "$SDK_DIR/cmdline-tools/latest"

# 3) Лицензии + базовые пакеты
yes | sdkmanager --licenses
sdkmanager "platform-tools" "cmdline-tools;latest"

# 4) Платформа и build-tools под compileSdk проекта
COMPILE_SDK=$(grep -R "compileSdk" "${PROJECT_DIR}/app/build.gradle.kts" | head -n1 | sed -E 's/[^0-9]*([0-9]+).*/\1/' || true)
[ -z "$COMPILE_SDK" ] && COMPILE_SDK=34
sdkmanager "platforms;android-${COMPILE_SDK}" || true
sdkmanager "build-tools;${COMPILE_SDK}.0.0" || sdkmanager "build-tools;34.0.0" || true

# 5) Записать корректный путь SDK
echo "sdk.dir=${SDK_DIR}" > "${PROJECT_DIR}/local.properties"
chown vscode:vscode "${PROJECT_DIR}/local.properties"

# (Опционально) мини-проверка в лог
java -version || true
which sdkmanager || true
