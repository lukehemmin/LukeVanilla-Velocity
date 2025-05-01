# 개발자 API 문서

이 문서는 LukeVanilla 메시징 시스템의 API를 활용하여 자체 플러그인에서 메시지를 송수신하는 방법을 설명합니다.

## 목차

- [Velocity API](#velocity-api)
- [Paper API](#paper-api)
- [메시지 형식](#메시지-형식)
- [예제 코드](#예제-코드)
- [오류 처리](#오류-처리)
- [모범 사례](#모범-사례)

## Velocity API

### 기본 사용법

Velocity 플러그인에서 LukeVanilla-Velocity API를 사용하여 메시지를 전송하는 기본 방법입니다.

1. 먼저 플러그인 의존성을 추가합니다:

```java
@Plugin(
    id = "your-plugin",
    name = "Your Plugin",
    version = "1.0.0",
    description = "Your plugin description",
    authors = {"YourName"},
    dependencies = {
        @Dependency(id = "lukevanilla-velocity")
    }
)
```

2. LukeVanillaVelocity 인스턴스를 얻습니다:

```java
private final ProxyServer server;
private LukeVanillaVelocity messagingPlugin;

@Inject
public YourPlugin(ProxyServer server) {
    this.server = server;
}

@Subscribe
public void onProxyInitialization(ProxyInitializeEvent event) {
    // LukeVanillaVelocity 플러그인 인스턴스 가져오기
    server.getPluginManager().getPlugin("lukevanilla-velocity").ifPresent(container -> {
        messagingPlugin = (LukeVanillaVelocity) container.getInstance().orElse(null);
        if (messagingPlugin != null) {
            getLogger().info("LukeVanilla-Velocity API가 성공적으로 로드되었습니다.");
        }
    });
}
```

3. 메시지 전송 메서드 사용:

```java
/**
 * 특정 서버로 메시지를 전송합니다.
 * 
 * @param targetServer 대상 서버 이름
 * @param message 전송할 메시지
 * @return 전송 성공 여부
 */
public boolean sendMessageToServer(String targetServer, String message) {
    if (messagingPlugin != null) {
        return messagingPlugin.sendProxyMessageToServer(targetServer, message);
    }
    return false;
}
```

### Velocity API 메서드

#### 메시지 전송

```java
/**
 * 프록시에서 특정 서버로 메시지를 전송합니다.
 * 
 * @param targetServer 대상 서버 이름
 * @param message 전송할 메시지
 * @return 전송 성공 여부
 */
public boolean sendProxyMessageToServer(String targetServer, String message);
```

#### 서버 이름으로 서버 인스턴스 찾기

```java
/**
 * 서버 이름으로 서버 인스턴스를 찾습니다.
 * 
 * @param serverName
 * @return RegisteredServer 인스턴스 또는 null
 */
public RegisteredServer getServerByName(String serverName);
```

## Paper API

### 기본 사용법

Paper 플러그인에서 LukeVanilla-Paper API를 사용하여 메시지를 전송 및 수신하는 기본 방법입니다.

1. 먼저 플러그인 의존성을 추가합니다:

```yaml
# plugin.yml
depend: [LukeVanilla-Paper]
```

2. LukeVanillaPaper 인스턴스를 얻습니다:

```java
private LukeVanillaPaper messagingPlugin;

@Override
public void onEnable() {
    // LukeVanillaPaper 플러그인 인스턴스 가져오기
    Plugin plugin = Bukkit.getPluginManager().getPlugin("LukeVanilla-Paper");
    if (plugin instanceof LukeVanillaPaper) {
        messagingPlugin = (LukeVanillaPaper) plugin;
        getLogger().info("LukeVanilla-Paper API가 성공적으로 로드되었습니다.");
    } else {
        getLogger().severe("LukeVanilla-Paper 플러그인을 찾을 수 없습니다!");
        Bukkit.getPluginManager().disablePlugin(this);
    }
}
```

3. 메시지 전송:

```java
/**
 * 다른 서버로 메시지를 전송합니다.
 * 
 * @param targetServer 대상 서버 이름
 * @param message 전송할 메시지
 * @param player 메시지 전송에 사용할 플레이어 (연결 통로로 사용됨)
 * @return 전송 성공 여부
 */
public boolean sendMessageToServer(String targetServer, String message, Player player) {
    if (messagingPlugin != null) {
        return messagingPlugin.getMessagingService().sendMessageToServer(targetServer, message, player);
    }
    return false;
}
```

4. 메시지 수신 리스너 등록:

```java
@EventHandler
public void onMessageReceived(MessageReceivedEvent event) {
    String message = event.getMessage();
    getLogger().info("메시지 수신: " + message);
    
    // 메시지 처리 로직
    // ...
}
```

### Paper API 메서드

#### 메시지 전송

```java
/**
 * 다른 서버로 문자열 메시지를 전송합니다.
 * 
 * @param targetServer 대상 서버 이름
 * @param message 전송할 메시지
 * @param player 전송에 사용할 플레이어
 * @return 전송 성공 여부
 */
public boolean sendMessageToServer(String targetServer, String message, Player player);

/**
 * 다른 서버로 바이너리 데이터를 전송합니다.
 * 
 * @param targetServer 대상 서버 이름
 * @param data 전송할 데이터
 * @param player 전송에 사용할 플레이어
 * @return 전송 성공 여부
 */
public boolean sendToServer(String targetServer, byte[] data, Player player);
```

#### 서버 이름 가져오기

```java
/**
 * 현재 서버의 이름을 가져옵니다.
 * 
 * @return 서버 이름
 */
public String getServerName();
```

## 메시지 형식

### 문자열 메시지

문자열 메시지는 기본 포맷으로 UTF-8 인코딩을 사용합니다.

```java
// 문자열 메시지 전송
messagingPlugin.getMessagingService().sendMessageToServer(
    "target-server", 
    "Hello World!", 
    player
);
```

### 바이너리 메시지

바이너리 데이터를 직접 전송해야 하는 경우 바이트 배열을 사용합니다.

```java
// 사용자 정의 바이너리 데이터 생성
ByteArrayDataOutput out = ByteStreams.newDataOutput();
out.writeUTF("CustomAction");
out.writeInt(123);
out.writeDouble(45.67);

// 바이너리 메시지 전송
messagingPlugin.getMessagingService().sendToServer(
    "target-server", 
    out.toByteArray(), 
    player
);
```

### 메시지 구조화

JSON 형식으로 메시지를 구조화하여 전송할 수 있습니다:

```java
// JSON 메시지 생성 (Gson 라이브러리 사용)
JsonObject message = new JsonObject();
message.addProperty("action", "playerInfo");
message.addProperty("playerName", player.getName());
message.addProperty("health", player.getHealth());
message.addProperty("level", player.getLevel());

// JSON 문자열로 변환하여 전송
messagingPlugin.getMessagingService().sendMessageToServer(
    "target-server", 
    new Gson().toJson(message), 
    player
);
```

## 예제 코드

### 예제 1: 특정 서버의 모든 플레이어에게 텔레포트 명령어 실행

```java
// Velocity 플러그인 코드
public void teleportPlayersToSpawn(String targetServer) {
    // 모든 플레이어를 스폰으로 텔레포트하는 명령 전송
    String message = "COMMAND:teleport_all_to_spawn";
    messagingPlugin.sendProxyMessageToServer(targetServer, message);
}

// Paper 플러그인 코드 (수신 측)
@EventHandler
public void onMessageReceived(MessageReceivedEvent event) {
    String message = event.getMessage();
    
    if (message.equals("COMMAND:teleport_all_to_spawn")) {
        // 모든 플레이어를 스폰으로 텔레포트
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.teleport(player.getWorld().getSpawnLocation());
            player.sendMessage(ChatColor.GREEN + "관리자에 의해 스폰으로 텔레포트되었습니다.");
        }
    }
}
```

### 예제 2: 서버 간 플레이어 데이터 동기화

```java
// Paper 플러그인 코드 (전송 측)
public void syncPlayerData(Player player, String targetServer) {
    // 플레이어 데이터를 JSON으로 직렬화
    JsonObject playerData = new JsonObject();
    playerData.addProperty("action", "syncPlayerData");
    playerData.addProperty("playerName", player.getName());
    playerData.addProperty("health", player.getHealth());
    playerData.addProperty("food", player.getFoodLevel());
    playerData.addProperty("exp", player.getExp());
    playerData.addProperty("level", player.getLevel());
    
    // 다른 서버로 데이터 전송
    messagingPlugin.getMessagingService().sendMessageToServer(
        targetServer, 
        new Gson().toJson(playerData), 
        player
    );
}

// Paper 플러그인 코드 (수신 측)
@EventHandler
public void onMessageReceived(MessageReceivedEvent event) {
    String message = event.getMessage();
    
    try {
        JsonObject data = new Gson().fromJson(message, JsonObject.class);
        
        if (data.has("action") && data.get("action").getAsString().equals("syncPlayerData")) {
            String playerName = data.get("playerName").getAsString();
            Player player = Bukkit.getPlayer(playerName);
            
            if (player != null && player.isOnline()) {
                // 플레이어 데이터 적용
                player.setHealth(data.get("health").getAsDouble());
                player.setFoodLevel(data.get("food").getAsInt());
                player.setExp(data.get("exp").getAsFloat());
                player.setLevel(data.get("level").getAsInt());
                
                player.sendMessage(ChatColor.GREEN + "다른 서버에서 데이터가 동기화되었습니다.");
            }
        }
    } catch (Exception e) {
        event.getPlugin().getLogger().severe("메시지 처리 중 오류 발생: " + e.getMessage());
    }
}
```

## 오류 처리

### 일반적인 오류 처리

예외 처리를 통해 안정적인 메시지 전송 및 수신을 보장합니다:

```java
try {
    boolean success = messagingPlugin.getMessagingService().sendMessageToServer(
        targetServer, 
        message, 
        player
    );
    
    if (!success) {
        getLogger().warning("메시지 전송 실패: " + targetServer);
        // 실패 처리 로직
    }
} catch (Exception e) {
    getLogger().severe("메시지 전송 중 예외 발생: " + e.getMessage());
    e.printStackTrace();
    // 예외 처리 로직
}
```

### 재시도 로직

중요한 메시지는 재시도 로직을 구현하는 것이 좋습니다:

```java
private boolean sendMessageWithRetry(String targetServer, String message, Player player, int maxRetries) {
    int attempts = 0;
    boolean success = false;
    
    while (!success && attempts < maxRetries) {
        attempts++;
        try {
            success = messagingPlugin.getMessagingService().sendMessageToServer(
                targetServer, 
                message, 
                player
            );
            
            if (!success) {
                getLogger().warning("메시지 전송 실패 (시도 " + attempts + "/" + maxRetries + ")");
                // 재시도 전 짧은 대기 시간 추가
                Thread.sleep(500);
            }
        } catch (Exception e) {
            getLogger().severe("메시지 전송 중 예외 발생 (시도 " + attempts + "/" + maxRetries + "): " + e.getMessage());
            try {
                Thread.sleep(500);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    return success;
}
```

## 모범 사례

### 1. 서버 가용성 확인

메시지를 전송하기 전에 대상 서버가 존재하고 온라인 상태인지 확인합니다:

```java
// Velocity 플러그인 코드
public boolean isServerAvailable(String serverName) {
    RegisteredServer server = messagingPlugin.getServerByName(serverName);
    return server != null && server.ping().join().isPresent();
}
```

### 2. 메시지 크기 제한

메시지 크기를 제한하여 네트워크 오버헤드를 방지합니다:

```java
private static final int MAX_MESSAGE_SIZE = 1024; // 최대 1KB

public boolean sendMessage(String targetServer, String message, Player player) {
    if (message.length() > MAX_MESSAGE_SIZE) {
        getLogger().warning("메시지 크기 초과: " + message.length() + " > " + MAX_MESSAGE_SIZE);
        return false;
    }
    
    return messagingPlugin.getMessagingService().sendMessageToServer(
        targetServer, 
        message, 
        player
    );
}
```

### 3. 메시지 형식 표준화

일관된 메시지 형식을 사용하여 처리 로직을 단순화합니다:

```java
public enum MessageType {
    COMMAND, INFO, WARNING, ERROR, DATA
}

public boolean sendFormattedMessage(String targetServer, MessageType type, String content, Player player) {
    JsonObject message = new JsonObject();
    message.addProperty("type", type.name());
    message.addProperty("content", content);
    message.addProperty("timestamp", System.currentTimeMillis());
    message.addProperty("source", getPlugin().getServerName());
    
    return messagingPlugin.getMessagingService().sendMessageToServer(
        targetServer, 
        new Gson().toJson(message), 
        player
    );
}
```

### 4. 비동기 메시지 처리

메시지 전송 및 처리를 비동기적으로 수행하여 메인 서버 스레드 차단을 방지합니다:

```java
// 비동기 메시지 전송
Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
    boolean success = messagingPlugin.getMessagingService().sendMessageToServer(
        targetServer, 
        message, 
        player
    );
    
    // 결과 처리 (다시 메인 스레드로)
    Bukkit.getScheduler().runTask(this, () -> {
        if (success) {
            player.sendMessage(ChatColor.GREEN + "메시지가 성공적으로 전송되었습니다.");
        } else {
            player.sendMessage(ChatColor.RED + "메시지 전송에 실패했습니다.");
        }
    });
});
```

### 5. 로깅 및 모니터링

메시지 트래픽을 모니터링하기 위한 로깅 시스템을 구현합니다:

```java
public boolean sendMessageWithLogging(String targetServer, String message, Player player) {
    long startTime = System.currentTimeMillis();
    boolean success = messagingPlugin.getMessagingService().sendMessageToServer(
        targetServer, 
        message, 
        player
    );
    long endTime = System.currentTimeMillis();
    
    // 메시지 전송 로깅
    getLogger().info(String.format(
        "메시지 전송 [대상=%s, 성공=%s, 시간=%dms, 크기=%d바이트]",
        targetServer,
        success,
        (endTime - startTime),
        message.getBytes(StandardCharsets.UTF_8).length
    ));
    
    return success;
}
``` 