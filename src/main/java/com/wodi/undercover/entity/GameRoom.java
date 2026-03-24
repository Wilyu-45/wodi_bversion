package com.wodi.undercover.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "game_rooms")
public class GameRoom {
    @Id
    @Column(name = "room_code")
    private String roomCode;

    @Column(name = "room_name")
    private String roomName;

    @Column(name = "host_nickname")
    private String hostNickname;

    @Column(name = "status")
    private String status = "waiting";

    @Transient
    private GameSettings settings;

    @Column(name = "player_count")
    private int playerCount = 0;

    @Column(name = "current_round")
    private int currentRound = 1;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    public GameRoom() {
        this.createTime = LocalDateTime.now();
    }

    public GameRoom(String roomCode, String roomName, String hostNickname) {
        this();
        this.roomCode = roomCode;
        this.roomName = roomName;
        this.hostNickname = hostNickname;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getHostNickname() {
        return hostNickname;
    }

    public void setHostNickname(String hostNickname) {
        this.hostNickname = hostNickname;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public void setPlayerCount(int playerCount) {
        this.playerCount = playerCount;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(int currentRound) {
        this.currentRound = currentRound;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public GameSettings getSettings() {
        return settings;
    }

    public void setSettings(GameSettings settings) {
        this.settings = settings;
    }
}