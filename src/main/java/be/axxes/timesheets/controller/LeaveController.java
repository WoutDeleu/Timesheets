package be.axxes.timesheets.controller;

import be.axxes.timesheets.dto.LeaveEntryDto;
import be.axxes.timesheets.model.LeaveType;
import be.axxes.timesheets.service.LeaveService;
import be.axxes.timesheets.service.SettingsService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.Month;

@Controller
@RequestMapping("/leave")
public class LeaveController {

    private final LeaveService leaveService;
    private final SettingsService settingsService;

    public LeaveController(LeaveService leaveService, SettingsService settingsService) {
        this.leaveService = leaveService;
        this.settingsService = settingsService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) Integer year,
                       @RequestParam(required = false) Integer month,
                       Model model) {
        var selectedYear = year != null ? year : LocalDate.now().getYear();
        var selectedMonth = month != null ? month : LocalDate.now().getMonthValue();

        model.addAttribute("currentPage", "leave");
        model.addAttribute("selectedYear", selectedYear);
        model.addAttribute("selectedMonth", selectedMonth);
        model.addAttribute("monthName", Month.of(selectedMonth).name());
        model.addAttribute("entries", leaveService.getEntriesForMonth(selectedYear, selectedMonth));
        model.addAttribute("vacationUsed", leaveService.countVacationDaysInYear(selectedYear));
        model.addAttribute("advUsed", leaveService.countAdvDaysInYear(selectedYear));
        model.addAttribute("sickWithoutNoteUsed", leaveService.countSickDaysWithoutNoteInYear(selectedYear));
        model.addAttribute("sickWithoutNoteTotal", settingsService.getSickDaysWithoutNotePerYear());
        model.addAttribute("leaveEntry", new LeaveEntryDto());
        model.addAttribute("leaveTypes", LeaveType.values());

        return "leave/list";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("leaveEntry") LeaveEntryDto dto,
                       BindingResult result,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("currentPage", "leave");
            model.addAttribute("leaveTypes", LeaveType.values());
            var year = dto.entryDate() != null ? dto.entryDate().getYear() : LocalDate.now().getYear();
            var month = dto.entryDate() != null ? dto.entryDate().getMonthValue() : LocalDate.now().getMonthValue();
            model.addAttribute("selectedYear", year);
            model.addAttribute("selectedMonth", month);
            model.addAttribute("entries", leaveService.getEntriesForMonth(year, month));
            model.addAttribute("vacationUsed", leaveService.countVacationDaysInYear(year));
            model.addAttribute("advUsed", leaveService.countAdvDaysInYear(year));
            model.addAttribute("sickWithoutNoteUsed", leaveService.countSickDaysWithoutNoteInYear(year));
            model.addAttribute("sickWithoutNoteTotal", settingsService.getSickDaysWithoutNotePerYear());
            return "leave/list";
        }
        leaveService.save(dto);
        redirectAttributes.addFlashAttribute("success", "Verlof opgeslagen");
        var entryDate = dto.entryDate();
        return "redirect:/leave?year=" + entryDate.getYear() + "&month=" + entryDate.getMonthValue();
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @RequestParam int year,
                         @RequestParam int month,
                         RedirectAttributes redirectAttributes) {
        leaveService.delete(id);
        redirectAttributes.addFlashAttribute("success", "Verlof verwijderd");
        return "redirect:/leave?year=" + year + "&month=" + month;
    }
}
