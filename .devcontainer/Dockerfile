FROM mcr.microsoft.com/devcontainers/base:ubuntu

# Утилиты + JDK 17
RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y \
      curl unzip zip wget ca-certificates openjdk-17-jdk && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

# Установим переменные окружения (будут в контейнере всегда)
ENV ANDROID_SDK_ROOT=/home/vscode/android-sdk
ENV ANDROID_HOME=/home/vscode/android-sdk
ENV PATH=$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/emulator
