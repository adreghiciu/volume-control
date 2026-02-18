FROM eclipse-temurin:21-jdk-jammy

# Install Android SDK dependencies
RUN apt-get update && apt-get install -y \
    wget \
    unzip \
    git \
    && rm -rf /var/lib/apt/lists/*

# Download and install Gradle 8.10
RUN cd /opt && \
    wget -q https://services.gradle.org/distributions/gradle-8.10-bin.zip && \
    unzip -q gradle-8.10-bin.zip && \
    rm gradle-8.10-bin.zip && \
    ln -s /opt/gradle-8.10/bin/gradle /usr/local/bin/gradle

ENV PATH="/opt/gradle-8.10/bin:$PATH"

# Set up Android SDK
ENV ANDROID_HOME=/opt/android-sdk
RUN mkdir -p $ANDROID_HOME

# Download and install Android SDK command-line tools
RUN wget -q https://dl.google.com/android/repository/commandlinetools-linux-9123335_latest.zip -O /tmp/cmdline-tools.zip && \
    unzip -q /tmp/cmdline-tools.zip -d $ANDROID_HOME && \
    mv $ANDROID_HOME/cmdline-tools $ANDROID_HOME/cmdline-tools-latest && \
    mkdir -p $ANDROID_HOME/cmdline-tools && \
    mv $ANDROID_HOME/cmdline-tools-latest $ANDROID_HOME/cmdline-tools/latest && \
    rm /tmp/cmdline-tools.zip

# Accept Android SDK licenses
RUN mkdir -p $ANDROID_HOME/licenses && \
    echo -e "\n24333f8a63b6825ea9c5514f83c2829b004d1fee" > $ANDROID_HOME/licenses/android-sdk-license && \
    echo -e "\n84831b9409646a918e30573bab4c9c91346d8abd" > $ANDROID_HOME/licenses/android-sdk-preview-license

# Update PATH
ENV PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH

# Install Android SDK components
RUN sdkmanager --no_https "platform-tools" && \
    sdkmanager --no_https "platforms;android-34" && \
    sdkmanager --no_https "build-tools;34.0.0"

# Set working directory
WORKDIR /workspace

# Copy the entire project
COPY . .

# Build the APK using gradle with full stacktrace
RUN gradle assembleDebug --stacktrace

# Rename and copy APK to output directory
RUN cp app/build/outputs/apk/debug/app-debug.apk /workspace/VolumeControl.apk

# Verify APK was created
CMD ["ls", "-lh", "/workspace/VolumeControl.apk"]
