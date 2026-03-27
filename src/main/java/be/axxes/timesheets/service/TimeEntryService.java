package be.axxes.timesheets.service;

import be.axxes.timesheets.dto.TimeEntryDto;
import be.axxes.timesheets.model.TimeEntry;
import be.axxes.timesheets.model.WorkLocation;
import be.axxes.timesheets.repository.ProjectRepository;
import be.axxes.timesheets.repository.TimeEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
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

    public List<TimeEntry> getEntriesForWeek(LocalDate anyDayInWeek) {
        var monday = anyDayInWeek.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        var friday = monday.plusDays(4);
        return timeEntryRepository.findByEntryDateBetweenOrderByEntryDateAsc(monday, friday);
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
        return new TimeEntryDto(
                entry.getId(),
                entry.getEntryDate(),
                entry.getProject().getId(),
                entry.getHoursWorked(),
                entry.getStartTime(),
                entry.getEndTime(),
                entry.getNotes(),
                entry.getWorkLocation()
        );
    }

    @Transactional
    public TimeEntry save(TimeEntryDto dto) {
        var entry = dto.id() != null
                ? timeEntryRepository.findById(dto.id())
                    .orElseThrow(() -> new IllegalArgumentException("Uurregistratie niet gevonden: " + dto.id()))
                : new TimeEntry();

        var project = projectRepository.findById(dto.projectId())
                .orElseThrow(() -> new IllegalArgumentException("Project niet gevonden: " + dto.projectId()));

        entry.setEntryDate(dto.entryDate());
        entry.setProject(project);
        entry.setHoursWorked(dto.hoursWorked());
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
