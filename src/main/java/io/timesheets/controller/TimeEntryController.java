package io.timesheets.controller;

import io.timesheets.dto.TimeEntryDto;
import io.timesheets.model.WorkLocation;
import io.timesheets.repository.LeaveEntryRepository;
import io.timesheets.service.ClockSessionService;
import io.timesheets.service.ProjectService;
import io.timesheets.service.SettingsService;
import io.timesheets.service.TimeEntryService;
import io.timesheets.service.HolidayService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

@Controller
@RequestMapping("/time-entry")
public class TimeEntryController {

    private final TimeEntryService timeEntryService;
    private final ProjectService projectService;
    private final HolidayService holidayService;
    private final SettingsService settingsService;
    private final LeaveEntryRepository leaveEntryRepository;
    private final ClockSessionService clockSessionService;

    public TimeEntryController(TimeEntryService timeEntryService,
                               ProjectService projectService,
                               HolidayService holidayService,
                               SettingsService settingsService,
                               LeaveEntryRepository leaveEntryRepository,
                               ClockSessionService clockSessionService) {
        this.timeEntryService = timeEntryService;
        this.projectService = projectService;
        this.holidayService = holidayService;
        this.settingsService = settingsService;
        this.leaveEntryRepository = leaveEntryRepository;
        this.clockSessionService = clockSessionService;
    }

    // --- Daily Form ---

    @GetMapping
    public String dailyView(@RequestParam(required = false) LocalDate date, Model model) {
        var selectedDate = date != null ? date : LocalDate.now();

        var projects = projectService.getAllActiveProjects();

        // Skip weekends when navigating
        var prevDay = selectedDate.minusDays(1);
        if (prevDay.getDayOfWeek() == DayOfWeek.SUNDAY) prevDay = prevDay.minusDays(2);
        else if (prevDay.getDayOfWeek() == DayOfWeek.SATURDAY) prevDay = prevDay.minusDays(1);
        var nextDay = selectedDate.plusDays(1);
        if (nextDay.getDayOfWeek() == DayOfWeek.SATURDAY) nextDay = nextDay.plusDays(2);
        else if (nextDay.getDayOfWeek() == DayOfWeek.SUNDAY) nextDay = nextDay.plusDays(1);

        model.addAttribute("currentPage", "time-entry");
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("prevDay", prevDay);
        model.addAttribute("nextDay", nextDay);
        model.addAttribute("entries", timeEntryService.getEntriesForDate(selectedDate));
        model.addAttribute("projects", projects);
        if (!model.containsAttribute("timeEntry")) {
            model.addAttribute("timeEntry", new TimeEntryDto(null, selectedDate, null, null, null, null, null, WorkLocation.OFFICE, BigDecimal.ZERO));
        }
        model.addAttribute("workLocations", WorkLocation.values());
        model.addAttribute("isHoliday", holidayService.isHoliday(selectedDate));

        // Calculate total gross hours for the day (net + break)
        var totalHours = computeGrossTotalForDate(selectedDate);
        model.addAttribute("totalHours", totalHours);
        model.addAttribute("dailyBalance", computeDailyBalance(selectedDate, totalHours));

        // Clock session
        var activeSession = clockSessionService.getActiveSession();
        model.addAttribute("clockSession", activeSession.orElse(null));
        model.addAttribute("clockedIn", activeSession.isPresent());

        return "time-entry/daily";
    }

