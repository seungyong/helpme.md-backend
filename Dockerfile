# syntax=docker/dockerfile:1
# 멀티 스테이지 빌드 사용

# 첫 번째 스테이지: 애플리케이션 빌드
# Eclipse Temurin 17 JDK Alpine 이미지를 불러와 'app-builder'라는 이름으로 지정
FROM eclipse-temurin:17-jdk-alpine AS app-builder

# 작업 디렉토리를 /workspace/app으로 설정
WORKDIR /workspace/app

# --mount=type=cache : 라이브러리 다운로드한 것을 캐싱
# --mount=type=bind : 내 컴퓨터의 소스파일을 복사하는 대신, 잠시 연결해서 읽기 (Docker가 내 컴퓨터의 파일을 읽어서 빌드하는 방식)
RUN --mount=type=cache,target=/root/.gradle \
    --mount=type=bind,source=gradlew,target=gradlew \
    --mount=type=bind,source=build.gradle,target=build.gradle \
    --mount=type=bind,source=settings.gradle,target=settings.gradle \
    --mount=type=bind,source=gradle,target=gradle \
    --mount=type=bind,source=src,target=src \
    ./gradlew clean build -x test # 테스트는 제외하고 빌드

# 빌드된 JAR 파일에서 필요한 라이브러리를 추출하여 build/dependency 디렉토리에 저장
# 라이브러리와 코드를 분리함으로써, 캐싱이 더 효과적으로 작동하도록 함
RUN mkdir -p build/dependency && (cd build/dependency; jar -xf ../libs/*-SNAPSHOT.jar)

# 두 번째 스테이지: JRE 생성 및 애플리케이션 실행 환경 설정
FROM eclipse-temurin:17-jdk-alpine AS jre-builder

# jlink를 사용하여 필요한 모듈만 포함된 커스텀 JRE를 생성
RUN jlink \
    # 필요한 Java 모듈만 추가하여 JRE를 생성
    --add-modules java.base,java.logging,java.naming,java.desktop,java.management,java.security.jgss,java.instrument,java.sql,jdk.unsupported \
    # 디버깅 정보 제거
    --strip-debug \
    # 설명서 제거
    --no-man-pages \
    # 헤더 파일 제거
    --no-header-files \
    # 압축 레벨 설정 (0-2, 0은 압축 안함, 2는 최대 압축)
    --compress=2 \
    # 생성된 JRE를 /customjre 디렉토리에 저장
    --output /customjre

# 세 번째 스테이지: 최종 이미지 설정
FROM alpine:latest

# 자바가 어딨는지 환경 변수로 알려줌
ENV JAVA_HOME=/jre
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# 두 번째 스테이지에서 생성된 커스텀 JRE를 복사하여 최종 이미지에 포함
COPY --from=jre-builder /customjre $JAVA_HOME

WORKDIR /app

# 톰캣 (스프링 내장 서버)이 임시 파일을 저장할 수 있도록 /tmp 디렉토리를 볼륨으로 설정
VOLUME /tmp

# 경로를 변수로 지정
ARG DEPENDENCY=/workspace/app/build/dependency

# 첫 번째 스테이지에서 빌드된 애플리케이션의 라이브러리와 코드를 복사하여 최종 이미지에 포함
COPY --from=app-builder ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=app-builder ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=app-builder ${DEPENDENCY}/BOOT-INF/classes /app

# 실행
ENTRYPOINT ["java", "-cp", "/app:/app/lib/*", "seungyong.helpmebackend.HelpmeBackendApplication"]
