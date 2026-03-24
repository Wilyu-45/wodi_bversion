package com.wodi.undercover.dto;

public class RegisterPlayerRequest {
    private String nickname;

    public RegisterPlayerRequest() {}

    public RegisterPlayerRequest(String nickname) {
        this.nickname = nickname;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}