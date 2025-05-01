// 명령어 등록 부분
private fun registerCommands() {
    // ... existing code ...
    
    // SendPlayer 명령어 등록
    val sendPlayerCommand = SendPlayerCommand(server, this)
    commandManager.register(
        commandManager.metaBuilder("sendplayer")
            .plugin(this)
            .build(),
        sendPlayerCommand
    )
    
    logger.info("명령어가 등록되었습니다.")
} 