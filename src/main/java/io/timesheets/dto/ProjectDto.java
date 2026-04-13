package io.timesheets.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class ProjectDto {

    private Long id;

    @NotBlank(message = "Naam is verplicht")
    @Size(max = 255, message = "Naam mag maximaal 255 tekens bevatten")
    private String name;

    @Size(max = 1000, message = "Beschrijving mag maximaal 1000 tekens bevatten")
    private String description;

    private BigDecimal dailyHourTarget;

    private BigDecimal defaultBreakDuration;

    private boolean billable = true;

    private boolean active = true;

    public ProjectDto() {
    }

    public ProjectDto(Long id, String name, String description, BigDecimal dailyHourTarget, BigDecimal defaultBreakDuration, boolean billable, boolean active) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.dailyHourTarget = dailyHourTarget;
        this.defaultBreakDuration = defaultBreakDuration;
        this.billable = billable;
        this.active = active;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getDailyHourTarget() { return dailyHourTarget; }
    public void setDailyHourTarget(BigDecimal dailyHourTarget) { this.dailyHourTarget = dailyHourTarget; }

    public BigDecimal getDefaultBreakDuration() { return defaultBreakDuration; }
    public void setDefaultBreakDuration(BigDecimal defaultBreakDuration) { this.defaultBreakDuration = defaultBreakDuration; }

    public boolean isBillable() { return billable; }
    public void setBillable(boolean billable) { this.billable = billable; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
