package io.timesheets.controller;

import io.timesheets.dto.TimeEntryDto;
import io.timesheets.model.WorkLocation;
import io.timesheets.service.OverviewService;
import io.timesheets.service.ProjectService;
import io.timesheets.service.TimeEntryService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;

@Controller
@RequestMapping("/overview")
public class OverviewController {

    private final OverviewService overviewService;
    private final TimeEntryService timeEntryService;
    private final ProjectService projectService;

    public OverviewController(OverviewService overviewService,
                              TimeEntryService timeEntryService,
                              ProjectService projectService) {
        this.overviewService = overviewService;
        this.timeEntryService = timeEntryService;
        this.projectService = projectService;
    }

    @GetMapping
    public String overview(@RequestParam(required = false) LocalDate week,
                           @RequestParam(required = false) String month,
                           Model model) {
        var overview = month != null
                ? overviewService.getMonthlyOverview(YearMonth.parse(month).getYear(), YearMonth.parse(month).getMonthValue())
                : overviewService.getWeeklyOverview(week != null ? week : LocalDate.now());

        model.addAttribute("currentPage", "overview");
        model.addAttribute("overview", overview);
        model.addAttribute("projects", projectService.getAllActiveProjects());
        model.addAttribute("workLocations", WorkLocation.values());

        return "overview/index";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("timeEntry") TimeEntryDto dto,
                       BindingResult result,
                       @RequestParam(required = false) String returnMode,
                       @RequestParam(required = false) String returnParam,
                       RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Ongeldige invoer — controleer de velden.");
            return buildRedirect(dto.entryDate(), returnMode, returnParam);
        }
        timeEntryService.save(dto);
        redirectAttributes.addFlashAttribute("success", "Uren opgeslagen");
        return buildRedirect(dto.entryDate(), returnMode, returnParam);
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @RequestParam LocalDate date,
                         @RequestParam(required = false) String returnMode,
                         @RequestParam(required = false) String returnParam,
                         RedirectAttributes redirectAttributes) {
        timeEntryService.delete(id);
        redirectAttributes.addFlashAttribute("success", "Uurregistratie verwijderd");
        return buildRedirect(date, returnMode, returnParam);
    }

    private String buildRedirect(LocalDate date, String returnMode, String returnParam) {
        if ("month".equals(returnMode) && returnParam != null) {
            return "redirect:/overview?month=" + returnParam;
        }
        var monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return "redirect:/overview?week=" + monday;
    }
}
