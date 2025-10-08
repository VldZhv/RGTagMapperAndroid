FROM mcr.microsoft.com/devcontainers/base:ubuntu

RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y \
      curl unzip zip ca-certificates openjdk-17-jdk && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

ENV ANDROID_SDK_ROOT=/home/vscode/android-sdk
ENV ANDROID_HOME=/home/vscode/android-sdk
ENV PATH=$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/emulator
