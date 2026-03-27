package be.axxes.timesheets.repository;

import be.axxes.timesheets.model.InternalActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface InternalActivityRepository extends JpaRepository<InternalActivity, Long> {

    List<InternalActivity> findByActivityDateBetweenOrderByActivityDateAsc(LocalDate start, LocalDate end);

    List<InternalActivity> findByActivityDate(LocalDate date);
}
