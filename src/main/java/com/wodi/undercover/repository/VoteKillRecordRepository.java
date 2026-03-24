package com.wodi.undercover.repository;

import com.wodi.undercover.entity.VoteKillRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoteKillRecordRepository extends JpaRepository<VoteKillRecord, Long> {

    List<VoteKillRecord> findByRoomCodeOrderByRoundNumberAscCreateTimeAsc(String roomCode);

    List<VoteKillRecord> findByRoomCodeAndRoundNumber(String roomCode, Integer roundNumber);
}
