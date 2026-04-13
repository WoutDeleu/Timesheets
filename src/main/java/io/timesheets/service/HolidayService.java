package io.timesheets.service;

import io.timesheets.model.Holiday;
import io.timesheets.model.HolidayType;
import io.timesheets.repository.HolidayRepository;
import io.timesheets.util.BelgianHolidayCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Service
public class HolidayService {

    private final HolidayRepository holidayRepository;

    public HolidayService(HolidayRepository holidayRepository) {
        this.holidayRepository = holidayRepository;
    }

    /**
     * Populates Belgian national holidays for the given year if they don't already exist.
     * Holidays falling on a weekend are still stored (for reference) but are NOT marked
     * as regular holidays — instead the user earns compensatory days they can schedule.
     */
    @Transactional
    public void populateHolidaysForYear(int year) {
        var holidays = BelgianHolidayCalculator.getHolidays(year);

        for (var entry : holidays.entrySet()) {
            if (!holidayRepository.existsByHolidayDate(entry.getKey())) {
                var holiday = new Holiday();
                holiday.setHolidayDate(entry.getKey());
                holiday.setName(entry.getValue());
                holiday.setHolidayType(HolidayType.NATIONAL);
                holiday.setEditable(false);
                holidayRepository.save(holiday);
            }
        }
    }

    public List<Holiday> getHolidaysForYear(int year) {
        var start = LocalDate.of(year, 1, 1);
        var end = LocalDate.of(year, 12, 31);
        return holidayRepository.findByHolidayDateBetweenOrderByHolidayDateAsc(start, end);
    }

    public boolean isHoliday(LocalDate date) {
        return holidayRepository.existsByHolidayDate(date);
    }

    /**
     * Returns the number of national holidays that fall on a weekend for the given year.
     */
    public long getWeekendHolidayCount(int year) {
        var holidays = BelgianHolidayCalculator.getHolidays(year);
        return holidays.keySet().stream()
                .filter(date -> date.getDayOfWeek() == DayOfWeek.SATURDAY
                        || date.getDayOfWeek() == DayOfWeek.SUNDAY)
                .count();
    }

    /**
     * Returns the number of compensatory days already scheduled for the given year.
     */
    public long getScheduledCompensatoryCount(int year) {
        var start = LocalDate.of(year, 1, 1);
        var end = LocalDate.of(year, 12, 31);
        return holidayRepository.countByHolidayTypeAndHolidayDateBetween(HolidayType.COMPENSATORY, start, end);
    }

    /**
     * Returns the number of compensatory days remaining to be scheduled for the given year.
     */
    public long getRemainingCompensatoryCount(int year) {
        return getWeekendHolidayCount(year) - getScheduledCompensatoryCount(year);
    }

    @Transactional
    public Holiday addCompanyHoliday(LocalDate date, String name) {
        var holiday = new Holiday();
        holiday.setHolidayDate(date);
        holiday.setName(name);
        holiday.setHolidayType(HolidayType.COMPANY);
        holiday.setEditable(true);
        return holidayRepository.save(holiday);
    }

    @Transactional
    public Holiday addCompensatoryHoliday(LocalDate date, String name) {
        var holiday = new Holiday();
        holiday.setHolidayDate(date);
        holiday.setName(name);
        holiday.setHolidayType(HolidayType.COMPENSATORY);
        holiday.setEditable(true);
        return holidayRepository.save(holiday);
    }

    @Transactional
    public void deleteHoliday(Long id) {
        holidayRepository.findById(id)
                .filter(Holiday::isEditable)
                .ifPresent(holidayRepository::delete);
    }
}
