package com.wodi.undercover.dto;

public class KillRequest {
    private String killerNickname;
    private String targetNickname;

    public KillRequest() {}

    public KillRequest(String killerNickname, String targetNickname) {
        this.killerNickname = killerNickname;
        this.targetNickname = targetNickname;
    }

    public String getKillerNickname() {
        return killerNickname;
    }

    public void setKillerNickname(String killerNickname) {
        this.killerNickname = killerNickname;
    }

    public String getTargetNickname() {
        return targetNickname;
    }

    public void setTargetNickname(String targetNickname) {
        this.targetNickname = targetNickname;
    }
}