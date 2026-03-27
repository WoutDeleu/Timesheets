package be.axxes.timesheets.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public class InternalActivityDto {

    private Long id;

    @NotNull(message = "Datum is verplicht")
    private LocalDate activityDate;

    @NotNull(message = "Uren is verplicht")
    @Positive(message = "Uren moet positief zijn")
    private BigDecimal hours = new BigDecimal("7.6");

    @NotBlank(message = "Beschrijving is verplicht")
    private String description;

    public InternalActivityDto() {
        this.activityDate = LocalDate.now();
    }

    public InternalActivityDto(Long id, LocalDate activityDate, BigDecimal hours, String description) {
        this.id = id;
        this.activityDate = activityDate;
        this.hours = hours;
        this.description = description;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getActivityDate() { return activityDate; }
    public void setActivityDate(LocalDate activityDate) { this.activityDate = activityDate; }

    public BigDecimal getHours() { return hours; }
    public void setHours(BigDecimal hours) { this.hours = hours; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
