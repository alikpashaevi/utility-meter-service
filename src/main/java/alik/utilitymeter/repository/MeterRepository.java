package alik.utilitymeter.repository;

import aj.org.objectweb.asm.commons.Remapper;
import alik.utilitymeter.dto.response.MeterResponse;
import alik.utilitymeter.entity.Meter;
import alik.utilitymeter.enums.MeterType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MeterRepository extends JpaRepository<Meter, UUID> {
  Optional<Meter> findBySerialNumber(String serialNumber);

  boolean existsByUserIdAndActiveTrue(UUID id);

  boolean existsByUserIdAndTypeAndActiveTrue(UUID id, MeterType type);

  List<Meter> findByUserIdAndActiveTrue(UUID userId);

  Optional<Meter> findByIdAndUserIdAndActiveTrue(UUID meterId, UUID userId);
}
