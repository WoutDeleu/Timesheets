package be.axxes.timesheets.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "time_entry")
public class TimeEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "hours_worked", nullable = false)
    private BigDecimal hoursWorked;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    private String notes;

    @Column(name = "break_duration", nullable = false)
    private BigDecimal breakDuration = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_location", nullable = false)
    private WorkLocation workLocation = WorkLocation.OFFICE;

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

    public LocalDate getEntryDate() { return entryDate; }
    public void setEntryDate(LocalDate entryDate) { this.entryDate = entryDate; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public BigDecimal getHoursWorked() { return hoursWorked; }
    public void setHoursWorked(BigDecimal hoursWorked) { this.hoursWorked = hoursWorked; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public BigDecimal getBreakDuration() { return breakDuration; }
    public void setBreakDuration(BigDecimal breakDuration) { this.breakDuration = breakDuration; }

    public WorkLocation getWorkLocation() { return workLocation; }
    public void setWorkLocation(WorkLocation workLocation) { this.workLocation = workLocation; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
