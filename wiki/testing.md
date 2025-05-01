# 테스트 및 디버깅 가이드

LukeVanilla-Velocity 시스템의 테스트 및 디버깅 방법에 대한 상세한 안내입니다.

- [설치 및 설정 가이드로 이동](installation.md)
- [사용자 가이드로 이동](user-guide.md)
- [개발자 API 문서로 이동](developer-api.md)
- [프로젝트 구조로 이동](project-structure.md)

## 목차

1. [테스트 환경 구성](#테스트-환경-구성)
2. [로깅 및 디버깅](#로깅-및-디버깅)
3. [일반적인 문제 해결](#일반적인-문제-해결)
4. [성능 모니터링](#성능-모니터링)
5. [테스트 자동화](#테스트-자동화)
6. [버전 호환성 테스트](#버전-호환성-테스트)

## 테스트 환경 구성

### 로컬 테스트 환경 설정

로컬 테스트 환경을 구성하는 방법은 다음과 같습니다:

1. **필요한 서버 구성**:
   - 1x Velocity 프록시 서버 (포트 25577)
   - 1x Paper "survival" 서버 (포트 25565)
   - 1x Paper "lobby" 서버 (포트 25566)

2. **디렉토리 구조 예시**:
   ```
   minecraft-test-environment/
   ├── velocity/
   │   ├── velocity.jar
   │   ├── plugins/
   │   │   └── lukevanilla-velocity-1.0.0.jar
   │   └── velocity.toml
   ├── survival/
   │   ├── paper.jar
   │   ├── plugins/
   │   │   └── lukepaper-1.0.0.jar
   │   └── server.properties
   └── lobby/
       ├── paper.jar
       ├── plugins/
       │   └── lukepaper-1.0.0.jar
       └── server.properties
   ```

3. **서버 스크립트 예시**:

   Linux/macOS 시작 스크립트 (`start.sh`):

   ```bash
   #!/bin/bash

   # 디렉토리 경로 설정
   BASE_DIR="$(pwd)"
   VELOCITY_DIR="${BASE_DIR}/velocity"
   SURVIVAL_DIR="${BASE_DIR}/survival"
   LOBBY_DIR="${BASE_DIR}/lobby"

   # 서버 시작 함수
   start_server() {
       local name=$1
       local dir=$2
       local jar=$3
       local args=$4

       echo "Starting ${name} server..."
       cd "${dir}" || exit
       java -Xms512M -Xmx512M ${args} -jar "${jar}" nogui &
       cd "${BASE_DIR}" || exit
   }

   # 백엔드 서버 먼저 시작
   start_server "lobby" "${LOBBY_DIR}" "paper.jar" ""
   sleep 5
   start_server "survival" "${SURVIVAL_DIR}" "paper.jar" ""
   sleep 5

   # 프록시 서버 시작
   start_server "Velocity" "${VELOCITY_DIR}" "velocity.jar" ""

   echo "모든 서버가 시작되었습니다."
   ```

   Windows 시작 스크립트 (`start.bat`):

   ```batch
   @echo off
   setlocal

   :: 디렉토리 경로 설정
   set BASE_DIR=%cd%
   set VELOCITY_DIR=%BASE_DIR%\velocity
   set SURVIVAL_DIR=%BASE_DIR%\survival
   set LOBBY_DIR=%BASE_DIR%\lobby

   :: 백엔드 서버 먼저 시작
   echo Starting lobby server...
   start "Lobby Server" /D "%LOBBY_DIR%" java -Xms512M -Xmx512M -jar paper.jar nogui
   timeout /t 5 /nobreak > nul

   echo Starting survival server...
   start "Survival Server" /D "%SURVIVAL_DIR%" java -Xms512M -Xmx512M -jar paper.jar nogui
   timeout /t 5 /nobreak > nul

   :: 프록시 서버 시작
   echo Starting Velocity server...
   start "Velocity Server" /D "%VELOCITY_DIR%" java -Xms512M -Xmx512M -jar velocity.jar

   echo 모든 서버가 시작되었습니다.
   endlocal
   ```

4. **서버 설정 검증**:
   - 모든 서버가 올바른 포트에서 실행되는지 확인
   - Velocity에서 모든 백엔드 서버가 등록되었는지 확인
   - 플러그인이 모든 서버에 제대로 로드되었는지 확인

### Docker 테스트 환경 (선택 사항)

Docker를 사용하여 테스트 환경을 구성할 수도 있습니다:

1. **Docker Compose 파일 예시** (`docker-compose.yml`):

```yaml
version: '3'

services:
  velocity:
    image: itzg/minecraft-server
    environment:
      TYPE: VELOCITY
      VELOCITY_SECRET: your_secret_key
    ports:
      - "25577:25577"
    volumes:
      - ./velocity-data:/server
      - ./plugins/velocity:/server/plugins
    depends_on:
      - survival
      - lobby

  survival:
    image: itzg/minecraft-server
    environment:
      TYPE: PAPER
      EULA: "TRUE"
      SERVER_PORT: 25565
      ONLINE_MODE: "FALSE"
      VELOCITY_SECRET: your_secret_key
    volumes:
      - ./survival-data:/data
      - ./plugins/paper:/data/plugins

  lobby:
    image: itzg/minecraft-server
    environment:
      TYPE: PAPER
      EULA: "TRUE"
      SERVER_PORT: 25566
      ONLINE_MODE: "FALSE"
      VELOCITY_SECRET: your_secret_key
    volumes:
      - ./lobby-data:/data
      - ./plugins/paper:/data/plugins
```

2. **Docker 환경 시작**:

```bash
docker-compose up -d
```

## 로깅 및 디버깅

### 로그 레벨 조정

디버깅을 위해 로그 레벨을 조정할 수 있습니다. Velocity와 Paper 모두 Log4j2를 사용합니다.

#### Velocity 로그 레벨 조정

`velocity/log4j2.xml` 파일을 생성하거나 수정합니다:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN">
    <Appenders>
        <Console name="Console" target="SYSTEM_OUT">
            <PatternLayout pattern="%highlight{[%d{HH:mm:ss} %level]: %msg%n}"/>
        </Console>
        <RollingRandomAccessFile name="File" fileName="logs/latest.log" filePattern="logs/%d{yyyy-MM-dd}-%i.log.gz">
            <PatternLayout pattern="[%d{HH:mm:ss}] [%t/%level]: %msg%n"/>
            <Policies>
                <TimeBasedTriggeringPolicy/>
                <OnStartupTriggeringPolicy/>
            </Policies>
        </RollingRandomAccessFile>
    </Appenders>
    <Loggers>
        <!-- LukeVanilla 로그 레벨을 DEBUG로 설정 -->
        <Logger name="com.lukehemmin.lukeVanillaVelocity" level="debug" additivity="false">
            <AppenderRef ref="Console"/>
            <AppenderRef ref="File"/>
        </Logger>
        <Root level="info">
            <AppenderRef ref="Console"/>
            <AppenderRef ref="File"/>
        </Root>
    </Loggers>
</Configuration>
```

#### Paper 로그 레벨 조정

`plugins/LukePaper/config.yml` 파일에 다음 설정을 추가:

```yaml
# 디버그 모드 활성화
debug: true
```

### 중요 로그 메시지 이해하기

#### 서버 상태 관련 로그

```
[INFO] 서버 상태 관리자가 초기화되었습니다.
[INFO] 서버 상태 확인 태스크가 시작되었습니다 (간격: 30000ms)
[DEBUG] survival 서버로 ping 메시지 전송됨
[DEBUG] survival 서버에서 pong 응답 수신됨
[INFO] survival 서버 상태 변경됨: 온라인
```

#### 메시지 전송 관련 로그

```
[DEBUG] 'survival' 서버로 메시지 전송 완료 (32 bytes)
[INFO] 프록시에서 'survival' 서버로 메시지 전송: 안녕하세요!
```

#### 플레이어 라우팅 관련 로그

```
[INFO] 플레이어 Player1의 초기 연결을 처리합니다.
[INFO] 플레이어 Player1(을)를 survival 서버로 연결합니다.
[INFO] 메인 서버가 온라인이 되어 로비의 플레이어 3명을 이동시킵니다.
[INFO] 플레이어 Player2(을)를 survival 서버로 이동시킵니다.
```

### 오류 로그 분석

#### 일반적인 오류 메시지와 해결 방법

| 오류 메시지 | 가능한 원인 | 해결 방법 |
|---|---|---|
| `대상 서버를 찾을 수 없습니다: survival` | 서버가 velocity.toml에 등록되지 않음 | velocity.toml에 서버 설정 추가 |
| `메시지 전송 중 오류 발생: Connection refused` | 대상 서버가 실행되지 않음 | 서버 시작 또는 네트워크 설정 확인 |
| `잘못된 대상 서버 이름 길이: -1` | 메시지 형식 오류 | 메시지 생성 코드 확인 |
| `메시지를 전송할 플레이어가 없습니다` | 서버에 플레이어가 없음 | 최소 한 명의 플레이어가 필요 |
| `메인 서버(survival)를 찾을 수 없습니다` | 서버 이름 불일치 | velocity.toml과 플러그인 코드의 서버 이름 확인 |

## 일반적인 문제 해결

### 연결 문제

#### 프록시-서버 연결 문제

**증상**: 프록시가 백엔드 서버에 연결하지 못합니다.

**진단 절차**:
1. Velocity 로그에서 연결 오류 확인
2. 백엔드 서버가 실행 중인지 확인
3. 포트 및 IP 주소 설정 확인
4. 방화벽 설정 확인

**해결 방법**:
- velocity.toml의 서버 주소 및 포트 수정
- 백엔드 서버 재시작
- 방화벽 규칙 업데이트

#### 플레이어 연결 문제

**증상**: 플레이어가 특정 서버로 연결되지 않습니다.

**진단 절차**:
1. 로그에서 자동 라우팅 관련 메시지 확인
2. 서버 상태가 올바르게 감지되었는지 확인
3. 플레이어의 현재 서버 확인

**해결 방법**:
- `/checkserver <서버명>` 명령어로 서버 상태 수동 확인
- 서버 상태 확인 간격 및 타임아웃 조정
- 수동으로 플레이어를 서버로 이동 (`/server <서버명>`)

### 메시지 전송 문제

#### 메시지가 서버에 도달하지 않음

**증상**: 한 서버에서 다른 서버로 메시지가 전송되지 않습니다.

**진단 절차**:
1. 두 서버 모두에서 플러그인이 활성화되었는지 확인
2. 메시지 채널이 올바르게 등록되었는지 확인
3. 메시지 형식이 올바른지 확인

**해결 방법**:
- 두 서버에서 플러그인 재로드
- velocity.toml에서 `bungee-plugin-message-channel = true` 확인
- 메시지 인코딩 및 디코딩 로직 확인

#### 메시지 형식 오류

**증상**: 메시지가 전송되지만 수신 측에서 파싱 오류가 발생합니다.

**진단 절차**:
1. 디버그 모드에서 원시 메시지 내용 확인
2. 메시지 인코딩/디코딩 로직 검토
3. 특수 문자 또는 길이 문제 확인

**해결 방법**:
- 메시지 길이 제한 준수
- 특수 문자 처리 방식 수정
- 버퍼 관리 코드 개선

### 서버 상태 감지 문제

#### 서버 상태가 올바르게 감지되지 않음

**증상**: 실행 중인 서버가 오프라인으로 표시되거나 그 반대의 경우가 발생합니다.

**진단 절차**:
1. ping/pong 메시지 교환 확인
2. 응답 시간 및 타임아웃 설정 검토
3. 네트워크 지연 측정

**해결 방법**:
- `pingInterval` 및 `responseTimeout` A값 조정
- 네트워크 연결 개선
- 서버 상태 확인 로직 수정

## 성능 모니터링

### 리소스 사용량 모니터링

#### CPU 및 메모리 사용량

LukeVanilla-Velocity 시스템의 리소스 사용량을 모니터링하는 방법:

1. **Spark 플러그인 사용**:
   - [Spark](https://www.spigotmc.org/resources/spark.57242/) 플러그인을 설치하여 CPU 및 메모리 사용량 모니터링
   - `/spark profiler` 명령어로 프로파일링 수행

   ```
   /spark profiler --timeout 30s
   ```

2. **플러그인 모니터링**:
   - [Plan](https://www.spigotmc.org/resources/plan-player-analytics.32536/) 플러그인을 사용하여 서버 성능 및 플러그인 사용량 모니터링

3. **서버 내 리소스 모니터링**:
   - `/tps` 명령어로 서버 TPS 모니터링
   - `/gc` 명령어로 가비지 컬렉션 수행 및 메모리 상태 확인

### 네트워크 트래픽 분석

1. **서버 간 메시지 양 확인**:
   ```
   [DEBUG] 'survival' 서버로 메시지 전송 완료 (32 bytes)
   ```

2. **상태 확인 메시지 양 측정**:
   - 각 ping/pong 메시지는 약 10-15 bytes
   - 기본 설정(30초 간격)에서 서버당 월간 약 1.5MB 트래픽 발생

3. **트래픽 최적화 방법**:
   - 불필요한 상태 확인 메시지 줄이기 (간격 조정)
   - 메시지 압축 또는 효율적인 형식 사용

## 테스트 자동화

### 단위 테스트 및 통합 테스트

프로젝트에는 다음과 같은 테스트가 포함되어 있습니다:

1. **단위 테스트**: 개별 클래스 및 기능 테스트
   - `ServerStatusManagerTest`: 서버 상태 관리 로직 테스트
   - `MessageFormatTest`: 메시지 인코딩/디코딩 테스트

2. **통합 테스트**: 여러 구성 요소 간의 상호 작용 테스트
   - `ServerRoutingTest`: 플레이어 라우팅 로직 테스트
   - `MessageRoutingTest`: 서버 간 메시지 라우팅 테스트

### 테스트 실행 방법

Gradle을 사용하여 테스트를 실행할 수 있습니다:

```bash
# 모든 테스트 실행
./gradlew test

# 특정 테스트 실행
./gradlew test --tests "com.lukehemmin.lukeVanillaVelocity.ServerStatusManagerTest"
```

### 부하 테스트

부하 테스트를 수행하여 시스템의 안정성과 성능을 평가할 수 있습니다:

1. **테스트 봇 사용**:
   - [Mineflayer](https://github.com/PrismarineJS/mineflayer) 라이브러리를 사용하여 테스트 봇 구현
   - 다수의 클라이언트 연결 시뮬레이션

2. **메시지 부하 테스트 스크립트**:

   ```javascript
   // message_load_test.js
   const mineflayer = require('mineflayer');
   
   // 설정
   const NUM_BOTS = 10;
   const SERVER_HOST = 'localhost';
   const SERVER_PORT = 25577;
   
   // 봇 생성 및 명령어 실행
   const bots = [];
   
   function createBot(index) {
     const bot = mineflayer.createBot({
       host: SERVER_HOST,
       port: SERVER_PORT,
       username: `TestBot${index}`,
       version: '1.19.4'
     });
     
     bot.once('spawn', () => {
       console.log(`Bot ${bot.username} spawned`);
       
       // 정기적으로 메시지 명령어 실행
       setInterval(() => {
         bot.chat(`/sendmessage survival 이것은 부하 테스트 메시지입니다. 봇: ${bot.username}`);
       }, 5000 + (index * 500)); // 봇마다 약간의 시간차를 두어 부하 분산
     });
     
     return bot;
   }
   
   // 봇 생성
   for (let i = 0; i < NUM_BOTS; i++) {
     setTimeout(() => {
       bots.push(createBot(i));
     }, i * 2000); // 2초 간격으로 봇 접속
   }
   ```

3. **실행 방법**:
   ```bash
   node message_load_test.js
   ```

## 버전 호환성 테스트

### Minecraft 버전 호환성

LukeVanilla-Velocity 시스템은 다음 버전에서 테스트되었습니다:

| 구성 요소 | 최소 버전 | 권장 버전 | 최대 테스트 버전 |
|----------|----------|----------|----------------|
| Velocity | 3.2.0    | 3.2.0+   | 3.3.0-SNAPSHOT |
| Paper    | 1.19.4   | 1.20.1+  | 1.20.4         |
| Java     | 17       | 17+      | 21             |

### 다양한 환경에서의 테스트

1. **온라인 모드와 오프라인 모드**:
   - 온라인 모드 (추천): Velocity 프록시는 인증을 처리하고 백엔드 서버는 오프라인 모드
   - 오프라인 모드: 모든 서버가 오프라인 모드로 설정될 수 있지만 보안 위험 존재

2. **IP 포워딩 모드**:
   - MODERN (추천): 더 안전한 포워딩 방식
   - LEGACY: 이전 버전 호환성을 위해 지원됨
   - NONE: 포워딩 없음, IP 정보가 서버 간에 공유되지 않음

3. **다양한 서버 수 구성**:
   - 최소 구성: 1 프록시 + 2 백엔드 서버 (survival, lobby)
   - 중간 구성: 1 프록시 + 3-5 백엔드 서버 (다양한 게임 모드)
   - 대규모 구성: 1 프록시 + 10+ 백엔드 서버 (테스트 완료)

### 호환성 문제 해결

1. **버전 불일치 문제**:
   - 증상: `NoSuchMethodError` 또는 `ClassNotFoundException` 오류
   - 해결: 모든 서버와 프록시가 호환되는 버전을 사용하도록 확인

2. **API 변경 문제**:
   - 증상: Velocity API 변경으로 인한 컴파일 오류
   - 해결: 코드를 최신 API에 맞게 업데이트하고 다시 빌드

3. **플러그인 충돌**:
   - 증상: 다른 플러그인과의 충돌로 인한 예기치 않은 동작
   - 해결: 충돌하는 플러그인 식별 및 호환성 패치 적용

더 자세한 정보는 [개발자 API 문서](developer-api.md)와 [프로젝트 구조](project-structure.md) 문서를 참조하세요. 