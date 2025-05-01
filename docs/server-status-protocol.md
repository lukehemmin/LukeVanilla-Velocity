# 서버 상태 확인 프로토콜

이 문서는 LukeVanilla-Velocity 프록시와 Paper 서버 간의 서버 상태 확인 프로토콜에 대한 상세한 명세를 제공합니다. 이 프로토콜은 서버의 온라인/오프라인 상태를 확인하고 플레이어를 적절한 서버로 라우팅하는 데 사용됩니다.

## 목차

1. [개요](#개요)
2. [상태 확인 채널](#상태-확인-채널)
3. [Ping/Pong 메커니즘](#pingpong-메커니즘)
4. [서버 상태 관리](#서버-상태-관리)
5. [플레이어 라우팅](#플레이어-라우팅)
6. [구현 가이드](#구현-가이드)
7. [문제 해결](#문제-해결)

## 개요

LukeVanilla-Velocity 시스템에서는 서버의 온라인/오프라인 상태를 실시간으로 모니터링하기 위해 ping/pong 메커니즘을 사용합니다. 이를 통해 다음과 같은 기능을 제공합니다:

1. 서버 상태의 실시간 모니터링
2. 서버 상태에 따른 플레이어 자동 라우팅
3. 서버 상태 변경 시 이벤트 발생 및 처리

## 상태 확인 채널

서버 상태 확인을 위해 `custom:status` 채널을 사용합니다.

### 채널 등록 (Paper 서버에서)

```java
@Override
public void onEnable() {
    // 상태 확인 채널 등록
    getServer().getMessenger().registerOutgoingPluginChannel(this, "custom:status");
    getServer().getMessenger().registerIncomingPluginChannel(this, "custom:status", this);
    
    getLogger().info("상태 확인 채널이 등록되었습니다: custom:status");
}
```

## Ping/Pong 메커니즘

### 1. Ping 메시지 형식

프록시에서 서버로 전송되는 ping 메시지 형식:

```
[메시지 길이(int)][메시지 내용(UTF-8)]
```

여기서 메시지 내용은 "ping" 문자열입니다.

### 2. Pong 메시지 형식

서버에서 프록시로 응답하는 pong 메시지 형식:

```
[메시지 길이(int)][메시지 내용(UTF-8)]
```

여기서 메시지 내용은 "pong" 문자열입니다.

### 3. 프로세스 흐름

1. **Ping 전송**: 프록시는 주기적으로(기본값: 30초) 각 등록된 서버에 ping 메시지를 전송합니다.
2. **Ping 수신**: 서버는 ping 메시지를 수신하고 pong으로 응답합니다.
3. **Pong 수신**: 프록시는 일정 시간(기본값: 5초) 내에 pong 응답을 받으면 서버를 온라인으로 간주합니다.
4. **타임아웃**: 프록시가 지정된 시간 내에 pong 응답을 받지 못하면 서버를 오프라인으로 간주합니다.

## 서버 상태 관리

### 1. 상태 저장 구조

프록시에서는 서버 상태를 다음과 같은 구조로 저장합니다:

```kotlin
// 서버 상태 맵 (서버 이름 -> 온라인 상태)
private val serverStatus = ConcurrentHashMap<String, Boolean>()
```

### 2. 상태 확인 타이머

프록시는 주기적으로 서버 상태를 확인하는 타이머를 실행합니다:

```kotlin
// 상태 확인 주기(초)
private val pingInterval = 30

// 응답 타임아웃(초)
private val responseTimeout = 5

// 상태 확인 타이머 설정
private fun setupStatusCheckTimer() {
    server.scheduler.buildTask(plugin) {
        checkAllServersStatus()
    }
    .repeat(Duration.ofSeconds(pingInterval.toLong()))
    .schedule()
}
```

### 3. 상태 변경 이벤트

서버 상태가 변경될 때 이벤트를 발생시켜 다른 컴포넌트가 이에 반응할 수 있게 합니다:

```kotlin
/**
 * 서버 상태를 업데이트하고 필요한 경우 이벤트를 발생시킵니다.
 */
fun updateServerStatus(serverName: String, isOnline: Boolean) {
    val previousStatus = serverStatus.getOrDefault(serverName, false)
    
    // 상태가 변경된 경우에만 이벤트 발생
    if (previousStatus != isOnline) {
        serverStatus[serverName] = isOnline
        
        // 로그 기록
        logger.info("서버 $serverName 상태 변경: ${if (isOnline) "온라인" else "오프라인"}")
        
        // 상태 변경 이벤트 발생
        val event = ServerStatusChangeEvent(serverName, isOnline)
        server.eventManager.fire(event)
        
        // 서버가 온라인으로 전환된 경우, 플레이어 이동 처리
        if (isOnline && serverName == "survival") {
            handleServerCameOnline(serverName)
        }
    }
}
```

## 플레이어 라우팅

### 1. 초기 연결 라우팅

플레이어가 처음 프록시에 연결할 때 서버 상태에 따라 적절한 서버로 라우팅합니다:

```kotlin
/**
 * 플레이어를 적절한 서버로 연결합니다.
 * 메인 서버가 온라인이면 그쪽으로, 아니면 대체 서버로 연결합니다.
 */
fun connectToAppropriateServer(player: Player) {
    // 기본 서버 (일반적으로 survival)
    val defaultServer = "survival"
    // 대체 서버 (일반적으로 lobby)
    val fallbackServer = "lobby"
    
    // 기본 서버가 온라인인지 확인
    val isDefaultServerOnline = isServerOnline(defaultServer)
    
    // 연결할 서버 선택
    val targetServerName = if (isDefaultServerOnline) defaultServer else fallbackServer
    
    // 서버 인스턴스 가져오기
    server.getServer(targetServerName).ifPresent { targetServer ->
        // 현재 연결된 서버와 다른 경우에만 연결 시도
        if (player.currentServer.isEmpty() || player.currentServer.get().server != targetServer) {
            player.createConnectionRequest(targetServer).fireAndForget()
            
            logger.info("플레이어 ${player.username}를 $targetServerName 서버로 연결합니다.")
            
            // 플레이어에게 알림
            player.sendMessage(Component.text("$targetServerName 서버로 연결합니다..."))
        }
    }
}
```

### 2. 서버 상태 변경 시 플레이어 이동

서버 상태가 변경될 때(특히 오프라인에서 온라인으로) 플레이어를 자동으로 이동시킵니다:

```kotlin
/**
 * 서버가 온라인 상태로 변경되었을 때 호출됩니다.
 * lobby에 있는 플레이어들을 survival 서버로 자동 이동시킵니다.
 */
private fun handleServerCameOnline(serverName: String) {
    // survival 서버가 아니면 무시
    if (serverName != "survival") {
        return
    }
    
    logger.info("survival 서버가 온라인 상태가 되었습니다. lobby에 있는 플레이어들을 이동시킵니다.")
    
    // lobby 서버 인스턴스 가져오기
    val lobbyServer = server.getServer("lobby").orElse(null) ?: return
    
    // survival 서버 인스턴스 가져오기
    val survivalServer = server.getServer("survival").orElse(null) ?: return
    
    // lobby에 있는 모든 플레이어를 survival 서버로 이동
    server.allPlayers.forEach { player ->
        player.currentServer.ifPresent { currentServer ->
            if (currentServer.server == lobbyServer) {
                // 플레이어에게 알림
                player.sendMessage(Component.text("survival 서버가 온라인 상태가 되었습니다. 이동합니다..."))
                
                // 서버 이동 요청
                player.createConnectionRequest(survivalServer).fireAndForget()
                
                logger.info("플레이어 ${player.username}를 lobby에서 survival 서버로 이동시킵니다.")
            }
        }
    }
}
```

## 구현 가이드

### Paper 서버에서 상태 확인 응답 구현

```java
/**
 * 플러그인 메시지 수신 처리
 */
@Override
public void onPluginMessageReceived(String channel, Player player, byte[] data) {
    if (channel.equals("custom:status")) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             DataInputStream in = new DataInputStream(bais)) {
            
            // 메시지 길이 읽기
            int messageLength = in.readInt();
            
            // 메시지 읽기
            byte[] messageBytes = new byte[messageLength];
            in.readFully(messageBytes);
            String message = new String(messageBytes, StandardCharsets.UTF_8);
            
            // ping 메시지에 pong으로 응답
            if (message.equals("ping")) {
                getLogger().info("Ping 메시지 수신, Pong으로 응답합니다.");
                sendPongResponse(player);
            }
        } catch (IOException e) {
            getLogger().severe("상태 메시지 처리 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

/**
 * Pong 응답을 전송합니다.
 */
private void sendPongResponse(Player player) {
    if (player == null || !player.isOnline()) {
        getLogger().warning("Pong 응답 전송을 위한 플레이어가 없습니다.");
        return;
    }
    
    try {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        
        // "pong" 메시지 인코딩
        byte[] pongBytes = "pong".getBytes(StandardCharsets.UTF_8);
        out.writeInt(pongBytes.length);
        out.write(pongBytes);
        
        // 메시지 전송
        player.sendPluginMessage(this, "custom:status", baos.toByteArray());
        getLogger().fine("Pong 응답을 전송했습니다.");
    } catch (IOException e) {
        getLogger().severe("Pong 응답 전송 중 오류 발생: " + e.getMessage());
        e.printStackTrace();
    }
}
```

### 커스텀 상태 응답 구현

일부 서버에서는 단순 pong 응답 대신 추가 정보(예: 플레이어 수, TPS 등)를 포함하는 커스텀 상태 응답을 구현할 수 있습니다:

```java
/**
 * 확장된 Pong 응답을 전송합니다.
 */
private void sendExtendedPongResponse(Player player) {
    if (player == null || !player.isOnline()) {
        return;
    }
    
    try {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        
        // 기본 형식: "pong:playerCount:tps"
        String serverInfo = String.format("pong:%d:%.2f", 
                                         getServer().getOnlinePlayers().size(),
                                         getServer().getTPS()[0]);
        
        byte[] infoBytes = serverInfo.getBytes(StandardCharsets.UTF_8);
        out.writeInt(infoBytes.length);
        out.write(infoBytes);
        
        player.sendPluginMessage(this, "custom:status", baos.toByteArray());
    } catch (IOException e) {
        getLogger().severe("확장 Pong 응답 전송 중 오류 발생: " + e.getMessage());
        e.printStackTrace();
    }
}
```

## 문제 해결

### 일반적인 문제

#### 서버가 항상 오프라인으로 표시됨

**가능한 원인**:
1. 채널이 올바르게 등록되지 않음
2. Ping 메시지가 서버에 도달하지 않음
3. Pong 응답이 프록시에 도달하지 않음
4. 메시지 형식이 잘못됨

**해결 방법**:
1. 채널 등록 코드 확인
2. 로그에서 ping/pong 메시지 확인
3. 방화벽 설정 확인
4. 메시지 인코딩/디코딩 로직 검토

#### 플레이어가 잘못된 서버로 라우팅됨

**가능한 원인**:
1. 서버 상태가 잘못 감지됨
2. 라우팅 로직에 오류가 있음
3. 서버 이름이 잘못 설정됨

**해결 방법**:
1. 서버 상태 확인 로직 검토
2. 라우팅 로직 검토
3. `velocity.toml`에서 서버 이름 확인

### 디버깅 팁

1. **상태 확인 로그 활성화**: 로그 수준을 DEBUG로 설정하여 상세한 상태 확인 로그 확인
2. **수동 상태 확인**: 디버그 명령어를 추가하여 수동으로 서버 상태 확인
3. **상태 변경 이벤트 모니터링**: 상태 변경 이벤트에 리스너를 추가하여 상태 변경 모니터링

```java
// 로그 수준 설정
@Override
public void onEnable() {
    // ... 기존 초기화 코드 ...
    
    // 디버그 모드 설정
    boolean debugMode = getConfig().getBoolean("debug-mode", false);
    if (debugMode) {
        // 로그 수준을 DEBUG로 설정
        ((ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("com.lukehemmin")).setLevel(ch.qos.logback.classic.Level.DEBUG);
        getLogger().info("디버그 모드가 활성화되었습니다.");
    }
}

// 수동 상태 확인 명령어
@Override
public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (command.getName().equalsIgnoreCase("checkstatus")) {
        // 온라인 플레이어를 통해 ping 메시지 전송
        if (!getServer().getOnlinePlayers().isEmpty()) {
            Player player = getServer().getOnlinePlayers().iterator().next();
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(baos);
                
                byte[] pingBytes = "ping".getBytes(StandardCharsets.UTF_8);
                out.writeInt(pingBytes.length);
                out.write(pingBytes);
                
                player.sendPluginMessage(this, "custom:status", baos.toByteArray());
                sender.sendMessage("상태 확인 메시지를 전송했습니다.");
                return true;
            } catch (IOException e) {
                sender.sendMessage("상태 확인 메시지 전송 중 오류 발생: " + e.getMessage());
                return false;
            }
        } else {
            sender.sendMessage("온라인 플레이어가 없어 메시지를 전송할 수 없습니다.");
            return false;
        }
    }
    return false;
}
```

이 문서는 LukeVanilla-Velocity 시스템의 서버 상태 확인 프로토콜에 대한 상세한 설명을 제공합니다. 이 프로토콜을 구현하여 서버 상태를 실시간으로 모니터링하고 플레이어를 적절한 서버로 라우팅할 수 있습니다. 