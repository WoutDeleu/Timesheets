package be.axxes.timesheets.service;

import be.axxes.timesheets.dto.TimeEntryDto;
import be.axxes.timesheets.model.ClockSession;
import be.axxes.timesheets.model.ClockSessionStatus;
import be.axxes.timesheets.model.WorkLocation;
import be.axxes.timesheets.repository.ClockSessionRepository;
import be.axxes.timesheets.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class ClockSessionService {

    private final ClockSessionRepository clockSessionRepository;
    private final ProjectRepository projectRepository;

    public ClockSessionService(ClockSessionRepository clockSessionRepository,
                               ProjectRepository projectRepository) {
        this.clockSessionRepository = clockSessionRepository;
        this.projectRepository = projectRepository;
    }

    public Optional<ClockSession> getActiveSession() {
        return clockSessionRepository.findByStatus(ClockSessionStatus.ACTIVE);
    }

    @Transactional
    public ClockSession clockIn(Long projectId, WorkLocation workLocation) {
        if (getActiveSession().isPresent()) {
            throw new IllegalStateException("Er is al een actieve kloksessie.");
        }
        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project niet gevonden."));
        var now = LocalDateTime.now();
        var session = new ClockSession();
        session.setProject(project);
        session.setSessionDate(now.toLocalDate());
        session.setClockInTime(now);
        session.setStatus(ClockSessionStatus.ACTIVE);
        session.setWorkLocation(workLocation != null ? workLocation : WorkLocation.OFFICE);
        return clockSessionRepository.save(session);
    }

    @Transactional
    public TimeEntryDto clockOut() {
        var session = getActiveSession()
                .orElseThrow(() -> new IllegalStateException("Geen actieve kloksessie gevonden."));

        var now = LocalDateTime.now();

        // Auto-end any ongoing break
        if (session.getBreakStart() != null) {
            var breakElapsed = Duration.between(session.getBreakStart(), now).getSeconds();
            session.setTotalBreakSeconds(session.getTotalBreakSeconds() + breakElapsed);
            session.setBreakStart(null);
        }

        session.setClockOutTime(now);
        session.setStatus(ClockSessionStatus.COMPLETED);
        clockSessionRepository.save(session);

        return buildTimeEntryDto(session);
    }

    @Transactional
    public ClockSession startBreak() {
        var session = getActiveSession()
                .orElseThrow(() -> new IllegalStateException("Geen actieve kloksessie gevonden."));
        if (session.getBreakStart() != null) {
            throw new IllegalStateException("Er loopt al een pauze.");
        }
        session.setBreakStart(LocalDateTime.now());
        return clockSessionRepository.save(session);
    }

    @Transactional
    public ClockSession endBreak() {
        var session = getActiveSession()
                .orElseThrow(() -> new IllegalStateException("Geen actieve kloksessie gevonden."));
        if (session.getBreakStart() == null) {
            throw new IllegalStateException("Er is geen pauze om te beëindigen.");
        }
        var breakElapsed = Duration.between(session.getBreakStart(), LocalDateTime.now()).getSeconds();
        session.setTotalBreakSeconds(session.getTotalBreakSeconds() + breakElapsed);
        session.setBreakStart(null);
        return clockSessionRepository.save(session);
    }

    @Transactional
    public void cancel() {
        var session = getActiveSession()
                .orElseThrow(() -> new IllegalStateException("Geen actieve kloksessie gevonden."));
        session.setBreakStart(null);
        session.setStatus(ClockSessionStatus.CANCELLED);
        clockSessionRepository.save(session);
    }

    private TimeEntryDto buildTimeEntryDto(ClockSession session) {
        var elapsed = Duration.between(session.getClockInTime(), session.getClockOutTime());
        var totalSeconds = elapsed.getSeconds();
        var breakSeconds = session.getTotalBreakSeconds();

        var grossHours = BigDecimal.valueOf(totalSeconds)
                .divide(BigDecimal.valueOf(3600), 2, RoundingMode.HALF_UP);
        var breakHours = BigDecimal.valueOf(breakSeconds)
                .divide(BigDecimal.valueOf(3600), 2, RoundingMode.HALF_UP);

        var startTime = session.getClockInTime().toLocalTime();
        var endTime = session.getClockOutTime().toLocalTime();

        return new TimeEntryDto(
                null,
                session.getSessionDate(),
                session.getProject().getId(),
                grossHours,
                startTime,
                endTime,
                session.getNotes(),
                session.getWorkLocation(),
                breakHours
        );
    }
}
