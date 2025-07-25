# 베이스 이미지로 OpenJDK 17 버전의 JRE 이미지 사용
FROM openjdk:17-jdk-slim

# 폰트 및 AWT 관련 패키지 설치
RUN apt-get update && apt-get install -y \
    fontconfig \
    fonts-dejavu-core \
    fonts-liberation \
    fonts-noto \
    libfontconfig1-dev \
    && rm -rf /var/lib/apt/lists/*

# 작업 디렉토리 설정
WORKDIR /app

# 외부에서 컨테이너의 8080 포트에 접근할 수 있도록 설정
EXPOSE 8080
EXPOSE 8081

# 외부에서 받아들일 변수 선언
#ARG JAR_FILE

# 애플리케이션의 jar 파일을 컨테이너에 추가
COPY ./build/libs/decase-0.0.1-SNAPSHOT.jar app.jar

# 애플리케이션 실행 (headless 옵션 추가)
ENTRYPOINT ["java","-Djava.awt.headless=true","-Djava.security.egd=file:/dev/./urandom","-Dspring.profiles.active=prod","-jar","app.jar"]
