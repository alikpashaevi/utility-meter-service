package alik.utilitymeter.dto.response;

import alik.utilitymeter.enums.MeterType;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MeterResponse {
  private UUID id;
  private UUID userId;
  private MeterType type;
  private String serialNumber;
  private String unit;
  private boolean active;
  private LocalDateTime createdAt;
}
