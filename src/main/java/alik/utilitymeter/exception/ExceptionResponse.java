package alik.utilitymeter.exception;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ExceptionResponse {
  private Instant timestamp;
  private int status;
  private String message;
  private String path;
  private String errorCode;
}
