package com.varenabled.historicalarchiver.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MatchEventLogJpaRepository extends JpaRepository<MatchEventLogEntity, Long> {

    Optional<MatchEventLogEntity> findByEventId(String eventId);
}
