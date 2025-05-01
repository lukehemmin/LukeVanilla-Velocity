package com.lukehemmin.lukeVanillaVelocity.commands

import com.lukehemmin.lukeVanillaVelocity.LukeVanillaVelocity
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.command.SimpleCommand
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

/**
 * 메시지 전송 명령어 클래스
 * 
 * 관리자가 명령어를 통해 특정 서버로 메시지를 전송할 수 있도록 하는 기능을 제공합니다.
 * 사용법: /sendmessage <서버명> <메시지>
 * 
 * @property plugin 메인 플러그인 인스턴스
 */
class MessageCommand(private val plugin: LukeVanillaVelocity) : SimpleCommand {
    
    /**
     * 명령어 실행 메서드
     * 
     * @param invocation 명령어 실행 컨텍스트
     */
    override fun execute(invocation: SimpleCommand.Invocation) {
        val source = invocation.source()
        val args = invocation.arguments()
        
        // 인자 수 확인
        if (args.size < 2) {
            source.sendMessage(Component.text("사용법: /sendmessage <서버명> <메시지>").color(NamedTextColor.RED))
            return
        }
        
        val targetServer = args[0]
        val message = args.copyOfRange(1, args.size).joinToString(" ")
        
        // 서버 존재 여부 확인
        if (plugin.getServerByName(targetServer) == null) {
            source.sendMessage(Component.text("서버를 찾을 수 없습니다: $targetServer").color(NamedTextColor.RED))
            return
        }
        
        // 메시지 전송
        val result = plugin.sendProxyMessageToServer(targetServer, message)
        if (result) {
            source.sendMessage(Component.text("'$targetServer' 서버로 메시지를 전송했습니다: $message").color(NamedTextColor.GREEN))
        } else {
            source.sendMessage(Component.text("메시지 전송에 실패했습니다.").color(NamedTextColor.RED))
        }
    }
    
    /**
     * 명령어 권한 확인 메서드
     * 
     * @param source 명령어 소스
     * @return 명령어 실행 권한 여부
     */
    override fun hasPermission(source: CommandSource): Boolean {
        return source.hasPermission("lukevanilla.command.sendmessage")
    }
    
    /**
     * 명령어 자동 완성 메서드
     * 
     * @param invocation 자동 완성 컨텍스트
     * @return 자동 완성 제안 목록
     */
    override fun suggest(invocation: SimpleCommand.Invocation): List<String> {
        val args = invocation.arguments()
        
        // 첫 번째 인자(서버명) 자동 완성
        if (args.size <= 1) {
            return plugin.getProxyServer().allServers
                .map { it.serverInfo.name }
                .filter { it.startsWith(args.getOrElse(0) { "" }, ignoreCase = true) }
        }
        
        // 그 외의 경우 빈 목록 반환
        return emptyList()
    }
} 