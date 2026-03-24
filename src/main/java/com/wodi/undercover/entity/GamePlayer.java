package com.wodi.undercover.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "game_players")
public class GamePlayer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_code", nullable = false)
    private String roomCode;

    @Column(name = "player_nickname", nullable = false)
    private String playerNickname;

    @Column(name = "player_id")
    private Long playerId;

    @Column(name = "statement1")
    private String statement1;

    @Column(name = "statement2")
    private String statement2;

    @Column(name = "statement3")
    private String statement3;

    @Column(name = "is_alive")
    private boolean isAlive = true;

    @Column(name = "death_cause")
    private String deathCause;

    @Column(name = "vote_count")
    private int voteCount = 0;

    @Column(name = "has_voted")
    private boolean hasVoted = false;

    @Column(name = "has_killed")
    private boolean hasKilled = false;

    @Column(name = "kill_target")
    private String killTarget;

    @Column(name = "word")
    private String word;

    @Column(name = "second_word")
    private String secondWord;

    @Column(name = "identity")
    private String identity;

    @Column(name = "speaking_status")
    private int speakingStatus = 0;

    @Column(name = "join_time")
    private LocalDateTime joinTime;

    public GamePlayer() {
        this.joinTime = LocalDateTime.now();
    }

    public GamePlayer(String roomCode, String playerNickname, Long playerId) {
        this();
        this.roomCode = roomCode;
        this.playerNickname = playerNickname;
        this.playerId = playerId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public String getStatement1() {
        return statement1;
    }

    public void setStatement1(String statement1) {
        this.statement1 = statement1;
    }

    public String getStatement2() {
        return statement2;
    }

    public void setStatement2(String statement2) {
        this.statement2 = statement2;
    }

    public String getStatement3() {
        return statement3;
    }

    public void setStatement3(String statement3) {
        this.statement3 = statement3;
    }

    public boolean isAlive() {
        return isAlive;
    }

    public void setAlive(boolean alive) {
        isAlive = alive;
    }

    public int getVoteCount() {
        return voteCount;
    }

    public void setVoteCount(int voteCount) {
        this.voteCount = voteCount;
    }

    public boolean isHasVoted() {
        return hasVoted;
    }

    public void setHasVoted(boolean hasVoted) {
        this.hasVoted = hasVoted;
    }

    public boolean isHasKilled() {
        return hasKilled;
    }

    public void setHasKilled(boolean hasKilled) {
        this.hasKilled = hasKilled;
    }

    public String getKillTarget() {
        return killTarget;
    }

    public void setKillTarget(String killTarget) {
        this.killTarget = killTarget;
    }

    public String getDeathCause() {
        return deathCause;
    }

    public void setDeathCause(String deathCause) {
        this.deathCause = deathCause;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getSecondWord() {
        return secondWord;
    }

    public void setSecondWord(String secondWord) {
        this.secondWord = secondWord;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public int getSpeakingStatus() {
        return speakingStatus;
    }

    public void setSpeakingStatus(int speakingStatus) {
        this.speakingStatus = speakingStatus;
    }

    public LocalDateTime getJoinTime() {
        return joinTime;
    }

    public void setJoinTime(LocalDateTime joinTime) {
        this.joinTime = joinTime;
    }
}