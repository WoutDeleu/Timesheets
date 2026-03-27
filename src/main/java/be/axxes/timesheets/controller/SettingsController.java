package be.axxes.timesheets.controller;

import be.axxes.timesheets.model.AppSetting;
import be.axxes.timesheets.repository.AppSettingRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/settings")
public class SettingsController {

    private final AppSettingRepository appSettingRepository;

    public SettingsController(AppSettingRepository appSettingRepository) {
        this.appSettingRepository = appSettingRepository;
    }

    @GetMapping
    public String settings(Model model) {
        model.addAttribute("currentPage", "settings");
        model.addAttribute("defaultDailyHours", getSettingValue("default_daily_hours", "7.6"));
        model.addAttribute("vacationDaysPerYear", getSettingValue("vacation_days_per_year", "20"));
        model.addAttribute("advDailySurplusHours", getSettingValue("adv_daily_surplus_hours", "0.4"));
        model.addAttribute("advDayHours", getSettingValue("adv_day_hours", "7.6"));
        model.addAttribute("sickDaysWithoutNotePerYear", getSettingValue("sick_days_without_note_per_year", "3"));

        return "settings/index";
    }

    @PostMapping
    public String save(@RequestParam Map<String, String> params, RedirectAttributes redirectAttributes) {
        var settingsToSave = Map.of(
                "default_daily_hours", params.getOrDefault("defaultDailyHours", "7.6"),
                "vacation_days_per_year", params.getOrDefault("vacationDaysPerYear", "20"),
                "adv_daily_surplus_hours", params.getOrDefault("advDailySurplusHours", "0.4"),
                "adv_day_hours", params.getOrDefault("advDayHours", "7.6"),
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

    private String getSettingValue(String key, String defaultValue) {
        return appSettingRepository.findBySettingKey(key)
                .map(AppSetting::getSettingValue)
                .orElse(defaultValue);
    }
}
