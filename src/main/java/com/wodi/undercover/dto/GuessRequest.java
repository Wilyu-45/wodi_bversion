package com.wodi.undercover.dto;

public class GuessRequest {
    private String guesserNickname;
    private String targetNickname;
    private String guessIdentity;
    private String status;

    public GuessRequest() {}

    public GuessRequest(String guesserNickname, String targetNickname, String guessIdentity) {
        this.guesserNickname = guesserNickname;
        this.targetNickname = targetNickname;
        this.guessIdentity = guessIdentity;
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
}