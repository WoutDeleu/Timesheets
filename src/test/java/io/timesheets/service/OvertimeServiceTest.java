package io.timesheets.service;

import io.timesheets.model.LeaveEntry;
import io.timesheets.model.LeaveType;
import io.timesheets.model.Project;
import io.timesheets.model.TimeEntry;
import io.timesheets.repository.LeaveEntryRepository;
import io.timesheets.repository.ProjectRepository;
import io.timesheets.repository.TimeEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OvertimeServiceTest {

    @Mock
    private TimeEntryRepository timeEntryRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private LeaveEntryRepository leaveEntryRepository;

    @Mock
    private SettingsService settingsService;

    private OvertimeService overtimeService;

    @BeforeEach
    void setUp() {
        overtimeService = new OvertimeService(timeEntryRepository, projectRepository, leaveEntryRepository, settingsService);
        when(settingsService.getDefaultDailyHours()).thenReturn(new BigDecimal("7.6"));
    }

    @Test
    void shouldCalculateOvertimeWhenExceedingProjectTarget() {
        var project = createProject(new BigDecimal("8.0"));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(leaveEntryRepository.findByEntryDateBetweenOrderByEntryDateAsc(any(), any())).thenReturn(List.of());

        var entries = List.of(
                createTimeEntry(LocalDate.of(2026, 1, 5), project, new BigDecimal("9.0")),  // 1h overtime
                createTimeEntry(LocalDate.of(2026, 1, 6), project, new BigDecimal("8.5"))   // 0.5h overtime
        );

        when(timeEntryRepository.findByProjectIdAndEntryDateBetween(eq(1L), any(), any())).thenReturn(entries);

        var overtime = overtimeService.calculateOvertimeForProjectInYear(1L, 2026);
        // (9-8) + (8.5-8) = 1.5h overtime
        assertEquals(0, new BigDecimal("1.5").compareTo(overtime));
    }

    @Test
    void shouldCalculateNegativeOvertimeWhenUnderTarget() {
        var project = createProject(new BigDecimal("8.0"));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(leaveEntryRepository.findByEntryDateBetweenOrderByEntryDateAsc(any(), any())).thenReturn(List.of());

        var entries = List.of(
                createTimeEntry(LocalDate.of(2026, 1, 5), project, new BigDecimal("7.0")),  // -1h
                createTimeEntry(LocalDate.of(2026, 1, 6), project, new BigDecimal("6.0"))   // -2h
        );

        when(timeEntryRepository.findByProjectIdAndEntryDateBetween(eq(1L), any(), any())).thenReturn(entries);

        var overtime = overtimeService.calculateOvertimeForProjectInYear(1L, 2026);
        // (7-8) + (6-8) = -3h undertime
        assertEquals(0, new BigDecimal("-3.0").compareTo(overtime));
    }

    @Test
    void shouldUseGlobalDefaultWhenNoProjectTarget() {
        var project = createProject(null);  // No target set
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(leaveEntryRepository.findByEntryDateBetweenOrderByEntryDateAsc(any(), any())).thenReturn(List.of());

        var entries = List.of(
                createTimeEntry(LocalDate.of(2026, 1, 5), project, new BigDecimal("8.0"))  // 0.4h overtime
        );

        when(timeEntryRepository.findByProjectIdAndEntryDateBetween(eq(1L), any(), any())).thenReturn(entries);

        var overtime = overtimeService.calculateOvertimeForProjectInYear(1L, 2026);
        assertEquals(0, new BigDecimal("0.4").compareTo(overtime));
    }

    @Test
    void shouldReturnZeroWhenNoEntries() {
        var project = createProject(new BigDecimal("8.0"));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(timeEntryRepository.findByProjectIdAndEntryDateBetween(eq(1L), any(), any())).thenReturn(List.of());
        when(leaveEntryRepository.findByEntryDateBetweenOrderByEntryDateAsc(any(), any())).thenReturn(List.of());

        var overtime = overtimeService.calculateOvertimeForProjectInYear(1L, 2026);
        assertEquals(0, BigDecimal.ZERO.compareTo(overtime));
    }

    @Test
    void shouldMixOvertimeAndUndertime() {
        var project = createProject(new BigDecimal("8.0"));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(leaveEntryRepository.findByEntryDateBetweenOrderByEntryDateAsc(any(), any())).thenReturn(List.of());

        var entries = List.of(
                createTimeEntry(LocalDate.of(2026, 1, 5), project, new BigDecimal("10.0")),  // +2h
                createTimeEntry(LocalDate.of(2026, 1, 6), project, new BigDecimal("6.0"))    // -2h
        );

        when(timeEntryRepository.findByProjectIdAndEntryDateBetween(eq(1L), any(), any())).thenReturn(entries);

        var overtime = overtimeService.calculateOvertimeForProjectInYear(1L, 2026);
        // (10-8) + (6-8) = 0
        assertEquals(0, BigDecimal.ZERO.compareTo(overtime));
    }

    @Test
    void shouldReduceTargetByLeaveHoursOnPartialSickDay() {
        // Half day sick (3.8h) + half day work (3.8h) = 0 overtime
        var project = createProject(new BigDecimal("7.6"));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        var sickDate = LocalDate.of(2026, 1, 5);
        var leaveEntry = new LeaveEntry();
        leaveEntry.setEntryDate(sickDate);
        leaveEntry.setLeaveType(LeaveType.SICK);
        leaveEntry.setHours(new BigDecimal("3.8"));
        when(leaveEntryRepository.findByEntryDateBetweenOrderByEntryDateAsc(any(), any()))
                .thenReturn(List.of(leaveEntry));

        var entries = List.of(
                createTimeEntry(sickDate, project, new BigDecimal("3.8"))  // half day work
        );
        when(timeEntryRepository.findByProjectIdAndEntryDateBetween(eq(1L), any(), any())).thenReturn(entries);

        var overtime = overtimeService.calculateOvertimeForProjectInYear(1L, 2026);
        // effectiveTarget = 7.6 - 3.8 = 3.8, worked 3.8 -> 0 overtime
        assertEquals(0, BigDecimal.ZERO.compareTo(overtime));
    }

    @Test
    void shouldUseDefaultTargetOnLeaveDayWhenProjectTargetIsHigher() {
        // Project target = 8h, but on a sick day the target drops to the standard daily target (7.6h)
        // Half day sick (3.8h) + half day work (3.8h) = 0 overtime (not -0.4)
        var project = createProject(new BigDecimal("8.0"));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        var sickDate = LocalDate.of(2026, 2, 9);
        var leaveEntry = new LeaveEntry();
        leaveEntry.setEntryDate(sickDate);
        leaveEntry.setLeaveType(LeaveType.SICK);
        leaveEntry.setHours(new BigDecimal("3.8"));
        when(leaveEntryRepository.findByEntryDateBetweenOrderByEntryDateAsc(any(), any()))
                .thenReturn(List.of(leaveEntry));

        var entries = List.of(
                createTimeEntry(sickDate, project, new BigDecimal("3.8"))
        );
        when(timeEntryRepository.findByProjectIdAndEntryDateBetween(eq(1L), any(), any())).thenReturn(entries);

        var overtime = overtimeService.calculateOvertimeForProjectInYear(1L, 2026);
        // Day target becomes 7.6 (standard), effective = 7.6 - 3.8 = 3.8, worked 3.8 -> 0
        assertEquals(0, BigDecimal.ZERO.compareTo(overtime));
    }

    @Test
    void shouldNotPenalizeOvertimeOnFullSickDay() {
        // Full sick day (7.6h) + no work = 0 overtime (not -7.6)
        var project = createProject(new BigDecimal("7.6"));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        when(leaveEntryRepository.findByEntryDateBetweenOrderByEntryDateAsc(any(), any()))
                .thenReturn(List.of());
        when(timeEntryRepository.findByProjectIdAndEntryDateBetween(eq(1L), any(), any()))
                .thenReturn(List.of());

        var overtime = overtimeService.calculateOvertimeForProjectInYear(1L, 2026);
        // No entries at all -> 0 overtime
        assertEquals(0, BigDecimal.ZERO.compareTo(overtime));
    }

    private Project createProject(BigDecimal dailyTarget) {
        var project = new Project();
        project.setId(1L);
        project.setName("Test Project");
        project.setDailyHourTarget(dailyTarget);
        project.setActive(true);
        return project;
    }

    private TimeEntry createTimeEntry(LocalDate date, Project project, BigDecimal hours) {
        var entry = new TimeEntry();
        entry.setEntryDate(date);
        entry.setProject(project);
        entry.setHoursWorked(hours);
        return entry;
    }
}
