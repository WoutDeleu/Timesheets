package be.axxes.timesheets.repository;

import be.axxes.timesheets.model.Holiday;
import be.axxes.timesheets.model.HolidayType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    List<Holiday> findByHolidayDateBetweenOrderByHolidayDateAsc(LocalDate start, LocalDate end);

    Optional<Holiday> findByHolidayDate(LocalDate date);

    boolean existsByHolidayDate(LocalDate date);

    long countByHolidayTypeAndHolidayDateBetween(HolidayType type, LocalDate start, LocalDate end);
}