    @PostMapping
    public String saveDailyEntry(@Valid @ModelAttribute("timeEntry") TimeEntryDto dto,
                                 BindingResult result,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            var selectedDate = dto.entryDate() != null ? dto.entryDate() : LocalDate.now();
            var projects = projectService.getAllActiveProjects();

            var prevDay = selectedDate.minusDays(1);
            if (prevDay.getDayOfWeek() == DayOfWeek.SUNDAY) prevDay = prevDay.minusDays(2);
            else if (prevDay.getDayOfWeek() == DayOfWeek.SATURDAY) prevDay = prevDay.minusDays(1);
            var nextDay = selectedDate.plusDays(1);
            if (nextDay.getDayOfWeek() == DayOfWeek.SATURDAY) nextDay = nextDay.plusDays(2);
            else if (nextDay.getDayOfWeek() == DayOfWeek.SUNDAY) nextDay = nextDay.plusDays(1);

            model.addAttribute("currentPage", "time-entry");
            model.addAttribute("selectedDate", selectedDate);
            model.addAttribute("prevDay", prevDay);
            model.addAttribute("nextDay", nextDay);
            model.addAttribute("entries", timeEntryService.getEntriesForDate(selectedDate));
            model.addAttribute("projects", projects);
            model.addAttribute("workLocations", WorkLocation.values());
            model.addAttribute("isHoliday", holidayService.isHoliday(selectedDate));
            var totalHours = computeGrossTotalForDate(selectedDate);
            model.addAttribute("totalHours", totalHours);
            model.addAttribute("dailyBalance", computeDailyBalance(selectedDate, totalHours));
            var activeSession = clockSessionService.getActiveSession();
            model.addAttribute("clockSession", activeSession.orElse(null));
            model.addAttribute("clockedIn", activeSession.isPresent());
            return "time-entry/daily";
        }
        timeEntryService.save(dto);
        redirectAttributes.addFlashAttribute("success", "Uren opgeslagen");
        return "redirect:/time-entry?date=" + dto.entryDate();
    }

    @PostMapping("/{id}/delete")
    public String deleteEntry(@PathVariable Long id,
                              @RequestParam LocalDate date,
                              RedirectAttributes redirectAttributes) {
        timeEntryService.delete(id);
        redirectAttributes.addFlashAttribute("success", "Uurregistratie verwijderd");
        return "redirect:/time-entry?date=" + date;
    }

    // --- Clock In/Out ---

    @PostMapping("/clock-in")
    public String clockIn(@RequestParam Long projectId,
                          @RequestParam(required = false) WorkLocation workLocation,
                          RedirectAttributes redirectAttributes) {
        try {
            clockSessionService.clockIn(projectId, workLocation);
            redirectAttributes.addFlashAttribute("success", "Ingeklokt");
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/time-entry";
    }

    @PostMapping("/clock-out")
    public String clockOut(RedirectAttributes redirectAttributes) {
        try {
            var dto = clockSessionService.clockOut();
            redirectAttributes.addFlashAttribute("timeEntry", dto);
            redirectAttributes.addFlashAttribute("success", "Uitgeklokt — controleer en sla de uren op.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/time-entry";
    }

    @PostMapping("/clock-break-start")
    public String startBreak(RedirectAttributes redirectAttributes) {
        try {
            clockSessionService.startBreak();
            redirectAttributes.addFlashAttribute("success", "Pauze gestart");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/time-entry";
    }

    @PostMapping("/clock-break-end")
    public String endBreak(RedirectAttributes redirectAttributes) {
        try {
            clockSessionService.endBreak();
            redirectAttributes.addFlashAttribute("success", "Pauze beëindigd");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/time-entry";
    }

    @PostMapping("/clock-cancel")
    public String cancelClock(RedirectAttributes redirectAttributes) {
        try {
            clockSessionService.cancel();
            redirectAttributes.addFlashAttribute("success", "Kloksessie geannuleerd");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/time-entry";
    }

    private BigDecimal computeGrossTotalForDate(LocalDate date) {
        return timeEntryService.getEntriesForDate(date).stream()
                .map(e -> e.getHoursWorked().add(e.getBreakDuration() != null ? e.getBreakDuration() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal computeDailyBalance(LocalDate date, BigDecimal grossTotal) {
        var entries = timeEntryService.getEntriesForDate(date);

        // Per-project balance: net hours (hoursWorked) minus project daily target
        var projectNetHours = new java.util.HashMap<Long, BigDecimal>();
        for (var entry : entries) {
            var pid = entry.getProject().getId();
            var hours = entry.getHoursWorked() != null ? entry.getHoursWorked() : BigDecimal.ZERO;
            projectNetHours.merge(pid, hours, BigDecimal::add);
        }

        var balance = BigDecimal.ZERO;
        for (var pid : projectNetHours.keySet()) {
            var project = entries.stream()
                    .filter(te -> te.getProject().getId().equals(pid))
                    .findFirst().get().getProject();
            var dailyTarget = project.getDailyHourTarget() != null
                    ? project.getDailyHourTarget()
                    : settingsService.getDefaultDailyHours();
            balance = balance.add(projectNetHours.get(pid).subtract(dailyTarget));
        }
        return balance;
    }
}
