package io.timesheets.service;

import io.timesheets.dto.LeaveEntryDto;
import io.timesheets.model.LeaveEntry;
import io.timesheets.model.LeaveType;
import io.timesheets.repository.LeaveEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
public class LeaveService {

    private final LeaveEntryRepository leaveEntryRepository;

    public LeaveService(LeaveEntryRepository leaveEntryRepository) {
        this.leaveEntryRepository = leaveEntryRepository;
    }

    public List<LeaveEntry> getEntriesForMonth(int year, int month) {
        var start = LocalDate.of(year, month, 1);
        var end = start.with(TemporalAdjusters.lastDayOfMonth());
        return leaveEntryRepository.findByEntryDateBetweenOrderByEntryDateAsc(start, end);
    }

    public List<LeaveEntry> getEntriesForYear(int year) {
        var start = LocalDate.of(year, 1, 1);
        var end = LocalDate.of(year, 12, 31);
        return leaveEntryRepository.findByEntryDateBetweenOrderByEntryDateAsc(start, end);
    }

    public List<LeaveEntry> getEntriesForDateRange(LocalDate start, LocalDate end) {
        return leaveEntryRepository.findByEntryDateBetweenOrderByEntryDateAsc(start, end);
    }

    public long countVacationDaysInYear(int year) {
        var start = LocalDate.of(year, 1, 1);
        var end = LocalDate.of(year, 12, 31);
        return leaveEntryRepository.countByLeaveTypeAndEntryDateBetween(LeaveType.VACATION, start, end);
    }

    public long countAdvDaysInYear(int year) {
        var start = LocalDate.of(year, 1, 1);
        var end = LocalDate.of(year, 12, 31);
        return leaveEntryRepository.countByLeaveTypeAndEntryDateBetween(LeaveType.ADV, start, end);
    }

    public long countSickDaysWithoutNoteInYear(int year) {
        var start = LocalDate.of(year, 1, 1);
        var end = LocalDate.of(year, 12, 31);
        return leaveEntryRepository.countByLeaveTypeAndDoctorsNoteAndEntryDateBetween(LeaveType.SICK, false, start, end);
    }

    @Transactional
    public LeaveEntry save(LeaveEntryDto dto) {
        var entry = dto.id() != null
                ? leaveEntryRepository.findById(dto.id())
                    .orElseThrow(() -> new IllegalArgumentException("Verlofregistratie niet gevonden: " + dto.id()))
                : new LeaveEntry();

        entry.setEntryDate(dto.entryDate());
        entry.setLeaveType(dto.leaveType());
        entry.setHours(dto.hours());
        entry.setNotes(dto.notes());
        entry.setDoctorsNote(dto.leaveType() != LeaveType.SICK || dto.doctorsNote() == null || dto.doctorsNote());

        return leaveEntryRepository.save(entry);
    }

    @Transactional
    public void delete(Long id) {
        leaveEntryRepository.deleteById(id);
    }
}
