# 설치 및 설정 가이드

LukeVanilla-Velocity 시스템의 설치 및 설정 방법에 대한 상세한 안내입니다.

- [프로젝트 구조로 이동](project-structure.md)
- [사용자 가이드로 이동](user-guide.md)
- [개발자 API 문서로 이동](developer-api.md)
- [테스트 및 디버깅으로 이동](testing.md)

## 목차

1. [요구 사항](#요구-사항)
2. [빠른 설치 가이드](#빠른-설치-가이드)
3. [상세 설치 절차](#상세-설치-절차)
4. [설정 파일 안내](#설정-파일-안내)
5. [고급 설정](#고급-설정)
6. [업데이트 방법](#업데이트-방법)
7. [백업 및 복원](#백업-및-복원)
8. [문제 해결](#문제-해결)

## 요구 사항

LukeVanilla-Velocity 시스템을 설치하기 전에 다음 요구 사항을 확인하세요:

### 소프트웨어 요구 사항

- **Velocity 프록시**: 버전 3.0.0 이상
- **Paper 서버**: 버전 1.16.5 이상 권장
- **Java**: Java 11 이상 (Java 17 권장)

### 하드웨어 권장 사항

- **프록시 서버**: 
  - CPU: 2코어 이상
  - RAM: 1GB 이상 (4GB 권장)
  - 저장 공간: 512MB 이상
  
- **게임 서버**: 
  - CPU: 4코어 이상
  - RAM: 4GB 이상 (서버당)
  - 저장 공간: 10GB 이상 (서버당)

### 네트워크 요구 사항

- 모든 서버와 프록시 간 안정적인 네트워크 연결
- 방화벽 설정: 프록시와 서버 사이의 통신 허용
- 비공인 IP를 사용하는 경우 모든 서버가 서로 통신할 수 있어야 함

## 빠른 설치 가이드

### 1. Velocity 프록시 설정

1. [Velocity](https://velocitypowered.com/downloads) 다운로드 및 설치
2. LukeVanilla-Velocity 플러그인 JAR 파일을 `plugins` 폴더에 복사
3. Velocity 서버 재시작

### 2. Paper 서버 설정

1. [Paper](https://papermc.io/downloads) 다운로드 및 설치
2. LukeVanilla-Paper 플러그인 JAR 파일을 각 서버의 `plugins` 폴더에 복사
3. 모든 Paper 서버 재시작

### 3. velocity.toml 설정

`velocity.toml` 파일에 다음 서버 정보를 추가합니다:

```toml
[servers]
  [servers.lobby]
    address = "127.0.0.1:25565"
    restricted = false
  
  [servers.survival]
    address = "127.0.0.1:25566"
    restricted = false
```

## 상세 설치 절차

### 1. Velocity 프록시 서버 설정

#### 1.1. Velocity 다운로드 및 설치

1. [Velocity 공식 사이트](https://velocitypowered.com/downloads)에서 최신 버전을 다운로드합니다.
2. 다운로드한 JAR 파일을 새 폴더(예: `velocity-server`)에 저장합니다.
3. 다음 명령어로 Velocity를 실행합니다:

```bash
java -Xms512M -Xmx512M -XX:+UseG1GC -XX:G1HeapRegionSize=4M -XX:+UnlockExperimentalVMOptions -XX:+ParallelRefProcEnabled -XX:+AlwaysPreTouch -jar velocity-*.jar
```

4. 처음 실행 시 서버가 종료되고 `velocity.toml` 파일이 생성됩니다.

#### 1.2. velocity.toml 설정

`velocity.toml` 파일을 열고 다음과 같이 설정합니다:

```toml
# velocity.toml 기본 설정
[server]
  port = 25577
  motd = "<gradient:blue:green>LukeVanilla Network"
  show-max-players = 500
  try-sequential-ip-forwarding = false
  forward-ip = true
  forward-player-address = false
  online-mode = true
  player-info-forwarding-mode = "modern"
  announce-forge = false
  
[forced-hosts]
  # 필요한 경우 강제 호스트 설정 추가

[servers]
  [servers.lobby]
    address = "127.0.0.1:25565"
    restricted = false
  
  [servers.survival]
    address = "127.0.0.1:25566"
    restricted = false

[plugins]
  # LukeVanilla-Velocity 설정
  [plugins.lukevanilla-velocity]
    # 기본 서버 설정 (플레이어가 처음 접속할 서버)
    default-server = "survival"
    # 대체 서버 설정 (기본 서버가 오프라인일 때 사용)
    fallback-server = "lobby"
    # 핑 간격 (단위: 초)
    ping-interval = 30
    # 핑 타임아웃 (단위: 초)
    ping-timeout = 5
```

IP 주소와 포트는 실제 서버 설정에 맞게 조정하세요.

#### 1.3. LukeVanilla-Velocity 설치

1. LukeVanilla-Velocity 플러그인 JAR 파일을 다운로드합니다.
2. 다운로드한 JAR 파일을 Velocity 서버의 `plugins` 폴더에 복사합니다.
3. Velocity 서버를 재시작합니다.

### 2. Paper 서버 설정

#### 2.1. Paper 다운로드 및 설치

1. [Paper 공식 사이트](https://papermc.io/downloads)에서 최신 버전을 다운로드합니다.
2. 각 서버를 위한 별도 폴더를 만듭니다(예: `lobby-server`, `survival-server`).
3. 다운로드한 JAR 파일을 각 폴더에 복사합니다.
4. 다음 명령어로 각 서버를 실행합니다:

```bash
# Lobby 서버 (25565 포트)
java -Xms2G -Xmx2G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1 -jar paper.jar nogui

# Survival 서버 (25566 포트)
java -Xms4G -Xmx4G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1 -jar paper.jar nogui
```

#### 2.2. server.properties 설정

각 서버의 `server.properties` 파일을 편집하여 다음과 같이 설정합니다:

Lobby 서버 (`lobby-server/server.properties`):
```properties
server-port=25565
server-ip=0.0.0.0
online-mode=false
velocity-support=true
velocity-online-mode=true
velocity-secret=your_velocity_forwarding_secret
```

Survival 서버 (`survival-server/server.properties`):
```properties
server-port=25566
server-ip=0.0.0.0
online-mode=false
velocity-support=true
velocity-online-mode=true
velocity-secret=your_velocity_forwarding_secret
```

`velocity-secret`는 Velocity의 `velocity.toml` 파일에 있는 `forwarding-secret` 값과 동일해야 합니다.

#### 2.3. LukeVanilla-Paper 설치

1. LukeVanilla-Paper 플러그인 JAR 파일을 다운로드합니다.
2. 다운로드한 JAR 파일을 각 Paper 서버의 `plugins` 폴더에 복사합니다.
3. 모든 Paper 서버를 재시작합니다.

### 3. 방화벽 설정

프록시와 서버 간 통신이 가능하도록 방화벽을 설정합니다:

#### Linux (iptables)

```bash
# Velocity 프록시 포트 열기
iptables -A INPUT -p tcp --dport 25577 -j ACCEPT

# 게임 서버 포트 열기
iptables -A INPUT -p tcp --dport 25565 -j ACCEPT
iptables -A INPUT -p tcp --dport 25566 -j ACCEPT

# 서버 간 통신 허용
iptables -A INPUT -s <서버_IP> -j ACCEPT

# 변경사항 저장
iptables-save > /etc/iptables/rules.v4
```

#### Windows (Windows Firewall)

1. 제어판 > 시스템 및 보안 > Windows Defender 방화벽 > 고급 설정 > 인바운드 규칙으로 이동
2. "새 규칙" > "포트" > "TCP" 선택
3. "특정 로컬 포트" 선택 후 "25577, 25565, 25566" 입력
4. "연결 허용" 선택 > 모든 네트워크 유형 선택 > 이름 입력 (예: "Minecraft Server Ports")
5. "마침" 클릭

## 설정 파일 안내

### velocity.toml 

Velocity 프록시의 주요 설정 파일입니다. LukeVanilla-Velocity 관련 중요 설정:

```toml
[plugins]
  [plugins.lukevanilla-velocity]
    # 기본 서버 (필수)
    default-server = "survival"
    
    # 대체 서버 (필수)
    fallback-server = "lobby"
    
    # 핑 간격 (초)
    ping-interval = 30
    
    # 핑 타임아웃 (초)
    ping-timeout = 5
    
    # 디버그 모드 사용 여부
    debug-mode = false
    
    # 플레이어 자동 이동 사용 여부
    auto-move-players = true
    
    # 메시지 전송 권한
    send-message-permission = "lukevanilla.command.sendmessage"
```

### LukeVanilla-Paper 설정

Paper 서버의 LukeVanilla-Paper 플러그인 설정 파일 (`plugins/LukeVanilla-Paper/config.yml`):

```yaml
# LukeVanilla-Paper 설정

# 프록시 연결 설정
proxy:
  # 메시지 채널 이름
  message-channel: "custom:msg"
  
  # 상태 채널 이름
  status-channel: "custom:status"
  
  # 자동 상태 보고 활성화
  auto-status-report: true

# 명령어 설정
commands:
  # 메시지 전송 명령어 이름
  send-message: "sendmsg"
  
  # 상태 보고 명령어 이름
  report-status: "reportstatus"

# 메시지 처리 설정
message-handling:
  # 명령어 실행 메시지 처리 활성화
  handle-command-messages: true
  
  # 공지사항 메시지 처리 활성화
  handle-announcement-messages: true
  
  # 콘솔에 수신된 메시지 로깅
  log-messages-to-console: true
```

## 고급 설정

### 커스텀 메시지 형식 정의

LukeVanilla-Paper 설정에 커스텀 메시지 형식을 추가할 수 있습니다:

```yaml
# 커스텀 메시지 형식
custom-message-formats:
  teleport:
    prefix: "teleport:"
    handler-class: "com.yourname.yourplugin.TeleportMessageHandler"
  
  playerinfo:
    prefix: "playerinfo:"
    handler-class: "com.yourname.yourplugin.PlayerInfoMessageHandler"
```

### 성능 최적화

대규모 서버 네트워크의 경우 다음 설정을 조정하여 성능을 최적화할 수 있습니다:

```toml
[plugins]
  [plugins.lukevanilla-velocity]
    # 서버 수가 많은 경우 핑 간격 증가
    ping-interval = 60
    
    # 네트워크 지연이 심한 경우 타임아웃 증가
    ping-timeout = 10
    
    # 메시지 처리 스레드 풀 크기
    message-thread-pool-size = 4
    
    # 상태 확인 일괄 처리 크기
    status-check-batch-size = 5
```

### 보안 강화

메시지 전송 기능을 보호하기 위한 추가 설정:

```toml
[plugins]
  [plugins.lukevanilla-velocity]
    # 메시지 전송 제한 (초당)
    message-rate-limit = 10
    
    # 특정 서버에서만 메시지 전송 허용
    allowed-message-sources = ["lobby", "survival"]
    
    # 메시지 필터링 활성화
    enable-message-filtering = true
    
    # 필터링할 메시지 패턴
    filtered-message-patterns = ["(?i)badword", "spam.*pattern"]
```

## 업데이트 방법

### Velocity 플러그인 업데이트

1. Velocity 서버를 중지합니다.
2. `plugins` 폴더에서 기존 LukeVanilla-Velocity JAR 파일을 백업합니다.
3. 새 버전의 JAR 파일을 `plugins` 폴더에 복사합니다.
4. Velocity 서버를 다시 시작합니다.

### Paper 플러그인 업데이트

1. 모든 Paper 서버를 중지합니다.
2. 각 서버의 `plugins` 폴더에서 기존 LukeVanilla-Paper JAR 파일을 백업합니다.
3. 새 버전의 JAR 파일을 각 서버의 `plugins` 폴더에 복사합니다.
4. 모든 Paper 서버를 다시 시작합니다.

### 설정 파일 업데이트

새 버전에서 설정 파일 구조가 변경된 경우:

1. 기존 설정 파일을 백업합니다.
2. 서버를 시작하여 새 설정 파일을 생성합니다.
3. 기존 설정을 새 파일로 수동으로 복사합니다.

## 백업 및 복원

### 백업 생성

정기적으로 다음 항목을 백업하는 것이 좋습니다:

1. Velocity 설정 및 플러그인:
   ```bash
   tar -czf velocity-backup-$(date +%F).tar.gz velocity.toml plugins/
   ```

2. Paper 서버 설정 및 플러그인:
   ```bash
   tar -czf paper-backup-$(date +%F).tar.gz server.properties plugins/
   ```

### 백업 자동화 스크립트

다음은 Linux/macOS에서 자동 백업을 위한 간단한 스크립트 예시입니다:

```bash
#!/bin/bash

# 변수 설정
VELOCITY_DIR="/path/to/velocity-server"
PAPER_DIRS=("/path/to/lobby-server" "/path/to/survival-server")
BACKUP_DIR="/path/to/backups"
DATE=$(date +%F)

# 백업 디렉토리 생성
mkdir -p "$BACKUP_DIR/velocity-$DATE"
mkdir -p "$BACKUP_DIR/paper-$DATE"

# Velocity 백업
cp "$VELOCITY_DIR/velocity.toml" "$BACKUP_DIR/velocity-$DATE/"
cp -r "$VELOCITY_DIR/plugins/LukeVanilla-Velocity" "$BACKUP_DIR/velocity-$DATE/"

# Paper 서버 백업
for dir in "${PAPER_DIRS[@]}"; do
  server_name=$(basename "$dir")
  mkdir -p "$BACKUP_DIR/paper-$DATE/$server_name"
  cp "$dir/server.properties" "$BACKUP_DIR/paper-$DATE/$server_name/"
  cp -r "$dir/plugins/LukeVanilla-Paper" "$BACKUP_DIR/paper-$DATE/$server_name/"
done

# 오래된 백업 삭제 (30일 이상)
find "$BACKUP_DIR" -type d -name "velocity-*" -mtime +30 -exec rm -rf {} \;
find "$BACKUP_DIR" -type d -name "paper-*" -mtime +30 -exec rm -rf {} \;

echo "백업 완료: $BACKUP_DIR"
```

### 복원 방법

1. Velocity 설정 복원:
   ```bash
   cp backup/velocity.toml /path/to/velocity-server/
   cp -r backup/plugins/LukeVanilla-Velocity /path/to/velocity-server/plugins/
   ```

2. Paper 서버 설정 복원:
   ```bash
   cp backup/server.properties /path/to/paper-server/
   cp -r backup/plugins/LukeVanilla-Paper /path/to/paper-server/plugins/
   ```

## 문제 해결

### 일반적인 설치 문제

#### 플러그인이 로드되지 않음

**증상**: 로그에 플러그인이 로드되었다는 메시지가 표시되지 않습니다.

**해결책**:
1. JAR 파일이 올바른 폴더에 있는지 확인합니다.
2. JAR 파일 이름이 올바른지 확인합니다.
3. Java 버전이 요구 사항을 충족하는지 확인합니다.
4. 의존성 플러그인이 모두 설치되어 있는지 확인합니다.

#### 서버 연결 오류

**증상**: `velocity.toml`에 등록된 서버에 연결할 수 없습니다.

**해결책**:
1. 서버 주소와 포트가 올바른지 확인합니다.
2. 서버가 실행 중인지 확인합니다.
3. 방화벽 설정을 확인합니다.
4. 로컬 네트워크 연결을 테스트합니다 (예: ping 테스트).

#### 메시지 전송 실패

**증상**: 메시지가 한 서버에서 다른 서버로 전송되지 않습니다.

**해결책**:
1. 양쪽 서버에 플러그인이 올바르게 설치되어 있는지 확인합니다.
2. 메시지 채널 설정이 동일한지 확인합니다.
3. 로그 파일에서 오류 메시지를 확인합니다.
4. 서버 이름이 `velocity.toml`에 정의된 이름과 일치하는지 확인합니다.

### 설정 파일 문제 해결

#### 설정 파일이 로드되지 않음

**증상**: 로그에 설정 파일 로드 오류 메시지가 표시됩니다.

**해결책**:
1. 파일 형식(TOML, YAML)이 올바른지 확인합니다.
2. 구문 오류가 없는지 확인합니다.
3. 파일 인코딩이 UTF-8인지 확인합니다.
4. 설정 파일을 기본 설정으로 다시 생성하고 변경 사항을 수동으로 추가합니다.

### 추가 지원 받기

추가 지원이 필요한 경우:

1. 프로젝트의 GitHub 이슈 트래커를 확인합니다.
2. 디버그 모드를 활성화하여 더 많은 로그 정보를 수집합니다.
3. [Discord 서버](#)에 참여하여 도움을 요청합니다.
4. [GitHub 이슈](#)를 제출하고 다음 정보를 포함합니다:
   - 사용 중인 플러그인 버전
   - 서버 소프트웨어 및 버전
   - 오류 메시지 또는 로그 파일
   - 문제 재현 단계 