package com.wodi.undercover.dto;

public class SubmitDescriptionRequest {
    private String playerNickname;
    private String description;
    private int round;

    public SubmitDescriptionRequest() {}

    public SubmitDescriptionRequest(String playerNickname, String description, int round) {
        this.playerNickname = playerNickname;
        this.description = description;
        this.round = round;
    }

    public String getPlayerNickname() {
        return playerNickname;
    }

    public void setPlayerNickname(String playerNickname) {
        this.playerNickname = playerNickname;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }
}