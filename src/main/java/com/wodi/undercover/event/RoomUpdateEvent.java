package com.wodi.undercover.event;

import com.wodi.undercover.entity.GameRoom;

public class RoomUpdateEvent {

    private final GameRoom room;
    private final String eventType;

    public RoomUpdateEvent(GameRoom room, String eventType) {
        this.room = room;
        this.eventType = eventType;
    }

    public GameRoom getRoom() {
        return room;
    }

    public String getEventType() {
        return eventType;
    }
}