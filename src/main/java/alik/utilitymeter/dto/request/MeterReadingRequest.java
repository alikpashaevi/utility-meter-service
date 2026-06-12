package alik.utilitymeter.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeterReadingRequest {

  @NotNull(message = "Reading value is required")
  @DecimalMin(value = "0.0", message = "Reading value must be non-negative")
  private BigDecimal value;

  @NotNull(message = "Reading year is required")
  @Min(value = 2000, message = "Reading year must be after 2000")
  @Max(value = 2100, message = "Reading year must be before 2100")
  private Integer readingYear;

  @NotNull(message = "Reading month is required")
  @Min(value = 1, message = "Reading month must be between 1 and 12")
  @Max(value = 12, message = "Reading month must be between 1 and 12")
  private Integer readingMonth;

  @Size(max = 500, message = "Note cannot exceed 500 characters")
  private String note;
}
