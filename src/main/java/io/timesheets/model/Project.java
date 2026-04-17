package io.timesheets.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "project")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "daily_hour_target")
    private BigDecimal dailyHourTarget;

    @Column(name = "default_break_duration")
    private BigDecimal defaultBreakDuration;

    @Column(nullable = false)
    private boolean billable = true;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "internal_project", nullable = false)
    private boolean internalProject = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
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

    public boolean isInternalProject() { return internalProject; }
    public void setInternalProject(boolean internalProject) { this.internalProject = internalProject; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
