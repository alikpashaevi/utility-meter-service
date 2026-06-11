package alik.utilitymeter.repository;

import alik.utilitymeter.entity.MeterReading;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MeterReadingRepository extends JpaRepository<MeterReading, UUID> {
}
