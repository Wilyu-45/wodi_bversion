package com.wodi.undercover.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "guesses")
public class Guess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_code", nullable = false)
    private String roomCode;

    @Column(name = "guesser_nickname", nullable = false)
    private String guesserNickname;

    @Column(name = "target_nickname", nullable = false)
    private String targetNickname;

    @Column(name = "guess_identity")
    private String guessIdentity;

    @Column(name = "status")
    private String status = "editing";

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    public Guess() {
        this.createTime = LocalDateTime.now();
    }

    public Guess(String roomCode, String guesserNickname, String targetNickname, String guessIdentity) {
        this();
        this.roomCode = roomCode;
        this.guesserNickname = guesserNickname;
        this.targetNickname = targetNickname;
        this.guessIdentity = guessIdentity;
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

    public String getGuesserNickname() {
        return guesserNickname;
    }

    public void setGuesserNickname(String guesserNickname) {
        this.guesserNickname = guesserNickname;
    }

    public String getTargetNickname() {
        return targetNickname;
    }

    public void setTargetNickname(String targetNickname) {
        this.targetNickname = targetNickname;
    }

    public String getGuessIdentity() {
        return guessIdentity;
    }

    public void setGuessIdentity(String guessIdentity) {
        this.guessIdentity = guessIdentity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getIsCorrect() {
        return isCorrect;
    }

    public void setIsCorrect(Boolean isCorrect) {
        this.isCorrect = isCorrect;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}