# 개발자 API 문서

LukeVanilla-Velocity 시스템의 API 및 확장 방법에 대한 설명입니다.

- [프로젝트 구조로 이동](project-structure.md)
- [설치 및 설정 가이드로 이동](installation.md)
- [사용자 가이드로 이동](user-guide.md)
- [테스트 및 디버깅으로 이동](testing.md)

## 목차

1. [개요](#개요)
2. [API 구조](#api-구조)
3. [Velocity 플러그인 API](#velocity-플러그인-api)
4. [Paper 플러그인 API](#paper-플러그인-api)
5. [메시지 형식](#메시지-형식)
6. [이벤트 시스템](#이벤트-시스템)
7. [확장 개발 방법](#확장-개발-방법)
8. [API 사용 예제](#api-사용-예제)

## 개요

LukeVanilla-Velocity 시스템은 다른 플러그인에서 활용할 수 있는 API를 제공합니다. 이 API를 통해 다음과 같은 기능을 구현할 수 있습니다:

1. 서버 간 메시지 전송 및 수신
2. 서버 상태 모니터링 및 알림 수신
3. 플레이어 라우팅 로직 확장
4. 메시지 처리 로직 확장

## API 구조

LukeVanilla-Velocity API는 두 가지 주요 모듈로 구성되어 있습니다:

1. **Velocity 플러그인 API**: 프록시에서 사용할 수 있는 API
2. **Paper 플러그인 API**: 각 서버에서 사용할 수 있는 API

## Velocity 플러그인 API

### 의존성 추가

Velocity 플러그인에서 LukeVanilla-Velocity API를 사용하려면 먼저 의존성을 추가해야 합니다.

```kotlin
// build.gradle.kts
dependencies {
    // 기존 의존성...
    compileOnly("com.lukehemmin:lukevanilla-velocity:1.0.0")
}
```

플러그인 정의 파일에 의존성을 선언합니다:

```json
// velocity-plugin.json
{
  "id": "your-plugin",
  "name": "YourPlugin",
  "version": "1.0.0",
  "dependencies": [
    {
      "id": "lukevanilla-velocity",
      "optional": false
    }
  ]
}
```

### 서비스 접근

LukeVanilla-Velocity 플러그인에 접근하는 방법:

```kotlin
@Plugin(/*...생략...*/)
class YourPlugin @Inject constructor(
    private val server: ProxyServer,
    private val logger: Logger
) {
    private lateinit var lukeVanillaAPI: LukeVanillaVelocity
    
    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        // LukeVanilla-Velocity 플러그인 인스턴스 얻기
        server.pluginManager.getPlugin("lukevanilla-velocity").ifPresent { container ->
            lukeVanillaAPI = container.instance.get() as LukeVanillaVelocity
            logger.info("LukeVanilla-Velocity API 연결 완료")
        }
    }
}
```

### 주요 API 메서드

#### 서버 간 메시지 전송

```kotlin
/**
 * 특정 서버로 메시지를 전송합니다.
 * 
 * @param targetServer 대상 서버 이름
 * @param message 전송할 메시지
 * @return 전송 성공 여부
 */
fun sendMessageToServer(targetServer: String, message: String): Boolean

/**
 * 모든 서버에 메시지를 브로드캐스트합니다.
 * 
 * @param message 전송할 메시지
 * @return 전송 성공 여부
 */
fun broadcastMessageToAllServers(message: String): Boolean
```

#### 서버 상태 확인

```kotlin
/**
 * 서버의 현재 상태를 확인합니다.
 * 
 * @param serverName 서버 이름
 * @return 서버 온라인 상태 (온라인: true, 오프라인: false)
 */
fun isServerOnline(serverName: String): Boolean

/**
 * 모든 서버의 상태를 확인합니다.
 * 
 * @return 서버 이름을 키로, 온라인 상태를 값으로 하는 맵
 */
fun getAllServerStatus(): Map<String, Boolean>

/**
 * 서버 상태 변화를 구독합니다.
 * 
 * @param listener 상태 변화 리스너
 */
fun subscribeToServerStatusChanges(listener: (String, Boolean) -> Unit)
```

#### 플레이어 라우팅

```kotlin
/**
 * 플레이어를 특정 서버로 라우팅합니다.
 * 
 * @param player 플레이어
 * @param targetServer 대상 서버
 * @return 라우팅 성공 여부
 */
fun routePlayerToServer(player: Player, targetServer: String): CompletableFuture<Boolean>

/**
 * 커스텀 라우팅 로직을 등록합니다.
 * 
 * @param router 커스텀 라우터 함수
 */
fun registerCustomRouter(router: (Player) -> String?)
```

## Paper 플러그인 API

### 의존성 추가

Paper 플러그인에서 LukeVanilla-Paper API를 사용하려면 먼저 의존성을 추가해야 합니다.

```kotlin
// build.gradle.kts
dependencies {
    // 기존 의존성...
    compileOnly("com.lukehemmin:lukevanilla-paper:1.0.0")
}
```

플러그인 정의 파일에 의존성을 선언합니다:

```yaml
# paper-plugin.yml
name: YourPlugin
version: 1.0.0
main: com.yourname.yourplugin.YourPlugin
api-version: 1.19
depend: [LukeVanillaPaper]
```

### 서비스 접근

LukeVanilla-Paper 플러그인에 접근하는 방법:

```java
public class YourPlugin extends JavaPlugin {
    private LukeVanillaPaper lukeVanillaAPI;
    
    @Override
    public void onEnable() {
        // LukeVanilla-Paper 플러그인 인스턴스 얻기
        Plugin plugin = getServer().getPluginManager().getPlugin("LukeVanillaPaper");
        if (plugin instanceof LukeVanillaPaper) {
            lukeVanillaAPI = (LukeVanillaPaper) plugin;
            getLogger().info("LukeVanilla-Paper API 연결 완료");
        }
    }
}
```

### 주요 API 메서드

#### 메시지 전송

```java
/**
 * 특정 서버로 메시지를 전송합니다.
 * 
 * @param targetServer 대상 서버 이름
 * @param message 전송할 메시지
 * @return 전송 성공 여부
 */
boolean sendMessageToServer(String targetServer, String message);

/**
 * 프록시를 통해 모든 서버에 메시지를 브로드캐스트합니다.
 * 
 * @param message 전송할 메시지
 * @return 전송 성공 여부
 */
boolean broadcastMessageToAllServers(String message);
```

#### 메시지 수신 리스너

```java
/**
 * 메시지 수신 리스너를 등록합니다.
 * 
 * @param listener 메시지 수신 시 호출될 리스너
 */
void registerMessageListener(MessageListener listener);

/**
 * 메시지 수신 리스너 인터페이스
 */
public interface MessageListener {
    /**
     * 메시지가 수신되었을 때 호출됩니다.
     * 
     * @param sourceServer 출발지 서버 이름 (프록시인 경우 "proxy")
     * @param message 수신된 메시지
     */
    void onMessageReceived(String sourceServer, String message);
}
```

#### 상태 응답 커스터마이징

```java
/**
 * 서버 상태 확인 응답을 커스터마이징합니다.
 * 
 * @param customStatusProvider 커스텀 상태 제공 함수
 */
void setCustomStatusProvider(Supplier<Boolean> customStatusProvider);
```

## 메시지 형식

LukeVanilla-Velocity 시스템에서는 두 가지 주요 메시지 형식이 사용됩니다:

### 1. 서버 간 메시지 형식

```
[목적지서버명 길이(int)][목적지서버명(UTF-8)][메시지 길이(int)][메시지 내용(UTF-8)]
```

#### 인코딩 예제 (Java)

```java
public byte[] encodeMessage(String targetServer, String message) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(baos);
    
    try {
        // 대상 서버 이름 인코딩
        byte[] targetServerBytes = targetServer.getBytes(StandardCharsets.UTF_8);
        out.writeInt(targetServerBytes.length);
        out.write(targetServerBytes);
        
        // 메시지 내용 인코딩
        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
        out.writeInt(messageBytes.length);
        out.write(messageBytes);
        
        return baos.toByteArray();
    } catch (IOException e) {
        e.printStackTrace();
        return new byte[0];
    }
}
```

#### 디코딩 예제 (Java)

```java
public MessageData decodeMessage(byte[] data) {
    ByteArrayInputStream bais = new ByteArrayInputStream(data);
    DataInputStream in = new DataInputStream(bais);
    
    try {
        // 대상 서버 이름 디코딩
        int targetServerLength = in.readInt();
        byte[] targetServerBytes = new byte[targetServerLength];
        in.readFully(targetServerBytes);
        String targetServer = new String(targetServerBytes, StandardCharsets.UTF_8);
        
        // 메시지 내용 디코딩
        int messageLength = in.readInt();
        byte[] messageBytes = new byte[messageLength];
        in.readFully(messageBytes);
        String message = new String(messageBytes, StandardCharsets.UTF_8);
        
        return new MessageData(targetServer, message);
    } catch (IOException e) {
        e.printStackTrace();
        return null;
    }
}

public class MessageData {
    private final String targetServer;
    private final String message;
    
    public MessageData(String targetServer, String message) {
        this.targetServer = targetServer;
        this.message = message;
    }
    
    // Getter 메서드
    public String getTargetServer() { return targetServer; }
    public String getMessage() { return message; }
}
```

### 2. 상태 메시지 형식

```
[메시지 길이(int)][메시지 내용(UTF-8)]
```

상태 메시지에는 두 가지 유형이 있습니다:

1. **Ping 메시지**: 프록시에서 서버로 전송되는 "ping" 메시지
2. **Pong 메시지**: 서버에서 프록시로 응답하는 "pong" 메시지

## 이벤트 시스템

LukeVanilla-Velocity 시스템은 이벤트 기반 아키텍처를 활용하여 다양한 이벤트에 반응할 수 있습니다.

### Velocity 이벤트

```kotlin
/**
 * 서버 상태 변경 이벤트
 */
class ServerStatusChangeEvent(
    val serverName: String,
    val isOnline: Boolean
) {
    companion object {
        fun create(serverName: String, isOnline: Boolean): ServerStatusChangeEvent {
            return ServerStatusChangeEvent(serverName, isOnline)
        }
    }
}

/**
 * 서버 간 메시지 라우팅 이벤트
 */
class MessageRoutingEvent(
    val sourceServer: String,
    val targetServer: String,
    val message: String,
    var cancelled: Boolean = false
) {
    companion object {
        fun create(sourceServer: String, targetServer: String, message: String): MessageRoutingEvent {
            return MessageRoutingEvent(sourceServer, targetServer, message)
        }
    }
}
```

#### 이벤트 구독 방법

```kotlin
@Subscribe
fun onServerStatusChange(event: ServerStatusChangeEvent) {
    logger.info("서버 ${event.serverName}의 상태가 ${if (event.isOnline) "온라인" else "오프라인"}으로 변경되었습니다.")
    
    // 상태 변경에 따른 로직 처리
}

@Subscribe
fun onMessageRouting(event: MessageRoutingEvent) {
    logger.info("서버 ${event.sourceServer}에서 ${event.targetServer}로 메시지 라우팅: ${event.message}")
    
    // 필요한 경우 메시지 라우팅 취소
    if (event.message.contains("banned_word")) {
        event.cancelled = true
        logger.warn("금지된 단어가 포함된 메시지 차단")
    }
}
```

### Paper 이벤트

```java
/**
 * 메시지 수신 이벤트
 */
public class MessageReceivedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final String sourceServer;
    private final String message;
    private boolean handled = false;
    
    public MessageReceivedEvent(String sourceServer, String message) {
        this.sourceServer = sourceServer;
        this.message = message;
    }
    
    // 액세서 메서드
    public String getSourceServer() { return sourceServer; }
    public String getMessage() { return message; }
    public boolean isHandled() { return handled; }
    public void setHandled(boolean handled) { this.handled = handled; }
    
    @Override
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}

/**
 * 메시지 전송 이벤트
 */
public class MessageSendEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final String targetServer;
    private String message;
    private boolean cancelled = false;
    
    public MessageSendEvent(String targetServer, String message) {
        this.targetServer = targetServer;
        this.message = message;
    }
    
    // 액세서 메서드
    public String getTargetServer() { return targetServer; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    @Override
    public boolean isCancelled() { return cancelled; }
    @Override
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    
    @Override
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
```

#### 이벤트 구독 방법

```java
@EventHandler
public void onMessageReceived(MessageReceivedEvent event) {
    getLogger().info("서버 " + event.getSourceServer() + "에서 메시지 수신: " + event.getMessage());
    
    // 메시지 처리 로직
    if (event.getMessage().startsWith("command:")) {
        // 명령어 처리
        event.setHandled(true); // 다른 핸들러가 처리하지 않도록 표시
    }
}

@EventHandler
public void onMessageSend(MessageSendEvent event) {
    getLogger().info("서버 " + event.getTargetServer() + "로 메시지 전송: " + event.getMessage());
    
    // 메시지 변환이나 필터링 수행
    String filteredMessage = filterMessage(event.getMessage());
    event.setMessage(filteredMessage);
    
    // 필요한 경우 메시지 전송 취소
    if (filteredMessage.isEmpty()) {
        event.setCancelled(true);
        getLogger().warn("빈 메시지 전송 취소");
    }
}
```

## 확장 개발 방법

LukeVanilla-Velocity 시스템을 확장하는 방법에는 여러 가지가 있습니다:

### 1. 메시지 처리기 확장

플러그인에서 특정 형식의 메시지를 처리하는 확장 기능을 개발할 수 있습니다.

#### Velocity 확장

```kotlin
class CustomMessageProcessor(private val plugin: YourPlugin) {
    
    // 메시지 처리 메서드
    fun processMessage(sourceServer: String, targetServer: String, message: String): Boolean {
        // 특정 형식의 메시지인지 확인
        if (message.startsWith("custom:")) {
            // 메시지 처리 로직
            val command = message.substringAfter("custom:")
            plugin.executeCustomCommand(command)
            return true // 메시지가 처리되었음을 표시
        }
        return false // 다른 처리기가 처리하도록 전달
    }
    
    // 확장 등록
    fun register(lukeVanillaAPI: LukeVanillaVelocity) {
        lukeVanillaAPI.registerMessageProcessor(this::processMessage)
    }
}
```

#### Paper 확장

```java
public class CustomMessageHandler implements MessageListener {
    private final YourPlugin plugin;
    
    public CustomMessageHandler(YourPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void onMessageReceived(String sourceServer, String message) {
        // 특정 형식의 메시지인지 확인
        if (message.startsWith("custom:")) {
            // 메시지 처리 로직
            String command = message.substring("custom:".length());
            plugin.executeCustomCommand(command);
        }
    }
    
    // 확장 등록
    public void register(LukeVanillaPaper lukeVanillaAPI) {
        lukeVanillaAPI.registerMessageListener(this);
    }
}
```

### 2. 서버 상태 관리 확장

서버 상태 확인 로직을 확장하거나 상태 변경에 반응하는 기능을 추가할 수 있습니다.

```kotlin
class EnhancedServerStatusManager(
    private val plugin: YourPlugin,
    private val lukeVanillaAPI: LukeVanillaVelocity
) {
    // 초기화 메서드
    fun initialize() {
        // 서버 상태 변경 구독
        lukeVanillaAPI.subscribeToServerStatusChanges { serverName, isOnline ->
            if (serverName == "resource" && !isOnline) {
                // 리소스 서버가 오프라인이 되면 백업 서버 활성화
                activateBackupResourceServer()
            }
        }
        
        // 주기적인 상태 모니터링
        plugin.server.scheduler.buildTask(plugin) {
            checkServerPerformance()
        }.repeat(Duration.ofMinutes(5)).schedule()
    }
    
    // 백업 서버 활성화 로직
    private fun activateBackupResourceServer() {
        // 백업 서버 활성화 로직
        plugin.logger.info("백업 리소스 서버 활성화 중...")
        // ...
    }
    
    // 서버 성능 모니터링
    private fun checkServerPerformance() {
        val serverStatus = lukeVanillaAPI.getAllServerStatus()
        
        for ((serverName, isOnline) in serverStatus) {
            if (isOnline) {
                // 온라인 서버에 대한 추가 성능 체크
                plugin.logger.info("서버 $serverName 성능 확인 중...")
                // ...
            }
        }
    }
}
```

### 3. 플레이어 라우팅 로직 확장

플레이어 라우팅 로직을 커스터마이징하여 플레이어를 특정 기준에 따라 다른 서버로 라우팅할 수 있습니다.

```kotlin
class CustomPlayerRouter(
    private val plugin: YourPlugin,
    private val lukeVanillaAPI: LukeVanillaVelocity
) {
    // 초기화 메서드
    fun initialize() {
        // 커스텀 라우터 등록
        lukeVanillaAPI.registerCustomRouter { player ->
            // VIP 플레이어는 VIP 서버로 라우팅
            if (player.hasPermission("vip.access")) {
                return@registerCustomRouter "vip"
            }
            
            // 신규 플레이어는 튜토리얼 서버로 라우팅
            if (isNewPlayer(player)) {
                return@registerCustomRouter "tutorial"
            }
            
            // 그 외의 경우 null 반환 (기본 라우팅 로직 사용)
            null
        }
    }
    
    // 신규 플레이어 확인 로직
    private fun isNewPlayer(player: Player): Boolean {
        // 플레이어가 신규인지 확인하는 로직
        // ...
        return false
    }
}
```

## API 사용 예제

### 예제 1: 서버 간 메시지 전송

#### Velocity 플러그인

```kotlin
@Plugin(
    id = "server-announcer",
    name = "ServerAnnouncer",
    version = "1.0.0",
    dependencies = [
        Dependency(id = "lukevanilla-velocity")
    ]
)
class ServerAnnouncerPlugin @Inject constructor(
    private val server: ProxyServer,
    private val logger: Logger
) {
    private lateinit var lukeVanillaAPI: LukeVanillaVelocity
    
    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        // API 접근
        server.pluginManager.getPlugin("lukevanilla-velocity").ifPresent { container ->
            lukeVanillaAPI = container.instance.get() as LukeVanillaVelocity
            logger.info("LukeVanilla-Velocity API 연결 완료")
            
            // 명령어 등록
            server.commandManager.register(
                "announce",
                SendAnnouncementCommand(this)
            )
        }
    }
    
    // 메시지 전송 메서드
    fun sendAnnouncement(targetServer: String, message: String): Boolean {
        return lukeVanillaAPI.sendMessageToServer(targetServer, "announcement:$message")
    }
    
    // 전체 서버 메시지 전송 메서드
    fun broadcastAnnouncement(message: String): Boolean {
        return lukeVanillaAPI.broadcastMessageToAllServers("announcement:$message")
    }
    
    // 명령어 클래스
    private inner class SendAnnouncementCommand(private val plugin: ServerAnnouncerPlugin) : SimpleCommand {
        override fun execute(invocation: SimpleCommand.Invocation) {
            val source = invocation.source()
            val args = invocation.arguments()
            
            if (args.size < 2) {
                source.sendMessage(Component.text("사용법: /announce <서버|all> <메시지>"))
                return
            }
            
            val target = args[0]
            val message = args.slice(1 until args.size).joinToString(" ")
            
            if (target.equals("all", ignoreCase = true)) {
                if (plugin.broadcastAnnouncement(message)) {
                    source.sendMessage(Component.text("모든 서버에 공지사항이 전송되었습니다."))
                } else {
                    source.sendMessage(Component.text("공지사항 전송 실패"))
                }
            } else {
                if (plugin.sendAnnouncement(target, message)) {
                    source.sendMessage(Component.text("$target 서버에 공지사항이 전송되었습니다."))
                } else {
                    source.sendMessage(Component.text("$target 서버에 공지사항 전송 실패"))
                }
            }
        }
    }
}
```

#### Paper 플러그인

```java
public class AnnouncementHandlerPlugin extends JavaPlugin {
    private LukeVanillaPaper lukeVanillaAPI;
    
    @Override
    public void onEnable() {
        // API 접근
        Plugin plugin = getServer().getPluginManager().getPlugin("LukeVanillaPaper");
        if (plugin instanceof LukeVanillaPaper) {
            lukeVanillaAPI = (LukeVanillaPaper) plugin;
            getLogger().info("LukeVanilla-Paper API 연결 완료");
            
            // 메시지 리스너 등록
            lukeVanillaAPI.registerMessageListener(new AnnouncementListener(this));
        } else {
            getLogger().severe("LukeVanilla-Paper 플러그인을 찾을 수 없습니다!");
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    // 공지사항 표시 메서드
    public void showAnnouncement(String message) {
        // 채팅 포맷 지정
        Component formattedMessage = Component.text()
            .append(Component.text("[공지사항] ", NamedTextColor.GOLD, TextDecoration.BOLD))
            .append(Component.text(message, NamedTextColor.WHITE))
            .build();
        
        // 모든 플레이어에게 메시지 전송
        for (Player player : getServer().getOnlinePlayers()) {
            player.sendMessage(formattedMessage);
        }
        
        // 콘솔에도 로그 기록
        getLogger().info("공지사항: " + message);
    }
    
    // 메시지 리스너 클래스
    private class AnnouncementListener implements MessageListener {
        private final AnnouncementHandlerPlugin plugin;
        
        public AnnouncementListener(AnnouncementHandlerPlugin plugin) {
            this.plugin = plugin;
        }
        
        @Override
        public void onMessageReceived(String sourceServer, String message) {
            // 공지사항 메시지인지 확인
            if (message.startsWith("announcement:")) {
                String announcement = message.substring("announcement:".length());
                plugin.showAnnouncement(announcement);
            }
        }
    }
}
```

### 예제 2: 서버 상태 모니터링

```kotlin
@Plugin(
    id = "server-monitor",
    name = "ServerMonitor",
    version = "1.0.0",
    dependencies = [
        Dependency(id = "lukevanilla-velocity")
    ]
)
class ServerMonitorPlugin @Inject constructor(
    private val server: ProxyServer,
    private val logger: Logger
) {
    private lateinit var lukeVanillaAPI: LukeVanillaVelocity
    private val statusHistory = ConcurrentHashMap<String, MutableList<Pair<Long, Boolean>>>()
    
    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        // API 접근
        server.pluginManager.getPlugin("lukevanilla-velocity").ifPresent { container ->
            lukeVanillaAPI = container.instance.get() as LukeVanillaVelocity
            logger.info("LukeVanilla-Velocity API 연결 완료")
            
            // 서버 상태 변경 구독
            lukeVanillaAPI.subscribeToServerStatusChanges { serverName, isOnline ->
                logger.info("서버 $serverName 상태 변경: ${if (isOnline) "온라인" else "오프라인"}")
                
                // 상태 변경 이력 기록
                val history = statusHistory.computeIfAbsent(serverName) { mutableListOf() }
                history.add(System.currentTimeMillis() to isOnline)
                
                // 이력 최대 크기 유지
                if (history.size > 100) {
                    history.removeAt(0)
                }
                
                // 오프라인 상태가 지속되면 알림
                if (!isOnline) {
                    checkOfflineDuration(serverName)
                }
            }
            
            // 주기적인 상태 보고서 생성
            server.scheduler.buildTask(this) {
                generateStatusReport()
            }.repeat(Duration.ofHours(1)).schedule()
        }
    }
    
    // 오프라인 상태 지속 시간 확인
    private fun checkOfflineDuration(serverName: String) {
        val history = statusHistory[serverName] ?: return
        if (history.isEmpty() || history.last().second) return
        
        // 마지막으로 온라인이었던 시점 찾기
        val lastOnlineIndex = history.indexOfLast { it.second }
        if (lastOnlineIndex == -1) return
        
        val lastOnlineTime = history[lastOnlineIndex].first
        val currentTime = System.currentTimeMillis()
        val offlineDuration = (currentTime - lastOnlineTime) / 60000 // 분 단위
        
        // 30분 이상 오프라인 상태면 경고
        if (offlineDuration >= 30) {
            logger.warn("서버 $serverName이 $offlineDuration분 동안 오프라인 상태입니다!")
            
            // 관리자에게 알림 전송
            val adminPlayers = server.allPlayers.filter { it.hasPermission("servermonitor.admin") }
            adminPlayers.forEach { player ->
                player.sendMessage(Component.text()
                    .append(Component.text("[서버 모니터] ", NamedTextColor.RED))
                    .append(Component.text("$serverName 서버가 $offlineDuration분 동안 오프라인 상태입니다!", NamedTextColor.YELLOW))
                    .build())
            }
        }
    }
    
    // 상태 보고서 생성
    private fun generateStatusReport() {
        val report = StringBuilder("서버 상태 보고서:\n")
        val allServerStatus = lukeVanillaAPI.getAllServerStatus()
        
        for ((serverName, isOnline) in allServerStatus) {
            report.append("- $serverName: ${if (isOnline) "온라인" else "오프라인"}\n")
            
            // 가용성 계산
            val history = statusHistory[serverName]
            if (history != null && history.size >= 10) {
                val onlineCount = history.count { it.second }
                val availability = onlineCount * 100.0 / history.size
                report.append("  가용성: ${String.format("%.2f", availability)}%\n")
            }
        }
        
        // 로그에 보고서 출력
        logger.info(report.toString())
        
        // 파일로 저장
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm")
            val timestamp = dateFormat.format(Date())
            val reportFile = File("reports/server_status_$timestamp.txt")
            reportFile.parentFile.mkdirs()
            reportFile.writeText(report.toString())
        } catch (e: Exception) {
            logger.error("보고서 저장 실패", e)
        }
    }
}
```

이 문서에서 제공하는 API와 예제는 LukeVanilla-Velocity 시스템을 확장하고 다른 플러그인과 통합하는 데 도움이 됩니다. 추가 질문이나 기능 요청은 GitHub 이슈 트래커에 등록해 주세요. 