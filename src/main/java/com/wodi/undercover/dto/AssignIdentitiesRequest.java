package com.wodi.undercover.dto;

public class AssignIdentitiesRequest {
    private int civilianCount;
    private int undercoverCount;
    private int blankCount;
    private int angelCount;

    public AssignIdentitiesRequest() {}

    public AssignIdentitiesRequest(int civilianCount, int undercoverCount, int blankCount, int angelCount) {
        this.civilianCount = civilianCount;
        this.undercoverCount = undercoverCount;
        this.blankCount = blankCount;
        this.angelCount = angelCount;
    }

    public int getCivilianCount() {
        return civilianCount;
    }

    public void setCivilianCount(int civilianCount) {
        this.civilianCount = civilianCount;
    }

    public int getUndercoverCount() {
        return undercoverCount;
    }

    public void setUndercoverCount(int undercoverCount) {
        this.undercoverCount = undercoverCount;
    }

    public int getBlankCount() {
        return blankCount;
    }

    public void setBlankCount(int blankCount) {
        this.blankCount = blankCount;
    }

    public int getAngelCount() {
        return angelCount;
    }

    public void setAngelCount(int angelCount) {
        this.angelCount = angelCount;
    }
}