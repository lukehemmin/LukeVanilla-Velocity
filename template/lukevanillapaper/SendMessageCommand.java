package com.lukehemmin.lukevanillapaper;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 메시지 전송 명령어 클래스
 * 
 * 서버에서 다른 서버로 메시지를 전송하는 명령어를 처리합니다.
 * 사용법: /sendmessage <대상서버> <메시지>
 */
public class SendMessageCommand implements CommandExecutor, TabCompleter {
    
    private final LukeVanillaPaper plugin;
    
    /**
     * 생성자
     * 
     * @param plugin 메인 플러그인 인스턴스
     */
    public SendMessageCommand(LukeVanillaPaper plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 명령어 실행 메서드
     * 
     * @param sender 명령어 실행자
     * @param command 실행된 명령어
     * @param label 명령어 레이블
     * @param args 명령어 인자
     * @return 명령어 처리 성공 여부
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 인자 수 확인
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "사용법: /sendmessage <대상서버> <메시지>");
            return false;
        }
        
        // 대상 서버와 메시지 추출
        String targetServer = args[0];
        StringBuilder messageBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            messageBuilder.append(args[i]).append(" ");
        }
        String message = messageBuilder.toString().trim();
        
        // 플레이어 확인 (Bungee/Velocity에 메시지를 보내려면 플레이어가 필요)
        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        } else {
            // 서버 콘솔에서 명령어를 실행한 경우 온라인 플레이어 중 하나를 사용
            if (Bukkit.getOnlinePlayers().isEmpty()) {
                sender.sendMessage(ChatColor.RED + "메시지 전송 실패: 온라인 플레이어가 없습니다.");
                return false;
            }
            player = Bukkit.getOnlinePlayers().iterator().next();
        }
        
        // 메시지 전송
        boolean success = plugin.getMessagingService().sendMessageToServer(targetServer, message, player);
        
        if (success) {
            sender.sendMessage(ChatColor.GREEN + "'" + targetServer + "' 서버로 메시지를 전송했습니다: " + message);
        } else {
            sender.sendMessage(ChatColor.RED + "메시지 전송에 실패했습니다.");
        }
        
        return true;
    }
    
    /**
     * 명령어 자동 완성 메서드
     * 
     * @param sender 명령어 실행자
     * @param command 실행된 명령어
     * @param alias 명령어 별칭
     * @param args 명령어 인자
     * @return 자동 완성 제안 목록
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // 권한 확인
        if (!sender.hasPermission("lukevanilla.command.sendmessage")) {
            return new ArrayList<>();
        }
        
        // 첫 번째 인자(서버명) 자동 완성
        if (args.length == 1) {
            // 서버 목록은 정적으로 정의 (실제로는 구성 파일에서 로드하거나 동적으로 결정)
            List<String> servers = plugin.getConfig().getStringList("servers");
            
            if (servers.isEmpty()) {
                // 기본 서버 목록 사용
                servers = new ArrayList<>();
                servers.add("lobby");
                servers.add("survival");
                servers.add("creative");
                servers.add("minigames");
            }
            
            // 입력된 접두사와 일치하는 서버만 필터링
            return servers.stream()
                .filter(server -> server.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        // 그 외의 인자는 자동 완성 없음
        return new ArrayList<>();
    }
} 