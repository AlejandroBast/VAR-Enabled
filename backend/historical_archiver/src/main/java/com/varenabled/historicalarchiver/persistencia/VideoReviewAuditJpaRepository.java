package com.varenabled.historicalarchiver.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoReviewAuditJpaRepository extends JpaRepository<VideoReviewAuditEntity, Long> {
}
