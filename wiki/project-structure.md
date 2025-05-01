# 프로젝트 구조

이 문서는 LukeVanilla 메시징 시스템의 프로젝트 구조와 코드 구성에 대해 설명합니다.

## 목차

- [개요](#개요)
- [Velocity 플러그인 구조](#velocity-플러그인-구조)
- [Paper 플러그인 구조](#paper-플러그인-구조)
- [메시징 프로토콜](#메시징-프로토콜)
- [빌드 시스템](#빌드-시스템)
- [확장 가능성](#확장-가능성)

## 개요

LukeVanilla 메시징 시스템은 두 개의 주요 구성 요소로 이루어져 있습니다:

1. **LukeVanilla-Velocity**: Velocity 프록시에서 실행되는 플러그인으로, 서버 간 메시지 라우팅을 담당합니다.
2. **LukeVanilla-Paper**: Paper 서버에서 실행되는 플러그인으로, 메시지 송수신 기능을 제공합니다.

이 두 구성 요소는 다음 디렉토리 구조로 조직되어 있습니다:

```
LukeVanilla-Velocity/
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   └── com/
│   │   │       └── lukehemmin/
│   │   │           └── lukeVanillaVelocity/
│   │   │               ├── commands/
│   │   │               └── ...
│   │   ├── resources/
│   │   └── templates/
│   └── test/
├── template/
│   └── lukevanillapaper/
│       ├── messaging/
│       └── ...
├── wiki/
├── build.gradle.kts
└── README.md
```

## Velocity 플러그인 구조

### 주요 패키지 및 클래스

```
com.lukehemmin.lukeVanillaVelocity/
├── LukeVanillaVelocity.kt          # 메인 플러그인 클래스
├── ServerToProxyMessageListener.kt # 서버→프록시 메시지 리스너
├── commands/
│   └── MessageCommand.kt           # 메시지 전송 명령어
└── BuildConstants.java             # 빌드 상수 (템플릿에서 생성됨)
```

### 주요 클래스 설명

#### `LukeVanillaVelocity`

메인 플러그인 클래스로, 다음 기능을 제공합니다:

- 플러그인 초기화 및 채널 등록
- 명령어 등록
- 서버 간 메시지 라우팅
- 프록시에서 서버로 메시지 전송

```kotlin
@Plugin(
    id = "lukevanilla-velocity", 
    name = "LukeVanilla-Velocity", 
    version = BuildConstants.VERSION,
    // ...
)
class LukeVanillaVelocity @Inject constructor(
    private val server: ProxyServer,
    private val logger: Logger,
    private val commandManager: CommandManager
) {
    // 메시징 채널 식별자
    private val messagingChannel: ChannelIdentifier = MinecraftChannelIdentifier.create("custom", "msg")
    
    // ...
}
```

#### `ServerToProxyMessageListener`

서버에서 프록시로 전송된 메시지를 처리하고 대상 서버로 라우팅하는 리스너 클래스입니다.

```kotlin
class ServerToProxyMessageListener(private val plugin: LukeVanillaVelocity) {
    
    private val messagingChannel = MinecraftChannelIdentifier.create("custom", "msg")
    
    @Subscribe
    fun onPluginMessage(event: PluginMessageEvent) {
        // 메시지 처리 및 라우팅 로직
    }
    
    private fun handleMessage(event: PluginMessageEvent) {
        // 메시지 디코딩 및 전달 로직
    }
}
```

#### `MessageCommand`

프록시에서 특정 서버로 메시지를 전송하는 명령어를 처리합니다.

```kotlin
class MessageCommand(private val plugin: LukeVanillaVelocity) : SimpleCommand {
    
    override fun execute(invocation: SimpleCommand.Invocation) {
        // 명령어 실행 로직
    }
    
    override fun hasPermission(source: CommandSource): Boolean {
        return source.hasPermission("lukevanilla.command.sendmessage")
    }
    
    override fun suggest(invocation: SimpleCommand.Invocation): List<String> {
        // 자동 완성 로직
    }
}
```

## Paper 플러그인 구조

### 주요 패키지 및 클래스

```
com.lukehemmin.lukevanillapaper/
├── LukeVanillaPaper.java              # 메인 플러그인 클래스
├── SendMessageCommand.java            # 메시지 전송 명령어
└── messaging/
    ├── MessagingService.java          # 메시징 서비스
    └── MessageReceivedEvent.java      # 메시지 수신 이벤트
```

### 주요 클래스 설명

#### `LukeVanillaPaper`

Paper 플러그인의 메인 클래스로, 다음 기능을 제공합니다:

- 플러그인 초기화 및 종료 처리
- 메시징 서비스 관리
- 명령어 등록

```java
public class LukeVanillaPaper extends JavaPlugin {
    
    private MessagingService messagingService;
    
    @Override
    public void onEnable() {
        // 초기화 로직
    }
    
    @Override
    public void onDisable() {
        // 종료 로직
    }
    
    // 유틸리티 메서드
}
```

#### `MessagingService`

메시지 송수신을 처리하는 핵심 서비스 클래스입니다.

```java
public class MessagingService implements PluginMessageListener {
    
    public static final String MESSAGING_CHANNEL = "custom:msg";
    
    private final LukeVanillaPaper plugin;
    
    // 채널 등록/해제 메서드
    
    // 메시지 전송 메서드
    
    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        // 메시지 수신 및 처리 로직
    }
}
```

#### `MessageReceivedEvent`

프록시나 다른 서버에서 메시지가 수신되면 발생하는 이벤트입니다.

```java
public class MessageReceivedEvent extends Event {
    
    private static final HandlerList HANDLERS = new HandlerList();
    private final LukeVanillaPaper plugin;
    private final String message;
    
    // 생성자 및 접근자 메서드
    
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
```

#### `SendMessageCommand`

서버에서 다른 서버로 메시지를 전송하는 명령어를 처리합니다.

```java
public class SendMessageCommand implements CommandExecutor, TabCompleter {
    
    private final LukeVanillaPaper plugin;
    
    // 명령어 실행 및 자동 완성 로직
}
```

## 메시징 프로토콜

### 프록시 → 서버 메시지 형식

```
[메시지길이(int)][메시지내용(byte[])]
```

1. `메시지길이`: 메시지 내용의 바이트 수를 나타내는 4바이트 정수
2. `메시지내용`: UTF-8 인코딩된 메시지 텍스트

### 서버 → 서버 메시지 형식

```
[목적지서버명길이(int)][목적지서버명(byte[])][메시지길이(int)][메시지내용(byte[])]
```

1. `목적지서버명길이`: 대상 서버 이름의 바이트 수를 나타내는 4바이트 정수
2. `목적지서버명`: UTF-8 인코딩된 대상 서버 이름
3. `메시지길이`: 메시지 내용의 바이트 수를 나타내는 4바이트 정수
4. `메시지내용`: UTF-8 인코딩된 메시지 텍스트

### 코드 예시: 메시지 인코딩 (서버 → 서버)

```java
ByteArrayDataOutput out = ByteStreams.newDataOutput();
byte[] targetServerBytes = targetServer.getBytes(StandardCharsets.UTF_8);

// 대상 서버 이름 길이와 내용 작성
out.writeInt(targetServerBytes.length);
out.write(targetServerBytes);

// 메시지 길이와 내용 작성
byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
out.writeInt(messageBytes.length);
out.write(messageBytes);

// 전송할 최종 바이트 배열
byte[] finalData = out.toByteArray();
```

### 코드 예시: 메시지 디코딩 (프록시에서)

```kotlin
val buf = Unpooled.wrappedBuffer(data)

try {
    // 목적지 서버 이름 읽기
    val targetServerNameLength = buf.readInt()
    val targetServerNameBytes = ByteArray(targetServerNameLength)
    buf.readBytes(targetServerNameBytes)
    val targetServerName = String(targetServerNameBytes, StandardCharsets.UTF_8)
    
    // 메시지 내용 읽기 
    val remainingBytes = ByteArray(buf.readableBytes())
    buf.readBytes(remainingBytes)
    
    // 메시지 처리 로직
    // ...
} finally {
    buf.release()
}
```

## 빌드 시스템

LukeVanilla 프로젝트는 Gradle을 사용하여 빌드됩니다.

### build.gradle.kts

```kotlin
plugins {
    kotlin("jvm") version "2.0.20-Beta1"
    kotlin("kapt") version "2.0.20-Beta1"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    // ...
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    kapt("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    
    // Paper API (테스트용)
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
}

tasks {
    runVelocity {
        velocityVersion("3.4.0-SNAPSHOT")
    }
    
    shadowJar {
        archiveClassifier.set("")
    }
}
```

### 템플릿 시스템

빌드 시 `src/main/templates` 디렉토리의 템플릿 파일이 처리되어 빌드 정보가 포함된 클래스를 생성합니다.

```kotlin
val templateSource = file("src/main/templates")
val templateDest = layout.buildDirectory.dir("generated/sources/templates")
val generateTemplates = tasks.register<Copy>("generateTemplates") {
    val props = mapOf("version" to project.version)
    inputs.properties(props)

    from(templateSource)
    into(templateDest)
    expand(props)
}

sourceSets.main.configure { java.srcDir(generateTemplates.map { it.outputs }) }
```

## 확장 가능성

LukeVanilla 메시징 시스템은 다양한 방향으로 확장 가능하게 설계되었습니다.

### 추가 가능한 기능

1. **메시지 유형 분류**: 메시지에 유형 필드를 추가하여 다양한 종류의 메시지를 지원

```java
public enum MessageType {
    CHAT, COMMAND, BROADCAST, DATA, SYSTEM
}

// 메시지 형식:
// [유형(byte)][목적지서버명길이(int)][목적지서버명(byte[])][메시지길이(int)][메시지내용(byte[])]
```

2. **메시지 우선순위**: 중요한 메시지가 먼저 처리되도록 우선순위 시스템 추가

```java
public enum MessagePriority {
    LOW, NORMAL, HIGH, CRITICAL
}

// 메시지를 우선순위에 따라 서로 다른 큐에 저장하고 처리
```

3. **메시지 압축**: 대용량 메시지 지원을 위한 압축 기능

```java
// 메시지 크기가 지정된 임계값을 초과하는 경우에만 압축 적용
if (messageBytes.length > COMPRESSION_THRESHOLD) {
    messageBytes = compressBytes(messageBytes);
    isCompressed = true;
}

// 메시지 헤더에 압축 여부 플래그 추가
out.writeBoolean(isCompressed);
```

4. **전송 확인 및 재시도 메커니즘**: 중요한 메시지의 신뢰성 보장

```java
// 각 메시지에 고유 ID 할당
String messageId = UUID.randomUUID().toString();

// 수신 확인 메시지 처리
if (message.startsWith("ACK:")) {
    String ackId = message.substring(4);
    pendingMessages.remove(ackId);
    return;
}

// 주기적으로 미확인 메시지 재전송
scheduler.runTaskTimerAsynchronously(plugin, () -> {
    for (Map.Entry<String, PendingMessage> entry : pendingMessages.entrySet()) {
        if (System.currentTimeMillis() - entry.getValue().timestamp > RETRY_TIMEOUT) {
            sendMessage(entry.getValue());
            entry.getValue().attempts++;
        }
    }
}, RETRY_CHECK_INTERVAL, RETRY_CHECK_INTERVAL);
```

### 구조 확장

1. **서비스 계층 추가**: 메시징 로직을 서비스 계층으로 분리하여 코드 구조 개선

```
com.lukehemmin.lukeVanillaVelocity/
├── LukeVanillaVelocity.kt
├── api/
│   └── MessagingAPI.kt           # 공개 API 인터페이스
├── services/
│   ├── MessagingService.kt       # 메시징 서비스 구현
│   └── MessageRouter.kt          # 메시지 라우팅 서비스
├── listeners/
│   └── ServerToProxyMessageListener.kt
├── commands/
│   └── MessageCommand.kt
└── models/
    ├── Message.kt                # 메시지 모델 클래스
    └── MessageType.kt            # 메시지 유형 열거형
```

2. **플러그인 확장 시스템**: 타 플러그인에서 확장할 수 있는 API 제공

```kotlin
interface MessagingExtension {
    fun onMessageReceived(message: Message): Boolean
    fun processOutgoingMessage(message: Message): Message
}

// 확장 등록 메서드
fun registerExtension(extension: MessagingExtension) {
    extensions.add(extension)
}

// 메시지 처리 시 확장 기능 호출
fun processMessage(message: Message): Boolean {
    for (extension in extensions) {
        if (extension.onMessageReceived(message)) {
            return true // 메시지가 처리됨
        }
    }
    // 기본 처리 로직
    return false
}
```

이러한 확장 가능성을 통해 LukeVanilla 메시징 시스템은 네트워크 규모와 요구 사항이 증가함에 따라 유연하게 확장할 수 있습니다. 