package be.axxes.timesheets.controller;

import be.axxes.timesheets.dto.TimeEntryDto;
import be.axxes.timesheets.model.WorkLocation;
import be.axxes.timesheets.service.ProjectService;
import be.axxes.timesheets.service.TimeEntryService;
import be.axxes.timesheets.service.HolidayService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;

@Controller
@RequestMapping("/time-entry")
public class TimeEntryController {

    private final TimeEntryService timeEntryService;
    private final ProjectService projectService;
    private final HolidayService holidayService;

    public TimeEntryController(TimeEntryService timeEntryService,
                               ProjectService projectService,
                               HolidayService holidayService) {
        this.timeEntryService = timeEntryService;
        this.projectService = projectService;
        this.holidayService = holidayService;
    }

    // --- Daily Form ---

    @GetMapping
    public String dailyView(@RequestParam(required = false) LocalDate date, Model model) {
        var selectedDate = date != null ? date : LocalDate.now();

        model.addAttribute("currentPage", "time-entry");
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("entries", timeEntryService.getEntriesForDate(selectedDate));
        model.addAttribute("projects", projectService.getAllActiveProjects());
        model.addAttribute("timeEntry", new TimeEntryDto(null, selectedDate, null, null, null, null, null, WorkLocation.OFFICE));
        model.addAttribute("workLocations", WorkLocation.values());
        model.addAttribute("isHoliday", holidayService.isHoliday(selectedDate));

        // Calculate total hours for the day
        var totalHours = timeEntryService.getEntriesForDate(selectedDate).stream()
                .map(e -> e.getHoursWorked())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("totalHours", totalHours);

        return "time-entry/daily";
    }

    @PostMapping
    public String saveDailyEntry(@Valid @ModelAttribute("timeEntry") TimeEntryDto dto,
                                 BindingResult result,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("currentPage", "time-entry");
            model.addAttribute("selectedDate", dto.entryDate());
            model.addAttribute("entries", timeEntryService.getEntriesForDate(dto.entryDate()));
            model.addAttribute("projects", projectService.getAllActiveProjects());
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

    // --- Weekly Grid ---

    @GetMapping("/weekly")
    public String weeklyView(@RequestParam(required = false) LocalDate week, Model model) {
        var monday = (week != null ? week : LocalDate.now())
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        var friday = monday.plusDays(4);

        var entries = timeEntryService.getEntriesForWeek(monday);
        var projects = projectService.getAllActiveProjects();

        // Build a grid: project -> day -> hours
        var weekDays = List.of(monday, monday.plusDays(1), monday.plusDays(2),
                monday.plusDays(3), friday);

        var grid = new LinkedHashMap<Long, LinkedHashMap<LocalDate, BigDecimal>>();
        var locationGrid = new LinkedHashMap<Long, LinkedHashMap<LocalDate, WorkLocation>>();
        for (var project : projects) {
            var dayMap = new LinkedHashMap<LocalDate, BigDecimal>();
            var locMap = new LinkedHashMap<LocalDate, WorkLocation>();
            for (var day : weekDays) {
                dayMap.put(day, BigDecimal.ZERO);
                locMap.put(day, WorkLocation.OFFICE);
            }
            grid.put(project.getId(), dayMap);
            locationGrid.put(project.getId(), locMap);
        }

        for (var entry : entries) {
            var projectId = entry.getProject().getId();
            if (grid.containsKey(projectId)) {
                var hours = entry.getHoursWorked() != null ? entry.getHoursWorked() : BigDecimal.ZERO;
                grid.get(projectId).put(entry.getEntryDate(), hours);
                locationGrid.get(projectId).put(entry.getEntryDate(),
                        entry.getWorkLocation() != null ? entry.getWorkLocation() : WorkLocation.OFFICE);
            }
        }

        // Calculate row totals per project
        var rowTotals = new LinkedHashMap<Long, BigDecimal>();
        for (var entry : grid.entrySet()) {
            var total = entry.getValue().values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            rowTotals.put(entry.getKey(), total);
        }

        // Check which days are holidays
        var holidays = new LinkedHashMap<LocalDate, Boolean>();
        for (var day : weekDays) {
            holidays.put(day, holidayService.isHoliday(day));
        }

        model.addAttribute("currentPage", "weekly");
        model.addAttribute("monday", monday);
        model.addAttribute("friday", friday);
        model.addAttribute("weekDays", weekDays);
        model.addAttribute("projects", projects);
        model.addAttribute("grid", grid);
        model.addAttribute("locationGrid", locationGrid);
        model.addAttribute("workLocations", WorkLocation.values());
        model.addAttribute("holidays", holidays);
        model.addAttribute("rowTotals", rowTotals);
        model.addAttribute("prevWeek", monday.minusWeeks(1));
        model.addAttribute("nextWeek", monday.plusWeeks(1));

        return "time-entry/weekly";
    }

    @PostMapping("/weekly")
    public String saveWeeklyEntries(@RequestParam LocalDate monday,
                                    @RequestParam java.util.Map<String, String> allParams,
                                    RedirectAttributes redirectAttributes) {
        // Params come as "hours_{projectId}_{date}" = "value"
        for (var param : allParams.entrySet()) {
            if (!param.getKey().startsWith("hours_")) continue;

            var parts = param.getKey().split("_");
            if (parts.length != 3) continue;

            var projectId = Long.parseLong(parts[1]);
            var date = LocalDate.parse(parts[2]);
            var value = param.getValue().trim();

            if (value.isEmpty() || "0".equals(value)) {
                // If there was an existing entry, we could delete it — for now skip
                continue;
            }

            var hours = new BigDecimal(value.replace(",", "."));
            if (hours.compareTo(BigDecimal.ZERO) > 0) {
                var locationKey = "location_" + projectId + "_" + date;
                var locationValue = allParams.getOrDefault(locationKey, "OFFICE");
                var workLocation = WorkLocation.valueOf(locationValue);
                var dto = new TimeEntryDto(null, date, projectId, hours, null, null, null, workLocation);
                timeEntryService.save(dto);
            }
        }

        redirectAttributes.addFlashAttribute("success", "Week opgeslagen");
        return "redirect:/time-entry/weekly?week=" + monday;
    }
}
