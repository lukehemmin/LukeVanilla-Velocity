package com.lukehemmin.lukeVanillaVelocity;

import com.google.inject.Inject
import com.lukehemmin.lukeVanillaVelocity.commands.MessageCommand
import com.velocitypowered.api.command.CommandManager
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.messages.ChannelIdentifier
import com.velocitypowered.api.proxy.messages.ChannelMessageSource
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier
import com.velocitypowered.api.proxy.server.RegisteredServer
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.slf4j.Logger
import java.nio.charset.StandardCharsets

/**
 * LukeVanilla-Velocity 메인 클래스
 * 
 * 서버 간 메시징 및 프록시-서버 간 통신을 처리하는 Velocity 플러그인입니다.
 * 구현 기능:
 * - 서버 ⇄ 프록시 ⇄ 서버 메시징 (서버 간 데이터 전송)
 * - 프록시 ⇄ 서버 메시징 (프록시에서 특정 서버로 메시지 전송)
 */
@Plugin(
    id = "lukevanilla-velocity", 
    name = "LukeVanilla-Velocity", 
    version = BuildConstants.VERSION,
    description = "Minecraft 서버 간 메시징을 위한 Velocity 플러그인",
    authors = ["LukeHemmin"]
)
class LukeVanillaVelocity @Inject constructor(
    private val server: ProxyServer,
    private val logger: Logger,
    private val commandManager: CommandManager
) {
    // 서버 간 메시징을 위한 채널 식별자
    private val messagingChannel: ChannelIdentifier = MinecraftChannelIdentifier.create("custom", "msg")
    
    /**
     * 프록시 초기화 이벤트 핸들러
     * 메시징 채널을 등록하고 리스너를 설정합니다.
     */
    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        // 메시징 채널 등록
        server.channelRegistrar.register(messagingChannel)
        
        // 서버로부터의 메시지 수신을 처리하는 리스너 등록
        server.eventManager.register(this, ServerToProxyMessageListener(this))
        
        // 명령어 등록
        registerCommands()
        
        logger.info("LukeVanilla-Velocity 메시징 시스템이 초기화되었습니다.")
        logger.info("등록된 채널: ${messagingChannel.id}")
    }
    
    /**
     * 명령어를 등록합니다.
     */
    private fun registerCommands() {
        // 메시지 전송 명령어 등록
        commandManager.register(
            "sendmessage", 
            MessageCommand(this),
            "smsg"
        )
        
        logger.info("명령어가 등록되었습니다: /sendmessage, /smsg")
    }
    
    /**
     * 대상 서버로 메시지를 전송합니다.
     * 
     * @param targetServer 대상 서버 이름
     * @param data 전송할 데이터
     * @param source 메시지 출처 (일반적으로 서버 인스턴스)
     * @return 전송 성공 여부
     */
    fun sendMessageToServer(targetServer: String, data: ByteArray, source: ChannelMessageSource): Boolean {
        // 대상 서버 찾기
        val server = getServerByName(targetServer) ?: run {
            logger.error("대상 서버를 찾을 수 없습니다: $targetServer")
            return false
        }
        
        try {
            // 서버로 메시지 전송
            server.sendPluginMessage(messagingChannel, data)
            logger.debug("'$targetServer' 서버로 메시지 전송 완료 (${data.size} bytes)")
            return true
        } catch (e: Exception) {
            logger.error("메시지 전송 중 오류 발생: ${e.message}", e)
            return false
        }
    }
    
    /**
     * 프록시에서 특정 서버로 메시지를 전송합니다.
     * 
     * @param targetServer 대상 서버 이름
     * @param message 전송할 메시지
     * @return 전송 성공 여부
     */
    fun sendProxyMessageToServer(targetServer: String, message: String): Boolean {
        val server = getServerByName(targetServer) ?: run {
            logger.error("대상 서버를 찾을 수 없습니다: $targetServer")
            return false
        }
        
        try {
            // 메시지 인코딩
            val msgBytes = message.toByteArray(StandardCharsets.UTF_8)
            val buffer = Unpooled.buffer()
            buffer.writeInt(msgBytes.size)
            buffer.writeBytes(msgBytes)
            
            // 서버로 메시지 전송
            server.sendPluginMessage(messagingChannel, buffer.array())
            logger.info("프록시에서 '$targetServer' 서버로 메시지 전송: $message")
            return true
        } catch (e: Exception) {
            logger.error("프록시 메시지 전송 중 오류 발생: ${e.message}", e)
            return false
        }
    }
    
    /**
     * 서버 이름으로 서버 인스턴스를 찾습니다.
     * 
     * @param serverName 서버 이름
     * @return 서버 인스턴스 또는 null
     */
    fun getServerByName(serverName: String): RegisteredServer? {
        return server.allServers.find { it.serverInfo.name.equals(serverName, ignoreCase = true) }
    }
    
    /**
     * 프록시 서버 인스턴스를 반환합니다.
     * 
     * @return ProxyServer 인스턴스
     */
    fun getProxyServer(): ProxyServer {
        return server
    }
    
    /**
     * 로거 인스턴스를 반환합니다.
     * 
     * @return Logger 인스턴스
     */
    fun getLogger(): Logger {
        return logger
    }
}
