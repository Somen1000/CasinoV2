package com.petoma.petoCasinoV2;

import jp.jyn.jecon.Jecon;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.math.BigDecimal;
import java.util.*;

public class GameManager {
    private List<String> winners = new ArrayList<>();

    private Jecon jecon;
    private final Map<Player, Integer> money = new HashMap<>();

    private final JavaPlugin plugin;
    private boolean gameActive = false;
    private Player host;
    private int betAmount;
    private final Set<Player> participants = new HashSet<>();
    private final Map<Player, String> userSelect = new HashMap<>();
    private final Set<Player> joinedPlayers = new HashSet<>();
    private int startCount;
    private BukkitTask countdownTask;
    private int dice1;
    private int dice2;

    public GameManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isGameActive() {
        return gameActive;
    }


    public void startGame(Player player, int amount) {
        if (gameActive) {
            String message = plugin.getConfig().getString("messages.game_another_started");
            player.sendMessage(message);
            return;
        }
        if (getMoney(player, amount)) {
            String message = plugin.getConfig().getString("messages.not_enough_money");
            message = message.replace("{player}", player.getName())
                    .replace("{amount}", String.valueOf(betAmount));
            player.sendMessage(message);
            return;
        }

        this.gameActive = true;
        this.host = player;
        this.betAmount = amount;

        String gameStartedMessage = plugin.getConfig().getString("messages.game_started");
        gameStartedMessage = gameStartedMessage.replace("{player}", player.getName())
                .replace("{amount}", String.valueOf(amount));
        Bukkit.broadcastMessage(gameStartedMessage);

        startCount = 75;
        startCount();

        String json = "[{\"text\":\"[\",\"color\":\"white\",\"bold\":false},{\"text\":\"ダブルサイコロ\",\"color\":\"light_purple\",\"bold\":true},{\"text\":\"] \",\"color\":\"white\",\"bold\":false},{\"text\":\" 以下の選択肢をクリックすることで参加します！\\n\\n\",\"color\":\"green\",\"bold\":true},{\"text\":\"＜\",\"color\":\"white\",\"bold\":true},{\"text\":\"小にかける!\",\"color\":\"red\",\"bold\":true,\"hoverEvent\":{\"action\":\"show_text\",\"value\":[{\"text\":\"小(サイコロの和 2~6)にかけます\\n当選すると掛け金の2倍の額をゲット！\",\"color\":\"aqua\"}]}," +
                "\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/chn low\"}},{\"text\":\"＞　＜\",\"color\":\"white\",\"bold\":true,\"hoverEvent\":{\"action\":\"show_text\",\"value\":[{\"text\":\"\",\"color\":\"gold\"}]}},{\"text\":\"ぴったりセブン!\",\"color\":\"aqua\",\"bold\":true,\"hoverEvent\":{\"action\":\"show_text\",\"value\":[{\"text\":\"ぴったセブン(サイコロの和 7) にかける\\n当選すると掛け金の 3倍! の額をゲット！\",\"color\":\"aqua\"}]}," +
                "\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/chn lucky7\"}},{\"text\":\"＞　＜\",\"color\":\"white\",\"bold\":true,\"hoverEvent\":{\"action\":\"show_text\",\"value\":[{\"text\":\"\",\"color\":\"aqua\"}]}},{\"text\":\"大にかける\",\"color\":\"green\",\"bold\":true,\"hoverEvent\":{\"action\":\"show_text\",\"value\":[{\"text\":\"小(サイコロの和 8~12)にかけます\\n当選すると掛け金の2倍の額をゲット！\",\"color\":\"aqua\"}]}," +
                "\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/chn high\"}},{\"text\":\"＞\",\"color\":\"white\",\"bold\":true,\"hoverEvent\":{\"action\":\"show_text\",\"value\":[{\"text\":\"\",\"color\":\"aqua\"}]}},{\"text\":\"  [\",\"color\":\"white\",\"bold\":false,\"hoverEvent\":{\"action\":\"show_text\",\"value\":[{\"text\":\"\",\"color\":\"aqua\"}]}},{\"text\":\"参加者\",\"color\":\"aqua\",\"bold\":false,\"hoverEvent\":{\"action\":\"show_text\",\"value\":[{\"text\":\"参加者一覧を表示\",\"color\":\"aqua\"}]}," +
                "\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/chn list\"}},{\"text\":\"]\",\"color\":\"white\",\"bold\":false,\"hoverEvent\":{\"action\":\"show_text\",\"value\":[{\"text\":\"\",\"color\":\"aqua\"}]}}]";

        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "tellraw @a " + json);
    }

    public boolean getMoney(Player player, int needMoney) {
        this.jecon = (Jecon) plugin.getServer().getPluginManager().getPlugin("Jecon");
        Optional<BigDecimal> value = jecon.getRepository().getDecimal(player.getUniqueId());

        int result = value.map(decimal -> decimal.intValue()).orElse(0);

        money.put(player, result);

        if (money.get(player) >= needMoney) {
            return false;
        }
        return true;
    }

    public void highJoin(Player player) {
        joinGameWithSelection(player, "high", "messages.choose_high");
    }

    public void mediumJoin(Player player) {
        joinGameWithSelection(player, "lucky7", "messages.choose_medium");
    }

    public void lowJoin(Player player) {
        joinGameWithSelection(player, "low", "messages.choose_low");
    }

    public void joinGameWithSelection(Player player, String selection, String messageKey) {
        if (!gameActive) {
            String message = plugin.getConfig().getString("messages.game_not_start");
            message = message.replace("{player}", player.getName())
                    .replace("{amount}", String.valueOf(betAmount));
            player.sendMessage(message);
            return;
        }

        if (startCount <= 15) return;

        if (participants.contains(player)) {
            String message = plugin.getConfig().getString("messages.already_participated");
            message = message.replace("{player}", player.getName())
                    .replace("{amount}", String.valueOf(betAmount));
            player.sendMessage(message);
            return;
        }

        if (getMoney(player, betAmount)) {
            String message = plugin.getConfig().getString("messages.not_enough_money");
            message = message.replace("{player}", player.getName())
                    .replace("{amount}", String.valueOf(betAmount));
            player.sendMessage(message);
            return;
        }

        // 参加費を引く処理
        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "money take " + player.getName() + " " + betAmount);

        // selectionが指定されている場合、userSelectに追加
        if (selection != null) {
            userSelect.put(player, selection);

            String message = plugin.getConfig().getString(messageKey);
            message = message.replace("{player}", player.getName());

            // 参加者全員にメッセージ送信
            for (Player allPlayer : participants) {
                allPlayer.sendMessage(message);
            }
        }

        // プレイヤーに参加メッセージを送信
        String message = plugin.getConfig().getString("messages.joined_game");
        message = message.replace("{player}", player.getName())
                .replace("{time}", String.valueOf(startCount - 15));
        player.sendMessage(message);

        // 参加音を全員に再生
        for (Player allPlayer : participants) {
            allPlayer.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }
        player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

        // プレイヤーを参加者リストに追加
        participants.add(player);
    }




    public void startCount() {
        Bukkit.getScheduler().runTaskLater(PetoCasinoV2.getInstance(), () -> {
            startCount--;
            if (startCount == 45 || startCount == 30 || startCount == 20 || startCount == 18 || startCount == 17 || startCount == 16) {
                countDownChat(startCount - 15);
            }
            if (startCount == 15) {
                prepareRoll();
            } else if (startCount == 10) {
                dice1();
            } else if (startCount == 5) {
                dice2();
            } else if (startCount == 1) {
                endGame();
            }
            if (startCount >= 0) {
                startCount();
            }
        }, 20L);
    }

    public void countDownChat(int sec) {
        for (Player player : participants) {
            String message = plugin.getConfig().getString("messages.game_time");
            message = message.replace("{time}", String.valueOf(sec));
            player.sendMessage(message);
            if (sec <= 3) {
                player.playSound(player, Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            }
        }
    }

    public void cancelCount() {

        if (countdownTask != null && !countdownTask.isCancelled()) {
            countdownTask.cancel();
            System.out.println("カウントダウンがキャンセルされました。");
        }
    }

    public void prepareRoll() {
        for (Player player : participants) {
            String message = plugin.getConfig().getString("messages.draw_started");
            player.sendMessage(message);
        }
    }

    public void endGame() {
        int sumDice = dice1 + dice2;
        winners.clear();
        String result = "";


        if (sumDice <= 6) {
            result = "low";
        }

        if (sumDice == 7) {
            result = "lucky7";
        }

        if (sumDice >= 8) {
            result = "high";
        }

        for (Map.Entry<Player, String> entry : userSelect.entrySet()) {
            Player player = entry.getKey();
            String userSelected = entry.getValue();
            int rewardAmount = 0;

            if (result.equals("low") && userSelected.equals("low")) {
                rewardAmount = betAmount * 2;
            } else if (result.equals("lucky7") && userSelected.equals("lucky7")) {
                rewardAmount = betAmount * 3;
            } else if (result.equals("high") && userSelected.equals("high")) {
                rewardAmount = betAmount * 2;
            }

            if (rewardAmount > 0) {
                String gameStartedMessage = plugin.getConfig().getString("messages.winner_paid");
                if (gameStartedMessage != null) {
                    gameStartedMessage = gameStartedMessage.replace("{select}", userSelected)
                            .replace("{amount}", String.valueOf(rewardAmount));
                } else {
                    gameStartedMessage = "報酬メッセージの設定が見つかりませんでした。";
                }

                player.sendMessage(gameStartedMessage);
                player.playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.6f, 1.4f);
                addWinner(player.getName());
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "money give " + player.getName() + " " + rewardAmount);
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "scoreboard players add " + player.getName() + " chn_profit_min0 " + rewardAmount);
            } else {
                String gameStartedMessage = plugin.getConfig().getString("messages.loser_message");
                if (gameStartedMessage != null) {
                    gameStartedMessage = gameStartedMessage.replace("{select}", userSelected);
                } else {
                    gameStartedMessage = "負けた場合のメッセージ設定が見つかりませんでした。";
                }

                player.sendMessage(gameStartedMessage);
                player.playSound(player, Sound.ENTITY_BLAZE_DEATH, 1f, 1f);
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "scoreboard players remove " + player.getName() + " chn_profit_min0 " + betAmount);
                int nowScoreboard = getPlayerScore(player);
                if (nowScoreboard < 0) {
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "scoreboard players set " + player.getName() + " chn_profit_min0 0");
                }
            }

            int finalScoreboard = getPlayerScore(player);
            player.sendMessage("§aあなたのイベント収支は" + finalScoreboard + "円です。(-にはなりません。)");
        }

        String gameStartedMessage = plugin.getConfig().getString("messages.winner_result");
        gameStartedMessage = gameStartedMessage.replace("{winners}", getWinnersAsString());
        for (Player allPlayer : participants) {
            allPlayer.sendMessage(gameStartedMessage);
        }
        this.gameActive = false;
        participants.clear();
        userSelect.clear();
    }

    public void addWinner(String playerName) {
        winners.add(playerName);
    }

    public void getParticipant(Player sendPlayer) {
        if (userSelect.isEmpty()) {
            sendPlayer.sendMessage("現在、参加者はありません。");
            return;
        }

        Map<String, List<String>> groupedSelections = new HashMap<>();

        for (Map.Entry<Player, String> entry : userSelect.entrySet()) {
            Player player = entry.getKey();
            String selection = entry.getValue();

            groupedSelections
                    .computeIfAbsent(selection, k -> new ArrayList<>())
                    .add(player.getName());
        }

        for (Map.Entry<String, List<String>> entry : groupedSelections.entrySet()) {
            String selection = entry.getKey();
            List<String> players = entry.getValue();
            String playerList = String.join(", ", players);

            String message = plugin.getConfig().getString("messages.list_player");
            if (message == null) {
                sendPlayer.sendMessage("メッセージの設定が見つかりません。");
                return;
            }

            message = message.replace("{select}", selection)
                    .replace("{players}", playerList.isEmpty() ? "参加者なし" : playerList);
            sendPlayer.sendMessage(message);
        }
    }

    public String getWinnersAsString() {
        if (winners.isEmpty()) {
            return "勝者なし";
        }
        return String.join(",", winners);
    }


    public void dice1() {
        dice1 = (int) (Math.random() * 6) + 1;
        for (Player player : participants) {
            String message = plugin.getConfig().getString("messages.draw_dice1");
            message = message.replace("{number}", String.valueOf(dice1));
            player.sendMessage(message);
            player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.8f);
        }
    }
    public void dice2() {
        dice2 = (int) (Math.random() * 6) + 1;
        for (Player player : participants) {
            String message = plugin.getConfig().getString("messages.draw_dice2");
            message = message.replace("{number}", String.valueOf(dice2));
            player.sendMessage(message);
            player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.8f);
        }
    }

    public int getPlayerScore(Player player) {
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = scoreboard.getObjective("chn_profit_min0");
        if (objective == null) {
            return 0;
        }
        Score score = objective.getScore(player.getName());

        return score.getScore();
    }
}
