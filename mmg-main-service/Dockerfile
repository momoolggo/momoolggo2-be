# 1. Java 21 Alpine 이미지 사용 (기존 선호 방식 유지)
FROM amazoncorretto:21-alpine

# 2. 작업 디렉토리 설정
WORKDIR /deploy

# 3. [중요] 멀티 모듈 경로 반영
# Kaniko가 루트에서 실행되므로, 모듈명을 포함한 경로에서 jar를 가져옵니다.
COPY mmg-main-service/build/libs/*.jar app.jar

# 4. 타임존 설정 (Asia/Seoul)
# Alpine 이미지는 가벼운 대신 tzdata가 없으므로 수동 설치가 필요합니다.
RUN apk add --no-cache tzdata && \
    ln -snf /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone

# 5. 환경 변수 설정
ENV TZ=Asia/Seoul
ENV JAVA_OPTS="-Duser.timezone=Asia/Seoul -Dspring.profiles.active=prod"

# 6. 실행 명령어
# 환경 변수를 참조하여 실행하도록 구성합니다.
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# 7. 포트 개방
EXPOSE 8080