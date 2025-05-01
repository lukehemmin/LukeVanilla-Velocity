package com.lukehemmin.lukeVanillaVelocity.commands

import com.google.inject.Inject
import com.lukehemmin.lukeVanillaVelocity.LukeVanillaVelocity
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.command.SimpleCommand
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

/**
 * 특정 플레이어를 원하는 서버로 이동시키는 명령어
 */
class SendPlayerCommand @Inject constructor(
    private val server: ProxyServer,
    private val plugin: LukeVanillaVelocity
) : SimpleCommand {

    override fun execute(invocation: SimpleCommand.Invocation) {
        val source = invocation.source()
        val args = invocation.arguments()

        // 인수 검증
        if (args.size < 2) {
            source.sendMessage(Component.text("사용법: /sendplayer <플레이어> <서버>").color(NamedTextColor.RED))
            return
        }

        val playerName = args[0]
        val targetServerName = args[1]

        // 서버 존재 여부 확인
        val targetServer = server.getServer(targetServerName)
        if (targetServer.isEmpty) {
            source.sendMessage(Component.text("서버 '$targetServerName'를 찾을 수 없습니다.").color(NamedTextColor.RED))
            return
        }

        // 플레이어 존재 여부 확인
        val player = server.getPlayer(playerName)
        if (player.isEmpty) {
            source.sendMessage(Component.text("플레이어 '$playerName'를 찾을 수 없습니다.").color(NamedTextColor.RED))
            return
        }

        // 서버 이동 시도
        val targetServerInstance = targetServer.get()
        player.get().createConnectionRequest(targetServerInstance).connect()
            .thenAccept { result ->
                if (result.isSuccessful) {
                    // 성공 메시지
                    source.sendMessage(
                        Component.text("플레이어 '${playerName}'를 서버 '${targetServerName}'로 이동시켰습니다.")
                            .color(NamedTextColor.GREEN)
                    )
                    
                    // 플레이어에게 알림
                    player.get().sendMessage(
                        Component.text("관리자에 의해 서버 '${targetServerName}'로 이동되었습니다.")
                            .color(NamedTextColor.YELLOW)
                    )
                } else {
                    // 실패 메시지
                    source.sendMessage(
                        Component.text("플레이어 '${playerName}'를 서버 '${targetServerName}'로 이동시키지 못했습니다: ${result.reasonPhrase}")
                            .color(NamedTextColor.RED)
                    )
                }
            }
    }

    override fun hasPermission(source: CommandSource, args: Array<String>): Boolean {
        return source.hasPermission("lukevanilla.command.sendplayer")
    }

    override fun suggest(invocation: SimpleCommand.Invocation): List<String> {
        val source = invocation.source()
        val args = invocation.arguments()

        // 첫 번째 인수: 플레이어 이름 추천
        if (args.size <= 1) {
            return server.allPlayers
                .map { it.username }
                .filter { it.startsWith(args.getOrElse(0) { "" }) }
        }

        // 두 번째 인수: 서버 이름 추천
        if (args.size == 2) {
            return server.allServers
                .map { it.serverInfo.name }
                .filter { it.startsWith(args[1]) }
        }

        return emptyList()
    }
} 