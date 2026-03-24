package com.wodi.undercover.dto;

public class CreateRoomRequest {
    private String roomName;
    private String hostNickname;

    public CreateRoomRequest() {}

    public CreateRoomRequest(String roomName, String hostNickname) {
        this.roomName = roomName;
        this.hostNickname = hostNickname;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getHostNickname() {
        return hostNickname;
    }

    public void setHostNickname(String hostNickname) {
        this.hostNickname = hostNickname;
    }
}