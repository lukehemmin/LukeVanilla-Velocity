# 테스트 및 디버깅 가이드

이 가이드는 LukeVanilla 메시징 시스템의 테스트, 디버깅 및 문제 해결에 대한 정보를 제공합니다.

## 목차

- [테스트 환경 설정](#테스트-환경-설정)
- [기본 기능 테스트](#기본-기능-테스트)
- [로그 분석](#로그-분석)
- [일반적인 문제 해결](#일반적인-문제-해결)
- [성능 테스트](#성능-테스트)
- [보안 테스트](#보안-테스트)

## 테스트 환경 설정

효과적인 테스트를 위해 로컬 개발 환경을 설정하는 방법을 설명합니다.

### 로컬 테스트 서버 설정

#### 1. 디렉토리 구조 생성

```
testing/
├── velocity/
│   ├── plugins/
│   │   └── LukeVanilla-Velocity-1.0-SNAPSHOT-all.jar
│   └── velocity.toml
├── server1/
│   ├── plugins/
│   │   └── LukeVanilla-Paper-1.0-SNAPSHOT.jar
│   └── server.properties
├── server2/
│   ├── plugins/
│   │   └── LukeVanilla-Paper-1.0-SNAPSHOT.jar
│   └── server.properties
└── start.bat (또는 start.sh)
```

#### 2. Velocity 설정 (velocity.toml)

```toml
# velocity.toml

# 기본 서버 설정
bind = "0.0.0.0:25577"
motd = "LukeVanilla Test Server"
show-max-players = 100
online-mode = true
force-key-authentication = true
player-info-forwarding-mode = "modern"

# 중요: 플러그인 메시지 채널을 활성화합니다
bungee-plugin-message-channel = true

[servers]
  [servers.server1]
    address = "127.0.0.1:25565"
    
  [servers.server2]
    address = "127.0.0.1:25566"

[forced-hosts]
  "server1.localhost" = "server1"
  "server2.localhost" = "server2"

[advanced]
  compression-threshold = 256
  compression-level = 6
  login-ratelimit = 3000
  connection-timeout = 5000
  read-timeout = 30000
```

#### 3. Paper 서버 설정 (server.properties)

**server1/server.properties**:
```properties
server-port=25565
online-mode=false
velocity-support=true
velocity-secret=your_secret_key_here
```

**server2/server.properties**:
```properties
server-port=25566
online-mode=false
velocity-support=true
velocity-secret=your_secret_key_here
```

#### 4. LukeVanilla-Paper 설정 (config.yml)

**server1/plugins/LukeVanilla-Paper/config.yml**:
```yaml
server-name: "server1"
servers:
  - "server1"
  - "server2"
messaging:
  debug: true
```

**server2/plugins/LukeVanilla-Paper/config.yml**:
```yaml
server-name: "server2"
servers:
  - "server1"
  - "server2"
messaging:
  debug: true
```

#### 5. 시작 스크립트 (Windows용 start.bat)

```batch
@echo off
echo 테스트 환경을 시작합니다...

start "Velocity" /D "%~dp0velocity" java -Xms512M -Xmx512M -jar velocity.jar

timeout /t 5

start "Server1" /D "%~dp0server1" java -Xms1G -Xmx1G -jar paper.jar nogui
start "Server2" /D "%~dp0server2" java -Xms1G -Xmx1G -jar paper.jar nogui

echo 모든 서버가 시작되었습니다!
```

#### 6. 시작 스크립트 (Linux/Mac용 start.sh)

```bash
#!/bin/bash
echo "테스트 환경을 시작합니다..."

cd velocity
java -Xms512M -Xmx512M -jar velocity.jar &
VELOCITY_PID=$!

sleep 5

cd ../server1
java -Xms1G -Xmx1G -jar paper.jar nogui &
SERVER1_PID=$!

cd ../server2
java -Xms1G -Xmx1G -jar paper.jar nogui &
SERVER2_PID=$!

echo "모든 서버가 시작되었습니다!"
echo "Velocity PID: $VELOCITY_PID"
echo "Server1 PID: $SERVER1_PID"
echo "Server2 PID: $SERVER2_PID"
```

## 기본 기능 테스트

메시징 시스템의 기본 기능을 테스트하는 방법입니다.

### 테스트 1: 프록시 → 서버 메시지

1. Velocity 프록시에 관리자로 로그인합니다.
2. 다음 명령어를 실행합니다:
   ```
   /sendmessage server1 테스트 메시지입니다
   ```
3. server1에서 메시지가 수신되는지 확인합니다.
4. 서버 로그에서 디버그 메시지를 확인합니다.

### 테스트 2: 서버 → 서버 메시지

1. server1에 관리자로 로그인합니다.
2. 다음 명령어를 실행합니다:
   ```
   /sendmessage server2 server1에서 보낸 테스트 메시지입니다
   ```
3. server2에서 메시지가 수신되는지 확인합니다.
4. 양쪽 서버의 로그에서 디버그 메시지를 확인합니다.

### 테스트 3: 권한 확인

1. 권한이 없는 일반 플레이어로 로그인합니다.
2. 다음 명령어를 실행합니다:
   ```
   /sendmessage server2 테스트 메시지
   ```
3. 권한 거부 메시지가 표시되는지 확인합니다.

### 테스트 4: 존재하지 않는 서버 테스트

1. 관리자로 로그인합니다.
2. 존재하지 않는 서버로 메시지를 전송합니다:
   ```
   /sendmessage nonexistent 이 메시지는 전송되지 않아야 합니다
   ```
3. 오류 메시지가 표시되는지 확인합니다.

## 로그 분석

디버그 모드에서 로그를 분석하여 문제를 식별하는 방법입니다.

### Velocity 로그 확인

Velocity 로그(`logs/latest.log`)에서 다음 항목을 확인합니다:

1. 플러그인 초기화 메시지:
   ```
   [INFO] LukeVanilla-Velocity 메시징 시스템이 초기화되었습니다.
   [INFO] 등록된 채널: custom:msg
   ```

2. 메시지 전송 관련 로그:
   ```
   [INFO] 프록시에서 'server1' 서버로 메시지 전송: 테스트 메시지
   ```

3. 오류 메시지:
   ```
   [ERROR] 대상 서버를 찾을 수 없습니다: nonexistent
   [ERROR] 메시지 전송 중 오류 발생: java.lang.Exception
   ```

### Paper 로그 확인

각 Paper 서버의 로그(`logs/latest.log`)에서 다음 항목을 확인합니다:

1. 플러그인 초기화 메시지:
   ```
   [INFO] LukeVanilla-Paper 메시징 시스템이 초기화되었습니다.
   [INFO] 서버 이름: server1
   [INFO] 메시징 채널 등록 완료: custom:msg
   ```

2. 메시지 수신 관련 로그:
   ```
   [INFO] 프록시로부터 메시지 수신: 테스트 메시지입니다
   ```

3. 메시지 전송 관련 로그:
   ```
   [INFO] 'server2' 서버로 메시지 전송 완료 (34 bytes)
   ```

4. 오류 메시지:
   ```
   [SEVERE] 메시지 처리 중 오류 발생: java.lang.Exception
   [WARNING] 메시지 전송 실패: 플레이어가 연결되어 있지 않습니다.
   ```

### 디버그 로그 활성화

Paper 플러그인에서 더 자세한 디버그 로그를 활성화하려면:

1. `plugins/LukeVanilla-Paper/config.yml` 파일에서 다음 설정을 수정합니다:
   ```yaml
   messaging:
     debug: true
   ```

2. 서버를 재시작하거나 `/reload confirm` 명령어를 실행합니다.

## 일반적인 문제 해결

### 문제 1: 메시지가 전송되지 않음

**증상**: 메시지를 전송했지만 대상 서버에서 수신되지 않습니다.

**해결 방법**:

1. `velocity.toml`에서 `bungee-plugin-message-channel = true`로 설정되어 있는지 확인합니다.
2. 대상 서버가 실행 중이고 Velocity에 정상적으로 연결되어 있는지 확인합니다:
   ```
   /server
   ```
3. Paper 서버의 `config.yml`에서 `server-name` 값이 Velocity의 서버 이름과 일치하는지 확인합니다.
4. 서버 로그에서 오류 메시지를 확인합니다.

### 문제 2: "플레이어가 연결되어 있지 않습니다" 오류

**증상**: Paper 서버에서 메시지를 전송할 때 "플레이어가 연결되어 있지 않습니다" 오류가 발생합니다.

**해결 방법**:

1. Paper 서버에 최소한 한 명의 플레이어가 접속해 있는지 확인합니다. (서버 간 메시지 전송에는 플레이어 연결이 필요합니다)
2. 콘솔에서 명령어를 실행한 경우, 접속한 플레이어가 있는지 확인합니다.
3. 플레이어가 없는 경우 대안 구현을 고려합니다:
   - Redis 또는 다른 메시징 시스템 사용
   - 주기적으로 서버 간 통신을 시도하는 대기 메커니즘 구현

### 문제 3: 권한 오류

**증상**: "해당 명령어를 실행할 권한이 없습니다" 오류가 표시됩니다.

**해결 방법**:

1. 플레이어에게 필요한 권한이 있는지 확인합니다:
   ```
   /lp user <playername> permission info
   ```
2. 필요한 경우 권한을 부여합니다:
   ```
   /lp user <playername> permission set lukevanilla.command.sendmessage true
   ```

### 문제 4: Paper 서버가 Velocity에 연결되지 않음

**증상**: Paper 서버가 시작되지만 Velocity에 등록되지 않습니다.

**해결 방법**:

1. `server.properties`에서 다음 설정을 확인합니다:
   - `online-mode=false`
   - `velocity-support=true`
   - `velocity-secret=your_secret_key_here` (Velocity의 forwarding-secret과 일치해야 함)
2. Velocity의 `velocity.toml`에서 서버 주소가 올바르게 구성되어 있는지 확인합니다.
3. 방화벽 설정을 확인하여 서버 간 통신이 허용되는지 확인합니다.

## 성능 테스트

메시징 시스템의 성능을 테스트하고 최적화하는 방법입니다.

### 메시지 처리량 테스트

다음 명령어를 사용하여 대량의 메시지를 전송하고 처리 시간을 측정합니다:

```
/sendmessage server1 PERF_TEST_START
(1초 대기)
/sendmessage server1 PERF_TEST_STOP
```

서버 로그에서 처리 시간을 확인합니다:
```
[INFO] 성능 테스트 시작: system-time-millis
[INFO] 성능 테스트 종료: system-time-millis
[INFO] 총 처리 시간: elapsed-ms ms
```

### 메모리 사용량 모니터링

1. 서버 시작 전과 대량의 메시지 전송 후 메모리 사용량을 비교합니다.
2. 메모리 누수가 있는지 확인합니다.
3. VisualVM 또는 JProfiler와 같은 도구를 사용하여 메모리 사용량을 분석합니다.

### 병렬 처리 테스트

여러 플레이어가 동시에 메시지를 전송할 때의 성능을 테스트합니다:

1. 여러 플레이어가 동시에 로그인합니다.
2. 각 플레이어가 다양한 대상 서버로 메시지를 전송합니다.
3. 서버 TPS(Ticks Per Second)를 모니터링하여 성능 저하가 있는지 확인합니다.

## 보안 테스트

메시징 시스템의 보안을 테스트하는 방법입니다.

### 권한 우회 테스트

1. 권한이 없는 일반 플레이어로 로그인합니다.
2. 다양한 방법으로 메시지 전송 명령어를 실행하려고 시도합니다.
3. 모든 시도가 적절하게 차단되는지 확인합니다.

### 메시지 내용 검증

1. 특수 문자나 매우 긴 메시지 등 다양한 형태의 메시지를 전송합니다.
2. 시스템이 모든 입력을 올바르게 처리하는지 확인합니다.
3. 메시지 길이 제한이 적용되는지 확인합니다.

### 서버 간 인증 테스트

1. 잘못된 `velocity-secret`을 사용하여 Paper 서버를 설정합니다.
2. 서버가 Velocity에 연결되지 않고 보안 경고가 로그에 기록되는지 확인합니다.

## 자동화된 테스트

대규모 테스트나 정기적인 테스트를 위한 자동화 방법입니다.

### JUnit 테스트 작성

플러그인 코드에 대한 단위 테스트 예시:

```java
@Test
public void testMessageFormatting() {
    // 테스트 케이스 설정
    String targetServer = "server1";
    String message = "Test message";
    
    // 메시지 형식화 로직 테스트
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    byte[] targetServerBytes = targetServer.getBytes(StandardCharsets.UTF_8);
    
    out.writeInt(targetServerBytes.length);
    out.write(targetServerBytes);
    
    byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
    out.writeInt(messageBytes.length);
    out.write(messageBytes);
    
    // 결과 확인
    byte[] result = out.toByteArray();
    
    // 메시지 디코딩 테스트
    ByteArrayDataInput in = ByteStreams.newDataInput(result);
    
    int serverNameLength = in.readInt();
    assertEquals(targetServer.length(), serverNameLength);
    
    byte[] serverNameBytes = new byte[serverNameLength];
    in.readFully(serverNameBytes);
    assertEquals(targetServer, new String(serverNameBytes, StandardCharsets.UTF_8));
    
    int msgLength = in.readInt();
    assertEquals(message.length(), msgLength);
    
    byte[] msgBytes = new byte[msgLength];
    in.readFully(msgBytes);
    assertEquals(message, new String(msgBytes, StandardCharsets.UTF_8));
}
```

### 스트레스 테스트 스크립트

다량의 메시지를 자동으로 전송하는 Python 스크립트 예시:

```python
#!/usr/bin/env python3
import socket
import time
import struct

def send_rcon_command(host, port, password, command):
    """RCON 프로토콜을 사용하여 Minecraft 서버에 명령어 전송"""
    # RCON 구현은 실제로 더 복잡합니다. 이 예시는 개념만 보여줍니다.
    pass

def stress_test(server, num_messages=100, delay=0.1):
    """지정된 서버에 다량의 메시지를 전송하는 스트레스 테스트"""
    start_time = time.time()
    
    for i in range(num_messages):
        command = f"/sendmessage server2 스트레스 테스트 메시지 #{i}"
        send_rcon_command("localhost", 25575, "password", command)
        time.sleep(delay)
    
    end_time = time.time()
    elapsed = end_time - start_time
    
    print(f"스트레스 테스트 완료:")
    print(f"- 전송된 메시지: {num_messages}")
    print(f"- 총 소요 시간: {elapsed:.2f}초")
    print(f"- 초당 메시지: {num_messages/elapsed:.2f}")

if __name__ == "__main__":
    stress_test("server1", num_messages=500, delay=0.05)
```

## 로그 자동 분석 도구

로그 파일을 자동으로 분석하여 오류 및 성능 문제를 식별하는 간단한 Python 스크립트:

```python
#!/usr/bin/env python3
import re
import sys
from collections import defaultdict

def analyze_log(log_file):
    """로그 파일을 분석하여 메시징 관련 이벤트와 오류를 보고"""
    error_count = 0
    message_count = 0
    message_times = []
    
    # 정규식 패턴
    error_pattern = r"\[(ERROR|SEVERE)\].*메시지.*"
    message_sent_pattern = r"\[INFO\].*메시지 전송.*"
    message_received_pattern = r"\[INFO\].*메시지 수신.*"
    
    with open(log_file, 'r', encoding='utf-8') as f:
        for line in f:
            if re.search(error_pattern, line):
                error_count += 1
                print(f"오류 발견: {line.strip()}")
            
            if re.search(message_sent_pattern, line) or re.search(message_received_pattern, line):
                message_count += 1
    
    print(f"\n분석 결과:")
    print(f"- 총 메시지 이벤트: {message_count}")
    print(f"- 오류 수: {error_count}")
    
    if error_count > 0:
        print(f"- 오류율: {error_count/message_count*100:.2f}%")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("사용법: python analyze_log.py <로그파일경로>")
        sys.exit(1)
        
    analyze_log(sys.argv[1])
``` 