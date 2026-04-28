package io.timesheets.repository;

import io.timesheets.model.Project;
import io.timesheets.model.TimeEntry;
import io.timesheets.model.WorkLocation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class TimeEntryRepositoryTest {

    @Autowired
    private TimeEntryRepository timeEntryRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Test
    void shouldAllowMultipleEntriesForSameProjectAndDateWithDifferentLocations() {
        var project = new Project();
        project.setName("Test Project");
        project.setDailyHourTarget(new BigDecimal("7.6"));
        project = projectRepository.save(project);

        var date = LocalDate.of(2026, 4, 28);

        // First entry: morning at home
        var entry1 = new TimeEntry();
        entry1.setEntryDate(date);
        entry1.setProject(project);
        entry1.setHoursWorked(new BigDecimal("4.0"));
        entry1.setWorkLocation(WorkLocation.HOME);
        entry1.setBreakDuration(BigDecimal.ZERO);
        timeEntryRepository.save(entry1);

        // Second entry: afternoon at office
        var entry2 = new TimeEntry();
        entry2.setEntryDate(date);
        entry2.setProject(project);
        entry2.setHoursWorked(new BigDecimal("4.0"));
        entry2.setWorkLocation(WorkLocation.OFFICE);
        entry2.setBreakDuration(BigDecimal.ZERO);
        timeEntryRepository.save(entry2);

        // Both entries should exist
        var entries = timeEntryRepository.findByEntryDate(date);
        assertThat(entries).hasSize(2);
        assertThat(entries).extracting(TimeEntry::getWorkLocation)
                .containsExactlyInAnyOrder(WorkLocation.HOME, WorkLocation.OFFICE);
    }
}
