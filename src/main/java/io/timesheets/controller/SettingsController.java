package io.timesheets.controller;

import io.timesheets.model.AppSetting;
import io.timesheets.repository.AppSettingRepository;
import io.timesheets.service.HolidayService;
import io.timesheets.util.HourFormatter;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Map;

@Controller
@RequestMapping("/settings")
public class SettingsController {

    private final AppSettingRepository appSettingRepository;
    private final HolidayService holidayService;

    public SettingsController(AppSettingRepository appSettingRepository,
                              HolidayService holidayService) {
        this.appSettingRepository = appSettingRepository;
        this.holidayService = holidayService;
    }

    @GetMapping
    public String settings(@RequestParam(required = false) Integer year, Model model) {
        model.addAttribute("currentPage", "settings");
        model.addAttribute("defaultDailyHours", getSettingValue("default_daily_hours", "7.6"));
        model.addAttribute("vacationDaysPerYear", getSettingValue("vacation_days_per_year", "20"));
        model.addAttribute("advDailySurplusHours", getSettingValue("adv_daily_surplus_hours", "0.4"));
        model.addAttribute("advDayHours", getSettingValue("adv_day_hours", "7.6"));
        model.addAttribute("sickDaysWithoutNotePerYear", getSettingValue("sick_days_without_note_per_year", "3"));

        // Holidays
        var selectedYear = year != null ? year : LocalDate.now().getYear();
        model.addAttribute("selectedYear", selectedYear);
        model.addAttribute("holidays", holidayService.getHolidaysForYear(selectedYear));
        model.addAttribute("remainingCompensatory", holidayService.getRemainingCompensatoryCount(selectedYear));
        model.addAttribute("weekendHolidayCount", holidayService.getWeekendHolidayCount(selectedYear));

        return "settings/index";
    }

    @PostMapping
    public String save(@RequestParam Map<String, String> params, RedirectAttributes redirectAttributes) {
        var settingsToSave = Map.of(
                "default_daily_hours", parseHourParam(params.getOrDefault("defaultDailyHours", "7.6")),
                "vacation_days_per_year", params.getOrDefault("vacationDaysPerYear", "20"),
                "adv_daily_surplus_hours", parseHourParam(params.getOrDefault("advDailySurplusHours", "0.4")),
                "adv_day_hours", parseHourParam(params.getOrDefault("advDayHours", "7.6")),
                "sick_days_without_note_per_year", params.getOrDefault("sickDaysWithoutNotePerYear", "3")
        );

        for (var entry : settingsToSave.entrySet()) {
            var setting = appSettingRepository.findBySettingKey(entry.getKey())
                    .orElseGet(() -> {
                        var s = new AppSetting();
                        s.setSettingKey(entry.getKey());
                        return s;
                    });
            setting.setSettingValue(entry.getValue());
            appSettingRepository.save(setting);
        }

        redirectAttributes.addFlashAttribute("success", "Instellingen opgeslagen");
        return "redirect:/settings";
    }

    @PostMapping("/holidays")
    public String addCompanyHoliday(@RequestParam LocalDate holidayDate,
                                    @RequestParam String name,
                                    RedirectAttributes redirectAttributes) {
        holidayService.addCompanyHoliday(holidayDate, name);
        redirectAttributes.addFlashAttribute("success", "Feestdag toegevoegd");
        return "redirect:/settings?year=" + holidayDate.getYear();
    }

    @PostMapping("/holidays/compensatory")
    public String addCompensatoryHoliday(@RequestParam LocalDate holidayDate,
                                         @RequestParam String name,
                                         RedirectAttributes redirectAttributes) {
        var year = holidayDate.getYear();
        if (holidayService.getRemainingCompensatoryCount(year) <= 0) {
            redirectAttributes.addFlashAttribute("error", "Geen compensatiedagen meer beschikbaar voor " + year);
            return "redirect:/settings?year=" + year;
        }
        holidayService.addCompensatoryHoliday(holidayDate, name);
        redirectAttributes.addFlashAttribute("success", "Compensatiedag ingepland");
        return "redirect:/settings?year=" + year;
    }

    @PostMapping("/holidays/{id}/delete")
    public String deleteHoliday(@PathVariable Long id,
                                @RequestParam int year,
                                RedirectAttributes redirectAttributes) {
        holidayService.deleteHoliday(id);
        redirectAttributes.addFlashAttribute("success", "Feestdag verwijderd");
        return "redirect:/settings?year=" + year;
    }

    private String parseHourParam(String value) {
        var parsed = HourFormatter.parse(value);
        return parsed != null ? parsed.toPlainString() : value;
    }

    private String getSettingValue(String key, String defaultValue) {
        return appSettingRepository.findBySettingKey(key)
                .map(AppSetting::getSettingValue)
                .orElse(defaultValue);
    }
}
