package com.lukehemmin.lukeVanillaVelocity

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PluginMessageEvent
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier
import io.netty.buffer.Unpooled
import java.nio.charset.StandardCharsets

/**
 * 서버 상태 메시지(ping/pong)를 처리하는 리스너 클래스
 *
 * @property plugin 메인 플러그인 인스턴스
 * @property statusManager 서버 상태 관리자 인스턴스
 */
class ServerStatusMessageListener(
    private val plugin: LukeVanillaVelocity,
    private val statusManager: ServerStatusManager
) {
    // 상태 확인을 위한 채널 식별자
    private val statusChannel = MinecraftChannelIdentifier.create("custom", "status")
    
    /**
     * 플러그인 메시지 이벤트 핸들러
     * 서버에서 프록시로 전송된 상태 메시지를 처리합니다.
     * 
     * @param event 플러그인 메시지 이벤트
     */
    @Subscribe
    fun onPluginMessage(event: PluginMessageEvent) {
        // 메시지가 상태 채널에서 온 것인지 확인
        if (event.identifier != statusChannel) {
            return
        }
        
        // 이미 처리된 메시지는 무시
        if (event.result.isAllowed) {
            event.setResult(PluginMessageEvent.ForwardResult.handled())
            
            try {
                // 메시지 처리
                handleStatusMessage(event)
            } catch (e: Exception) {
                plugin.getLogger().error("상태 메시지 처리 중 오류 발생", e)
            }
        }
    }
    
    /**
     * 상태 메시지를 처리합니다.
     * 
     * @param event 플러그인 메시지 이벤트
     */
    private fun handleStatusMessage(event: PluginMessageEvent) {
        val data = event.data
        val buf = Unpooled.wrappedBuffer(data)
        
        try {
            // 메시지 내용 읽기
            val messageLength = buf.readInt()
            if (messageLength <= 0 || messageLength > 1024) {
                plugin.getLogger().warn("잘못된 상태 메시지 길이: $messageLength")
                return
            }
            
            val messageBytes = ByteArray(messageLength)
            buf.readBytes(messageBytes)
            val message = String(messageBytes, StandardCharsets.UTF_8)
            
            // 메시지가 "pong"인 경우 서버 상태 업데이트
            if (message == "pong") {
                // 메시지 소스가 서버인 경우에만 처리
                if (event.source is com.velocitypowered.api.proxy.server.ServerConnection) {
                    val serverConnection = event.source as com.velocitypowered.api.proxy.server.ServerConnection
                    val serverName = serverConnection.serverInfo.name
                    
                    // 서버 상태 관리자에 pong 응답 알림
                    statusManager.receivePong(serverName)
                }
            }
        } finally {
            buf.release()
        }
    }
} 