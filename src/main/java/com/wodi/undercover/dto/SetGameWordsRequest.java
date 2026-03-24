package com.wodi.undercover.dto;

public class SetGameWordsRequest {
    private String civilianWord;
    private String undercoverWord;
    private String blankHint;
    private String restrictedWord;
    private Integer civilianCount;
    private Integer undercoverCount;
    private Integer blankCount;
    private Integer angelCount;

    public SetGameWordsRequest() {}

    public SetGameWordsRequest(String civilianWord, String undercoverWord, String blankHint) {
        this.civilianWord = civilianWord;
        this.undercoverWord = undercoverWord;
        this.blankHint = blankHint;
    }

    public String getCivilianWord() { return civilianWord; }
    public void setCivilianWord(String civilianWord) { this.civilianWord = civilianWord; }

    public String getUndercoverWord() { return undercoverWord; }
    public void setUndercoverWord(String undercoverWord) { this.undercoverWord = undercoverWord; }

    public String getBlankHint() { return blankHint; }
    public void setBlankHint(String blankHint) { this.blankHint = blankHint; }

    public String getRestrictedWord() { return restrictedWord; }
    public void setRestrictedWord(String restrictedWord) { this.restrictedWord = restrictedWord; }

    public Integer getCivilianCount() { return civilianCount; }
    public void setCivilianCount(Integer civilianCount) { this.civilianCount = civilianCount; }

    public Integer getUndercoverCount() { return undercoverCount; }
    public void setUndercoverCount(Integer undercoverCount) { this.undercoverCount = undercoverCount; }

    public Integer getBlankCount() { return blankCount; }
    public void setBlankCount(Integer blankCount) { this.blankCount = blankCount; }

    public Integer getAngelCount() { return angelCount; }
    public void setAngelCount(Integer angelCount) { this.angelCount = angelCount; }
}