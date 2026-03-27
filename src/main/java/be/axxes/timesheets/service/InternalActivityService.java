package be.axxes.timesheets.service;

import be.axxes.timesheets.dto.InternalActivityDto;
import be.axxes.timesheets.model.InternalActivity;
import be.axxes.timesheets.repository.InternalActivityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
public class InternalActivityService {

    private final InternalActivityRepository internalActivityRepository;

    public InternalActivityService(InternalActivityRepository internalActivityRepository) {
        this.internalActivityRepository = internalActivityRepository;
    }

    public List<InternalActivity> getEntriesForDate(LocalDate date) {
        return internalActivityRepository.findByActivityDate(date);
    }

    public List<InternalActivity> getEntriesForMonth(int year, int month) {
        var start = LocalDate.of(year, month, 1);
        var end = start.with(TemporalAdjusters.lastDayOfMonth());
        return internalActivityRepository.findByActivityDateBetweenOrderByActivityDateAsc(start, end);
    }

    public List<InternalActivity> getEntriesForYear(int year) {
        var start = LocalDate.of(year, 1, 1);
        var end = LocalDate.of(year, 12, 31);
        return internalActivityRepository.findByActivityDateBetweenOrderByActivityDateAsc(start, end);
    }

    public List<InternalActivity> getEntriesForDateRange(LocalDate start, LocalDate end) {
        return internalActivityRepository.findByActivityDateBetweenOrderByActivityDateAsc(start, end);
    }

    @Transactional
    public InternalActivity save(InternalActivityDto dto) {
        var entry = dto.getId() != null
                ? internalActivityRepository.findById(dto.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Interne activiteit niet gevonden: " + dto.getId()))
                : new InternalActivity();

        entry.setActivityDate(dto.getActivityDate());
        entry.setHours(dto.getHours());
        entry.setDescription(dto.getDescription());

        return internalActivityRepository.save(entry);
    }

    @Transactional
    public void delete(Long id) {
        internalActivityRepository.deleteById(id);
    }
}
