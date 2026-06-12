package alik.utilitymeter.service;

import alik.utilitymeter.dto.internal.AuthenticatedUser;
import alik.utilitymeter.dto.request.MeterReadingRequest;
import alik.utilitymeter.dto.response.MeterReadingResponse;
import alik.utilitymeter.entity.Meter;
import alik.utilitymeter.entity.MeterReading;
import alik.utilitymeter.exception.BadRequestException;
import alik.utilitymeter.exception.ConflictException;
import alik.utilitymeter.mapper.MeterReadingMapper;
import alik.utilitymeter.repository.MeterReadingRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeterReadingService {

  private final MeterReadingRepository meterReadingRepository;
  private final MeterService meterService;
  private final MeterReadingMapper meterReadingMapper;

  @Transactional
  public MeterReadingResponse submitReading(UUID meterId, MeterReadingRequest request, AuthenticatedUser currentUser) {
    log.info("Submitting reading for meter: {} by user: {}", meterId, currentUser.id());

    Meter meter = meterService.getMeterAndVerifyAccess(meterId, currentUser.id(), currentUser.role());

    LocalDate requestDate = LocalDate.of(request.getReadingYear(), request.getReadingMonth(), 1);

    if (meterReadingRepository.existsByMeterIdAndReadingDate(meterId, requestDate)) {
      throw new ConflictException("A reading already exists for this year and month: "
          + request.getReadingYear() + "-" + request.getReadingMonth());
    }

    Page<MeterReading> previousPage = meterReadingRepository.findPreviousReading(
        meterId, requestDate, PageRequest.of(0, 1));
    if (previousPage.hasContent()) {
      MeterReading prev = previousPage.getContent().getFirst();
      if (prev.getValue().compareTo(request.getValue()) > 0) {
        throw new BadRequestException("Reading value must be greater than or equal to the previous reading: "
            + prev.getValue() + " from " + prev.getReadingDate().getYear() + "-" + prev.getReadingDate().getMonthValue());
      }
    }

    Page<MeterReading> nextPage = meterReadingRepository.findNextReading(
        meterId, requestDate, PageRequest.of(0, 1));
    if (nextPage.hasContent()) {
      MeterReading next = nextPage.getContent().getFirst();
      if (next.getValue().compareTo(request.getValue()) < 0) {
        throw new BadRequestException("Reading value must be less than or equal to the subsequent reading: "
            + next.getValue() + " from " + next.getReadingDate().getYear() + "-" + next.getReadingDate().getMonthValue());
      }
    }

    MeterReading reading = meterReadingMapper.toEntity(request);
    reading.setMeter(meter);
    reading.setSubmittedBy(currentUser.keycloakSubject());

    MeterReading saved = meterReadingRepository.saveAndFlush(reading);

    BigDecimal previousValue = previousPage.hasContent() ? previousPage.getContent().getFirst().getValue() : BigDecimal.ZERO;
    BigDecimal monthlyConsumption = saved.getValue().subtract(previousValue);

    MeterReadingResponse response = meterReadingMapper.toResponseDto(saved);
    response.setMonthlyConsumption(monthlyConsumption);
    return response;
  }

  @Transactional(readOnly = true)
  public Page<MeterReadingResponse> getReadingsForMeter(UUID meterId, Pageable pageable, AuthenticatedUser currentUser) {
    log.info("Fetching readings for meter: {} by user: {}", meterId, currentUser.id());

    meterService.getMeterAndVerifyAccess(meterId, currentUser.id(), currentUser.role());

    Page<MeterReading> readingsPage = meterReadingRepository.findByMeterId(meterId, pageable);

    return readingsPage.map(current -> {
      MeterReadingResponse res = meterReadingMapper.toResponseDto(current);

      Page<MeterReading> previousPage = meterReadingRepository.findPreviousReading(
          meterId, current.getReadingDate(), PageRequest.of(0, 1));
      
      BigDecimal previousValue = previousPage.hasContent() ? previousPage.getContent().getFirst().getValue() : BigDecimal.ZERO;
      BigDecimal consumption = current.getValue().subtract(previousValue);
      res.setMonthlyConsumption(consumption);

      return res;
    });
  }
}
