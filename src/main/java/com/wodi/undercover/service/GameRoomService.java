package com.wodi.undercover.service;

import com.wodi.undercover.entity.GamePlayer;
import com.wodi.undercover.entity.GameRoom;
import com.wodi.undercover.entity.GameSettings;
import com.wodi.undercover.entity.Guess;
import com.wodi.undercover.entity.Player;
import com.wodi.undercover.entity.VoteKillRecord;
import com.wodi.undercover.repository.GamePlayerRepository;
import com.wodi.undercover.repository.GameRoomRepository;
import com.wodi.undercover.repository.GameSettingsRepository;
import com.wodi.undercover.repository.GuessRepository;
import com.wodi.undercover.repository.PlayerRepository;
import com.wodi.undercover.repository.VoteKillRecordRepository;
import com.wodi.undercover.websocket.GameWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GameRoomService {

    @Autowired
    private GameRoomRepository gameRoomRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private GamePlayerRepository gamePlayerRepository;

    @Autowired
    private GameSettingsRepository gameSettingsRepository;

    @Autowired
    private GuessRepository guessRepository;

    @Autowired
    private VoteKillRecordRepository voteKillRecordRepository;

    @Autowired
    @Lazy
    private GameLogicService gameLogicService;

    @Autowired
    @Lazy
    private GameWebSocketHandler webSocketHandler;

    @Transactional
    public Player registerPlayer(String nickname) {
        if (playerRepository.existsByNickname(nickname)) {
            throw new RuntimeException("昵称已被使用");
        }

        String bindCode = generateBindCode();
        while (playerRepository.existsByBindCode(bindCode)) {
            bindCode = generateBindCode();
        }

        Player player = new Player(nickname, bindCode);
        return playerRepository.save(player);
    }

    public Player getPlayerByBindCode(String bindCode) {
        return playerRepository.findByBindCode(bindCode).orElse(null);
    }

    public Player getPlayerByNickname(String nickname) {
        return playerRepository.findByNickname(nickname).orElse(null);
    }

    @Transactional
    public GameRoom createRoom(String roomName, String hostNickname) {
        String roomCode = generateRoomCode();
        while (gameRoomRepository.findByRoomCode(roomCode).isPresent()) {
            roomCode = generateRoomCode();
        }

        GameRoom room = new GameRoom(roomCode, roomName, hostNickname);
        room = gameRoomRepository.save(room);

        GamePlayer hostPlayer = new GamePlayer(roomCode, hostNickname, null);
        hostPlayer.setIdentity("judge");
        gamePlayerRepository.save(hostPlayer);

        room.setPlayerCount(1);
        gameRoomRepository.save(room);

        return room;
    }

    @Transactional
    public GameRoom joinRoom(String roomCode, String playerNickname) {
        GameRoom room = gameRoomRepository.findByRoomCode(roomCode).orElse(null);
        if (room == null) {
            throw new RuntimeException("房间不存在");
        }

        if (!"waiting".equals(room.getStatus())) {
            throw new RuntimeException("游戏已开始，无法加入");
        }

        Optional<GamePlayer> existingPlayer = gamePlayerRepository.findByRoomCodeAndPlayerNickname(roomCode, playerNickname);
        if (existingPlayer.isPresent()) {
            throw new RuntimeException("该昵称已在房间中");
        }

        Player player = playerRepository.findByNickname(playerNickname).orElse(null);

        GamePlayer gamePlayer = new GamePlayer(roomCode, playerNickname, player != null ? player.getId() : null);
        gamePlayerRepository.save(gamePlayer);

        room.setPlayerCount(room.getPlayerCount() + 1);
        gameRoomRepository.save(room);

        return room;
    }

    public GameRoom getRoom(String roomCode) {
        return gameRoomRepository.findByRoomCode(roomCode).orElse(null);
    }

    public List<GamePlayer> getRoomPlayers(String roomCode) {
        return gamePlayerRepository.findByRoomCode(roomCode);
    }

    public GamePlayer getPlayerByRoomAndNickname(String roomCode, String nickname) {
        return gamePlayerRepository.findByRoomCodeAndPlayerNickname(roomCode, nickname).orElse(null);
    }

    @Transactional
    public void leaveRoom(String roomCode, String playerNickname) {
        GameRoom room = gameRoomRepository.findByRoomCode(roomCode).orElse(null);
        if (room == null) return;

        Optional<GamePlayer> playerOpt = gamePlayerRepository.findByRoomCodeAndPlayerNickname(roomCode, playerNickname);
        if (playerOpt.isPresent()) {
            GamePlayer player = playerOpt.get();
            if ("judge".equals(player.getIdentity())) {
                guessRepository.deleteByRoomCode(roomCode);
                gameSettingsRepository.deleteByRoomCode(roomCode);
                gamePlayerRepository.deleteByRoomCode(roomCode);
                gameRoomRepository.delete(room);
                return;
            }

            gamePlayerRepository.delete(player);
            room.setPlayerCount(room.getPlayerCount() - 1);
            gameRoomRepository.save(room);
        }
    }

    @Transactional
    public GameSettings createSettings(String roomCode, String civilianWord, String undercoverWord, String blankHint,
                                       String restrictedWord,
                                       Integer civilianCount, Integer undercoverCount, Integer blankCount, Integer angelCount) {
        GameRoom room = gameRoomRepository.findByRoomCode(roomCode).orElse(null);
        if (room == null) {
            throw new RuntimeException("房间不存在");
        }

        GameSettings settings = gameSettingsRepository.findByRoomCode(roomCode).orElse(new GameSettings());
        settings.setRoomCode(roomCode);

        if (civilianWord != null) {
            settings.setCivilianWord(civilianWord);
        }
        if (undercoverWord != null) {
            settings.setUndercoverWord(undercoverWord);
        }
        if (blankHint != null) {
            settings.setBlankHint(blankHint);
        }
        if (restrictedWord != null) {
            settings.setRestrictedWord(restrictedWord);
        }
        if (civilianCount != null) {
            settings.setCivilianCount(civilianCount);
        }
        if (undercoverCount != null) {
            settings.setUndercoverCount(undercoverCount);
        }
        if (blankCount != null) {
            settings.setBlankCount(blankCount);
        }
        if (angelCount != null) {
            settings.setAngelCount(angelCount);
        }

        GameSettings savedSettings = gameSettingsRepository.save(settings);

        if (room != null) {
            webSocketHandler.notifyRoomUpdate(room);
        }

        return savedSettings;
    }

    public GameSettings getSettings(String roomCode) {
         return gameSettingsRepository.findByRoomCode(roomCode).orElse(null);
     }

     @Transactional
    public void setPlayerIdentity(String roomCode, String nickname, String identity) {
        GamePlayer player = gamePlayerRepository.findByRoomCodeAndPlayerNickname(roomCode, nickname).orElse(null);
        if (player == null) {
            throw new RuntimeException("玩家不存在");
        }
        player.setIdentity(identity);
        gamePlayerRepository.save(player);

        GameRoom room = gameRoomRepository.findByRoomCode(roomCode).orElse(null);
        if (room != null) {
            webSocketHandler.notifyRoomUpdate(room);
        }
    }

    public void notifyRoomUpdateWithSettings(String roomCode) {
        GameRoom room = gameRoomRepository.findByRoomCode(roomCode).orElse(null);
        if (room != null) {
            GameSettings settings = getSettings(roomCode);
            room.setSettings(settings);
            webSocketHandler.notifyRoomUpdate(room);
        }
    }

    @Transactional
    public void assignIdentities(String roomCode, int civilianCount, int undercoverCount, int blankCount, int angelCount) {
        GameSettings settings = getSettings(roomCode);
        if (settings != null) {
            if (civilianCount == 0) civilianCount = settings.getCivilianCount() != null ? settings.getCivilianCount() : 3;
            if (undercoverCount == 0) undercoverCount = settings.getUndercoverCount() != null ? settings.getUndercoverCount() : 1;
            if (blankCount == 0) blankCount = settings.getBlankCount() != null ? settings.getBlankCount() : 0;
            if (angelCount == 0) angelCount = settings.getAngelCount() != null ? settings.getAngelCount() : 0;
        }

        List<GamePlayer> players = gamePlayerRepository.findByRoomCode(roomCode);
        players.removeIf(p -> "judge".equals(p.getIdentity()));

        int totalAssignments = civilianCount + undercoverCount + blankCount + angelCount;
        if (totalAssignments != players.size()) {
            throw new RuntimeException("角色总人数(" + totalAssignments + ")与玩家人数(" + players.size() + ")不一致");
        }

        long seed = System.currentTimeMillis();
        Random random = new Random(seed);

        for (int i = players.size() - 1; i > 0; i--) {
            int swapIndex = random.nextInt(i + 1);
            GamePlayer temp = players.get(i);
            players.set(i, players.get(swapIndex));
            players.set(swapIndex, temp);
        }

        for (int i = 0; i < totalAssignments; i++) {
            GamePlayer player = players.get(i);
            if (i < civilianCount) {
                player.setIdentity("civilian");
            } else if (i < civilianCount + undercoverCount) {
                player.setIdentity("undercover");
            } else if (i < civilianCount + undercoverCount + blankCount) {
                player.setIdentity("blank");
            } else {
                player.setIdentity("angel");
            }
            gamePlayerRepository.save(player);
        }
    }

    @Transactional
    public void distributeWords(String roomCode) {
        GameSettings settings = gameSettingsRepository.findByRoomCode(roomCode).orElse(null);
        if (settings == null) {
            throw new RuntimeException("游戏设置不存在");
        }

        List<GamePlayer> players = gamePlayerRepository.findByRoomCode(roomCode);

        for (GamePlayer player : players) {
            String identity = player.getIdentity();
            String word = null;
            String secondWord = null;

            switch (identity) {
                case "civilian":
                    word = settings.getCivilianWord();
                    break;
                case "undercover":
                    word = settings.getUndercoverWord();
                    break;
                case "blank":
                    word = settings.getBlankHint();
                    break;
                case "angel":
                    word = settings.getCivilianWord();
                    secondWord = settings.getUndercoverWord();
                    break;
            }

            player.setWord(word);
            player.setSecondWord(secondWord);
            gamePlayerRepository.save(player);
        }

        GameRoom room = gameRoomRepository.findByRoomCode(roomCode).orElse(null);
        if (room != null) {
            webSocketHandler.notifyRoomUpdate(room);
        }
    }

    @Transactional
    public void startGame(String roomCode) {
        GameRoom room = gameRoomRepository.findByRoomCode(roomCode).orElse(null);
        if (room == null) {
            throw new RuntimeException("房间不存在");
        }

        GameSettings settings = getSettings(roomCode);
        if (settings != null) {
            List<GamePlayer> players = gamePlayerRepository.findByRoomCode(roomCode);
            long totalPlayers = players.stream().filter(p -> !"judge".equals(p.getIdentity())).count();
            settings.setTotalPlayerCount((int) totalPlayers);
            gameSettingsRepository.save(settings);
        }

        room.setStatus("playing");
        gameRoomRepository.save(room);
        webSocketHandler.notifyRoomUpdate(room);
    }

    @Transactional
    public void startSetting(String roomCode) {
        GameRoom room = gameRoomRepository.findByRoomCode(roomCode).orElse(null);
        if (room == null) {
            throw new RuntimeException("房间不存在");
        }

        room.setStatus("setting");
        gameRoomRepository.save(room);
        webSocketHandler.notifyRoomUpdate(room);
    }

    @Transactional
    public void endGame(String roomCode) {
        GameRoom room = gameRoomRepository.findByRoomCode(roomCode).orElse(null);
        if (room == null) return;

        resetGame(roomCode);
    }

    @Transactional
    public void nextRound(String roomCode) {
        GameRoom room = gameRoomRepository.findByRoomCode(roomCode).orElse(null);
        if (room == null) {
            throw new RuntimeException("房间不存在");
        }

        gameLogicService.processVotingResult(roomCode);

        gameLogicService.checkGameEnd(roomCode);

        room = gameRoomRepository.findByRoomCode(roomCode).orElse(null);
        if (room == null || room.getStatus().endsWith("_win")) {
            webSocketHandler.notifyRoomUpdate(room);
            return;
        }

        List<GamePlayer> players = gamePlayerRepository.findByRoomCode(roomCode);
        for (GamePlayer player : players) {
            player.setHasVoted(false);
            player.setHasKilled(false);
            gamePlayerRepository.save(player);
        }

        room.setCurrentRound(room.getCurrentRound() + 1);
        gameRoomRepository.save(room);

        GameSettings settings = getSettings(roomCode);
        if (settings != null) {
            settings.setGamePhase("day");
            settings.setVotingFinished(false);
            settings.setVoteComplete(false);
            settings.setKillComplete(false);
            gameSettingsRepository.save(settings);
        }

        webSocketHandler.notifyRoomUpdate(room);
    }

    @Transactional
    public void togglePhase(String roomCode) {
        GameSettings settings = getSettings(roomCode);
        if (settings == null) {
            throw new RuntimeException("游戏设置不存在");
        }

        String currentPhase = settings.getGamePhase();
        if (currentPhase == null || "day".equals(currentPhase)) {
            settings.setGamePhase("night");
        } else {
            settings.setGamePhase("day");
        }
        gameSettingsRepository.save(settings);
        webSocketHandler.notifyRoomUpdate(gameRoomRepository.findByRoomCode(roomCode).orElse(null));
    }

    @Transactional
    public void setSpeakingStatus(String roomCode, String playerNickname, Integer speakingStatus) {
        GamePlayer player = gamePlayerRepository.findByRoomCodeAndPlayerNickname(roomCode, playerNickname)
                .orElseThrow(() -> new RuntimeException("玩家不存在"));
        player.setSpeakingStatus(speakingStatus);
        gamePlayerRepository.save(player);
        GameRoom room = gameRoomRepository.findByRoomCode(roomCode).orElse(null);
        if (room != null) {
            webSocketHandler.notifyRoomUpdate(room);
        }
    }

    @Transactional
    public void submitStatement(String roomCode, String playerNickname, String statement, Integer round) {
        GamePlayer player = gamePlayerRepository.findByRoomCodeAndPlayerNickname(roomCode, playerNickname)
                .orElseThrow(() -> new RuntimeException("玩家不存在"));

        if (round == null || round < 1 || round > 3) {
            throw new RuntimeException("无效的轮数");
        }

        if (round == 1) {
            player.setStatement1(statement);
        } else if (round == 2) {
            player.setStatement2(statement);
        } else if (round == 3) {
            player.setStatement3(statement);
        }

        player.setSpeakingStatus(0);
        gamePlayerRepository.save(player);

        GameRoom room = gameRoomRepository.findByRoomCode(roomCode).orElse(null);
        if (room != null) {
            webSocketHandler.notifyRoomUpdate(room);
        }
    }

    @Transactional
    public Guess submitGuess(String roomCode, String guesserNickname, String targetNickname, String guessIdentity, String status) {
        GameRoom room = gameRoomRepository.findByRoomCode(roomCode).orElse(null);
        if (room == null) {
            throw new RuntimeException("房间不存在");
        }

        GamePlayer guesser = gamePlayerRepository.findByRoomCodeAndPlayerNickname(roomCode, guesserNickname).orElse(null);
        if (guesser == null) {
            throw new RuntimeException("猜测者不存在");
        }

        GamePlayer target = gamePlayerRepository.findByRoomCodeAndPlayerNickname(roomCode, targetNickname).orElse(null);
        if (target == null) {
            throw new RuntimeException("目标玩家不存在");
        }

        List<Guess> existingGuesses = guessRepository.findByRoomCodeAndGuesserNicknameAndTargetNickname(roomCode, guesserNickname, targetNickname);
        Guess guess;
        if (!existingGuesses.isEmpty()) {
            guess = existingGuesses.get(0);
            guess.setGuessIdentity(guessIdentity);
            guess.setStatus(status);
            guess.setIsCorrect(target.getIdentity().equals(guessIdentity));
        } else {
            guess = new Guess(roomCode, guesserNickname, targetNickname, guessIdentity);
            guess.setStatus(status);
            guess.setIsCorrect(target.getIdentity().equals(guessIdentity));
        }

        return guessRepository.save(guess);
    }

    @Transactional
    public void killPlayer(String roomCode, String playerNickname, String targetNickname) {
        GamePlayer killer = gamePlayerRepository.findByRoomCodeAndPlayerNickname(roomCode, playerNickname)
                .orElseThrow(() -> new RuntimeException("玩家不存在"));
        GamePlayer target = gamePlayerRepository.findByRoomCodeAndPlayerNickname(roomCode, targetNickname)
                .orElseThrow(() -> new RuntimeException("目标玩家不存在"));

        GameSettings settings = getSettings(roomCode);
        if (settings == null || !settings.getKillComplete()) {
            throw new RuntimeException("刀人阶段未开启");
        }

        if (killer.isHasKilled()) {
            throw new RuntimeException("本轮已刀人");
        }

        killer.setHasKilled(true);
        killer.setKillTarget(targetNickname);
        gamePlayerRepository.save(killer);

        GameRoom room = gameRoomRepository.findByRoomCode(roomCode).orElse(null);
        if (room != null) {
            webSocketHandler.notifyRoomUpdate(room);
        }
    }

    public List<Guess> getGuessesByRoom(String roomCode) {
        return guessRepository.findByRoomCode(roomCode);
    }

    public List<VoteKillRecord> getVoteKillRecordsByRoom(String roomCode) {
        return voteKillRecordRepository.findByRoomCodeOrderByRoundNumberAscCreateTimeAsc(roomCode);
    }

    @Transactional
    public void whiteBoardGuess(String roomCode, String playerNickname, String civilianWord, String undercoverWord) {
        GameSettings settings = getSettings(roomCode);
        if (settings == null) {
            throw new RuntimeException("游戏设置不存在");
        }

        GamePlayer player = gamePlayerRepository.findByRoomCodeAndPlayerNickname(roomCode, playerNickname)
                .orElseThrow(() -> new RuntimeException("玩家不存在"));

        if (!"blank".equals(player.getIdentity())) {
            throw new RuntimeException("只有白板玩家才能猜词");
        }

        settings.setWhiteBoardGuessActive(true);
        settings.setWhiteBoardGuessCivilianWord(civilianWord);
        settings.setWhiteBoardGuessUndercoverWord(undercoverWord);
        gameSettingsRepository.save(settings);

        notifyRoomUpdateWithSettings(roomCode);
    }

    @Transactional
    public void verifyWhiteBoardGuess(String roomCode, Boolean correct) {
        GameSettings settings = getSettings(roomCode);
        if (settings == null) {
            throw new RuntimeException("游戏设置不存在");
        }

        if (!settings.getWhiteBoardGuessActive()) {
            throw new RuntimeException("白板尚未提交猜测");
        }

        settings.setWhiteBoardGuessCorrect(correct);
        gameSettingsRepository.save(settings);

        GameRoom room = gameRoomRepository.findByRoomCode(roomCode).orElse(null);
        if (room != null) {
            if (Boolean.TRUE.equals(correct)) {
                room.setStatus("blank_win");
            } else {
                room.setStatus("civilian_win");
            }
            gameRoomRepository.save(room);
            notifyRoomUpdateWithSettings(roomCode);
        }
    }

    private String generateBindCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789!@#$%";
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }

    private String generateRoomCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }

    @Transactional
    public void resetGame(String roomCode) {
        GameRoom room = gameRoomRepository.findByRoomCode(roomCode).orElse(null);
        if (room == null) {
            throw new RuntimeException("房间不存在");
        }

        GameSettings settings = gameSettingsRepository.findByRoomCode(roomCode).orElse(null);
        if (settings != null) {
            settings.setCivilianWord(null);
            settings.setUndercoverWord(null);
            settings.setBlankHint(null);
            settings.setCivilianCount(null);
            settings.setUndercoverCount(null);
            settings.setBlankCount(null);
            settings.setAngelCount(null);
            settings.setGamePhase("day");
            gameSettingsRepository.save(settings);
        }

        List<GamePlayer> players = gamePlayerRepository.findByRoomCode(roomCode);
        for (GamePlayer player : players) {
            if ("judge".equals(player.getIdentity())) continue;
            player.setIdentity(null);
            player.setWord(null);
            player.setAlive(true);
            player.setStatement1(null);
            player.setStatement2(null);
            player.setStatement3(null);
            player.setSpeakingStatus(0);
            player.setHasVoted(false);
            player.setHasKilled(false);
            player.setKillTarget(null);
            player.setDeathCause(null);
            player.setVoteCount(0);
            gamePlayerRepository.save(player);
        }

        List<Guess> guesses = guessRepository.findByRoomCode(roomCode);
        guessRepository.deleteAll(guesses);

        List<VoteKillRecord> voteKillRecords = voteKillRecordRepository.findByRoomCodeOrderByRoundNumberAscCreateTimeAsc(roomCode);
        voteKillRecordRepository.deleteAll(voteKillRecords);

        if (settings != null) {
            settings.setVoteComplete(false);
            settings.setKillComplete(false);
            settings.setVotingFinished(false);
            settings.setWhiteBoardGuessActive(false);
            settings.setWhiteBoardGuessCivilianWord(null);
            settings.setWhiteBoardGuessUndercoverWord(null);
            settings.setWhiteBoardGuessCorrect(null);
            gameSettingsRepository.save(settings);
        }

        room.setStatus("waiting");
        room.setCurrentRound(1);
        gameRoomRepository.save(room);

        webSocketHandler.notifyRoomUpdate(room);
    }

    @Transactional
    public void enableVoting(String roomCode) {
        GameSettings settings = getSettings(roomCode);
        if (settings == null) {
            throw new RuntimeException("游戏设置不存在");
        }
        settings.setVoteComplete(true);
        settings.setKillComplete(false);
        gameSettingsRepository.save(settings);

        List<GamePlayer> players = gamePlayerRepository.findByRoomCode(roomCode);
        for (GamePlayer player : players) {
            player.setHasVoted(false);
            gamePlayerRepository.save(player);
        }

        webSocketHandler.notifyRoomUpdate(gameRoomRepository.findByRoomCode(roomCode).orElse(null));
    }

    @Transactional
    public void finishVoting(String roomCode) {
        GameSettings settings = getSettings(roomCode);
        if (settings == null) {
            throw new RuntimeException("游戏设置不存在");
        }
        if (!settings.getVoteComplete()) {
            throw new RuntimeException("投票未开启");
        }

        gameLogicService.processVotingResult(roomCode);

        List<GamePlayer> allPlayers = gamePlayerRepository.findByRoomCode(roomCode);
        long aliveCount = allPlayers.stream().filter(p -> p.isAlive() && !"judge".equals(p.getIdentity())).count();
        settings.setAlivePlayerCount((int) aliveCount);
        gameSettingsRepository.save(settings);

        gameLogicService.checkGameEndWithAliveCount(roomCode, (int) aliveCount);

        settings.setVoteComplete(false);
        settings.setVotingFinished(true);
        gameSettingsRepository.save(settings);
        webSocketHandler.notifyRoomUpdate(gameRoomRepository.findByRoomCode(roomCode).orElse(null));
    }

    @Transactional
    public void enableKilling(String roomCode) {
        GameSettings settings = getSettings(roomCode);
        if (settings == null) {
            throw new RuntimeException("游戏设置不存在");
        }
        if (settings.getVoteComplete()) {
            throw new RuntimeException("请先结束投票");
        }
        settings.setKillComplete(true);
        gameSettingsRepository.save(settings);
        webSocketHandler.notifyRoomUpdate(gameRoomRepository.findByRoomCode(roomCode).orElse(null));
    }

    @Transactional
    public void finishKilling(String roomCode) {
        GameSettings settings = getSettings(roomCode);
        if (settings == null) {
            throw new RuntimeException("游戏设置不存在");
        }

        GameRoom room = gameRoomRepository.findByRoomCode(roomCode).orElse(null);
        int currentRound = room != null ? room.getCurrentRound() : 1;

        List<GamePlayer> allPlayers = gamePlayerRepository.findByRoomCode(roomCode);
        List<GamePlayer> killers = allPlayers.stream()
                .filter(p -> p.isHasKilled() && p.getKillTarget() != null && !p.getKillTarget().isEmpty())
                .collect(Collectors.toList());

        Set<String> deadNicknames = new HashSet<>();
        Map<String, String> deathCauses = new HashMap<>();
        List<VoteKillRecord> records = new ArrayList<>();

        for (GamePlayer killer : killers) {
            String killerIdentity = killer.getIdentity();
            boolean isGoodKiller = "civilian".equals(killerIdentity) || "angel".equals(killerIdentity) || "blank".equals(killerIdentity);

            if (isGoodKiller) {
                String killerName = killer.getPlayerNickname();
                String targetName = killer.getKillTarget();
                if (!deadNicknames.contains(killerName)) {
                    deadNicknames.add(killerName);
                    deathCauses.put(killerName, "自刀");
                }
                VoteKillRecord record = new VoteKillRecord(roomCode, currentRound,
                        killerName, targetName != null ? targetName : killerName, "kill", true);
                records.add(record);
            } else {
                String targetName = killer.getKillTarget();
                GamePlayer actualTarget = allPlayers.stream()
                        .filter(p -> p.getPlayerNickname().equals(targetName))
                        .findFirst().orElse(null);

                if (actualTarget != null && !deadNicknames.contains(targetName)) {
                    deadNicknames.add(targetName);
                    deathCauses.put(targetName, "被刀");
                }
                VoteKillRecord record = new VoteKillRecord(roomCode, currentRound,
                        killer.getPlayerNickname(), targetName, "kill", true);
                records.add(record);
            }
        }

        for (GamePlayer player : allPlayers) {
            if (deadNicknames.contains(player.getPlayerNickname())) {
                player.setAlive(false);
                player.setDeathCause(deathCauses.get(player.getPlayerNickname()));
                VoteKillRecord rec = records.stream()
                        .filter(r -> r.getKilledNickname().equals(player.getPlayerNickname()))
                        .findFirst().orElse(null);
                if (rec != null) {
                    voteKillRecordRepository.save(rec);
                }
            }
            player.setHasKilled(false);
            player.setKillTarget(null);
            gamePlayerRepository.save(player);
        }

        settings.setKillComplete(false);
        settings.setVotingFinished(false);

        List<GamePlayer> allPlayersAfterKill = gamePlayerRepository.findByRoomCode(roomCode);
        long aliveCount = allPlayersAfterKill.stream().filter(p -> p.isAlive() && !"judge".equals(p.getIdentity())).count();
        settings.setAlivePlayerCount((int) aliveCount);
        gameSettingsRepository.save(settings);

        System.out.println("finishKilling: Calling checkGameEnd for room: " + roomCode + ", aliveCount: " + aliveCount);
        gameLogicService.checkGameEndWithAliveCount(roomCode, (int) aliveCount);

        room = gameRoomRepository.findByRoomCode(roomCode).orElse(null);
        System.out.println("finishKilling: After checkGameEnd, room status is: " + (room != null ? room.getStatus() : "null"));
        webSocketHandler.notifyRoomUpdate(room);
    }
}