# Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q clean package -DskipTests

# Runtime. Debian rather than Alpine: Tesseract's native libs and tessdata are
# packaged there, and the OCR path loads them via JNI.
FROM eclipse-temurin:21-jre
RUN apt-get update \
 && apt-get install -y --no-install-recommends tesseract-ocr tesseract-ocr-eng libtesseract-dev \
 && rm -rf /var/lib/apt/lists/*
# Tesseract's native libs live in an architecture-specific directory
# (x86_64-linux-gnu, aarch64-linux-gnu, ...). Expose them at a stable path so
# the JNI loader path in configuration does not have to name the architecture.
RUN set -eux; \
    libdir="$(dirname "$(find /usr/lib -name 'libtesseract.so*' | head -1)")"; \
    mkdir -p /opt/tesseract; \
    ln -sfn "$libdir" /opt/tesseract/lib
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Uploads live here. On Render's free tier this is ephemeral — it is wiped on
# every restart and cold start, so uploaded documents do not survive.
RUN mkdir -p /tmp/tassist-storage
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
