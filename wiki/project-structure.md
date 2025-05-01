# 프로젝트 구조

LukeVanilla-Velocity 시스템의 코드 및 프로젝트 구성에 대한 상세한 설명입니다.

- [설치 및 설정 가이드로 이동](installation.md)
- [사용자 가이드로 이동](user-guide.md)
- [개발자 API 문서로 이동](developer-api.md)
- [테스트 및 디버깅으로 이동](testing.md)

## 목차

1. [프로젝트 개요](#프로젝트-개요)
2. [디렉토리 구조](#디렉토리-구조)
3. [Velocity 플러그인 구조](#velocity-플러그인-구조)
4. [Paper 플러그인 구조](#paper-플러그인-구조)
5. [핵심 컴포넌트](#핵심-컴포넌트)
6. [빌드 시스템](#빌드-시스템)
7. [템플릿 사용법](#템플릿-사용법)

## 프로젝트 개요

LukeVanilla-Velocity 프로젝트는 Minecraft 서버 네트워크에서 서버 간 메시징과 서버 상태 모니터링을 제공하는 플러그인 시스템입니다. 프로젝트는 두 가지 주요 구성 요소로 구성됩니다:

1. **Velocity 프록시 플러그인**: 메시지 라우팅, 서버 상태 관리 및 플레이어 라우팅을 담당합니다.
2. **Paper 서버 플러그인**: 메시지 수신 및 처리, 상태 확인 응답을 담당합니다.

### 주요 기능

- **서버 간 메시징**: 한 서버에서 다른 서버로 메시지를 전송할 수 있습니다.
- **서버 상태 관리**: 서버의 온라인/오프라인 상태를 모니터링합니다.
- **자동 플레이어 라우팅**: 서버 상태에 따라 플레이어를 적절한 서버로 라우팅합니다.

### 기술 스택

- **언어**: Kotlin
- **빌드 도구**: Gradle
- **의존성**: Velocity API, Paper API
- **메시징 프로토콜**: Minecraft Plugin Messaging Channel
- **버전 관리**: Git

## 디렉토리 구조

전체 프로젝트 구조는 다음과 같습니다:

```
LukeVanilla-Velocity/
├── src/
│   └── main/
│       ├── kotlin/
│       │   └── com/
│       │       └── lukehemmin/
│       │           ├── lukeVanillaVelocity/
│       │           │   ├── commands/
│       │           │   │   └── MessageCommand.kt
│       │           │   ├── LukeVanillaVelocity.kt
│       │           │   ├── ServerStatusManager.kt
│       │           │   ├── PlayerConnectionListener.kt
│       │           │   ├── ServerStatusMessageListener.kt
│       │           │   └── ServerToProxyMessageListener.kt
│       │           └── lukepaper/
│       │               └── LukePaperPlugin.kt
│       └── resources/
│           ├── paper-plugin.yml
│           └── velocity-plugin.json
├── template/
│   └── lukevanillapaper/
│       ├── SendMessageCommand.java
│       ├── messaging/
│       │   ├── MessageFormat.java
│       │   └── MessageReceiver.java
│       └── LukeVanillaPaper.java
├── wiki/
│   ├── installation.md
│   ├── user-guide.md
│   ├── developer-api.md
│   ├── testing.md
│   └── project-structure.md
├── build.gradle.kts
├── settings.gradle.kts
├── velocity-example.toml
└── README.md
```

## Velocity 플러그인 구조

### 핵심 클래스

#### LukeVanillaVelocity.kt

메인 플러그인 클래스로, 다음과 같은 역할을 합니다:

- 플러그인 초기화 및 등록
- 메시징 채널 관리
- 서버 상태 관리자 초기화
- 메시지 전송 메서드 제공
- 명령어 등록

```kotlin
@Plugin(
    id = "lukevanilla-velocity", 
    name = "LukeVanilla-Velocity", 
    version = BuildConstants.VERSION,
    description = "Minecraft 서버 간 메시징을 위한 Velocity 플러그인",
    authors = ["LukeHemmin"]
)
class LukeVanillaVelocity @Inject constructor(
    private val server: ProxyServer,
    private val logger: Logger,
    private val commandManager: CommandManager
) {
    // 서버 간 메시징을 위한 채널 식별자
    private val messagingChannel: ChannelIdentifier = MinecraftChannelIdentifier.create("custom", "msg")
    
    // 서버 상태 관리자
    private lateinit var serverStatusManager: ServerStatusManager
    
    // 기타 메서드...
}
```

#### ServerStatusManager.kt

서버 상태를 관리하고 플레이어 라우팅을 담당하는 클래스입니다:

- 서버 상태 모니터링
- ping/pong 메커니즘을 통한 서버 상태 확인
- 플레이어 연결 관리
- 서버 상태 변경 시 플레이어 이동

```kotlin
class ServerStatusManager(
    private val plugin: LukeVanillaVelocity,
    private val server: ProxyServer,
    private val logger: Logger
) {
    // 서버 상태 확인을 위한 채널 식별자
    private val statusChannel = MinecraftChannelIdentifier.create("custom", "status")
    
    // 서버 상태 저장 맵 (서버명 -> 상태)
    private val serverStatus = ConcurrentHashMap<String, Boolean>()
    
    // 기타 속성 및 메서드...
}
```

#### ServerStatusMessageListener.kt

서버 상태 메시지(ping/pong)를 처리하는 리스너 클래스입니다:

- 상태 채널 메시지 수신
- pong 메시지 처리
- 서버 상태 업데이트

```kotlin
class ServerStatusMessageListener(
    private val plugin: LukeVanillaVelocity,
    private val statusManager: ServerStatusManager
) {
    // 상태 확인을 위한 채널 식별자
    private val statusChannel = MinecraftChannelIdentifier.create("custom", "status")
    
    // 기타 메서드...
}
```

#### PlayerConnectionListener.kt

플레이어 연결 이벤트를 처리하는 리스너 클래스입니다:

- 초기 서버 연결 처리
- 서버 상태에 따른 플레이어 라우팅

```kotlin
class PlayerConnectionListener(
    private val plugin: LukeVanillaVelocity,
    private val statusManager: ServerStatusManager
) {
    // 기타 메서드...
}
```

#### ServerToProxyMessageListener.kt

서버 간 메시지 전송을 처리하는 리스너 클래스입니다:

- 메시지 채널 리스닝
- 메시지 디코딩
- 목적지 서버로 메시지 라우팅

```kotlin
class ServerToProxyMessageListener(private val plugin: LukeVanillaVelocity) {
    // 메시징에 사용되는 채널 식별자
    private val messagingChannel = MinecraftChannelIdentifier.create("custom", "msg")
    
    // 기타 메서드...
}
```

#### 명령어 클래스

`commands` 패키지에는 플러그인에서 제공하는 명령어 클래스들이 포함되어 있습니다:

- `MessageCommand.kt`: 메시지 전송 명령어 처리

```kotlin
class MessageCommand(private val plugin: LukeVanillaVelocity) 
    : SimpleCommand {
    
    override fun execute(invocation: SimpleCommand.Invocation) {
        // 명령어 처리 로직...
    }
    
    // 기타 메서드...
}
```

## Paper 플러그인 구조

### 핵심 클래스

#### LukePaperPlugin.kt

Paper 서버 측 플러그인의 메인 클래스입니다:

- 플러그인 메시지 채널 등록
- 메시지 수신 및 처리
- 다른 서버로 메시지 전송
- ping/pong 응답 처리

```kotlin
class LukePaperPlugin : JavaPlugin(), PluginMessageListener {
    // 메시징 채널
    private val MESSAGING_CHANNEL = "custom:msg"
    // 상태 확인 채널
    private val STATUS_CHANNEL = "custom:status"
    
    // 기타 메서드...
}
```

## 핵심 컴포넌트

### 메시징 시스템

LukeVanilla-Velocity의 메시징 시스템은 Minecraft의 Plugin Messaging Channel을 기반으로 합니다. 메시지 구조는 다음과 같습니다:

1. **서버 간 메시지 포맷**:
   ```
   [목적지서버명 길이(int)][목적지서버명(UTF-8)][메시지 길이(int)][메시지 내용(UTF-8)]
   ```

2. **상태 메시지 포맷**:
   ```
   [메시지 길이(int)][메시지 내용(UTF-8)]
   ```

메시지 전송 흐름은 다음과 같습니다:

1. **서버 → 프록시 → 서버**:
   - 출발지 서버: 메시지 인코딩 및 전송
   - 프록시: 메시지 수신, 디코딩, 목적지 확인, 재전송
   - 목적지 서버: 메시지 수신 및 처리

2. **프록시 → 서버**:
   - 프록시: 메시지 인코딩 및 전송
   - 서버: 메시지 수신 및 처리

### 서버 상태 관리 시스템

서버 상태 관리 시스템은 다음과 같이 작동합니다:

1. **상태 확인 메커니즘**:
   - 프록시: 30초마다 survival 서버에 ping 메시지 전송
   - 서버: ping 메시지를 수신하고 pong으로 응답
   - 프록시: pong 응답 수신 시 서버를 온라인으로 표시

2. **타임아웃 처리**:
   - 5초 내에 pong 응답이 없으면 서버를 오프라인으로 표시

3. **플레이어 라우팅**:
   - 플레이어 접속 시 서버 상태에 따라 적절한 서버로 연결
   - survival 서버가 온라인으로 전환되면 lobby에 있는 플레이어를 자동으로 이동

## 빌드 시스템

프로젝트는 Gradle을 사용하여 빌드됩니다. 주요 빌드 파일은 다음과 같습니다:

### build.gradle.kts

```kotlin
plugins {
    kotlin("jvm") version "1.6.0"
    id("com.github.johnrengelman.shadow") version "7.1.0"
}

group = "com.lukehemmin"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.velocitypowered.com/snapshots/")
}

dependencies {
    // Velocity API
    compileOnly("com.velocitypowered:velocity-api:3.2.0")
    kapt("com.velocitypowered:velocity-api:3.2.0")
    
    // Paper API
    compileOnly("io.papermc.paper:paper-api:1.19.4-R0.1-SNAPSHOT")
    
    // 기타 의존성...
}

// 빌드 태스크...
```

### settings.gradle.kts

```kotlin
rootProject.name = "LukeVanilla-Velocity"
```

## 템플릿 사용법

`template` 디렉토리에는 Paper 서버용 플러그인을 개발할 때 사용할 수 있는 템플릿 코드가 포함되어 있습니다.

### 기본 템플릿 구조

```
template/
└── lukevanillapaper/
    ├── SendMessageCommand.java
    ├── messaging/
    │   ├── MessageFormat.java
    │   └── MessageReceiver.java
    └── LukeVanillaPaper.java
```

### 템플릿 사용 방법

1. `template/lukevanillapaper` 디렉토리의 내용을 새 Paper 플러그인 프로젝트로 복사합니다.
2. 패키지 이름과 클래스 이름을 필요에 따라 수정합니다.
3. 필요한 기능을 추가하거나 수정합니다.

### 템플릿 클래스 설명

#### LukeVanillaPaper.java

Paper 서버 플러그인의 메인 클래스 템플릿입니다.

```java
public class LukeVanillaPaper extends JavaPlugin implements PluginMessageListener {
    // 메시징 채널
    private static final String MESSAGING_CHANNEL = "custom:msg";
    // 상태 확인 채널
    private static final String STATUS_CHANNEL = "custom:status";
    
    // 기타 메서드...
}
```

#### SendMessageCommand.java

메시지 전송 명령어 처리 클래스 템플릿입니다.

```java
public class SendMessageCommand implements CommandExecutor {
    private final LukeVanillaPaper plugin;
    
    // 기타 메서드...
}
```

#### messaging/MessageFormat.java

메시지 형식 및 인코딩/디코딩 유틸리티 클래스 템플릿입니다.

```java
public class MessageFormat {
    /**
     * 메시지를 인코딩합니다.
     */
    public static byte[] encodeMessage(String targetServer, String message) {
        // 인코딩 로직...
    }
    
    /**
     * 메시지를 디코딩합니다.
     */
    public static MessageData decodeMessage(byte[] data) {
        // 디코딩 로직...
    }
    
    // 기타 메서드 및 내부 클래스...
}
```

#### messaging/MessageReceiver.java

메시지 수신 처리 클래스 템플릿입니다.

```java
public class MessageReceiver {
    private final LukeVanillaPaper plugin;
    
    /**
     * 수신된 메시지를 처리합니다.
     */
    public void handleMessage(String message) {
        // 메시지 처리 로직...
    }
    
    // 기타 메서드...
}
```

이러한 템플릿을 기반으로 커스텀 Paper 서버 플러그인을 쉽게 개발할 수 있습니다. 템플릿은 기본적인 메시징 기능과 상태 확인 기능을 포함하고 있으며, 필요에 따라 확장할 수 있습니다. 