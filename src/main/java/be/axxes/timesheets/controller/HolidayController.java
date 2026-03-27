package be.axxes.timesheets.controller;

import be.axxes.timesheets.service.HolidayService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/holidays")
public class HolidayController {

    private final HolidayService holidayService;

    public HolidayController(HolidayService holidayService) {
        this.holidayService = holidayService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) Integer year, Model model) {
        var selectedYear = year != null ? year : LocalDate.now().getYear();

        model.addAttribute("currentPage", "holidays");
        model.addAttribute("selectedYear", selectedYear);
        model.addAttribute("holidays", holidayService.getHolidaysForYear(selectedYear));
        model.addAttribute("remainingCompensatory", holidayService.getRemainingCompensatoryCount(selectedYear));
        model.addAttribute("weekendHolidayCount", holidayService.getWeekendHolidayCount(selectedYear));
        model.addAttribute("scheduledCompensatory", holidayService.getScheduledCompensatoryCount(selectedYear));

        return "holidays/list";
    }

    @PostMapping
    public String addCompanyHoliday(@RequestParam LocalDate holidayDate,
                                    @RequestParam String name,
                                    RedirectAttributes redirectAttributes) {
        holidayService.addCompanyHoliday(holidayDate, name);
        redirectAttributes.addFlashAttribute("success", "Feestdag toegevoegd");
        return "redirect:/holidays?year=" + holidayDate.getYear();
    }

    @PostMapping("/compensatory")
    public String addCompensatoryHoliday(@RequestParam LocalDate holidayDate,
                                         @RequestParam String name,
                                         RedirectAttributes redirectAttributes) {
        var year = holidayDate.getYear();
        if (holidayService.getRemainingCompensatoryCount(year) <= 0) {
            redirectAttributes.addFlashAttribute("error", "Geen compensatiedagen meer beschikbaar voor " + year);
            return "redirect:/holidays?year=" + year;
        }
        holidayService.addCompensatoryHoliday(holidayDate, name);
        redirectAttributes.addFlashAttribute("success", "Compensatiedag ingepland");
        return "redirect:/holidays?year=" + year;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @RequestParam int year,
                         RedirectAttributes redirectAttributes) {
        holidayService.deleteHoliday(id);
        redirectAttributes.addFlashAttribute("success", "Feestdag verwijderd");
        return "redirect:/holidays?year=" + year;
    }
}
