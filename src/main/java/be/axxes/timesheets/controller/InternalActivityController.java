package be.axxes.timesheets.controller;

import be.axxes.timesheets.dto.InternalActivityDto;
import be.axxes.timesheets.service.InternalActivityService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

@Controller
@RequestMapping("/internal")
public class InternalActivityController {

    private final InternalActivityService internalActivityService;

    public InternalActivityController(InternalActivityService internalActivityService) {
        this.internalActivityService = internalActivityService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) Integer year,
                       @RequestParam(required = false) Integer month,
                       Model model) {
        var selectedYear = year != null ? year : LocalDate.now().getYear();
        var selectedMonth = month != null ? month : LocalDate.now().getMonthValue();

        var entries = internalActivityService.getEntriesForMonth(selectedYear, selectedMonth);
        var monthTotal = entries.stream()
                .map(e -> e.getHours())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("currentPage", "internal");
        model.addAttribute("selectedYear", selectedYear);
        model.addAttribute("selectedMonth", selectedMonth);
        model.addAttribute("monthName", Month.of(selectedMonth).getDisplayName(TextStyle.FULL, new Locale("nl", "BE")));
        model.addAttribute("entries", entries);
        model.addAttribute("monthTotal", monthTotal);
        model.addAttribute("internalActivity", new InternalActivityDto());

        return "internal/list";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("internalActivity") InternalActivityDto dto,
                       BindingResult result,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            var selectedYear = dto.getActivityDate() != null ? dto.getActivityDate().getYear() : LocalDate.now().getYear();
            var selectedMonth = dto.getActivityDate() != null ? dto.getActivityDate().getMonthValue() : LocalDate.now().getMonthValue();

            var entries = internalActivityService.getEntriesForMonth(selectedYear, selectedMonth);
            var monthTotal = entries.stream()
                    .map(e -> e.getHours())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            model.addAttribute("currentPage", "internal");
            model.addAttribute("selectedYear", selectedYear);
            model.addAttribute("selectedMonth", selectedMonth);
            model.addAttribute("monthName", Month.of(selectedMonth).getDisplayName(TextStyle.FULL, new Locale("nl", "BE")));
            model.addAttribute("entries", entries);
            model.addAttribute("monthTotal", monthTotal);
            return "internal/list";
        }
        internalActivityService.save(dto);
        redirectAttributes.addFlashAttribute("success", "Interne activiteit opgeslagen");
        var entryDate = dto.getActivityDate();
        return "redirect:/internal?year=" + entryDate.getYear() + "&month=" + entryDate.getMonthValue();
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @RequestParam int year,
                         @RequestParam int month,
                         RedirectAttributes redirectAttributes) {
        internalActivityService.delete(id);
        redirectAttributes.addFlashAttribute("success", "Interne activiteit verwijderd");
        return "redirect:/internal?year=" + year + "&month=" + month;
    }
}
