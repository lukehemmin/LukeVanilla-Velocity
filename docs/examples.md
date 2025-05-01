# Paper 플러그인 구현 예제

이 문서는 LukeVanilla-Velocity 시스템과 호환되는 Paper 플러그인을 구현하기 위한 실용적인 예제를 제공합니다. 여기서 제공하는 예제 코드를 참고하여 자신만의 Paper 플러그인을 개발할 수 있습니다.

## 목차

1. [기본 플러그인 템플릿](#기본-플러그인-템플릿)
2. [서버 간 메시징 예제](#서버-간-메시징-예제)
3. [서버 상태 응답 예제](#서버-상태-응답-예제)
4. [이벤트 처리 예제](#이벤트-처리-예제)
5. [고급 예제](#고급-예제)

## 기본 플러그인 템플릿

아래는 LukeVanilla-Velocity 시스템과 호환되는 Paper 플러그인의 기본 템플릿입니다.

### 메인 클래스

```java
package com.example.lukepaper;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.entity.Player;

public class LukePaperPlugin extends JavaPlugin implements PluginMessageListener {
    private MessageHandler messageHandler;
    private StatusHandler statusHandler;
    
    @Override
    public void onEnable() {
        // 설정 파일 생성
        saveDefaultConfig();
        
        // 메시지 핸들러 초기화
        messageHandler = new MessageHandler(this);
        messageHandler.registerChannels();
        
        // 상태 핸들러 초기화
        statusHandler = new StatusHandler(this);
        statusHandler.registerChannels();
        
        // 명령어 등록
        getCommand("sendmsg").setExecutor(new SendMessageCommand(this));
        
        getLogger().info("LukePaper 플러그인이 활성화되었습니다.");
    }
    
    @Override
    public void onDisable() {
        // 채널 등록 해제
        if (messageHandler != null) {
            messageHandler.unregisterChannels();
        }
        
        if (statusHandler != null) {
            statusHandler.unregisterChannels();
        }
        
        getLogger().info("LukePaper 플러그인이 비활성화되었습니다.");
    }
    
    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] data) {
        // 채널에 따라 적절한 핸들러로 전달
        if (channel.equals("custom:msg")) {
            messageHandler.handleMessage(player, data);
        } else if (channel.equals("custom:status")) {
            statusHandler.handleStatusMessage(player, data);
        }
    }
    
    public MessageHandler getMessageHandler() {
        return messageHandler;
    }
    
    public StatusHandler getStatusHandler() {
        return statusHandler;
    }
}
```

### plugin.yml

```yaml
name: LukePaper
version: '1.0.0'
main: com.example.lukepaper.LukePaperPlugin
api-version: '1.19'
description: LukeVanilla-Velocity와 호환되는 Paper 플러그인
commands:
  sendmsg:
    description: 다른 서버로 메시지 전송
    usage: /sendmsg <서버> <메시지>
    permission: lukepaper.sendmsg
permissions:
  lukepaper.sendmsg:
    description: 서버 간 메시지 전송 권한
    default: op
```

## 서버 간 메시징 예제

### MessageHandler 클래스

```java
package com.example.lukepaper;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class MessageHandler {
    private final JavaPlugin plugin;
    
    public MessageHandler(JavaPlugin plugin) {
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
    public void handleMessage(Player player, byte[] data) {
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
            
            // 이벤트 발행
            plugin.getServer().getPluginManager().callEvent(new CommandReceivedEvent(command));
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
}
```

### SendMessageCommand 클래스

```java
package com.example.lukepaper;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SendMessageCommand implements CommandExecutor {
    private final LukePaperPlugin plugin;
    
    public SendMessageCommand(LukePaperPlugin plugin) {
        this.plugin = plugin;
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
            plugin.getMessageHandler().sendMessage(targetServer, message);
        } else {
            // 플레이어가 실행한 경우
            Player player = (Player) sender;
            plugin.getMessageHandler().sendMessage(targetServer, message, player);
        }
        
        sender.sendMessage("§a메시지가 " + targetServer + " 서버로 전송되었습니다: " + message);
        return true;
    }
}
```

### ServerMessageEvent 클래스

```java
package com.example.lukepaper;

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

### CommandReceivedEvent 클래스

```java
package com.example.lukepaper;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class CommandReceivedEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final String command;
    
    public CommandReceivedEvent(String command) {
        this.command = command;
    }
    
    public String getCommand() {
        return command;
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

## 서버 상태 응답 예제

### StatusHandler 클래스

```java
package com.example.lukepaper;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class StatusHandler {
    private final JavaPlugin plugin;
    
    public StatusHandler(JavaPlugin plugin) {
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
                
                // 기본 Pong 또는 확장 Pong 중 선택
                if (plugin.getConfig().getBoolean("status.extended-pong", false)) {
                    sendExtendedPongResponse(player);
                } else {
                    sendPongResponse(player);
                }
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
     * 확장된 Pong 응답을 전송합니다.
     * 서버 상태에 대한 추가 정보를 포함합니다.
     */
    private void sendExtendedPongResponse(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            
            // 서버 정보 수집
            int playerCount = plugin.getServer().getOnlinePlayers().size();
            double tps = getServerTPS();
            long freeMemory = Runtime.getRuntime().freeMemory() / (1024 * 1024); // MB 단위
            
            // 포맷: "pong:playerCount:tps:freeMemory"
            String serverInfo = String.format("pong:%d:%.2f:%d", playerCount, tps, freeMemory);
            
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
     */
    private double getServerTPS() {
        try {
            // Paper API를 사용하여 TPS 획득
            return plugin.getServer().getTPS()[0];
        } catch (Exception e) {
            return 20.0; // 접근할 수 없는 경우 기본값 반환
        }
    }
}
```

### config.yml

```yaml
# LukePaper 플러그인 설정

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
  extended-pong: true
```

## 이벤트 처리 예제

### MessageListener 클래스

```java
package com.example.lukepaper;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MessageListener implements Listener {
    private final LukePaperPlugin plugin;
    
    public MessageListener(LukePaperPlugin plugin) {
        this.plugin = plugin;
        
        // 리스너 등록
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    @EventHandler
    public void onServerMessage(ServerMessageEvent event) {
        String message = event.getMessage();
        plugin.getLogger().info("메시지 이벤트 수신: " + message);
        
        // 예시: 공지사항 메시지 처리
        if (message.startsWith("ANNOUNCE:")) {
            String announcement = message.substring(9); // "ANNOUNCE:" 제거
            Bukkit.broadcastMessage("§b[공지] §f" + announcement);
        }
        
        // 예시: 특정 서버에게만 응답 메시지 전송
        if (message.startsWith("REQUEST:")) {
            String request = message.substring(8); // "REQUEST:" 제거
            handleRequest(request);
        }
    }
    
    @EventHandler
    public void onCommandReceived(CommandReceivedEvent event) {
        String command = event.getCommand();
        plugin.getLogger().info("명령어 이벤트 수신: " + command);
        
        // 특정 명령어에 대한 추가 처리 로직
        if (command.startsWith("stats")) {
            // 통계 정보 수집 및 처리
            collectAndSendStats();
        }
    }
    
    /**
     * 요청에 대한 처리를 수행합니다.
     */
    private void handleRequest(String request) {
        switch (request) {
            case "PLAYER_COUNT":
                int playerCount = plugin.getServer().getOnlinePlayers().size();
                plugin.getMessageHandler().sendMessage("proxy", "RESPONSE:PLAYER_COUNT:" + playerCount);
                break;
                
            case "SERVER_INFO":
                String serverInfo = collectServerInfo();
                plugin.getMessageHandler().sendMessage("proxy", "RESPONSE:SERVER_INFO:" + serverInfo);
                break;
                
            default:
                plugin.getLogger().warning("알 수 없는 요청: " + request);
                break;
        }
    }
    
    /**
     * 서버 정보를 수집합니다.
     */
    private String collectServerInfo() {
        int playerCount = plugin.getServer().getOnlinePlayers().size();
        double tps = plugin.getServer().getTPS()[0];
        long uptime = System.currentTimeMillis() - plugin.getServer().getStartTime();
        
        return String.format("players=%d;tps=%.2f;uptime=%d", playerCount, tps, uptime);
    }
    
    /**
     * 통계 정보를 수집하고 전송합니다.
     */
    private void collectAndSendStats() {
        // 통계 수집 로직 구현
        StringBuilder stats = new StringBuilder();
        stats.append("players=").append(plugin.getServer().getOnlinePlayers().size()).append(",");
        stats.append("tps=").append(String.format("%.2f", plugin.getServer().getTPS()[0])).append(",");
        stats.append("memory=").append(Runtime.getRuntime().freeMemory() / (1024 * 1024)).append("MB");
        
        // 통계 전송
        plugin.getMessageHandler().sendMessage("proxy", "STATS:" + stats.toString());
    }
}
```

## 고급 예제

### 서버 상태 이벤트 활용 예시

```java
package com.example.lukepaper;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * 서버 상태 관련 이벤트를 처리하는 리스너
 */
public class ServerStatusListener implements Listener {
    private final LukePaperPlugin plugin;
    private boolean serverReady = false;
    
    public ServerStatusListener(LukePaperPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * 서버가 완전히 시작된 후 한 번 호출되는 메서드
     */
    public void onServerReady() {
        if (serverReady) return;
        serverReady = true;
        
        plugin.getLogger().info("서버가 준비되었습니다. 상태 정보를 프록시에 알립니다.");
        
        // 서버 준비 메시지 전송
        plugin.getMessageHandler().sendMessage("proxy", "STATUS:READY");
        
        // 서버 정보 전송
        sendServerInfo();
    }
    
    /**
     * 서버 정보를 프록시에 전송
     */
    private void sendServerInfo() {
        String serverType = plugin.getConfig().getString("server-type", "unknown");
        int maxPlayers = plugin.getServer().getMaxPlayers();
        
        StringBuilder info = new StringBuilder();
        info.append("SERVER_INFO:");
        info.append("type=").append(serverType).append(";");
        info.append("max_players=").append(maxPlayers).append(";");
        info.append("version=").append(plugin.getServer().getBukkitVersion());
        
        plugin.getMessageHandler().sendMessage("proxy", info.toString());
    }
    
    /**
     * 서버 시작 후 10초 후에 서버 준비 상태 알림
     */
    public void scheduleReadyCheck() {
        plugin.getServer().getScheduler().runTaskLater(plugin, this::onServerReady, 200L); // 10초 (20 tick/초)
    }
}
```

### 주기적인 통계 보고 태스크

```java
package com.example.lukepaper;

import org.bukkit.scheduler.BukkitRunnable;

/**
 * 주기적으로 서버 통계를 수집하고 프록시에 보고하는 태스크
 */
public class StatsReportTask extends BukkitRunnable {
    private final LukePaperPlugin plugin;
    private final int reportInterval;
    
    public StatsReportTask(LukePaperPlugin plugin) {
        this.plugin = plugin;
        this.reportInterval = plugin.getConfig().getInt("stats.report-interval", 60);
    }
    
    /**
     * 태스크를 시작합니다.
     */
    public void start() {
        // reportInterval초마다 실행 (tick = 초 * 20)
        this.runTaskTimer(plugin, 20 * 10, 20L * reportInterval);
        plugin.getLogger().info("통계 보고 태스크가 시작되었습니다. 간격: " + reportInterval + "초");
    }
    
    @Override
    public void run() {
        // 서버 통계 수집
        collectAndSendStats();
    }
    
    /**
     * 서버 통계를 수집하고 전송합니다.
     */
    private void collectAndSendStats() {
        // 기본 통계 정보
        int playerCount = plugin.getServer().getOnlinePlayers().size();
        double[] tps = plugin.getServer().getTPS();
        long usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
        
        // JSON 형식으로 통계 정보 구성
        String stats = String.format(
            "{\"players\":%d,\"tps\":%.2f,\"memory\":%d,\"uptime\":%d}",
            playerCount, tps[0], usedMemory,
            (System.currentTimeMillis() - plugin.getServer().getStartTime()) / 1000
        );
        
        // 통계 전송
        plugin.getMessageHandler().sendMessage("proxy", "STATS:" + stats);
        
        if (plugin.getConfig().getBoolean("debug-mode", false)) {
            plugin.getLogger().info("통계 정보가 프록시로 전송되었습니다: " + stats);
        }
    }
}
```

### 명령어 시스템 확장

```java
package com.example.lukepaper;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 확장된 명령어 시스템
 */
public class CommandManager {
    private final LukePaperPlugin plugin;
    
    public CommandManager(LukePaperPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 다른 서버에 플레이어 이동 명령 전송
     */
    public void sendPlayerTeleport(String targetServer, Player player, String targetPlayer) {
        // 명령어 구성: TELEPORT:playerName:x:y:z:world
        String command = String.format(
            "TELEPORT:%s:%s",
            player.getName(),
            targetPlayer
        );
        
        plugin.getMessageHandler().sendMessage(targetServer, command);
        player.sendMessage("§a" + targetPlayer + " 플레이어에게 텔레포트 요청을 보냈습니다.");
    }
    
    /**
     * 플레이어 정보 요청
     */
    public void requestPlayerInfo(String targetServer, String playerName, CommandSender requester) {
        String command = "PLAYER_INFO:" + playerName;
        
        plugin.getMessageHandler().sendMessage(targetServer, command);
        requester.sendMessage("§a" + playerName + "의 정보를 " + targetServer + " 서버에 요청했습니다.");
        
        // 응답 처리를 위한 콜백 등록 (고급 구현에서는 콜백 시스템 사용)
    }
    
    /**
     * 전체 서버에 채팅 메시지 브로드캐스트
     */
    public void broadcastChat(Player player, String message) {
        // 메시지 형식: CHAT:playerName:message
        String command = String.format(
            "CHAT:%s:%s",
            player.getName(),
            message
        );
        
        plugin.getMessageHandler().broadcast(command);
        player.sendMessage("§a메시지가 모든 서버에 브로드캐스트 되었습니다.");
    }
    
    /**
     * 특정 서버에 명령어 실행
     */
    public void executeRemoteCommand(String targetServer, String command, CommandSender sender) {
        plugin.getMessageHandler().sendCommand(targetServer, command);
        sender.sendMessage("§a" + targetServer + " 서버에 명령어를 전송했습니다: " + command);
    }
}
```

이 문서에서 제공하는 예제 코드를 기반으로 LukeVanilla-Velocity 시스템과 호환되는 Paper 플러그인을 구현할 수 있습니다. 각 예제는 구체적인 구현 방법을 보여주며, 필요에 따라 수정하여 사용할 수 있습니다. 