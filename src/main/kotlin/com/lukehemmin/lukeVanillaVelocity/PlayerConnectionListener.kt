package com.lukehemmin.lukeVanillaVelocity

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.ServerPreConnectEvent
import com.velocitypowered.api.proxy.Player

/**
 * 플레이어 연결 이벤트를 처리하는 리스너 클래스
 *
 * @property plugin 메인 플러그인 인스턴스
 * @property statusManager 서버 상태 관리자 인스턴스
 */
class PlayerConnectionListener(
    private val plugin: LukeVanillaVelocity,
    private val statusManager: ServerStatusManager
) {
    /**
     * 플레이어가 서버에 연결하기 전 이벤트 핸들러
     * 서버 상태에 따라 적절한 서버로 플레이어를 라우팅합니다.
     * 
     * @param event 서버 사전 연결 이벤트
     */
    @Subscribe
    fun onServerPreConnect(event: ServerPreConnectEvent) {
        val player = event.player
        val targetServer = event.originalServer
        
        // 첫 연결 시에만 자동 라우팅 적용
        if (player.currentServer.isEmpty()) {
            // 서버 상태에 따라 플레이어를 적절한 서버로 라우팅
            handleInitialConnection(event)
        }
    }
    
    /**
     * 플레이어의 초기 연결을 처리합니다.
     * 서버 상태에 따라 적절한 서버로 라우팅합니다.
     * 
     * @param event 서버 사전 연결 이벤트
     */
    private fun handleInitialConnection(event: ServerPreConnectEvent) {
        val player = event.player
        val logger = plugin.getLogger()
        
        logger.info("플레이어 ${player.username}의 초기 연결을 처리합니다.")
        
        // 상태 매니저를 통해 적절한 서버로 연결
        statusManager.connectToAppropriateServer(player)
    }
} 