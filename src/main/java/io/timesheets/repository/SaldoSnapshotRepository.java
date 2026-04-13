package io.timesheets.repository;

import io.timesheets.model.SaldoSnapshot;
import io.timesheets.model.SaldoType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SaldoSnapshotRepository extends JpaRepository<SaldoSnapshot, Long> {

    Optional<SaldoSnapshot> findTopBySaldoTypeOrderBySnapshotDateDesc(SaldoType saldoType);

    Optional<SaldoSnapshot> findTopBySaldoTypeAndProjectIdOrderBySnapshotDateDesc(SaldoType saldoType, Long projectId);

    List<SaldoSnapshot> findBySaldoTypeAndSnapshotDateBetweenOrderBySnapshotDateAsc(
            SaldoType saldoType, LocalDate start, LocalDate end);
}
