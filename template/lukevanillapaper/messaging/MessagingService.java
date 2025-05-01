package com.lukehemmin.lukevanillapaper.messaging;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.lukehemmin.lukevanillapaper.LukeVanillaPaper;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.nio.charset.StandardCharsets;

/**
 * Paper 서버 메시징 서비스
 * 
 * Velocity 프록시와의 통신을 처리하며, 다음 기능을 제공합니다:
 * - 다른 서버로 메시지 전송 (서버 ⇄ 프록시 ⇄ 서버 통신)
 * - 프록시로부터 메시지 수신 및 처리 (프록시 ⇄ 서버 통신)
 */
public class MessagingService implements PluginMessageListener {
    
    // 메시징 채널 상수
    public static final String MESSAGING_CHANNEL = "custom:msg";
    
    private final LukeVanillaPaper plugin;
    
    /**
     * 생성자
     * 
     * @param plugin 메인 플러그인 인스턴스
     */
    public MessagingService(LukeVanillaPaper plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 메시징 채널을 등록합니다.
     */
    public void registerChannels() {
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, MESSAGING_CHANNEL);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, MESSAGING_CHANNEL, this);
        plugin.getLogger().info("메시징 채널 등록 완료: " + MESSAGING_CHANNEL);
    }
    
    /**
     * 메시징 채널을 해제합니다.
     */
    public void unregisterChannels() {
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, MESSAGING_CHANNEL);
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, MESSAGING_CHANNEL, this);
        plugin.getLogger().info("메시징 채널 해제 완료: " + MESSAGING_CHANNEL);
    }
    
    /**
     * 다른 서버로 메시지를 전송합니다.
     * 
     * @param targetServer 대상 서버 이름
     * @param data 전송할 데이터
     * @param player 전송에 사용할 플레이어 (연결된 플레이어가 필요함)
     * @return 전송 성공 여부
     */
    public boolean sendToServer(String targetServer, byte[] data, Player player) {
        if (player == null || !player.isOnline()) {
            plugin.getLogger().warning("메시지 전송 실패: 플레이어가 연결되어 있지 않습니다.");
            return false;
        }
        
        try {
            // 메시지 포맷: [대상서버명 길이][대상서버명][원본 데이터]
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            byte[] targetServerBytes = targetServer.getBytes(StandardCharsets.UTF_8);
            
            // 대상 서버 이름 길이와 내용 작성
            out.writeInt(targetServerBytes.length);
            out.write(targetServerBytes);
            
            // 전송할 데이터 추가
            out.write(data);
            
            // 메시지 전송
            player.sendPluginMessage(plugin, MESSAGING_CHANNEL, out.toByteArray());
            plugin.getLogger().info("'" + targetServer + "' 서버로 메시지 전송 완료 (" + data.length + " bytes)");
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("메시지 전송 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 다른 서버로 문자열 메시지를 전송합니다.
     * 
     * @param targetServer 대상 서버 이름
     * @param message 전송할 메시지
     * @param player 전송에 사용할 플레이어
     * @return 전송 성공 여부
     */
    public boolean sendMessageToServer(String targetServer, String message, Player player) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
        
        out.writeInt(messageBytes.length);
        out.write(messageBytes);
        
        return sendToServer(targetServer, out.toByteArray(), player);
    }
    
    /**
     * 프록시로부터 메시지를 수신합니다.
     * 
     * @param channel 수신 채널
     * @param player 관련 플레이어
     * @param message 수신된 메시지 데이터
     */
    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(MESSAGING_CHANNEL)) {
            return;
        }
        
        try {
            ByteArrayDataInput in = ByteStreams.newDataInput(message);
            
            // 메시지 길이 읽기
            int messageLength = in.readInt();
            if (messageLength <= 0 || messageLength > message.length - 4) {
                plugin.getLogger().warning("잘못된 메시지 길이: " + messageLength);
                return;
            }
            
            // 메시지 내용 읽기
            byte[] messageBytes = new byte[messageLength];
            in.readFully(messageBytes);
            
            String receivedMessage = new String(messageBytes, StandardCharsets.UTF_8);
            plugin.getLogger().info("프록시로부터 메시지 수신: " + receivedMessage);
            
            // 여기에서 메시지 처리 로직을 구현
            // 예: 이벤트 발생, 알림 표시 등
            plugin.getServer().getPluginManager().callEvent(
                new MessageReceivedEvent(plugin, receivedMessage)
            );
        } catch (Exception e) {
            plugin.getLogger().severe("메시지 처리 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 