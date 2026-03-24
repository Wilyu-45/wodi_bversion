package com.wodi.undercover.repository;

import com.wodi.undercover.entity.GameSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GameSettingsRepository extends JpaRepository<GameSettings, Long> {
    Optional<GameSettings> findByRoomCode(String roomCode);
    void deleteByRoomCode(String roomCode);
}