package com.petoma.petoCasinoV2;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandHandler implements CommandExecutor {
    private final PetoCasinoV2 plugin;
    private final GameManager gameManager;

    public CommandHandler(PetoCasinoV2 plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cこのコマンドはプレイヤー専用です！");
            return true;
        }

        Player player = (Player) sender;

        // 引数の数が足りない場合
        if (args.length < 1) {
            player.sendMessage("§c使用方法: /chn <start|high|low|lucky7> <金額>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start":
                if (args.length < 2) {
                    player.sendMessage("§c使用方法: /chn start <金額>");
                    return true;
                }
                try {
                    int amount = Integer.parseInt(args[1]);
                    gameManager.startGame(player, amount);
                } catch (NumberFormatException e) {
                    player.sendMessage("§c金額は数字で指定してください！");
                }
                break;

            case "high":
                gameManager.highJoin(player);
                break;

            case "low":
                gameManager.lowJoin(player);
                break;

            case "lucky7":
                gameManager.mediumJoin(player);
                break;

            case "list":
                gameManager.getParticipant(player);
                break;


            default:
                player.sendMessage("§c無効なサブコマンドです！");
                break;
        }

        return true;
    }
}
