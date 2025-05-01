# LukeVanilla-Velocity

Minecraft 네트워크를 위한 Velocity 프록시 플러그인으로, 서버 간 메시징과 서버 상태 모니터링 및 자동 라우팅 기능을 제공합니다.

## 주요 기능

### 서버 간 메시징
- **서버 ⇄ 프록시 ⇄ 서버** 메시징: 백엔드 서버(Paper) 간에 데이터 전송
- **프록시 ⇄ 서버** 메시징: 프록시에서 특정 서버로 메시지 전송

### 서버 상태 관리 및 자동 라우팅
- **서버 상태 모니터링**: survival 서버의 온라인/오프라인 상태 확인
- **자동 라우팅**: 서버 상태에 따라 플레이어를 적절한 서버(survival 또는 lobby)로 연결
- **자동 이동**: survival 서버가 온라인으로 전환되면 lobby에 있는 플레이어들을 자동으로 이동

## 구성 요소

- **LukeVanilla-Velocity** (Velocity 프록시 플러그인)
- **LukePaper** (Paper 서버 플러그인)

## 설치 및 설정

### Velocity 프록시

1. `build/libs` 디렉토리에서 `lukevanilla-velocity-1.0.0.jar` 파일을 Velocity 서버의 `plugins` 폴더에 복사합니다.
2. `velocity-example.toml`을 참고하여 `velocity.toml` 파일을 설정합니다:
   ```toml
   [servers]
     survival = "127.0.0.1:25565"
     lobby = "127.0.0.1:25566"
   
   [plugins]
     bungee-plugin-message-channel = true
   ```

### Paper 서버

1. `build/libs` 디렉토리에서 `lukepaper-1.0.0.jar` 파일을 각 Paper 서버의 `plugins` 폴더에 복사합니다.
2. 각 Paper 서버의 `server.properties` 파일에서 다음 설정을 확인합니다:
   ```properties
   # survival 서버
   server-port=25565
   
   # lobby 서버
   server-port=25566
   ```

## 빌드 방법

```bash
# 프로젝트 빌드
./gradlew build

# 개별 모듈 빌드
./gradlew :velocity:build
./gradlew :paper:build
```

## 사용 방법

### 메시지 전송

Velocity 프록시에서 명령어를 사용하여 메시지를 전송할 수 있습니다:

```
/sendmessage <서버명> <메시지>
```

예: `/sendmessage survival 안녕하세요!`

### 서버 상태 확인 및 자동 라우팅

- 프록시는 30초마다 survival 서버의 상태를 확인합니다.
- 플레이어가 접속하면 survival 서버가 온라인인 경우 해당 서버로, 오프라인인 경우 lobby 서버로 자동 연결됩니다.
- survival 서버가 온라인으로 전환되면 lobby에 있는 플레이어들이 자동으로 survival 서버로 이동합니다.

## 라이선스

MIT License

## 기여하기

이슈 제출 및 PR은 언제나 환영합니다!

## 문서

자세한 내용은 위키 문서를 참조하세요:

- [🔧 설치 및 설정 가이드](wiki/installation.md)
- [📖 사용자 가이드](wiki/user-guide.md)
- [👨‍💻 개발자 API 문서](wiki/developer-api.md)
- [🧪 테스트 및 디버깅](wiki/testing.md)
- [🔄 프로젝트 구조](wiki/project-structure.md)

## 빠른 시작

### 빌드 방법

```bash
# 프로젝트 루트 디렉토리에서
./gradlew shadowJar
```

빌드된 JAR 파일은 `build/libs/` 디렉토리에 생성됩니다.

### 기본 명령어

- `/sendmessage <서버명> <메시지>`: 특정 서버로 메시지 전송
- `/smsg <서버명> <메시지>`: 위 명령어의 단축 버전

## 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다. 