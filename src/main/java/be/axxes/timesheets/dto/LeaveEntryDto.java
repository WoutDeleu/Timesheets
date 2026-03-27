package be.axxes.timesheets.dto;

import be.axxes.timesheets.model.LeaveType;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record LeaveEntryDto(
        Long id,

        @NotNull(message = "Datum is verplicht")
        LocalDate entryDate,

        @NotNull(message = "Verloftype is verplicht")
        LeaveType leaveType,

        BigDecimal hours,

        String notes,

        Boolean doctorsNote
) {
    public LeaveEntryDto() {
        this(null, LocalDate.now(), null, new BigDecimal("7.6"), null, true);
    }
}
