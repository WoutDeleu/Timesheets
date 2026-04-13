package io.timesheets.service;

import io.timesheets.dto.TimeEntryDto;
import io.timesheets.model.TimeEntry;
import io.timesheets.model.WorkLocation;
import io.timesheets.repository.ProjectRepository;
import io.timesheets.repository.TimeEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
public class TimeEntryService {

    private final TimeEntryRepository timeEntryRepository;
    private final ProjectRepository projectRepository;

    public TimeEntryService(TimeEntryRepository timeEntryRepository,
                            ProjectRepository projectRepository) {
        this.timeEntryRepository = timeEntryRepository;
        this.projectRepository = projectRepository;
    }

    public List<TimeEntry> getEntriesForDate(LocalDate date) {
        return timeEntryRepository.findByEntryDate(date);
    }

public List<TimeEntry> getEntriesForMonth(int year, int month) {
        var start = LocalDate.of(year, month, 1);
        var end = start.with(TemporalAdjusters.lastDayOfMonth());
        return timeEntryRepository.findByEntryDateBetweenOrderByEntryDateAsc(start, end);
    }

    public List<TimeEntry> getEntriesForDateRange(LocalDate start, LocalDate end) {
        return timeEntryRepository.findByEntryDateBetweenOrderByEntryDateAsc(start, end);
    }

    public TimeEntryDto toDto(TimeEntry entry) {
        var breakDuration = entry.getBreakDuration() != null ? entry.getBreakDuration() : BigDecimal.ZERO;
        // Return gross hours (net + break) so the UI shows what the user originally entered
        var grossHours = entry.getHoursWorked().add(breakDuration);
        return new TimeEntryDto(
                entry.getId(),
                entry.getEntryDate(),
                entry.getProject().getId(),
                grossHours,
                entry.getStartTime(),
                entry.getEndTime(),
                entry.getNotes(),
                entry.getWorkLocation(),
                breakDuration
        );
    }

    @Transactional
    public TimeEntry save(TimeEntryDto dto) {
        TimeEntry entry;
        if (dto.id() != null) {
            entry = timeEntryRepository.findById(dto.id())
                    .orElseThrow(() -> new IllegalArgumentException("Uurregistratie niet gevonden: " + dto.id()));
        } else {
            // Look up existing entry by date+project to support upsert from weekly grid
            entry = timeEntryRepository.findByEntryDateAndProjectId(dto.entryDate(), dto.projectId())
                    .orElse(new TimeEntry());
        }

        var project = projectRepository.findById(dto.projectId())
                .orElseThrow(() -> new IllegalArgumentException("Project niet gevonden: " + dto.projectId()));

        var breakDuration = dto.breakDuration() != null ? dto.breakDuration() : BigDecimal.ZERO;
        var netHours = dto.hoursWorked().subtract(breakDuration);

        entry.setEntryDate(dto.entryDate());
        entry.setProject(project);
        entry.setHoursWorked(netHours);
        entry.setBreakDuration(breakDuration);
        entry.setStartTime(dto.startTime());
        entry.setEndTime(dto.endTime());
        entry.setNotes(dto.notes());
        entry.setWorkLocation(dto.workLocation() != null ? dto.workLocation() : WorkLocation.OFFICE);

        return timeEntryRepository.save(entry);
    }

    @Transactional
    public void delete(Long id) {
        timeEntryRepository.deleteById(id);
    }
}
