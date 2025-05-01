package com.lukehemmin.lukeVanillaVelocity

import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier
import com.velocitypowered.api.proxy.server.RegisteredServer
import com.velocitypowered.api.scheduler.ScheduledTask
import io.netty.buffer.Unpooled
import org.slf4j.Logger
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 서버 상태를 관리하고 서버 상태에 따라 플레이어를 적절한 서버로 라우팅하는 클래스
 *
 * @property plugin 메인 플러그인 인스턴스
 * @property server Velocity 프록시 서버 인스턴스
 * @property logger 로깅을 위한 로거 인스턴스
 */
class ServerStatusManager(
    private val plugin: LukeVanillaVelocity,
    private val server: ProxyServer,
    private val logger: Logger
) {
    // 서버 상태 확인을 위한 채널 식별자
    private val statusChannel = MinecraftChannelIdentifier.create("custom", "status")
    
    // 서버 상태 저장 맵 (서버명 -> 상태)
    private val serverStatus = ConcurrentHashMap<String, Boolean>()
    
    // 서버 응답 시간 저장 맵 (서버명 -> 마지막 응답 시간)
    private val serverLastResponse = ConcurrentHashMap<String, Long>()
    
    // 서버 ping 간격 (밀리초)
    private val pingInterval = 30000L
    
    // 서버 응답 타임아웃 (밀리초)
    private val responseTimeout = 5000L
    
    // 상태 확인 태스크
    private var statusCheckTask: ScheduledTask? = null
    
    // 메인 서버(survival) 이름
    private val mainServerName = "survival"
    
    // 대체 서버(lobby) 이름
    private val fallbackServerName = "lobby"
    
    /**
     * 서버 상태 관리자를 초기화합니다.
     */
    fun initialize() {
        // 상태 확인 채널 등록
        server.channelRegistrar.register(statusChannel)
        
        // 주기적인 서버 상태 확인 태스크 시작
        startStatusCheckTask()
        
        // 초기 상태 설정
        serverStatus[mainServerName] = false
        serverStatus[fallbackServerName] = true
        
        logger.info("서버 상태 관리자가 초기화되었습니다.")
    }
    
    /**
     * 주기적으로 서버 상태를 확인하는 태스크를 시작합니다.
     */
    private fun startStatusCheckTask() {
        statusCheckTask = server.scheduler.buildTask(plugin) {
            checkServerStatus()
        }
        .repeat(Duration.ofMillis(pingInterval))
        .schedule()
        
        logger.info("서버 상태 확인 태스크가 시작되었습니다 (간격: ${pingInterval}ms)")
    }
    
    /**
     * 서버 상태를 확인합니다.
     */
    fun checkServerStatus() {
        val mainServer = server.getServer(mainServerName).orElse(null)
        
        if (mainServer == null) {
            logger.warn("메인 서버($mainServerName)를 찾을 수 없습니다.")
            return
        }
        
        try {
            sendPingToServer(mainServer)
            
            // 타임아웃 확인
            val currentTime = System.currentTimeMillis()
            val lastResponse = serverLastResponse.getOrDefault(mainServerName, 0L)
            
            // 응답 시간이 타임아웃보다 오래되었으면 오프라인으로 간주
            val isOnline = currentTime - lastResponse < responseTimeout
            updateServerStatus(mainServerName, isOnline)
            
            logger.debug("$mainServerName 서버 상태 확인: ${if (isOnline) "온라인" else "오프라인"}")
            
            // 서버 상태가 변경되었고 온라인 상태라면 로비에 있는 플레이어들을 메인 서버로 이동
            if (isOnline && !serverStatus.getOrDefault(mainServerName, false)) {
                movePlayersToMainServer()
            }
        } catch (e: Exception) {
            logger.error("서버 상태 확인 중 오류 발생: ${e.message}", e)
        }
    }
    
    /**
     * 서버 상태를 업데이트합니다.
     * 
     * @param serverName 서버 이름
     * @param isOnline 서버 온라인 상태
     */
    fun updateServerStatus(serverName: String, isOnline: Boolean) {
        val previousStatus = serverStatus.getOrDefault(serverName, false)
        
        // 상태가 변경된 경우에만 로깅
        if (previousStatus != isOnline) {
            logger.info("$serverName 서버 상태 변경됨: ${if (isOnline) "온라인" else "오프라인"}")
            serverStatus[serverName] = isOnline
        }
    }
    
    /**
     * 서버로 ping 메시지를 전송합니다.
     * 
     * @param server 대상 서버
     */
    private fun sendPingToServer(server: RegisteredServer) {
        try {
            // ping 메시지 생성
            val buffer = Unpooled.buffer()
            val pingMessage = "ping"
            val pingBytes = pingMessage.toByteArray(StandardCharsets.UTF_8)
            
            buffer.writeInt(pingBytes.size)
            buffer.writeBytes(pingBytes)
            
            // 서버로 전송
            server.sendPluginMessage(statusChannel, buffer.array())
            logger.debug("${server.serverInfo.name} 서버로 ping 메시지 전송됨")
        } catch (e: Exception) {
            logger.warn("${server.serverInfo.name} 서버로 ping 메시지 전송 실패: ${e.message}")
        }
    }
    
    /**
     * 서버에서 pong 응답을 받았을 때 호출됩니다.
     * 
     * @param serverName 응답을 보낸 서버 이름
     */
    fun receivePong(serverName: String) {
        // 현재 시간을 마지막 응답 시간으로 설정
        serverLastResponse[serverName] = System.currentTimeMillis()
        updateServerStatus(serverName, true)
        
        logger.debug("$serverName 서버에서 pong 응답 수신됨")
    }
    
    /**
     * 플레이어를 적절한 서버로 연결합니다.
     * 메인 서버가 온라인이면 그쪽으로, 아니면 대체 서버로 연결합니다.
     * 
     * @param player 연결할 플레이어
     */
    fun connectToAppropriateServer(player: Player) {
        val isMainServerOnline = serverStatus.getOrDefault(mainServerName, false)
        val targetServerName = if (isMainServerOnline) mainServerName else fallbackServerName
        
        server.getServer(targetServerName).ifPresent { targetServer ->
            player.createConnectionRequest(targetServer).fireAndForget()
            logger.info("플레이어 ${player.username}(을)를 $targetServerName 서버로 연결합니다.")
        }
    }
    
    /**
     * 로비에 있는 모든 플레이어를 메인 서버로 이동시킵니다.
     * 메인 서버가 온라인인 경우에만 실행됩니다.
     */
    private fun movePlayersToMainServer() {
        val isMainServerOnline = serverStatus.getOrDefault(mainServerName, true)
        
        if (!isMainServerOnline) {
            logger.warn("메인 서버가 오프라인이므로 플레이어 이동을 취소합니다.")
            return
        }
        
        server.getServer(fallbackServerName).ifPresent { lobbyServer ->
            server.getServer(mainServerName).ifPresent { mainServer ->
                // 로비에 있는 모든 플레이어 가져오기
                val playersInLobby = lobbyServer.playersConnected
                
                if (playersInLobby.isEmpty()) {
                    logger.debug("로비에 연결된 플레이어가 없습니다.")
                    return
                }
                
                logger.info("메인 서버가 온라인이 되어 로비의 플레이어 ${playersInLobby.size}명을 이동시킵니다.")
                
                // 각 플레이어를 메인 서버로 이동
                for (player in playersInLobby) {
                    player.createConnectionRequest(mainServer).fireAndForget()
                    logger.info("플레이어 ${player.username}(을)를 $mainServerName 서버로 이동시킵니다.")
                }
            }
        }
    }
    
    /**
     * 서버 상태 관리자를 정리합니다.
     */
    fun shutdown() {
        // 상태 확인 태스크 취소
        statusCheckTask?.cancel()
        
        // 채널 등록 해제
        try {
            server.channelRegistrar.unregister(statusChannel)
        } catch (e: Exception) {
            logger.error("채널 등록 해제 중 오류 발생: ${e.message}", e)
        }
        
        logger.info("서버 상태 관리자가 종료되었습니다.")
    }
} 