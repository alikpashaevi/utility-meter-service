package alik.utilitymeter.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import alik.utilitymeter.dto.internal.AuthenticatedUser;
import alik.utilitymeter.dto.request.MeterReadingRequest;
import alik.utilitymeter.dto.response.MeterReadingResponse;
import alik.utilitymeter.entity.Meter;
import alik.utilitymeter.entity.MeterReading;
import alik.utilitymeter.enums.MeterType;
import alik.utilitymeter.enums.Role;
import alik.utilitymeter.exception.ConflictException;
import alik.utilitymeter.mapper.MeterReadingMapper;
import alik.utilitymeter.repository.MeterReadingRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MeterReadingServiceTest {

  @Mock
  private MeterReadingRepository meterReadingRepository;

  @Mock
  private MeterService meterService;


  @InjectMocks
  private MeterReadingService meterReadingService;

  private static final UUID METER_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();
  private static final String KC_SUBJECT = "kc-subject-123";
  private static final AuthenticatedUser USER =
      new AuthenticatedUser(USER_ID, KC_SUBJECT, "test@example.com", Role.USER);

  @Test
  void submitReading_shouldThrowConflict_whenReadingAlreadyExistsForMonth() {
    MeterReadingRequest request = MeterReadingRequest.builder()
        .value(new BigDecimal("200.000"))
        .readingYear(2025)
        .readingMonth(3)
        .build();

    Meter meter = Meter.builder().id(METER_ID).build();
    LocalDate requestDate = LocalDate.of(2025, 3, 1);

    when(meterService.getMeterAndVerifyAccess(METER_ID, USER_ID, Role.USER)).thenReturn(meter);
    when(meterReadingRepository.existsByMeterIdAndReadingDate(METER_ID, requestDate)).thenReturn(true);

    assertThatThrownBy(() -> meterReadingService.submitReading(METER_ID, request, USER))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("A reading already exists for this year and month");
  }
}
