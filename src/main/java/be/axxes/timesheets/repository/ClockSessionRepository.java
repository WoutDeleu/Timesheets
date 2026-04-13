package be.axxes.timesheets.repository;

import be.axxes.timesheets.model.ClockSession;
import be.axxes.timesheets.model.ClockSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClockSessionRepository extends JpaRepository<ClockSession, Long> {

    Optional<ClockSession> findByStatus(ClockSessionStatus status);
}
