package com.wodi.undercover.dto;

public class JoinRoomRequest {
    private String roomCode;
    private String playerNickname;

    public JoinRoomRequest() {}

    public JoinRoomRequest(String roomCode, String playerNickname) {
        this.roomCode = roomCode;
        this.playerNickname = playerNickname;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public String getPlayerNickname() {
        return playerNickname;
    }

    public void setPlayerNickname(String playerNickname) {
        this.playerNickname = playerNickname;
    }
}