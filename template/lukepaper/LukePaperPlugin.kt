package com.lukehemmin.lukepaper

import io.netty.buffer.Unpooled
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.plugin.messaging.PluginMessageListener
import java.nio.charset.StandardCharsets

/**
 * LukePaper 플러그인 메인 클래스
 * 
 * Velocity 프록시와 통신하는 Paper 서버 플러그인입니다.
 * - 프록시로부터 메시지 수신
 * - 프록시로 메시지 전송
 * - 상태 확인(ping/pong) 메시지 처리
 */
class LukePaperPlugin : JavaPlugin(), PluginMessageListener {
    // 메시징 채널
    private val MESSAGING_CHANNEL = "custom:msg"
    // 상태 확인 채널
    private val STATUS_CHANNEL = "custom:status"
    
    override fun onEnable() {
        // 메시징 채널 등록
        server.messenger.registerIncomingPluginChannel(this, MESSAGING_CHANNEL, this)
        server.messenger.registerOutgoingPluginChannel(this, MESSAGING_CHANNEL)
        
        // 상태 확인 채널 등록
        server.messenger.registerIncomingPluginChannel(this, STATUS_CHANNEL, this)
        server.messenger.registerOutgoingPluginChannel(this, STATUS_CHANNEL)
        
        logger.info("LukePaper 플러그인이 활성화되었습니다.")
        logger.info("등록된 채널: $MESSAGING_CHANNEL, $STATUS_CHANNEL")
    }
    
    override fun onDisable() {
        // 채널 등록 해제
        server.messenger.unregisterIncomingPluginChannel(this)
        server.messenger.unregisterOutgoingPluginChannel(this)
        
        logger.info("LukePaper 플러그인이 비활성화되었습니다.")
    }
    
    /**
     * 플러그인 메시지 수신 핸들러
     */
    override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
        when (channel) {
            MESSAGING_CHANNEL -> handleMessagingChannel(message)
            STATUS_CHANNEL -> handleStatusChannel(message, player)
        }
    }
    
    /**
     * 메시징 채널 메시지 처리
     */
    private fun handleMessagingChannel(message: ByteArray) {
        val buffer = Unpooled.wrappedBuffer(message)
        
        try {
            // 메시지 길이 읽기
            val messageLength = buffer.readInt()
            
            // 메시지 내용 읽기
            val messageBytes = ByteArray(messageLength)
            buffer.readBytes(messageBytes)
            val messageContent = String(messageBytes, StandardCharsets.UTF_8)
            
            // 메시지 처리
            logger.info("프록시로부터 메시지 수신: $messageContent")
            
            // 여기에 메시지 처리 로직 추가
            // 예: 모든 플레이어에게 메시지 전달
            Bukkit.getOnlinePlayers().forEach { p ->
                p.sendMessage("§b[프록시] §f$messageContent")
            }
        } finally {
            buffer.release()
        }
    }
    
    /**
     * 상태 확인 채널 메시지 처리
     */
    private fun handleStatusChannel(message: ByteArray, player: Player) {
        val buffer = Unpooled.wrappedBuffer(message)
        
        try {
            // 메시지 길이 읽기
            val messageLength = buffer.readInt()
            
            // 메시지 내용 읽기
            val messageBytes = ByteArray(messageLength)
            buffer.readBytes(messageBytes)
            val messageContent = String(messageBytes, StandardCharsets.UTF_8)
            
            // ping 메시지인 경우 pong으로 응답
            if (messageContent == "ping") {
                logger.debug("프록시로부터 ping 수신, pong으로 응답합니다.")
                sendPongResponse(player)
            }
        } finally {
            buffer.release()
        }
    }
    
    /**
     * pong 응답을 프록시로 전송
     */
    private fun sendPongResponse(player: Player) {
        // pong 메시지 생성
        val pongMessage = "pong"
        val pongBytes = pongMessage.toByteArray(StandardCharsets.UTF_8)
        
        val buffer = Unpooled.buffer()
        buffer.writeInt(pongBytes.size)
        buffer.writeBytes(pongBytes)
        
        // 프록시로 전송
        player.sendPluginMessage(this, STATUS_CHANNEL, buffer.array())
    }
    
    /**
     * 다른 서버로 메시지 전송
     * 
     * @param targetServer 대상 서버 이름
     * @param message 전송할 메시지
     * @param player 메시지 전송에 사용할 플레이어
     */
    fun sendToServer(targetServer: String, message: String, player: Player?) {
        if (player == null || !player.isOnline) {
            logger.warn("메시지를 보낼 플레이어가 없거나 오프라인 상태입니다.")
            return
        }
        
        try {
            // 대상 서버 이름 인코딩
            val serverNameBytes = targetServer.toByteArray(StandardCharsets.UTF_8)
            
            // 메시지 인코딩
            val messageBytes = message.toByteArray(StandardCharsets.UTF_8)
            
            // 버퍼에 데이터 작성
            val buffer = Unpooled.buffer()
            buffer.writeInt(serverNameBytes.size)
            buffer.writeBytes(serverNameBytes)
            buffer.writeInt(messageBytes.size)
            buffer.writeBytes(messageBytes)
            
            // 프록시로 전송
            player.sendPluginMessage(this, MESSAGING_CHANNEL, buffer.array())
            logger.info("'$targetServer' 서버로 메시지 전송: $message")
        } catch (e: Exception) {
            logger.error("메시지 전송 중 오류 발생: ${e.message}", e)
        }
    }
} 