# 서버 간 메시징 프로토콜

이 문서는 LukeVanilla-Velocity 프록시와 Paper 서버 간의 메시징 프로토콜에 대한 상세한 명세를 제공합니다. 이 프로토콜은 서버 간 통신 및 데이터 교환을 가능하게 합니다.

## 목차

1. [개요](#개요)
2. [통신 채널](#통신-채널)
3. [메시지 형식](#메시지-형식)
4. [서버 간 통신 (Server-to-Server)](#서버-간-통신-server-to-server)
5. [프록시-서버 통신 (Proxy-to-Server)](#프록시-서버-통신-proxy-to-server)
6. [명령어 처리](#명령어-처리)
7. [구현 가이드](#구현-가이드)
8. [예제 코드](#예제-코드)
9. [문제 해결](#문제-해결)

## 개요

LukeVanilla-Velocity 메시징 프로토콜은 Velocity 프록시와 연결된 Paper 서버 간의 통신을 위한 표준화된 방법을 제공합니다. 이 프로토콜을 통해 다음과 같은 기능을 구현할 수 있습니다:

1. 서버 간 데이터 및 명령어 전송
2. 프록시에서 특정 서버 또는 모든 서버로 명령어 전송
3. 서버 상태 업데이트 및 통계 보고
4. 다양한 이벤트에 대한 알림 전송

이 프로토콜은 Minecraft의 Plugin Messaging 시스템을 기반으로 하며, 바이너리 데이터 전송을 통해 효율적인 통신을 구현합니다.

## 통신 채널

### 채널 정의

LukeVanilla-Velocity 시스템은 다음과 같은 두 가지 주요 채널을 사용합니다:

1. **`custom:msg`**: 서버 간 일반 메시지 및 명령어 전송을 위한 채널
2. **`custom:status`**: 서버 상태 확인을 위한 채널 (자세한 내용은 [서버 상태 확인 프로토콜](server-status-protocol.md) 참조)

### 채널 등록 (Paper 서버에서)

Paper 서버에서는 다음과 같이 채널을 등록해야 합니다:

```java
@Override
public void onEnable() {
    // 메시지 채널 등록
    getServer().getMessenger().registerOutgoingPluginChannel(this, "custom:msg");
    getServer().getMessenger().registerIncomingPluginChannel(this, "custom:msg", this);
    
    getLogger().info("메시지 채널이 등록되었습니다: custom:msg");
}

@Override
public void onDisable() {
    // 채널 등록 해제
    getServer().getMessenger().unregisterOutgoingPluginChannel(this, "custom:msg");
    getServer().getMessenger().unregisterIncomingPluginChannel(this, "custom:msg", this);
    
    getLogger().info("메시지 채널 등록이 해제되었습니다: custom:msg");
}
```

## 메시지 형식

### 1. 서버 간 메시지 형식 (Server-to-Server)

서버에서 다른 서버로 메시지를 전송할 때 사용하는 형식:

```
[목적지 서버 이름 길이(byte)][목적지 서버 이름(UTF-8)][메시지 길이(int)][메시지 내용(UTF-8)]
```

구성 요소:
- **목적지 서버 이름 길이**: 1바이트, 목적지 서버 이름의 바이트 길이
- **목적지 서버 이름**: 목적지 서버의 식별자(velocity.toml에 정의된 서버 이름)
- **메시지 길이**: 4바이트, 메시지 내용의 바이트 길이
- **메시지 내용**: 실제 전송할 메시지 데이터

### 2. 명령어 메시지 형식 (Command)

명령어를 전송할 때 사용하는 형식:

```
[프리픽스(UTF-8:"CMD:")][명령어 내용(UTF-8)]
```

구성 요소:
- **프리픽스**: "CMD:" 문자열, 이 메시지가 명령어임을 나타냄
- **명령어 내용**: 실행할 명령어 문자열

### 3. 브로드캐스트 메시지 형식 (Broadcast)

모든 서버에 메시지를 전송할 때 사용하는 형식:

```
[목적지 서버 이름(UTF-8:"*")][메시지 길이(int)][메시지 내용(UTF-8)]
```

구성 요소:
- **목적지 서버 이름**: "*" 문자열, 모든 서버를 대상으로 함을 나타냄
- **메시지 길이**: 4바이트, 메시지 내용의 바이트 길이
- **메시지 내용**: 실제 전송할 메시지 데이터

## 서버 간 통신 (Server-to-Server)

### 메시지 전송 과정

1. **메시지 인코딩**: 소스 서버에서 메시지를 정의된 형식으로 인코딩
2. **프록시로 전송**: 인코딩된 메시지를 `custom:msg` 채널을 통해 프록시로 전송
3. **메시지 라우팅**: 프록시에서 목적지 서버 이름을 확인하고 해당 서버로 메시지 전달
4. **메시지 수신 및 처리**: 목적지 서버에서 메시지를 수신하고 디코딩하여 처리

### 메시지 인코딩 (Paper 서버)

```java
/**
 * 다른 서버로 메시지를 전송합니다.
 *
 * @param targetServer 목적지 서버 이름
 * @param message 전송할 메시지
 * @param player 메시지 전송에 사용할 플레이어
 */
public void sendMessageToServer(String targetServer, String message, Player player) {
    if (player == null || !player.isOnline()) {
        getLogger().warning("메시지 전송을 위한 플레이어가 없습니다.");
        return;
    }
    
    try {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        
        // 목적지 서버 이름 인코딩
        byte[] serverNameBytes = targetServer.getBytes(StandardCharsets.UTF_8);
        out.writeByte(serverNameBytes.length);
        out.write(serverNameBytes);
        
        // 메시지 인코딩
        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
        out.writeInt(messageBytes.length);
        out.write(messageBytes);
        
        // 메시지 전송
        player.sendPluginMessage(this, "custom:msg", baos.toByteArray());
        getLogger().info("서버 " + targetServer + "로 메시지 전송: " + message);
    } catch (IOException e) {
        getLogger().severe("메시지 인코딩 중 오류 발생: " + e.getMessage());
        e.printStackTrace();
    }
}
```

### 메시지 디코딩 (Paper 서버)

```java
@Override
public void onPluginMessageReceived(String channel, Player player, byte[] data) {
    if (channel.equals("custom:msg")) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             DataInputStream in = new DataInputStream(bais)) {
            
            // 메시지 길이 읽기
            int messageLength = in.readInt();
            
            // 메시지 읽기
            byte[] messageBytes = new byte[messageLength];
            in.readFully(messageBytes);
            String message = new String(messageBytes, StandardCharsets.UTF_8);
            
            // 메시지 처리
            processReceivedMessage(message, player);
        } catch (IOException e) {
            getLogger().severe("메시지 디코딩 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

/**
 * 수신된 메시지를 처리합니다.
 */
private void processReceivedMessage(String message, Player player) {
    // 메시지 유형 확인 (명령어인지 일반 메시지인지)
    if (message.startsWith("CMD:")) {
        // 명령어 처리
        String command = message.substring(4); // "CMD:" 제거
        executeCommand(command);
    } else {
        // 일반 메시지 처리
        getLogger().info("서버 간 메시지 수신: " + message);
        
        // 메시지 처리를 위한 이벤트 발생
        getServer().getPluginManager().callEvent(new ServerMessageReceivedEvent(message));
    }
}

/**
 * 수신된 명령어를 실행합니다.
 */
private void executeCommand(String command) {
    getLogger().info("서버 간 명령어 실행: " + command);
    getServer().dispatchCommand(getServer().getConsoleSender(), command);
}
```

## 프록시-서버 통신 (Proxy-to-Server)

### 메시지 라우팅 (Velocity 프록시)

```kotlin
/**
 * 서버에서 프록시로 전송된 메시지를 목적지 서버로 라우팅합니다.
 */
fun routeMessage(sourcePlayer: Player, data: ByteArray) {
    try {
        val bais = ByteArrayInputStream(data)
        val `in` = DataInputStream(bais)
        
        // 목적지 서버 이름 읽기
        val targetServerNameLength = `in`.readByte().toInt()
        val targetServerNameBytes = ByteArray(targetServerNameLength)
        `in`.readFully(targetServerNameBytes)
        val targetServerName = String(targetServerNameBytes, StandardCharsets.UTF_8)
        
        // 메시지 길이 읽기
        val messageLength = `in`.readInt()
        val messageBytes = ByteArray(messageLength)
        `in`.readFully(messageBytes)
        
        // 브로드캐스트 메시지 처리 ("*" 대상)
        if (targetServerName == "*") {
            broadcastToAllServers(messageBytes, sourcePlayer)
            return
        }
        
        // 특정 서버로 메시지 전달
        server.getServer(targetServerName).ifPresent { targetServer ->
            // 타겟 서버에 연결된 플레이어 찾기
            val targetPlayers = findPlayersOnServer(targetServer)
            
            if (targetPlayers.isEmpty()) {
                logger.warning("서버 $targetServerName에 연결된 플레이어가 없어 메시지 전달이 불가능합니다.")
                return@ifPresent
            }
            
            // 첫 번째 플레이어를 통해 메시지 전달
            val targetPlayer = targetPlayers.first()
            
            // 원본 메시지에서 서버 이름 부분을 제거하고 메시지 길이와 내용만 전달
            val forwardData = ByteArrayOutputStream()
            val out = DataOutputStream(forwardData)
            
            out.writeInt(messageLength)
            out.write(messageBytes)
            
            targetPlayer.sendPluginMessage(Identity.nil(), "custom:msg", forwardData.toByteArray())
            logger.info("서버 ${sourcePlayer.currentServer.get().serverInfo.name}에서 서버 $targetServerName로 메시지를 라우팅했습니다.")
        } ?: run {
            logger.warning("목적지 서버 $targetServerName을 찾을 수 없습니다.")
        }
    } catch (e: Exception) {
        logger.error("메시지 라우팅 중 오류 발생", e)
    }
}

/**
 * 지정된 서버에 연결된 플레이어 목록을 반환합니다.
 */
private fun findPlayersOnServer(targetServer: RegisteredServer): List<Player> {
    return server.allPlayers
        .filter { player -> 
            player.currentServer.map { it.server == targetServer }.orElse(false)
        }
}

/**
 * 모든 서버에 메시지를 브로드캐스트합니다.
 */
private fun broadcastToAllServers(messageBytes: ByteArray, sourcePlayer: Player) {
    val sourceServerName = sourcePlayer.currentServer.get().serverInfo.name
    
    server.allServers
        .filter { it.serverInfo.name != sourceServerName } // 소스 서버 제외
        .forEach { targetServer ->
            val targetPlayers = findPlayersOnServer(targetServer)
            if (targetPlayers.isNotEmpty()) {
                val targetPlayer = targetPlayers.first()
                
                // 메시지 길이와 내용만 전달
                val forwardData = ByteArrayOutputStream()
                val out = DataOutputStream(forwardData)
                
                out.writeInt(messageBytes.size)
                out.write(messageBytes)
                
                targetPlayer.sendPluginMessage(Identity.nil(), "custom:msg", forwardData.toByteArray())
                logger.info("서버 $sourceServerName에서 서버 ${targetServer.serverInfo.name}로 브로드캐스트 메시지를 전달했습니다.")
            }
        }
    
    logger.info("모든 서버에 메시지를 브로드캐스트했습니다.")
}
```

## 명령어 처리

### 1. 명령어 전송 (Paper 서버에서)

```java
/**
 * 다른 서버에 명령어를 전송합니다.
 *
 * @param targetServer 목적지 서버 이름
 * @param command 실행할 명령어
 * @param player 메시지 전송에 사용할 플레이어
 */
public void sendCommandToServer(String targetServer, String command, Player player) {
    // 명령어 형식으로 변환 (CMD: 프리픽스 추가)
    String cmdMessage = "CMD:" + command;
    
    // 기존 메시지 전송 메서드 활용
    sendMessageToServer(targetServer, cmdMessage, player);
}

/**
 * 모든 서버에 명령어를 브로드캐스트합니다.
 *
 * @param command 실행할 명령어
 * @param player 메시지 전송에 사용할 플레이어
 */
public void broadcastCommand(String command, Player player) {
    String cmdMessage = "CMD:" + command;
    sendMessageToServer("*", cmdMessage, player);
}
```

### 2. 명령어 실행 구현 예시

```java
/**
 * 서버 간 메시지 전송 명령어를 처리합니다.
 */
@Override
public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (command.getName().equalsIgnoreCase("sendmsg")) {
        if (args.length < 2) {
            sender.sendMessage("§c사용법: /sendmsg <서버> <메시지>");
            return false;
        }
        
        String targetServer = args[0];
        
        // 나머지 인수를 메시지로 조합
        StringBuilder messageBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            messageBuilder.append(args[i]).append(" ");
        }
        String message = messageBuilder.toString().trim();
        
        // 콘솔에서 실행한 경우
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다.");
            return false;
        }
        
        Player player = (Player) sender;
        sendMessageToServer(targetServer, message, player);
        
        sender.sendMessage("§a메시지가 " + targetServer + " 서버로 전송되었습니다: " + message);
        return true;
    }
    else if (command.getName().equalsIgnoreCase("runcmd")) {
        if (args.length < 2) {
            sender.sendMessage("§c사용법: /runcmd <서버> <명령어>");
            return false;
        }
        
        String targetServer = args[0];
        
        // 나머지 인수를 명령어로 조합
        StringBuilder commandBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            commandBuilder.append(args[i]).append(" ");
        }
        String commandToRun = commandBuilder.toString().trim();
        
        // 콘솔에서 실행한 경우
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다.");
            return false;
        }
        
        Player player = (Player) sender;
        sendCommandToServer(targetServer, commandToRun, player);
        
        sender.sendMessage("§a명령어가 " + targetServer + " 서버로 전송되었습니다: " + commandToRun);
        return true;
    }
    
    return false;
}
```

## 구현 가이드

### Paper 플러그인 구현 단계

1. **채널 등록**: 플러그인 활성화 시 `custom:msg` 채널을 등록합니다.
2. **메시지 리스너 구현**: `onPluginMessageReceived` 메서드를 구현하여 수신된 메시지를 처리합니다.
3. **메시지 전송 메서드 구현**: 다른 서버로 메시지를 전송하는 메서드를 구현합니다.
4. **명령어 및 이벤트 처리**: 수신된 메시지에 대한 명령어 실행 및 이벤트 처리 로직을 구현합니다.
5. **오류 처리**: 메시지 전송 및 수신 중 발생할 수 있는 오류에 대한 처리를 구현합니다.

### 메시지 유틸리티 클래스 구현 예시

```java
/**
 * 서버 간 메시징을 위한 유틸리티 클래스
 */
public class ServerMessaging {
    private final JavaPlugin plugin;
    
    public ServerMessaging(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 메시지 채널을 등록합니다.
     */
    public void registerChannels() {
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "custom:msg");
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "custom:msg", 
            (channel, player, data) -> onMessageReceived(channel, player, data));
        
        plugin.getLogger().info("메시지 채널이 등록되었습니다: custom:msg");
    }
    
    /**
     * 메시지 채널 등록을 해제합니다.
     */
    public void unregisterChannels() {
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, "custom:msg");
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, "custom:msg", 
            (channel, player, data) -> {});
        
        plugin.getLogger().info("메시지 채널 등록이 해제되었습니다: custom:msg");
    }
    
    /**
     * 다른 서버로 메시지를 전송합니다.
     */
    public void sendMessage(String targetServer, String message) {
        // 온라인 플레이어가 없으면 메시지를 전송할 수 없음
        if (plugin.getServer().getOnlinePlayers().isEmpty()) {
            plugin.getLogger().warning("온라인 플레이어가 없어 메시지를 전송할 수 없습니다.");
            return;
        }
        
        // 첫 번째 온라인 플레이어를 통해 메시지 전송
        Player player = plugin.getServer().getOnlinePlayers().iterator().next();
        sendMessage(targetServer, message, player);
    }
    
    /**
     * 특정 플레이어를 통해 다른 서버로 메시지를 전송합니다.
     */
    public void sendMessage(String targetServer, String message, Player player) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            
            // 목적지 서버 이름 인코딩
            byte[] serverNameBytes = targetServer.getBytes(StandardCharsets.UTF_8);
            out.writeByte(serverNameBytes.length);
            out.write(serverNameBytes);
            
            // 메시지 인코딩
            byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
            out.writeInt(messageBytes.length);
            out.write(messageBytes);
            
            // 메시지 전송
            player.sendPluginMessage(plugin, "custom:msg", baos.toByteArray());
            plugin.getLogger().info("서버 " + targetServer + "로 메시지 전송: " + message);
        } catch (IOException e) {
            plugin.getLogger().severe("메시지 인코딩 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 메시지를 수신했을 때 호출되는 메서드
     */
    private void onMessageReceived(String channel, Player player, byte[] data) {
        if (!channel.equals("custom:msg")) {
            return;
        }
        
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             DataInputStream in = new DataInputStream(bais)) {
            
            // 메시지 길이 읽기
            int messageLength = in.readInt();
            
            // 메시지 읽기
            byte[] messageBytes = new byte[messageLength];
            in.readFully(messageBytes);
            String message = new String(messageBytes, StandardCharsets.UTF_8);
            
            // 메시지 처리
            processMessage(message);
        } catch (IOException e) {
            plugin.getLogger().severe("메시지 디코딩 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 수신된 메시지를 처리합니다.
     */
    private void processMessage(String message) {
        // 명령어 메시지 처리
        if (message.startsWith("CMD:")) {
            String command = message.substring(4); // "CMD:" 제거
            plugin.getLogger().info("서버 간 명령어 실행: " + command);
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
            return;
        }
        
        // 일반 메시지 처리
        plugin.getLogger().info("서버 간 메시지 수신: " + message);
        
        // 이벤트 발생
        plugin.getServer().getPluginManager().callEvent(new ServerMessageEvent(message));
    }
    
    /**
     * 다른 서버에 명령어를 전송합니다.
     */
    public void sendCommand(String targetServer, String command) {
        sendMessage(targetServer, "CMD:" + command);
    }
    
    /**
     * 모든 서버에 메시지를 브로드캐스트합니다.
     */
    public void broadcast(String message) {
        sendMessage("*", message);
    }
    
    /**
     * 모든 서버에 명령어를 브로드캐스트합니다.
     */
    public void broadcastCommand(String command) {
        sendMessage("*", "CMD:" + command);
    }
}

/**
 * 서버 메시지 수신 이벤트
 */
public class ServerMessageEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final String message;
    
    public ServerMessageEvent(String message) {
        this.message = message;
    }
    
    public String getMessage() {
        return message;
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
```

## 예제 코드

### Paper 플러그인 예제

```java
public class ExamplePlugin extends JavaPlugin implements PluginMessageListener {
    private ServerMessaging messaging;
    
    @Override
    public void onEnable() {
        // 메시징 초기화
        messaging = new ServerMessaging(this);
        messaging.registerChannels();
        
        // 명령어 등록
        getCommand("sendmsg").setExecutor(this);
        getCommand("runcmd").setExecutor(this);
        
        // 이벤트 리스너 등록
        getServer().getPluginManager().registerEvents(new MessageListener(), this);
        
        getLogger().info("플러그인이 활성화되었습니다.");
    }
    
    @Override
    public void onDisable() {
        // 채널 등록 해제
        messaging.unregisterChannels();
        
        getLogger().info("플러그인이 비활성화되었습니다.");
    }
    
    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] data) {
        // ServerMessaging에서 처리함
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("sendmsg")) {
            if (args.length < 2) {
                sender.sendMessage("§c사용법: /sendmsg <서버> <메시지>");
                return false;
            }
            
            String targetServer = args[0];
            
            // 나머지 인수를 메시지로 조합
            StringBuilder messageBuilder = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                messageBuilder.append(args[i]).append(" ");
            }
            String message = messageBuilder.toString().trim();
            
            messaging.sendMessage(targetServer, message);
            
            sender.sendMessage("§a메시지가 " + targetServer + " 서버로 전송되었습니다: " + message);
            return true;
        }
        else if (command.getName().equalsIgnoreCase("runcmd")) {
            if (args.length < 2) {
                sender.sendMessage("§c사용법: /runcmd <서버> <명령어>");
                return false;
            }
            
            String targetServer = args[0];
            
            // 나머지 인수를 명령어로 조합
            StringBuilder commandBuilder = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                commandBuilder.append(args[i]).append(" ");
            }
            String commandToRun = commandBuilder.toString().trim();
            
            messaging.sendCommand(targetServer, commandToRun);
            
            sender.sendMessage("§a명령어가 " + targetServer + " 서버로 전송되었습니다: " + commandToRun);
            return true;
        }
        
        return false;
    }
    
    /**
     * 메시지 이벤트 리스너
     */
    private class MessageListener implements Listener {
        @EventHandler
        public void onServerMessage(ServerMessageEvent event) {
            getLogger().info("메시지 이벤트 수신: " + event.getMessage());
            
            // 메시지 처리 로직 구현
            // ...
        }
    }
}
```

## 문제 해결

### 일반적인 문제

#### 메시지가 전송되지 않음

**가능한 원인**:
1. 채널이 올바르게 등록되지 않음
2. 메시지 형식이 잘못됨
3. 온라인 플레이어가 없음

**해결 방법**:
1. 채널 등록 코드 확인
2. 메시지 인코딩 로직 검토
3. 메시지 전송에 사용할 온라인 플레이어 확인

#### 메시지가 목적지 서버에 도달하지 않음

**가능한 원인**:
1. 목적지 서버 이름이 잘못됨
2. 프록시 측 라우팅 로직에 오류가 있음
3. 목적지 서버에 연결된 플레이어가 없음

**해결 방법**:
1. velocity.toml에서 서버 이름 확인
2. 프록시 로그에서 라우팅 오류 확인
3. 목적지 서버에 플레이어가 있는지 확인

#### 명령어가 실행되지 않음

**가능한 원인**:
1. 명령어 형식이 잘못됨
2. 명령어 실행 권한 문제
3. 명령어 처리 로직에 오류가 있음

**해결 방법**:
1. 명령어 형식 확인 (CMD: 프리픽스 포함 여부)
2. 콘솔 권한으로 명령어가 실행되는지 확인
3. 명령어 처리 로직 검토

### 디버깅 팁

1. **로깅 활성화**: 메시지 전송 및 수신 시 상세한 로그 기록
2. **단계별 테스트**: 간단한 메시지부터 시작하여 점진적으로 테스트
3. **패킷 검사**: 바이트 배열의 각 요소를 출력하여 메시지 형식 확인

```java
/**
 * 바이트 배열을 디버깅 목적으로 출력합니다.
 */
private void debugBytes(byte[] data) {
    StringBuilder sb = new StringBuilder("바이트 데이터: ");
    for (byte b : data) {
        sb.append(String.format("%02X ", b));
    }
    getLogger().info(sb.toString());
}
```

이 문서는 LukeVanilla-Velocity 시스템의 서버 간 메시징 프로토콜에 대한 상세한 설명을 제공합니다. 이 프로토콜을 구현하여 서버 간 효율적인 통신과 데이터 교환을 구현할 수 있습니다. 