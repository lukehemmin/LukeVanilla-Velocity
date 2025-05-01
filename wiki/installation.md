# 설치 및 설정 가이드

이 가이드는 LukeVanilla 메시징 시스템의 설치 및 구성 방법을 설명합니다.

## 목차

- [요구 사항](#요구-사항)
- [Velocity 플러그인 설치](#velocity-플러그인-설치)
- [Paper 플러그인 설치](#paper-플러그인-설치)
- [설정 파일](#설정-파일)
- [보안 고려 사항](#보안-고려-사항)
- [문제 해결](#문제-해결)

## 요구 사항

### 시스템 요구 사항

- **Velocity 버전**: 3.1.0 이상 (3.4.0 권장)
- **Paper 버전**: 1.19.4 이상 (1.20.4 권장)
- **Java 버전**: Java 17 이상
- **메모리**: 최소 512MB (Velocity), 2GB (Paper)

### 네트워크 요구 사항

- Velocity 프록시 서버와 모든 Paper 서버 간의 안정적인 네트워크 연결
- Velocity와 Paper 서버 간의 플러그인 메시지 채널 통신 허용

## Velocity 플러그인 설치

1. 최신 버전의 `LukeVanilla-Velocity-x.x.x-all.jar`를 [다운로드](https://github.com/LukeHemmin/LukeVanilla/releases)합니다.

2. 다운로드한 JAR 파일을 Velocity 서버의 `plugins` 디렉토리에 복사합니다.

3. Velocity 서버를 재시작하거나 `/reload` 명령어를 실행합니다.

4. `velocity.toml` 설정 파일에서 다음 설정을 확인/수정합니다:
   ```toml
   # velocity.toml
   
   # 이 설정이 반드시 true로 설정되어 있어야 합니다
   bungee-plugin-message-channel = true
   ```

5. 로그에서 다음과 같은 메시지를 확인하여 플러그인이 제대로 로드되었는지 확인합니다:
   ```
   [INFO] LukeVanilla-Velocity 메시징 시스템이 초기화되었습니다.
   [INFO] 등록된 채널: custom:msg
   [INFO] 명령어가 등록되었습니다: /sendmessage, /smsg
   ```

## Paper 플러그인 설치

1. 최신 버전의 `LukeVanilla-Paper-x.x.x.jar`를 [다운로드](https://github.com/LukeHemmin/LukeVanilla/releases)합니다.

2. 다운로드한 JAR 파일을 각 Paper 서버의 `plugins` 디렉토리에 복사합니다.

3. 각 서버를 재시작하거나 `/reload confirm` 명령어를 실행합니다.

4. 각 Paper 서버의 `plugins/LukeVanilla-Paper/config.yml` 파일이 자동으로 생성됩니다.

5. 로그에서 다음과 같은 메시지를 확인하여 플러그인이 제대로 로드되었는지 확인합니다:
   ```
   [INFO] LukeVanilla-Paper 메시징 시스템이 초기화되었습니다.
   [INFO] 서버 이름: server1
   [INFO] 메시징 채널 등록 완료: custom:msg
   ```

## 설정 파일

### Velocity 설정

현재 버전에서는 별도의 설정 파일이 없습니다. 추후 업데이트에서 추가될 예정입니다.

### Paper 설정

`plugins/LukeVanilla-Paper/config.yml` 파일을 수정하여 각 서버의 설정을 구성할 수 있습니다:

```yaml
# 서버 이름 (Velocity 프록시 구성의 서버 이름과 일치해야 함)
server-name: "server1"

# 서버 목록 (자동 완성 및 기타 기능에 사용)
servers:
  - "lobby"
  - "survival"
  - "creative"
  - "minigames"

# 메시징 설정
messaging:
  # 디버그 모드 (true로 설정하면 더 많은 로그 출력)
  debug: false
  
  # 전송 실패 시 재시도 횟수
  retry-count: 3
```

**중요**: 각 서버의 `server-name` 값이 Velocity의 `velocity.toml` 파일에 구성된 서버 이름과 정확히 일치해야 합니다.

### velocity.toml 서버 구성 예시

```toml
[servers]
  # 로비 서버 구성
  [servers.lobby]
    address = "127.0.0.1:25565"
    
  # 서바이벌 서버 구성
  [servers.survival]
    address = "127.0.0.1:25566"
    
  # 크리에이티브 서버 구성
  [servers.creative]
    address = "127.0.0.1:25567"
```

## 권한 설정

### Velocity 권한

- `lukevanilla.command.sendmessage` - `/sendmessage` 및 `/smsg` 명령어 사용 권한

### Paper 권한

- `lukevanilla.command.sendmessage` - `/sendmessage` 및 `/smsg` 명령어 사용 권한

기본적으로 모든 권한은 OP(관리자)에게만 부여됩니다.

## 보안 고려 사항

1. **메시지 권한 관리**: 메시지 전송 명령어 권한은 신뢰할 수 있는 스태프에게만 부여하세요.

2. **서버 간 통신**: 모든 서버 간 통신은 내부 네트워크에서만 이루어지도록 구성하세요.

3. **플레이어 필터링**: Paper 플러그인에서 메시지 처리에 플레이어 필터링 로직을 추가하는 것을 고려하세요.

## 문제 해결

### 일반적인 문제

#### 메시지가 전송되지 않음

1. `velocity.toml`에서 `bungee-plugin-message-channel = true`로 설정되어 있는지 확인
2. 모든 Paper 서버의 `server-name`이 Velocity 구성과 일치하는지 확인
3. 서버 간에 네트워크 문제가 없는지 확인

#### Paper 서버가 Velocity에 연결되지 않음

1. Paper 서버가 Velocity 프록시 정보를 올바르게 구성했는지 확인
2. 서버의 `server.properties`에서 `online-mode=false`로 설정되어 있는지 확인
3. Velocity의 forwarding secret이 올바르게 구성되었는지 확인

#### 명령어가 작동하지 않음

1. 플레이어가 필요한 권한을 가지고 있는지 확인
2. 명령어 구문이 올바른지 확인 (`/sendmessage <서버명> <메시지>`)
3. 대상 서버가 실행 중이고 프록시에 등록되어 있는지 확인

### 로그 파일

문제 해결을 위해 다음 로그 파일을 확인하세요:

- Velocity 로그: `logs/latest.log`
- Paper 서버 로그: `logs/latest.log`

디버그 모드를 활성화하여 더 자세한 로그를 확인할 수 있습니다 (Paper 플러그인 설정에서 `messaging.debug: true`로 설정).

### 추가 도움

더 많은 도움이 필요하시면:

- [이슈 트래커](https://github.com/LukeHemmin/LukeVanilla/issues)에 문제를 제출해주세요.
- [디스코드 서버](https://discord.gg/lukevanilla)에 문의하세요. 