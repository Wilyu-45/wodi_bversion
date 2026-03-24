package com.wodi.undercover.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wodi.undercover.dto.ApiResponse;
import com.wodi.undercover.entity.GamePlayer;
import com.wodi.undercover.entity.GameRoom;
import com.wodi.undercover.service.GameRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GameWebSocketHandler implements WebSocketHandler {

    @Autowired
    private GameRoomService gameRoomService;

    @Autowired
    private ObjectMapper objectMapper;

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionNicknameMap = new ConcurrentHashMap<>();
    private final Map<String, String> nicknameSessionMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String nickname = getNicknameFromSession(session);
        if (nickname != null) {
            sessions.put(session.getId(), session);
            sessionNicknameMap.put(session.getId(), nickname);
            nicknameSessionMap.put(nickname, session.getId());

            sendMessage(session, ApiResponse.success("WebSocket连接成功", null));
        } else {
            session.close(CloseStatus.BAD_DATA);
        }
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        if (message instanceof TextMessage) {
            String payload = ((TextMessage) message).getPayload();

            try {
                GameMessage request = objectMapper.readValue(payload, GameMessage.class);
                handleWebSocketMessage(session, request);
            } catch (Exception e) {
                sendMessage(session, ApiResponse.error("消息格式错误"));
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String nickname = sessionNicknameMap.get(session.getId());
        if (nickname != null) {
            nicknameSessionMap.remove(nickname);
        }
        sessionNicknameMap.remove(session.getId());
        sessions.remove(session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String nickname = sessionNicknameMap.get(session.getId());
        if (nickname != null) {
            nicknameSessionMap.remove(nickname);
        }
        sessionNicknameMap.remove(session.getId());
        sessions.remove(session.getId());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    private void handleWebSocketMessage(WebSocketSession session, GameMessage request) {
        String nickname = sessionNicknameMap.get(session.getId());
        if (nickname == null) return;

        List<GamePlayer> players = null;
        for (GamePlayer p : gameRoomService.getRoomPlayers(request.getRoomCode())) {
            if (p.getPlayerNickname().equals(nickname)) {
                players = gameRoomService.getRoomPlayers(request.getRoomCode());
                break;
            }
        }

        if (players == null) return;

        GameRoom room = gameRoomService.getRoom(request.getRoomCode());
        if (room == null) return;

        switch (request.getType()) {
            case "GET_ROOM_INFO":
                sendRoomInfo(room);
                break;
            case "PLAYER_JOINED":
                broadcastRoomInfo(room);
                break;
            case "IDENTITY_ASSIGNED":
            case "WORD_ASSIGNED":
            case "GAME_STARTED":
            case "DESCRIPTION_SUBMITTED":
            case "VOTE_SUBMITTED":
            case "KILL_SUBMITTED":
            case "PHASE_CHANGED":
            case "SPEAKER_CHANGED":
                broadcastRoomInfo(room);
                break;
        }
    }

    private void sendRoomInfo(GameRoom room) {
        broadcastToRoom(room, ApiResponse.success(room));
    }

    private void broadcastRoomInfo(GameRoom room) {
        broadcastToRoom(room, ApiResponse.success(room));
    }

    private void broadcastToRoom(GameRoom room, Object data) {
        List<GamePlayer> players = gameRoomService.getRoomPlayers(room.getRoomCode());

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("roomCode", room.getRoomCode());
        responseData.put("roomName", room.getRoomName());
        responseData.put("hostNickname", room.getHostNickname());
        responseData.put("status", room.getStatus());
        responseData.put("playerCount", room.getPlayerCount());
        responseData.put("currentRound", room.getCurrentRound());
        responseData.put("players", players);

        var roomSettings = room.getSettings();
        if (roomSettings == null) {
            roomSettings = gameRoomService.getSettings(room.getRoomCode());
        }

        if (roomSettings != null) {
            Map<String, Object> settingsMap = new HashMap<>();
            settingsMap.put("whiteBoardGuessActive", roomSettings.getWhiteBoardGuessActive());
            settingsMap.put("whiteBoardGuessCivilianWord", roomSettings.getWhiteBoardGuessCivilianWord());
            settingsMap.put("whiteBoardGuessUndercoverWord", roomSettings.getWhiteBoardGuessUndercoverWord());
            settingsMap.put("whiteBoardGuessCorrect", roomSettings.getWhiteBoardGuessCorrect());
            settingsMap.put("blankHint", roomSettings.getBlankHint());
            settingsMap.put("voteComplete", roomSettings.getVoteComplete());
            settingsMap.put("votingFinished", roomSettings.getVotingFinished());
            settingsMap.put("killComplete", roomSettings.getKillComplete());
            responseData.put("settings", settingsMap);
        }

        for (GamePlayer player : players) {
            String sessionId = nicknameSessionMap.get(player.getPlayerNickname());
            if (sessionId != null) {
                WebSocketSession session = sessions.get(sessionId);
                if (session != null && session.isOpen()) {
                    try {
                        sendMessage(session, ApiResponse.success(responseData));
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    private void sendMessage(WebSocketSession session, ApiResponse<?> response) throws IOException {
        String message = objectMapper.writeValueAsString(response);
        session.sendMessage(new TextMessage(message));
    }

    private String getNicknameFromSession(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null && query.contains("nickname=")) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("nickname=")) {
                    return param.substring("nickname=".length());
                }
            }
        }
        return null;
    }

    public void notifyRoomUpdate(GameRoom room) {
        broadcastRoomInfo(room);
    }

    public static class GameMessage {
        private String type;
        private String roomCode;
        private Object data;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getRoomCode() {
            return roomCode;
        }

        public void setRoomCode(String roomCode) {
            this.roomCode = roomCode;
        }

        public Object getData() {
            return data;
        }

        public void setData(Object data) {
            this.data = data;
        }
    }
}