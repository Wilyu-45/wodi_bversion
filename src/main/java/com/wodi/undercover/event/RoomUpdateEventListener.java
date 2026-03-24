package com.wodi.undercover.event;

import com.wodi.undercover.entity.GameRoom;
import com.wodi.undercover.service.GameRoomService;
import com.wodi.undercover.websocket.GameWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class RoomUpdateEventListener {

    @Autowired
    private GameWebSocketHandler webSocketHandler;

    @Autowired
    private GameRoomService gameRoomService;

    @Async
    @EventListener
    public void handleRoomUpdate(RoomUpdateEvent event) {
        String roomCode = event.getRoom().getRoomCode();
        if (roomCode != null) {
            GameRoom room = gameRoomService.getRoom(roomCode);
            if (room != null) {
                webSocketHandler.notifyRoomUpdate(room);
            }
        }
    }
}