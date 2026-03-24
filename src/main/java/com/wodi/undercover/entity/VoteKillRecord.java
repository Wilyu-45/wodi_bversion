package com.wodi.undercover.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "vote_kill_record")
public class VoteKillRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_code", nullable = false)
    private String roomCode;

    @Column(name = "round_number")
    private Integer roundNumber;

    @Column(name = "voter_nickname")
    private String voterNickname;

    @Column(name = "voted_nickname")
    private String votedNickname;

    @Column(name = "killer_nickname")
    private String killerNickname;

    @Column(name = "killed_nickname")
    private String killedNickname;

    @Column(name = "action_type")
    private String actionType;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    public VoteKillRecord() {
        this.createTime = LocalDateTime.now();
    }

    public VoteKillRecord(String roomCode, Integer roundNumber, String voterNickname, String votedNickname, String actionType) {
        this();
        this.roomCode = roomCode;
        this.roundNumber = roundNumber;
        this.voterNickname = voterNickname;
        this.votedNickname = votedNickname;
        this.actionType = actionType;
    }

    public VoteKillRecord(String roomCode, Integer roundNumber, String killerNickname, String killedNickname, String actionType, boolean isKill) {
        this();
        this.roomCode = roomCode;
        this.roundNumber = roundNumber;
        this.killerNickname = killerNickname;
        this.killedNickname = killedNickname;
        this.actionType = actionType;
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

    public Integer getRoundNumber() {
        return roundNumber;
    }

    public void setRoundNumber(Integer roundNumber) {
        this.roundNumber = roundNumber;
    }

    public String getVoterNickname() {
        return voterNickname;
    }

    public void setVoterNickname(String voterNickname) {
        this.voterNickname = voterNickname;
    }

    public String getVotedNickname() {
        return votedNickname;
    }

    public void setVotedNickname(String votedNickname) {
        this.votedNickname = votedNickname;
    }

    public String getKillerNickname() {
        return killerNickname;
    }

    public void setKillerNickname(String killerNickname) {
        this.killerNickname = killerNickname;
    }

    public String getKilledNickname() {
        return killedNickname;
    }

    public void setKilledNickname(String killedNickname) {
        this.killedNickname = killedNickname;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
