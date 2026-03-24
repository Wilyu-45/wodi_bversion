package com.wodi.undercover.dto;

public class VoteRequest {
    private String voterNickname;
    private String targetNickname;

    public VoteRequest() {}

    public VoteRequest(String voterNickname, String targetNickname) {
        this.voterNickname = voterNickname;
        this.targetNickname = targetNickname;
    }

    public String getVoterNickname() {
        return voterNickname;
    }

    public void setVoterNickname(String voterNickname) {
        this.voterNickname = voterNickname;
    }

    public String getTargetNickname() {
        return targetNickname;
    }

    public void setTargetNickname(String targetNickname) {
        this.targetNickname = targetNickname;
    }
}