package alik.utilitymeter.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeterReadingResponse {
  private UUID id;
  private UUID meterId;
  private BigDecimal value;
  private int readingYear;
  private int readingMonth;
  private String note;
  private String submittedBy;
  private LocalDateTime createdAt;
  private BigDecimal monthlyConsumption;
}
