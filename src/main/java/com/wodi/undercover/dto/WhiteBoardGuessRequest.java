package com.wodi.undercover.dto;

public class WhiteBoardGuessRequest {
    private String civilianWord;
    private String undercoverWord;
    private String playerNickname;

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

    public String getPlayerNickname() {
        return playerNickname;
    }

    public void setPlayerNickname(String playerNickname) {
        this.playerNickname = playerNickname;
    }
}
