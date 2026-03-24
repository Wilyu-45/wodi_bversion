package com.wodi.undercover.service;

import com.wodi.undercover.entity.GamePlayer;
import com.wodi.undercover.entity.GameRoom;
import com.wodi.undercover.entity.GameSettings;
import com.wodi.undercover.entity.VoteKillRecord;
import com.wodi.undercover.repository.GamePlayerRepository;
import com.wodi.undercover.repository.GameRoomRepository;
import com.wodi.undercover.repository.GameSettingsRepository;
import com.wodi.undercover.repository.VoteKillRecordRepository;
import com.wodi.undercover.websocket.GameWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GameLogicService {
    @Autowired
    private GameRoomRepository gameRoomRepository;

    @Autowired
    private GamePlayerRepository gamePlayerRepository;

    @Autowired
    private GameSettingsRepository gameSettingsRepository;

    @Autowired
    private VoteKillRecordRepository voteKillRecordRepository;

    @Autowired
    @Lazy
    private GameWebSocketHandler webSocketHandler;

    private final Map<String, Map<Long, Long>> roomVotes = new HashMap<>();
    private final Map<String, Map<Long, Long>> roomKills = new HashMap<>();
    private final Map<String, List<String>> roomDescriptions = new HashMap<>();
    private final Map<String, Integer> roomCurrentSpeaker = new HashMap<>();

    @Transactional
    public boolean submitDescription(String roomCode, Long playerId, String description, int round) {
        GameRoom room = gameRoomRepository.findByRoomCode(roomCode).orElse(null);
        if (room == null || !"playing".equals(room.getStatus())) {
            return false;
        }

        GamePlayer speaker = gamePlayerRepository.findById(playerId).orElse(null);
        if (speaker == null || !speaker.getRoomCode().equals(roomCode)) {
            return false;
        }

        Integer currentRound = roomDescriptions.computeIfAbsent(roomCode, k -> new java.util.ArrayList<>()).size() / getAlivePlayersCount(roomCode);
        if (currentRound >= 3) {
            return false;
        }

        if (round == 1) {
            speaker.setStatement1(description);
        } else if (round == 2) {
            speaker.setStatement2(description);
        } else if (round == 3) {
            speaker.setStatement3(description);
        }

        gamePlayerRepository.save(speaker);
        return true;
    }

    @Transactional
    public boolean vote(String roomCode, Long voterId, Long targetId) {
        GameRoom room = gameRoomRepository.findByRoomCode(roomCode).orElse(null);
        if (room == null || !"playing".equals(room.getStatus())) {
            return false;
        }

        GamePlayer voter = gamePlayerRepository.findById(voterId).orElse(null);
        GamePlayer target = gamePlayerRepository.findById(targetId).orElse(null);

        if (voter == null || target == null) {
            return false;
        }

        if (!voter.isAlive() || !target.isAlive()) {
            return false;
        }

        GameSettings settings = gameSettingsRepository.findByRoomCode(roomCode).orElse(null);
        if (settings == null || !settings.getVoteComplete()) {
            return false;
        }

        if (voter.isHasVoted() || voter.isHasKilled()) {
            return false;
        }

        if (!roomVotes.containsKey(roomCode)) {
            roomVotes.put(roomCode, new HashMap<>());
        }

        if (roomVotes.get(roomCode).containsKey(voterId)) {
            return false;
        }

        voter.setHasVoted(true);
        gamePlayerRepository.save(voter);
        roomVotes.get(roomCode).put(voterId, targetId);

        GameRoom roomObj = gameRoomRepository.findByRoomCode(roomCode).orElse(null);
        VoteKillRecord record = new VoteKillRecord(roomCode, roomObj != null ? roomObj.getCurrentRound() : 1,
                voter.getPlayerNickname(), target.getPlayerNickname(), "vote");
        voteKillRecordRepository.save(record);

        return true;
    }

    @Transactional
    public boolean kill(String roomCode, Long killerId, Long targetId) {
        GameRoom room = gameRoomRepository.findByRoomCode(roomCode).orElse(null);
        if (room == null || !"playing".equals(room.getStatus())) {
            return false;
        }

        GamePlayer killer = gamePlayerRepository.findById(killerId).orElse(null);
        GamePlayer target = gamePlayerRepository.findById(targetId).orElse(null);

        if (killer == null || target == null) {
            return false;
        }

        if (!killer.isAlive() || !target.isAlive()) {
            return false;
        }

        if ("judge".equals(killer.getIdentity())) {
            target.setAlive(false);
            gamePlayerRepository.save(target);
        } else {
            killer.setAlive(false);
            gamePlayerRepository.save(killer);
        }

        checkGameEnd(roomCode);
        return true;
    }

    @Transactional
    public boolean eliminatePlayer(String roomCode, Long playerId) {
        GamePlayer player = gamePlayerRepository.findById(playerId).orElse(null);
        if (player == null || !player.getRoomCode().equals(roomCode)) {
            return false;
        }

        player.setAlive(false);
        player.setDeathCause("投票");
        gamePlayerRepository.save(player);

        checkGameEnd(roomCode);
        return true;
    }

    private int getAlivePlayersCount(String roomCode) {
        List<GamePlayer> players = gamePlayerRepository.findByRoomCode(roomCode);
        return (int) players.stream().filter(GamePlayer::isAlive).count();
    }

    private boolean checkVotingComplete(String roomCode) {
        int alivePlayers = getAlivePlayersCount(roomCode);
        Map<Long, Long> votes = roomVotes.getOrDefault(roomCode, new HashMap<>());
        return votes.size() >= alivePlayers;
    }

    public void processVotingResult(String roomCode) {
        Map<Long, Integer> voteCountMap = new HashMap<>();
        Map<Long, Long> votes = roomVotes.getOrDefault(roomCode, new HashMap<>());

        for (Long targetId : votes.values()) {
            voteCountMap.put(targetId, voteCountMap.getOrDefault(targetId, 0) + 1);
        }

        for (Map.Entry<Long, Integer> entry : voteCountMap.entrySet()) {
            Long playerId = entry.getKey();
            Integer count = entry.getValue();
            GamePlayer player = gamePlayerRepository.findById(playerId).orElse(null);
            if (player != null) {
                player.setVoteCount(player.getVoteCount() + count);
                gamePlayerRepository.save(player);
            }
        }

        if (voteCountMap.isEmpty()) {
            votes.clear();
            return;
        }

        int maxVotes = voteCountMap.values().stream().max(Integer::compareTo).orElse(0);
        long countOfMaxVotes = voteCountMap.values().stream().filter(v -> v == maxVotes).count();

        if (countOfMaxVotes > 1) {
            System.out.println("processVotingResult: 平票 " + maxVotes + " 票，共 " + countOfMaxVotes + " 人，本轮不淘汰任何人");
            votes.clear();
            return;
        }

        Long eliminatedPlayerId = voteCountMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        if (eliminatedPlayerId != null) {
            eliminatePlayer(roomCode, eliminatedPlayerId);
        }

        votes.clear();
    }

    public void checkGameEnd(String roomCode) {
        List<GamePlayer> players = gamePlayerRepository.findByRoomCode(roomCode);
        List<GamePlayer> alivePlayers = players.stream()
                .filter(GamePlayer::isAlive)
                .collect(Collectors.toList());

        long aliveCount = alivePlayers.size();
        long undercoverCount = alivePlayers.stream()
                .filter(p -> "undercover".equals(p.getIdentity()))
                .count();
        long blankCount = alivePlayers.stream()
                .filter(p -> "blank".equals(p.getIdentity()))
                .count();

        GameRoom room = gameRoomRepository.findByRoomCode(roomCode).orElse(null);
        GameSettings settings = gameSettingsRepository.findByRoomCode(roomCode).orElse(null);
        if (room == null || settings == null) {
            System.out.println("checkGameEnd: room or settings is null");
            return;
        }

        System.out.println("checkGameEnd DEBUG - roomCode: " + roomCode + 
            ", aliveCount: " + aliveCount + 
            ", undercoverCount: " + undercoverCount + 
            ", blankCount: " + blankCount +
            ", totalPlayerCount: " + settings.getTotalPlayerCount() +
            ", currentStatus: " + room.getStatus());

        if (undercoverCount == 0 && blankCount == 0) {
            System.out.println("checkGameEnd: Setting civilian_win");
            room.setStatus("civilian_win");
            gameRoomRepository.save(room);
            webSocketHandler.notifyRoomUpdate(room);
            return;
        }

        if (undercoverCount == 0 && blankCount > 0) {
            System.out.println("checkGameEnd: Setting white_board_guess");
            room.setStatus("white_board_guess");
            gameRoomRepository.save(room);
            webSocketHandler.notifyRoomUpdate(room);
            return;
        }

        int totalPlayerCount = settings.getTotalPlayerCount() != null ? settings.getTotalPlayerCount() : (int) players.stream().filter(p -> !"judge".equals(p.getIdentity())).count();
        int winThreshold = totalPlayerCount < 6 ? 2 : 3;
        System.out.println("checkGameEnd: totalPlayerCount: " + totalPlayerCount + ", winThreshold: " + winThreshold);
        
        if (aliveCount <= winThreshold && undercoverCount > 0) {
            System.out.println("checkGameEnd: Setting undercover_win!");
            room.setStatus("undercover_win");
            gameRoomRepository.save(room);
            webSocketHandler.notifyRoomUpdate(room);
        } else {
            System.out.println("checkGameEnd: No win condition met, aliveCount: " + aliveCount + ", winThreshold: " + winThreshold + ", undercoverCount: " + undercoverCount);
        }
    }

    public void checkGameEndWithAliveCount(String roomCode, int aliveCount) {
        List<GamePlayer> players = gamePlayerRepository.findByRoomCode(roomCode);
        List<GamePlayer> alivePlayers = players.stream()
                .filter(GamePlayer::isAlive)
                .collect(Collectors.toList());

        long undercoverCount = alivePlayers.stream()
                .filter(p -> "undercover".equals(p.getIdentity()))
                .count();
        long blankCount = alivePlayers.stream()
                .filter(p -> "blank".equals(p.getIdentity()))
                .count();

        GameRoom room = gameRoomRepository.findByRoomCode(roomCode).orElse(null);
        GameSettings settings = gameSettingsRepository.findByRoomCode(roomCode).orElse(null);
        if (room == null || settings == null) {
            System.out.println("checkGameEndWithAliveCount: room or settings is null");
            return;
        }

        System.out.println("checkGameEndWithAliveCount DEBUG - roomCode: " + roomCode + 
            ", aliveCount: " + aliveCount + 
            ", undercoverCount: " + undercoverCount + 
            ", blankCount: " + blankCount +
            ", totalPlayerCount: " + settings.getTotalPlayerCount() +
            ", currentStatus: " + room.getStatus());

        if (undercoverCount == 0 && blankCount == 0) {
            System.out.println("checkGameEndWithAliveCount: Setting civilian_win");
            room.setStatus("civilian_win");
            gameRoomRepository.save(room);
            webSocketHandler.notifyRoomUpdate(room);
            return;
        }

        if (undercoverCount == 0 && blankCount > 0) {
            System.out.println("checkGameEndWithAliveCount: Setting white_board_guess");
            room.setStatus("white_board_guess");
            gameRoomRepository.save(room);
            webSocketHandler.notifyRoomUpdate(room);
            return;
        }

        int totalPlayerCount = settings.getTotalPlayerCount() != null ? settings.getTotalPlayerCount() : (int) players.stream().filter(p -> !"judge".equals(p.getIdentity())).count();
        int winThreshold = totalPlayerCount < 6 ? 2 : 3;
        System.out.println("checkGameEndWithAliveCount: totalPlayerCount: " + totalPlayerCount + ", winThreshold: " + winThreshold);
        
        if (aliveCount <= winThreshold && undercoverCount > 0) {
            System.out.println("checkGameEndWithAliveCount: Setting undercover_win! aliveCount=" + aliveCount + ", winThreshold=" + winThreshold);
            room.setStatus("undercover_win");
            gameRoomRepository.save(room);
            webSocketHandler.notifyRoomUpdate(room);
        } else {
            System.out.println("checkGameEndWithAliveCount: No win condition met, aliveCount: " + aliveCount + ", winThreshold: " + winThreshold + ", undercoverCount: " + undercoverCount);
        }
    }

    public GamePlayer getPlayer(String roomCode, Long playerId) {
        return gamePlayerRepository.findById(playerId).orElse(null);
    }

    public List<GamePlayer> getAlivePlayers(String roomCode) {
        List<GamePlayer> players = gamePlayerRepository.findByRoomCode(roomCode);
        return players.stream()
                .filter(GamePlayer::isAlive)
                .collect(Collectors.toList());
    }
}