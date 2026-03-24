package com.wodi.undercover.repository;

import com.wodi.undercover.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
    Optional<Player> findByNickname(String nickname);
    Optional<Player> findByBindCode(String bindCode);
    boolean existsByNickname(String nickname);
    boolean existsByBindCode(String bindCode);
}