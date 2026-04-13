package io.timesheets.repository;

import io.timesheets.model.TimeEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TimeEntryRepository extends JpaRepository<TimeEntry, Long> {

    List<TimeEntry> findByEntryDateBetweenOrderByEntryDateAsc(LocalDate start, LocalDate end);

    List<TimeEntry> findByEntryDate(LocalDate date);

    List<TimeEntry> findByProjectIdAndEntryDateBetween(Long projectId, LocalDate start, LocalDate end);

    Optional<TimeEntry> findByEntryDateAndProjectId(LocalDate date, Long projectId);
}
