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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    MeterReading previousReading = validateReading(meterId, requestDate, request.getValue());

    MeterReading reading = meterReadingMapper.toEntity(request);
    reading.setMeter(meter);
    reading.setSubmittedBy(currentUser.keycloakSubject());

    MeterReading saved = meterReadingRepository.saveAndFlush(reading);

    BigDecimal previousValue = previousReading != null ? previousReading.getValue() : BigDecimal.ZERO;
    BigDecimal monthlyConsumption = saved.getValue().subtract(previousValue);

    MeterReadingResponse response = meterReadingMapper.toResponseDto(saved);
    response.setMonthlyConsumption(monthlyConsumption);
    return response;
  }

  private MeterReading validateReading(UUID meterId, LocalDate requestDate, BigDecimal requestValue) {
    if (meterReadingRepository.existsByMeterIdAndReadingDate(meterId, requestDate)) {
      throw new ConflictException("A reading already exists for this year and month: "
          + requestDate.getYear() + "-" + requestDate.getMonthValue());
    }

    Page<MeterReading> previousPage = meterReadingRepository.findPreviousReading(
        meterId, requestDate, PageRequest.of(0, 1));
    MeterReading prev = null;
    if (previousPage.hasContent()) {
      prev = previousPage.getContent().getFirst();
      if (prev.getValue().compareTo(requestValue) > 0) {
        throw new BadRequestException("Reading value must be greater than or equal to the previous reading: "
            + prev.getValue() + " from " + prev.getReadingDate().getYear() + "-" + prev.getReadingDate().getMonthValue());
      }
    }

    Page<MeterReading> nextPage = meterReadingRepository.findNextReading(
        meterId, requestDate, PageRequest.of(0, 1));
    if (nextPage.hasContent()) {
      MeterReading next = nextPage.getContent().getFirst();
      if (next.getValue().compareTo(requestValue) < 0) {
        throw new BadRequestException("Reading value must be less than or equal to the subsequent reading: "
            + next.getValue() + " from " + next.getReadingDate().getYear() + "-" + next.getReadingDate().getMonthValue());
      }
    }

    return prev;
  }

  @Transactional(readOnly = true)
  public Page<MeterReadingResponse> getReadingsForMeter(UUID meterId, Pageable pageable, AuthenticatedUser currentUser) {
    log.info("Fetching readings for meter: {} by user: {}", meterId, currentUser.id());

    meterService.getMeterAndVerifyAccess(meterId, currentUser.id(), currentUser.role());

    Page<MeterReading> readingsPage = meterReadingRepository.findByMeterId(meterId, pageable);

    if (readingsPage.isEmpty()) {
      return Page.empty(pageable);
    }

    Map<UUID, BigDecimal> consumptions = calculateConsumptionsForPage(meterId, readingsPage);

    return readingsPage.map(current -> {
      MeterReadingResponse res = meterReadingMapper.toResponseDto(current);
      res.setMonthlyConsumption(consumptions.getOrDefault(current.getId(), BigDecimal.ZERO));
      return res;
    });
  }

  private Map<UUID, BigDecimal> calculateConsumptionsForPage(UUID meterId, Page<MeterReading> readingsPage) {
    LocalDate minDate = readingsPage.stream()
        .map(MeterReading::getReadingDate)
        .min(LocalDate::compareTo)
        .orElse(LocalDate.now());

    LocalDate maxDate = readingsPage.stream()
        .map(MeterReading::getReadingDate)
        .max(LocalDate::compareTo)
        .orElse(LocalDate.now());

    List<MeterReading> rangeReadings = meterReadingRepository.findByMeterIdAndReadingDateBetweenOrderByReadingDateAsc(meterId, minDate, maxDate);
    Page<MeterReading> prevPage = meterReadingRepository.findPreviousReading(meterId, minDate, PageRequest.of(0, 1));

    List<MeterReading> chronological = new ArrayList<>();
    if (prevPage.hasContent()) {
      chronological.add(prevPage.getContent().getFirst());
    }
    chronological.addAll(rangeReadings);

    Map<UUID, BigDecimal> consumptions = new HashMap<>();
    for (int i = 0; i < chronological.size(); i++) {
      MeterReading current = chronological.get(i);
      BigDecimal prevVal = (i > 0) ? chronological.get(i - 1).getValue() : BigDecimal.ZERO;
      consumptions.put(current.getId(), current.getValue().subtract(prevVal));
    }
    return consumptions;
  }
}
