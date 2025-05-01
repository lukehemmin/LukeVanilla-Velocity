# LukeVanilla 메시징 시스템

Minecraft 네트워크에서 Velocity 프록시와 Paper 서버 간 양방향 메시징 시스템입니다.

## 기능

- **서버 ⇄ 프록시 ⇄ 서버** 메시징: 서버 간 데이터 교환
- **프록시 ⇄ 서버** 메시징: 프록시에서 특정 서버로 명령 전송

## 구성 요소

- **LukeVanilla-Velocity**: Velocity 프록시 플러그인
- **LukeVanilla-Paper**: Paper 서버 플러그인 (구현 예정)

## 빌드 방법

### Velocity 플러그인 빌드

```bash
# 프로젝트 루트 디렉토리에서
./gradlew shadowJar
```

빌드된 JAR 파일은 `build/libs/` 디렉토리에 생성됩니다.

## 설치 방법

### Velocity 플러그인 설치

1. `build/libs/LukeVanilla-Velocity-1.0-SNAPSHOT-all.jar` 파일을 Velocity 서버의 `plugins` 디렉토리에 복사합니다.
2. Velocity 서버를 재시작합니다.
3. `velocity.toml` 설정 파일에서 `bungee-plugin-message-channel = true`로 설정되어 있는지 확인합니다.

### Paper 플러그인 설치

1. Paper 플러그인 JAR 파일을 Paper 서버의 `plugins` 디렉토리에 복사합니다.
2. 서버를 재시작합니다.
3. `plugins/LukeVanilla-Paper/config.yml` 파일에서 서버 이름을 설정합니다.

## 사용 방법

### Velocity 명령어

- `/sendmessage <서버명> <메시지>`: 프록시에서 특정 서버로 메시지 전송
- `/smsg <서버명> <메시지>`: 위 명령어의 단축 버전

### Paper 명령어

- `/sendmessage <서버명> <메시지>`: 현재 서버에서 다른 서버로 메시지 전송
- `/smsg <서버명> <메시지>`: 위 명령어의 단축 버전

## 테스트 환경 구성

1. Velocity 서버 설정:
   - `velocity.toml`에서 `bungee-plugin-message-channel = true` 설정
   - 적어도 두 개의 백엔드 서버 구성

2. Paper 서버 설정:
   - 각 서버의 `config.yml`에서 서버 이름 구성

3. 두 플러그인을 모두 설치하고 서버를 재시작

4. 명령어를 사용하여 메시지 전송 테스트

## 개발자 API

플러그인 개발자는 이 시스템을 사용하여 서버 간 데이터 전송에 활용할 수 있습니다.

### Velocity API 예시

```java
// LukeVanillaVelocity 인스턴스 가져오기
LukeVanillaVelocity messagingPlugin = (LukeVanillaVelocity) proxyServer.getPluginManager()
        .getPlugin("lukevanilla-velocity").orElse(null);

// 메시지 전송
if (messagingPlugin != null) {
    messagingPlugin.sendProxyMessageToServer("서버명", "메시지 내용");
}
```

### Paper API 예시

```java
// LukeVanillaPaper 인스턴스 가져오기
LukeVanillaPaper messagingPlugin = (LukeVanillaPaper) Bukkit.getPluginManager().getPlugin("LukeVanilla-Paper");

// 메시지 수신 이벤트 리스닝
@EventHandler
public void onMessageReceived(MessageReceivedEvent event) {
    String message = event.getMessage();
    // 메시지 처리 로직
}

// 메시지 전송
if (messagingPlugin != null && player != null) {
    messagingPlugin.getMessagingService().sendMessageToServer("서버명", "메시지 내용", player);
}
```

## 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다. 