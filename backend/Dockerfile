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
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Uploads live here. On Render's free tier this is ephemeral — it is wiped on
# every restart and cold start, so uploaded documents do not survive.
RUN mkdir -p /tmp/tassist-storage
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
