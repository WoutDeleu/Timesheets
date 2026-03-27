package be.axxes.timesheets.service;

import be.axxes.timesheets.repository.AppSettingRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class SettingsService {

    private final AppSettingRepository appSettingRepository;

    public SettingsService(AppSettingRepository appSettingRepository) {
        this.appSettingRepository = appSettingRepository;
    }

    public BigDecimal getDefaultDailyHours() {
        return getDecimal("default_daily_hours", new BigDecimal("7.6"));
    }

    public int getVacationDaysPerYear() {
        return getInt("vacation_days_per_year", 20);
    }

    public BigDecimal getAdvDailySurplusHours() {
        return getDecimal("adv_daily_surplus_hours", new BigDecimal("0.4"));
    }

    public BigDecimal getAdvDayHours() {
        return getDecimal("adv_day_hours", new BigDecimal("7.6"));
    }

    public int getSickDaysWithoutNotePerYear() {
        return getInt("sick_days_without_note_per_year", 3);
    }

    private BigDecimal getDecimal(String key, BigDecimal defaultValue) {
        return appSettingRepository.findBySettingKey(key)
                .map(s -> new BigDecimal(s.getSettingValue()))
                .orElse(defaultValue);
    }

    private int getInt(String key, int defaultValue) {
        return appSettingRepository.findBySettingKey(key)
                .map(s -> Integer.parseInt(s.getSettingValue()))
                .orElse(defaultValue);
    }
}
