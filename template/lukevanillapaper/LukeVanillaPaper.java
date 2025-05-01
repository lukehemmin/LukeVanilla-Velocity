package com.lukehemmin.lukevanillapaper;

import com.lukehemmin.lukevanillapaper.messaging.MessagingService;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * LukeVanilla-Paper 메인 클래스
 * 
 * Paper 서버에서 실행되는 플러그인으로, Velocity 프록시와의 메시징을 처리합니다.
 * 서버 간 메시지 전송 및 프록시로부터의 메시지 수신 기능을 제공합니다.
 */
public class LukeVanillaPaper extends JavaPlugin {
    
    private MessagingService messagingService;
    
    @Override
    public void onEnable() {
        // 설정 파일 저장
        saveDefaultConfig();
        
        // 메시징 서비스 초기화
        messagingService = new MessagingService(this);
        messagingService.registerChannels();
        
        // 명령어 등록
        getCommand("sendmessage").setExecutor(new SendMessageCommand(this));
        
        getLogger().info("LukeVanilla-Paper 메시징 시스템이 초기화되었습니다.");
        getLogger().info("서버 이름: " + getServerName());
    }
    
    @Override
    public void onDisable() {
        // 메시징 서비스 종료
        if (messagingService != null) {
            messagingService.unregisterChannels();
        }
        
        getLogger().info("LukeVanilla-Paper 플러그인이 비활성화되었습니다.");
    }
    
    /**
     * 메시징 서비스 인스턴스를 반환합니다.
     * 
     * @return MessagingService 인스턴스
     */
    public MessagingService getMessagingService() {
        return messagingService;
    }
    
    /**
     * 설정 파일에서 서버 이름을 가져옵니다.
     * 
     * @return 서버 이름
     */
    public String getServerName() {
        return getConfig().getString("server-name", "unknown");
    }
} 