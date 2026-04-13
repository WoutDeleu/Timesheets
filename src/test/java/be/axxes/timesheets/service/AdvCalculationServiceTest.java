package be.axxes.timesheets.service;

import be.axxes.timesheets.model.LeaveEntry;
import be.axxes.timesheets.model.LeaveType;
import be.axxes.timesheets.model.Project;
import be.axxes.timesheets.model.TimeEntry;
import be.axxes.timesheets.repository.LeaveEntryRepository;
import be.axxes.timesheets.repository.TimeEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdvCalculationServiceTest {

    @Mock
    private TimeEntryRepository timeEntryRepository;

    @Mock
    private LeaveEntryRepository leaveEntryRepository;

    @Mock
    private SettingsService settingsService;

    @Mock
    private HolidayService holidayService;

    private AdvCalculationService advCalculationService;

    @BeforeEach
    void setUp() {
        advCalculationService = new AdvCalculationService(timeEntryRepository, leaveEntryRepository, settingsService, holidayService);
        when(settingsService.getAdvDailySurplusHours()).thenReturn(new BigDecimal("0.4"));
    }

    @Test
    void shouldAccumulateSurplusFromEightHourDays() {
        var project = createProject();
        // 5 workdays, all 8h
        var entries = List.of(
                createTimeEntry(LocalDate.of(2026, 1, 5), project, new BigDecimal("8.0")),  // Monday
                createTimeEntry(LocalDate.of(2026, 1, 6), project, new BigDecimal("8.0")),  // Tuesday
                createTimeEntry(LocalDate.of(2026, 1, 7), project, new BigDecimal("8.0")),  // Wednesday
                createTimeEntry(LocalDate.of(2026, 1, 8), project, new BigDecimal("8.0")),  // Thursday
                createTimeEntry(LocalDate.of(2026, 1, 9), project, new BigDecimal("8.0"))   // Friday
        );

        when(timeEntryRepository.findByEntryDateBetweenOrderByEntryDateAsc(any(), any())).thenReturn(entries);
        when(leaveEntryRepository.findByEntryDateBetweenOrderByEntryDateAsc(any(), any())).thenReturn(List.of());

        var surplus = advCalculationService.calculateAccumulatedSurplusHours(2026);
        // 5 days * 0.4h = 2.0h
        assertEquals(0, new BigDecimal("2.0").compareTo(surplus));
    }

    @Test
    void shouldNotCountDaysUnderEightHours() {
        var project = createProject();
        var entries = List.of(
                createTimeEntry(LocalDate.of(2026, 1, 5), project, new BigDecimal("7.6")),  // Monday - only 7.6h
                createTimeEntry(LocalDate.of(2026, 1, 6), project, new BigDecimal("8.0"))   // Tuesday - 8h
        );

        when(timeEntryRepository.findByEntryDateBetweenOrderByEntryDateAsc(any(), any())).thenReturn(entries);
        when(leaveEntryRepository.findByEntryDateBetweenOrderByEntryDateAsc(any(), any())).thenReturn(List.of());

        var surplus = advCalculationService.calculateAccumulatedSurplusHours(2026);
        // Only 1 day counts: 0.4h
        assertEquals(0, new BigDecimal("0.4").compareTo(surplus));
    }

    @Test
    void shouldExcludeLeaveDays() {
        var project = createProject();
        // Monday is a sick day but also has time entries (edge case)
        var entries = List.of(
                createTimeEntry(LocalDate.of(2026, 1, 5), project, new BigDecimal("8.0")),  // Monday
                createTimeEntry(LocalDate.of(2026, 1, 6), project, new BigDecimal("8.0"))   // Tuesday
        );

        var leaveEntries = List.of(
                createLeaveEntry(LocalDate.of(2026, 1, 5), LeaveType.SICK)  // Monday is sick
        );

        when(timeEntryRepository.findByEntryDateBetweenOrderByEntryDateAsc(any(), any())).thenReturn(entries);
        when(leaveEntryRepository.findByEntryDateBetweenOrderByEntryDateAsc(any(), any())).thenReturn(leaveEntries);

        var surplus = advCalculationService.calculateAccumulatedSurplusHours(2026);
        // Monday excluded (sick), only Tuesday counts: 0.4h
        assertEquals(0, new BigDecimal("0.4").compareTo(surplus));
    }

    @Test
    void shouldExcludeWeekends() {
        var project = createProject();
        var entries = List.of(
                createTimeEntry(LocalDate.of(2026, 1, 10), project, new BigDecimal("8.0")),  // Saturday
                createTimeEntry(LocalDate.of(2026, 1, 11), project, new BigDecimal("8.0"))   // Sunday
        );

        when(timeEntryRepository.findByEntryDateBetweenOrderByEntryDateAsc(any(), any())).thenReturn(entries);
        when(leaveEntryRepository.findByEntryDateBetweenOrderByEntryDateAsc(any(), any())).thenReturn(List.of());

        var surplus = advCalculationService.calculateAccumulatedSurplusHours(2026);
        assertEquals(0, BigDecimal.ZERO.compareTo(surplus));
    }

    @Test
    void shouldCalculateEarnedAdvDaysCorrectly() {
        var project = createProject();
        // 19 workdays at 8h = 19 * 0.4 = 7.6h = exactly 1 ADV day
        var entries = new java.util.ArrayList<TimeEntry>();
        var date = LocalDate.of(2026, 1, 5); // Monday
        for (int i = 0; i < 19; i++) {
            // Skip weekends
            while (date.getDayOfWeek() == java.time.DayOfWeek.SATURDAY ||
                   date.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
                date = date.plusDays(1);
            }
            entries.add(createTimeEntry(date, project, new BigDecimal("8.0")));
            date = date.plusDays(1);
        }

        when(timeEntryRepository.findByEntryDateBetweenOrderByEntryDateAsc(any(), any())).thenReturn(entries);
        when(leaveEntryRepository.findByEntryDateBetweenOrderByEntryDateAsc(any(), any())).thenReturn(List.of());
        when(settingsService.getAdvDayHours()).thenReturn(new BigDecimal("7.6"));

        var earned = advCalculationService.calculateEarnedAdvDays(2026);
        // 19 * 0.4 = 7.6h / 7.6h = 1.00 ADV day
        assertEquals(0, new BigDecimal("1.00").compareTo(earned));
    }

    @Test
    void shouldReturnZeroWhenNoDaysWorked() {
        when(timeEntryRepository.findByEntryDateBetweenOrderByEntryDateAsc(any(), any())).thenReturn(List.of());
        when(leaveEntryRepository.findByEntryDateBetweenOrderByEntryDateAsc(any(), any())).thenReturn(List.of());

        var surplus = advCalculationService.calculateAccumulatedSurplusHours(2026);
        assertEquals(0, BigDecimal.ZERO.compareTo(surplus));
    }

    @Test
    void shouldSumMultipleProjectEntriesOnSameDay() {
        var project1 = createProject();
        var project2 = createProject();
        project2.setId(2L);

        // Two entries on Monday: 4h + 4h = 8h total -> should count
        var entries = List.of(
                createTimeEntry(LocalDate.of(2026, 1, 5), project1, new BigDecimal("4.0")),
                createTimeEntry(LocalDate.of(2026, 1, 5), project2, new BigDecimal("4.0"))
        );

        when(timeEntryRepository.findByEntryDateBetweenOrderByEntryDateAsc(any(), any())).thenReturn(entries);
        when(leaveEntryRepository.findByEntryDateBetweenOrderByEntryDateAsc(any(), any())).thenReturn(List.of());

        var surplus = advCalculationService.calculateAccumulatedSurplusHours(2026);
        assertEquals(0, new BigDecimal("0.4").compareTo(surplus));
    }

    @Test
    void shouldPredictYearEndAdvDaysAccountingForLeaveAndHolidays() {
        var year = LocalDate.now().getYear();

        // No time entries yet (start of year scenario) — current surplus = 0
        when(timeEntryRepository.findByEntryDateBetweenOrderByEntryDateAsc(any(), any())).thenReturn(List.of());
        when(leaveEntryRepository.findByEntryDateBetweenOrderByEntryDateAsc(any(), any())).thenReturn(List.of());
        when(holidayService.getHolidaysForYear(year)).thenReturn(List.of());
        when(settingsService.getAdvDayHours()).thenReturn(new BigDecimal("7.6"));
        when(settingsService.getVacationDaysPerYear()).thenReturn(20);
        when(settingsService.getSickDaysWithoutNotePerYear()).thenReturn(3);
        when(leaveEntryRepository.countByLeaveTypeAndEntryDateBetween(eq(LeaveType.VACATION), any(), any())).thenReturn(0L);
        when(leaveEntryRepository.countByLeaveTypeAndEntryDateBetween(eq(LeaveType.ADV), any(), any())).thenReturn(0L);
        when(leaveEntryRepository.countByLeaveTypeAndDoctorsNoteAndEntryDateBetween(eq(LeaveType.SICK), eq(false), any(), any())).thenReturn(0L);

        var predicted = advCalculationService.predictYearEndAdvDays(year);

        // Predicted should be >= 0 and reasonable (less than ~13 ADV days for a full year)
        assertTrue(predicted.compareTo(BigDecimal.ZERO) >= 0, "Predicted ADV days should be non-negative");
        assertTrue(predicted.compareTo(new BigDecimal("15")) < 0, "Predicted ADV days should be reasonable");
    }

    private Project createProject() {
        var project = new Project();
        project.setId(1L);
        project.setName("Test Project");
        return project;
    }

    private TimeEntry createTimeEntry(LocalDate date, Project project, BigDecimal hours) {
        var entry = new TimeEntry();
        entry.setEntryDate(date);
        entry.setProject(project);
        entry.setHoursWorked(hours);
        return entry;
    }

    private LeaveEntry createLeaveEntry(LocalDate date, LeaveType type) {
        var entry = new LeaveEntry();
        entry.setEntryDate(date);
        entry.setLeaveType(type);
        return entry;
    }
}
