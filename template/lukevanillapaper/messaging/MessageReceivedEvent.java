package com.lukehemmin.lukevanillapaper.messaging;

import com.lukehemmin.lukevanillapaper.LukeVanillaPaper;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * 메시지 수신 이벤트 클래스
 * 
 * 프록시로부터 메시지가 수신되었을 때 발생하는 이벤트입니다.
 * 다른 플러그인에서 이 이벤트를 리스닝하여 메시지에 대응할 수 있습니다.
 */
public class MessageReceivedEvent extends Event {
    
    private static final HandlerList HANDLERS = new HandlerList();
    private final LukeVanillaPaper plugin;
    private final String message;
    
    /**
     * 생성자
     * 
     * @param plugin 메인 플러그인 인스턴스
     * @param message 수신된 메시지
     */
    public MessageReceivedEvent(LukeVanillaPaper plugin, String message) {
        this.plugin = plugin;
        this.message = message;
    }
    
    /**
     * 플러그인 인스턴스를 반환합니다.
     * 
     * @return 플러그인 인스턴스
     */
    public LukeVanillaPaper getPlugin() {
        return plugin;
    }
    
    /**
     * 수신된 메시지를 반환합니다.
     * 
     * @return 수신된 메시지
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * HandlerList를 반환합니다.
     * 
     * @return 핸들러 목록
     */
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    
    /**
     * 정적 HandlerList를 반환합니다.
     * 
     * @return 정적 핸들러 목록
     */
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
} 