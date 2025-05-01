package com.lukehemmin.lukeVanillaVelocity

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PluginMessageEvent
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier
import com.velocitypowered.api.proxy.server.RegisteredServer
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import java.nio.charset.StandardCharsets

/**
 * 서버에서 프록시로 보내는 메시지를 처리하는 리스너 클래스
 *
 * 서버 간 메시징을 위해 서버에서 프록시로 전송된 메시지를 처리하고,
 * 필요한 경우 해당 메시지를 목적지 서버로 라우팅합니다.
 *
 * @property plugin 메인 플러그인 인스턴스
 */
class ServerToProxyMessageListener(private val plugin: LukeVanillaVelocity) {
    
    // 메시징에 사용되는 채널 식별자
    private val messagingChannel = MinecraftChannelIdentifier.create("custom", "msg")
    
    /**
     * 플러그인 메시지 이벤트 핸들러
     * 서버에서 프록시로 전송된 메시지를 처리합니다.
     * 
     * @param event 플러그인 메시지 이벤트
     */
    @Subscribe
    fun onPluginMessage(event: PluginMessageEvent) {
        // 메시지가 우리의 채널에서 온 것인지 확인
        if (event.identifier != messagingChannel) {
            return
        }
        
        // 이미 처리된 메시지는 무시
        if (event.result.isAllowed) {
            event.setResult(PluginMessageEvent.ForwardResult.handled())
            
            try {
                // 메시지 처리 및 라우팅
                handleMessage(event)
            } catch (e: Exception) {
                plugin.getLogger().error("메시지 처리 중 오류 발생", e)
            }
        }
    }
    
    /**
     * 수신된 메시지를 처리하고 필요한 경우 다른 서버로 라우팅합니다.
     * 메시지 형식: [목적지서버명(byte[])][메시지길이(int)][메시지내용(byte[])]
     * 
     * @param event 플러그인 메시지 이벤트
     */
    private fun handleMessage(event: PluginMessageEvent) {
        val data = event.data
        val buf = Unpooled.wrappedBuffer(data)
        
        try {
            // 목적지 서버 이름 읽기
            val targetServerNameLength = buf.readInt()
            if (targetServerNameLength <= 0 || targetServerNameLength > 32) {
                plugin.getLogger().warn("잘못된 대상 서버 이름 길이: $targetServerNameLength")
                return
            }
            
            val targetServerNameBytes = ByteArray(targetServerNameLength)
            buf.readBytes(targetServerNameBytes)
            val targetServerName = String(targetServerNameBytes, StandardCharsets.UTF_8)
            
            // 메시지 내용 읽기 
            val remainingBytes = ByteArray(buf.readableBytes())
            buf.readBytes(remainingBytes)
            
            plugin.getLogger().info("서버 간 메시지 수신됨: 대상 서버 = $targetServerName, 데이터 크기 = ${remainingBytes.size} bytes")
            
            // 메시지를 대상 서버로 라우팅
            val result = plugin.sendMessageToServer(targetServerName, remainingBytes, event.source)
            if (!result) {
                plugin.getLogger().warn("메시지를 대상 서버($targetServerName)로 라우팅할 수 없습니다.")
            }
        } finally {
            buf.release()
        }
    }
} 