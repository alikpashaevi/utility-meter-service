package alik.utilitymeter.repository;

import alik.utilitymeter.entity.MeterReading;
import java.util.List;
import java.util.UUID;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MeterReadingRepository extends JpaRepository<MeterReading, UUID> {

  List<MeterReading> findByMeterIdOrderByReadingDateAsc(UUID meterId);

  Page<MeterReading> findByMeterId(UUID meterId, Pageable pageable);

  @Query("SELECT r FROM MeterReading r WHERE r.meter.id = :meterId AND r.readingDate < :date ORDER BY r.readingDate DESC")
  Page<MeterReading> findPreviousReading(@Param("meterId") UUID meterId, @Param("date") LocalDate date, Pageable pageable);

  @Query("SELECT r FROM MeterReading r WHERE r.meter.id = :meterId AND r.readingDate > :date ORDER BY r.readingDate ASC")
  Page<MeterReading> findNextReading(@Param("meterId") UUID meterId, @Param("date") LocalDate date, Pageable pageable);

  boolean existsByMeterIdAndReadingDate(UUID meterId, LocalDate readingDate);
}
