package io.timesheets.repository;

import io.timesheets.model.LeaveEntry;
import io.timesheets.model.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LeaveEntryRepository extends JpaRepository<LeaveEntry, Long> {

    List<LeaveEntry> findByEntryDateBetweenOrderByEntryDateAsc(LocalDate start, LocalDate end);

    Optional<LeaveEntry> findByEntryDate(LocalDate date);

    List<LeaveEntry> findByLeaveTypeAndEntryDateBetween(LeaveType leaveType, LocalDate start, LocalDate end);

    long countByLeaveTypeAndEntryDateBetween(LeaveType leaveType, LocalDate start, LocalDate end);

    long countByLeaveTypeAndDoctorsNoteAndEntryDateBetween(LeaveType leaveType, boolean doctorsNote, LocalDate start, LocalDate end);
}
