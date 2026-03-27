package be.axxes.timesheets.config;

import be.axxes.timesheets.service.HolidayService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Populates Belgian national holidays for the current and next year on startup.
 */
@Component
public class HolidayInitializer implements ApplicationRunner {

    private final HolidayService holidayService;

    public HolidayInitializer(HolidayService holidayService) {
        this.holidayService = holidayService;
    }

    @Override
    public void run(ApplicationArguments args) {
        int currentYear = LocalDate.now().getYear();
        holidayService.populateHolidaysForYear(currentYear);
        holidayService.populateHolidaysForYear(currentYear + 1);
    }
}
