package com.wodi.undercover.repository;

import com.wodi.undercover.entity.GamePlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GamePlayerRepository extends JpaRepository<GamePlayer, Long> {
    List<GamePlayer> findByRoomCode(String roomCode);
    Optional<GamePlayer> findByRoomCodeAndPlayerNickname(String roomCode, String playerNickname);
    Optional<GamePlayer> findByRoomCodeAndPlayerId(String roomCode, Long playerId);
    void deleteByRoomCode(String roomCode);
}