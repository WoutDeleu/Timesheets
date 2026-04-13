package io.timesheets.dto;

import io.timesheets.model.WorkLocation;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record TimeEntryDto(
        Long id,

        @NotNull(message = "Datum is verplicht")
        LocalDate entryDate,

        @NotNull(message = "Project is verplicht")
        Long projectId,

        @NotNull(message = "Uren is verplicht")
        @Positive(message = "Uren moet positief zijn")
        BigDecimal hoursWorked,

        LocalTime startTime,

        LocalTime endTime,

        String notes,

        WorkLocation workLocation,

        BigDecimal breakDuration
) {
    public TimeEntryDto() {
        this(null, LocalDate.now(), null, null, null, null, null, WorkLocation.OFFICE, BigDecimal.ZERO);
    }
}
