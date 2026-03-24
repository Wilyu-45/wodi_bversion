package com.wodi.undercover.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "game_settings")
public class GameSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_code", nullable = false, unique = true)
    private String roomCode;

    @Column(name = "civilian_word")
    private String civilianWord;

    @Column(name = "undercover_word")
    private String undercoverWord;

    @Column(name = "blank_hint")
    private String blankHint;

    @Column(name = "restricted_word")
    private String restrictedWord;

    @Column(name = "civilian_count")
    private Integer civilianCount;

    @Column(name = "undercover_count")
    private Integer undercoverCount;

    @Column(name = "blank_count")
    private Integer blankCount;

    @Column(name = "angel_count")
    private Integer angelCount;

    @Column(name = "total_player_count")
    private Integer totalPlayerCount;

    @Column(name = "alive_player_count")
    private Integer alivePlayerCount;

    @Column(name = "game_phase")
    private String gamePhase = "day";

    @Column(name = "vote_complete")
    private Boolean votingActive = false;

    @Column(name = "kill_complete")
    private Boolean killingActive = false;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    public GameSettings() {
        this.createTime = LocalDateTime.now();
    }

    public GameSettings(String roomCode, String civilianWord, String undercoverWord, String blankHint) {
        this();
        this.roomCode = roomCode;
        this.civilianWord = civilianWord;
        this.undercoverWord = undercoverWord;
        this.blankHint = blankHint;
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

    public String getCivilianWord() {
        return civilianWord;
    }

    public void setCivilianWord(String civilianWord) {
        this.civilianWord = civilianWord;
    }

    public String getUndercoverWord() {
        return undercoverWord;
    }

    public void setUndercoverWord(String undercoverWord) {
        this.undercoverWord = undercoverWord;
    }

    public String getBlankHint() {
        return blankHint;
    }

    public void setBlankHint(String blankHint) {
        this.blankHint = blankHint;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public Integer getCivilianCount() {
        return civilianCount;
    }

    public void setCivilianCount(Integer civilianCount) {
        this.civilianCount = civilianCount;
    }

    public Integer getUndercoverCount() {
        return undercoverCount;
    }

    public void setUndercoverCount(Integer undercoverCount) {
        this.undercoverCount = undercoverCount;
    }

    public Integer getBlankCount() {
        return blankCount;
    }

    public void setBlankCount(Integer blankCount) {
        this.blankCount = blankCount;
    }

    public Integer getAngelCount() {
        return angelCount;
    }

    public void setAngelCount(Integer angelCount) {
        this.angelCount = angelCount;
    }

    public Integer getTotalPlayerCount() {
        return totalPlayerCount;
    }

    public void setTotalPlayerCount(Integer totalPlayerCount) {
        this.totalPlayerCount = totalPlayerCount;
    }

    public Integer getAlivePlayerCount() {
        return alivePlayerCount;
    }

    public void setAlivePlayerCount(Integer alivePlayerCount) {
        this.alivePlayerCount = alivePlayerCount;
    }

    public String getGamePhase() {
        return gamePhase;
    }

    public void setGamePhase(String gamePhase) {
        this.gamePhase = gamePhase;
    }

    public Boolean getVoteComplete() {
        return votingActive;
    }

    public void setVoteComplete(Boolean voteComplete) {
        this.votingActive = voteComplete;
    }

    public Boolean getKillComplete() {
        return killingActive;
    }

    public void setKillComplete(Boolean killComplete) {
        this.killingActive = killComplete;
    }

    public String getRestrictedWord() {
        return restrictedWord;
    }

    public void setRestrictedWord(String restrictedWord) {
        this.restrictedWord = restrictedWord;
    }

    @Column(name = "white_board_guess_active")
    private Boolean whiteBoardGuessActive = false;

    @Column(name = "voting_finished")
    private Boolean votingFinished = false;

    @Column(name = "white_board_guess_correct")
    private Boolean whiteBoardGuessCorrect = null;

    public Boolean getWhiteBoardGuessActive() {
        return whiteBoardGuessActive;
    }

    public void setWhiteBoardGuessActive(Boolean whiteBoardGuessActive) {
        this.whiteBoardGuessActive = whiteBoardGuessActive;
    }

    public Boolean getVotingFinished() {
        return votingFinished;
    }

    public void setVotingFinished(Boolean votingFinished) {
        this.votingFinished = votingFinished;
    }

    public Boolean getWhiteBoardGuessCorrect() {
        return whiteBoardGuessCorrect;
    }

    public void setWhiteBoardGuessCorrect(Boolean whiteBoardGuessCorrect) {
        this.whiteBoardGuessCorrect = whiteBoardGuessCorrect;
    }

    @Column(name = "white_board_guess_civilian_word")
    private String whiteBoardGuessCivilianWord;

    @Column(name = "white_board_guess_undercover_word")
    private String whiteBoardGuessUndercoverWord;

    public String getWhiteBoardGuessCivilianWord() {
        return whiteBoardGuessCivilianWord;
    }

    public void setWhiteBoardGuessCivilianWord(String whiteBoardGuessCivilianWord) {
        this.whiteBoardGuessCivilianWord = whiteBoardGuessCivilianWord;
    }

    public String getWhiteBoardGuessUndercoverWord() {
        return whiteBoardGuessUndercoverWord;
    }

    public void setWhiteBoardGuessUndercoverWord(String whiteBoardGuessUndercoverWord) {
        this.whiteBoardGuessUndercoverWord = whiteBoardGuessUndercoverWord;
    }
}