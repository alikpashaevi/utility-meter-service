package alik.utilitymeter.dto.request;

import alik.utilitymeter.enums.MeterType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeterRegistrationRequest {

  @NotNull(message = "Meter type is required")
  private MeterType type;

  @NotBlank(message = "Meter label is required")
  @Size(max = 255, message = "Meter label cannot exceed 255 characters")
  private String label;

  @NotBlank(message = "Serial number is required")
  @Size(max = 100, message = "Serial number cannot exceed 100 characters")
  private String serialNumber;

  @NotBlank(message = "Unit of measurement is required")
  @Size(max = 20, message = "Unit cannot exceed 20 characters")
  private String unit;


}
