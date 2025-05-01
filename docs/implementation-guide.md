# LukeVanilla Paper 플러그인 구현 가이드

이 문서는 LukeVanilla-Velocity 시스템과 호환되는 Paper 플러그인을 구현하기 위한 상세한 가이드를 제공합니다. 이 가이드를 따라 개발된 Paper 플러그인은 LukeVanilla-Velocity 프록시와 원활하게 통신할 수 있습니다.

## 목차

1. [개요](#개요)
2. [프로젝트 설정](#프로젝트-설정)
3. [기본 구조](#기본-구조)
4. [메시징 시스템 구현](#메시징-시스템-구현)
5. [서버 상태 모니터링 구현](#서버-상태-모니터링-구현)
6. [예제 코드](#예제-코드)
7. [테스트 및 디버깅](#테스트-및-디버깅)
8. [배포 및 설치](#배포-및-설치)

## 개요

LukeVanilla Paper 플러그인은 다음 두 가지 주요 기능을 담당합니다:

1. **서버 간 메시징**: 다른 Paper 서버와 메시지 및 명령어 교환
2. **서버 상태 모니터링**: Velocity 프록시에서 서버의 온라인/오프라인 상태 모니터링 지원

이 가이드에서는 이러한 기능을 구현하는 방법과 LukeVanilla-Velocity 프록시와 호환되는 플러그인을 개발하는 과정을 설명합니다.

## 프로젝트 설정

### 필수 의존성

Paper 플러그인 개발을 위해 다음 의존성이 필요합니다:

```groovy
// Gradle 예시
dependencies {
    compileOnly 'io.papermc.paper:paper-api:1.19.4-R0.1-SNAPSHOT'
}
```

```xml
<!-- Maven 예시 -->
<dependencies>
    <dependency>
        <groupId>io.papermc.paper</groupId>
        <artifactId>paper-api</artifactId>
        <version>1.19.4-R0.1-SNAPSHOT</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

### plugin.yml 설정

```yaml
name: LukeVanillaPaper
version: '1.0.0'
main: com.lukehemmin.lukepaper.LukeVanillaPaper
api-version: '1.19'
description: Paper 서버용 LukeVanilla 플러그인
authors: [YourName]
commands:
  sendmsg:
    description: 다른 서버로 메시지 전송
    usage: /sendmsg <서버> <메시지>
    permission: lukevanilla.sendmsg
  runcmd:
    description: 다른 서버에서 명령어 실행
    usage: /runcmd <서버> <명령어>
    permission: lukevanilla.runcmd
permissions:
  lukevanilla.sendmsg:
    description: 서버 간 메시지 전송 권한
    default: op
  lukevanilla.runcmd:
    description: 서버 간 명령어 실행 권한
    default: op
```

## 기본 구조

### 메인 클래스

```java
package com.lukehemmin.lukepaper;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.entity.Player;

public class LukeVanillaPaper extends JavaPlugin implements PluginMessageListener {
    private ServerMessaging messaging;
    private ServerStatus status;
    
    @Override
    public void onEnable() {
        // 설정 파일 생성
        saveDefaultConfig();
        
        // 메시징 시스템 초기화
        messaging = new ServerMessaging(this);
        messaging.registerChannels();
        
        // 서버 상태 모니터링 초기화
        status = new ServerStatus(this);
        status.registerChannels();
        
        // 명령어 등록
        getCommand("sendmsg").setExecutor(new SendMessageCommand(this, messaging));
        getCommand("runcmd").setExecutor(new RunCommandCommand(this, messaging));
        
        // 이벤트 리스너 등록
        getServer().getPluginManager().registerEvents(new MessageListener(this), this);
        
        getLogger().info("LukeVanilla Paper 플러그인이 활성화되었습니다.");
    }
    
    @Override
    public void onDisable() {
        // 채널 등록 해제
        if (messaging != null) {
            messaging.unregisterChannels();
        }
        
        if (status != null) {
            status.unregisterChannels();
        }
        
        getLogger().info("LukeVanilla Paper 플러그인이 비활성화되었습니다.");
    }
    
    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] data) {
        // 채널에 따라 적절한 핸들러로 전달
        if (channel.equals("custom:msg")) {
            messaging.handleIncomingMessage(player, data);
        } else if (channel.equals("custom:status")) {
            status.handleStatusMessage(player, data);
        }
    }
    
    public ServerMessaging getMessaging() {
        return messaging;
    }
    
    public ServerStatus getStatus() {
        return status;
    }
}
```

## 메시징 시스템 구현

### ServerMessaging 클래스

```java
package com.lukehemmin.lukepaper;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;

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
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "custom:msg", (PluginMessageListener) plugin);
        
        plugin.getLogger().info("메시지 채널이 등록되었습니다: custom:msg");
    }
    
    /**
     * 메시지 채널 등록을 해제합니다.
     */
    public void unregisterChannels() {
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, "custom:msg");
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, "custom:msg");
        
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
     * 수신된 메시지를 처리합니다.
     */
    public void handleIncomingMessage(Player player, byte[] data) {
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
        ServerMessageEvent event = new ServerMessageEvent(message);
        plugin.getServer().getPluginManager().callEvent(event);
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
```

### ServerMessageEvent 클래스

```java
package com.lukehemmin.lukepaper;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

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

## 서버 상태 모니터링 구현

### ServerStatus 클래스

```java
package com.lukehemmin.lukepaper;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class ServerStatus {
    private final JavaPlugin plugin;
    
    public ServerStatus(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 상태 채널을 등록합니다.
     */
    public void registerChannels() {
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "custom:status");
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "custom:status", (PluginMessageListener) plugin);
        
        plugin.getLogger().info("상태 채널이 등록되었습니다: custom:status");
    }
    
    /**
     * 상태 채널 등록을 해제합니다.
     */
    public void unregisterChannels() {
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, "custom:status");
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, "custom:status");
        
        plugin.getLogger().info("상태 채널 등록이 해제되었습니다: custom:status");
    }
    
    /**
     * 상태 메시지를 처리합니다.
     */
    public void handleStatusMessage(Player player, byte[] data) {
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
                plugin.getLogger().info("Ping 메시지 수신, Pong으로 응답합니다.");
                sendPongResponse(player);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("상태 메시지 처리 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Pong 응답을 전송합니다.
     */
    private void sendPongResponse(Player player) {
        if (player == null || !player.isOnline()) {
            plugin.getLogger().warning("Pong 응답 전송을 위한 플레이어가 없습니다.");
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
            player.sendPluginMessage(plugin, "custom:status", baos.toByteArray());
            plugin.getLogger().fine("Pong 응답을 전송했습니다.");
        } catch (IOException e) {
            plugin.getLogger().severe("Pong 응답 전송 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 확장된 Pong 응답을 전송합니다. (선택적 구현)
     * 서버 상태에 대한 추가 정보를 포함합니다.
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
                                             plugin.getServer().getOnlinePlayers().size(),
                                             getServerTPS());
            
            byte[] infoBytes = serverInfo.getBytes(StandardCharsets.UTF_8);
            out.writeInt(infoBytes.length);
            out.write(infoBytes);
            
            player.sendPluginMessage(plugin, "custom:status", baos.toByteArray());
            plugin.getLogger().fine("확장 Pong 응답을 전송했습니다: " + serverInfo);
        } catch (IOException e) {
            plugin.getLogger().severe("확장 Pong 응답 전송 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 서버의 TPS를 가져옵니다.
     * Paper API에서는 getServerTPS() 메서드가 제공되지만, 이 예제에서는 간단히 구현합니다.
     */
    private double getServerTPS() {
        try {
            // Paper API의 TPS 액세스 메서드 사용
            return plugin.getServer().getTPS()[0];
        } catch (Exception e) {
            // 액세스할 수 없는 경우 기본값 반환
            return 20.0;
        }
    }
}
```

## 예제 코드

### 메시지 전송 명령어

```java
package com.lukehemmin.lukepaper;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class SendMessageCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final ServerMessaging messaging;
    
    public SendMessageCommand(JavaPlugin plugin, ServerMessaging messaging) {
        this.plugin = plugin;
        this.messaging = messaging;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
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
            // 온라인 플레이어가 없으면 메시지를 전송할 수 없음
            if (plugin.getServer().getOnlinePlayers().isEmpty()) {
                sender.sendMessage("§c온라인 플레이어가 없어 메시지를 전송할 수 없습니다.");
                return false;
            }
            
            // 첫 번째 온라인 플레이어를 통해 메시지 전송
            Player player = plugin.getServer().getOnlinePlayers().iterator().next();
            messaging.sendMessage(targetServer, message, player);
        } else {
            // 플레이어가 실행한 경우
            messaging.sendMessage(targetServer, message, (Player) sender);
        }
        
        sender.sendMessage("§a메시지가 " + targetServer + " 서버로 전송되었습니다: " + message);
        return true;
    }
}
```

### 명령어 실행 명령어

```java
package com.lukehemmin.lukepaper;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class RunCommandCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final ServerMessaging messaging;
    
    public RunCommandCommand(JavaPlugin plugin, ServerMessaging messaging) {
        this.plugin = plugin;
        this.messaging = messaging;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
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
            // 온라인 플레이어가 없으면 메시지를 전송할 수 없음
            if (plugin.getServer().getOnlinePlayers().isEmpty()) {
                sender.sendMessage("§c온라인 플레이어가 없어 명령어를 전송할 수 없습니다.");
                return false;
            }
            
            // 첫 번째 온라인 플레이어를 통해 명령어 전송
            Player player = plugin.getServer().getOnlinePlayers().iterator().next();
            messaging.sendCommand(targetServer, commandToRun);
        } else {
            // 플레이어가 실행한 경우
            messaging.sendCommand(targetServer, commandToRun);
        }
        
        sender.sendMessage("§a명령어가 " + targetServer + " 서버로 전송되었습니다: " + commandToRun);
        return true;
    }
}
```

### 메시지 리스너

```java
package com.lukehemmin.lukepaper;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class MessageListener implements Listener {
    private final JavaPlugin plugin;
    
    public MessageListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onServerMessage(ServerMessageEvent event) {
        plugin.getLogger().info("메시지 이벤트 수신: " + event.getMessage());
        
        // 여기에 메시지 처리 로직 구현
        // 예: 특정 메시지 형식에 따른 처리
        String message = event.getMessage();
        
        // 예시: 공지사항 메시지 처리
        if (message.startsWith("ANNOUNCE:")) {
            String announcement = message.substring(9); // "ANNOUNCE:" 제거
            plugin.getServer().broadcastMessage("§b[공지] §f" + announcement);
        }
        
        // 예시: 플레이어 정보 요청 처리
        if (message.equals("REQUEST:PLAYER_COUNT")) {
            int playerCount = plugin.getServer().getOnlinePlayers().size();
            // 결과 전송 (이 예시에서는 로그만 출력)
            plugin.getLogger().info("플레이어 수 요청에 응답: " + playerCount);
        }
    }
}
```

## 테스트 및 디버깅

### 로그 수준 설정

```java
// 로그 수준 설정 (디버그 모드)
private void setupLogging() {
    boolean debugMode = getConfig().getBoolean("debug-mode", false);
    if (debugMode) {
        try {
            // Paper/Spigot의 로깅 시스템을 사용하여 로그 수준 설정
            java.util.logging.Logger logger = getLogger();
            logger.setLevel(java.util.logging.Level.FINE);
            
            // 또는 Bukkit의 로깅 설정 사용
            org.bukkit.Bukkit.getLogger().setLevel(java.util.logging.Level.FINE);
            
            getLogger().info("디버그 모드가 활성화되었습니다.");
        } catch (Exception e) {
            getLogger().warning("로그 수준 설정 중 오류 발생: " + e.getMessage());
        }
    }
}
```

### config.yml 설정

```yaml
# LukeVanilla Paper 플러그인 설정

# 디버그 모드 (true/false)
debug-mode: false

# 메시징 설정
messaging:
  # 메시지 전송 시도 최대 횟수
  max-retries: 3
  # 재시도 간격 (밀리초)
  retry-interval: 1000

# 서버 상태 설정
status:
  # Ping 응답에 추가 정보 포함 (true/false)
  extended-pong: false
```

### 디버깅 유틸리티

```java
/**
 * 바이트 배열을 디버깅 목적으로 출력합니다.
 */
private void debugBytes(byte[] data) {
    if (!getConfig().getBoolean("debug-mode", false)) {
        return;
    }
    
    StringBuilder sb = new StringBuilder("바이트 데이터: ");
    for (byte b : data) {
        sb.append(String.format("%02X ", b));
    }
    getLogger().info(sb.toString());
}

/**
 * 메시지 전송/수신 로그를 기록합니다.
 */
private void logMessage(String direction, String target, String message) {
    if (!getConfig().getBoolean("debug-mode", false)) {
        return;
    }
    
    getLogger().info(String.format("[%s] %s: %s", direction, target, message));
}
```

## 배포 및 설치

### 배포 준비

1. **플러그인 JAR 파일 생성**:
   - Gradle: `./gradlew shadowJar` 또는 `./gradlew build`
   - Maven: `mvn package`

2. **필요한 파일 확인**:
   - JAR 파일
   - config.yml (기본 설정 파일)
   - README.md (설치 및 사용 지침)

### 설치 지침

1. **JAR 파일 복사**:
   - Paper 서버의 `plugins` 디렉토리에 JAR 파일 복사

2. **서버 설정**:
   - Paper 서버 재시작
   - 첫 실행 시 생성된 `plugins/LukeVanillaPaper/config.yml` 수정 (필요한 경우)
   - 다시 서버 재시작하여 설정 적용

3. **권한 설정**:
   - 권한 플러그인이 있는 경우, 필요한 권한 설정
   ```
   lukevanilla.sendmsg - 메시지 전송 권한
   lukevanilla.runcmd - 명령어 실행 권한
   ```

### 설정 최적화

```yaml
# 대규모 네트워크를 위한 최적화된 설정

# 디버그 모드 (운영 환경에서는 false로 설정)
debug-mode: false

# 메시징 설정
messaging:
  # 메시지 대기열 크기
  queue-size: 100
  # 메시지 처리 스레드 수
  worker-threads: 2
  # 메시지 전송 시도 최대 횟수
  max-retries: 3
  # 재시도 간격 (밀리초)
  retry-interval: 1000
  # 메시지 타임아웃 (밀리초)
  timeout: 5000

# 서버 상태 설정
status:
  # Ping 응답에 추가 정보 포함
  extended-pong: true
  # 서버 통계 수집 간격 (초)
  stats-interval: 60
```

이 가이드를 따라 LukeVanilla-Velocity 프록시와 호환되는 Paper 플러그인을 구현할 수 있습니다. 제시된 예제 코드를 기반으로 필요에 맞게 커스터마이징하여 사용하세요. 