package com.wodi.undercover.repository;

import com.wodi.undercover.entity.Guess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GuessRepository extends JpaRepository<Guess, Long> {

    List<Guess> findByRoomCode(String roomCode);

    List<Guess> findByRoomCodeAndGuesserNickname(String roomCode, String guesserNickname);

    List<Guess> findByRoomCodeAndTargetNickname(String roomCode, String targetNickname);

    List<Guess> findByRoomCodeAndGuesserNicknameAndTargetNickname(String roomCode, String guesserNickname, String targetNickname);

    void deleteByRoomCode(String roomCode);
}