package io.timesheets.repository;

import io.timesheets.model.ClockSession;
import io.timesheets.model.ClockSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClockSessionRepository extends JpaRepository<ClockSession, Long> {

    Optional<ClockSession> findByStatus(ClockSessionStatus status);
}
