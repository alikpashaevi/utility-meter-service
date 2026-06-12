package alik.utilitymeter.service;

import alik.utilitymeter.dto.internal.AuthenticatedUser;
import alik.utilitymeter.dto.request.MeterReadingRequest;
import alik.utilitymeter.dto.response.MeterReadingResponse;
import alik.utilitymeter.entity.Meter;
import alik.utilitymeter.entity.MeterReading;
import alik.utilitymeter.exception.BadRequestException;
import alik.utilitymeter.exception.ConflictException;
import alik.utilitymeter.exception.NotFoundException;
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
    LocalDate requestDate = toReadingDate(request);

    checkDuplicateDate(meterId, requestDate, null);
    MeterReading previousReading = validateChronologicalOrder(meterId, requestDate, request.getValue(), null);

    MeterReading reading = meterReadingMapper.toEntity(request);
    reading.setMeter(meter);
    reading.setSubmittedBy(currentUser.keycloakSubject());

    MeterReading saved = meterReadingRepository.saveAndFlush(reading);
    return buildResponse(saved, previousReading);
  }

  @Transactional
  public MeterReadingResponse modifyReading(UUID meterId, UUID readingId, MeterReadingRequest request, AuthenticatedUser currentUser) {
    log.info("Modifying reading: {} for meter: {} by user: {}", readingId, meterId, currentUser.id());

    meterService.getMeterAndVerifyAccess(meterId, currentUser.id(), currentUser.role());

    MeterReading reading = meterReadingRepository.findById(readingId)
        .orElseThrow(() -> new NotFoundException("Meter reading not found with id: " + readingId));

    if (!reading.getMeter().getId().equals(meterId)) {
      throw new BadRequestException("Reading does not belong to the specified meter");
    }

    LocalDate requestDate = toReadingDate(request);

    checkDuplicateDate(meterId, requestDate, readingId);
    MeterReading previousReading = validateChronologicalOrder(meterId, requestDate, request.getValue(), readingId);

    reading.setValue(request.getValue());
    reading.setReadingDate(requestDate);
    reading.setNote(request.getNote());

    MeterReading saved = meterReadingRepository.saveAndFlush(reading);
    return buildResponse(saved, previousReading);
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

  private LocalDate toReadingDate(MeterReadingRequest request) {
    return LocalDate.of(request.getReadingYear(), request.getReadingMonth(), 1);
  }

  private void checkDuplicateDate(UUID meterId, LocalDate date, UUID excludeId) {
    boolean exists = (excludeId == null)
        ? meterReadingRepository.existsByMeterIdAndReadingDate(meterId, date)
        : meterReadingRepository.existsByMeterIdAndReadingDateAndIdNot(meterId, date, excludeId);

    if (exists) {
      throw new ConflictException("A reading already exists for this year and month: "
          + date.getYear() + "-" + date.getMonthValue());
    }
  }

  private MeterReading validateChronologicalOrder(UUID meterId, LocalDate date, BigDecimal value, UUID excludeId) {
    MeterReading prev = findPreviousReading(meterId, date, excludeId);
    if (prev != null && prev.getValue().compareTo(value) > 0) {
      throw new BadRequestException("Reading value must be greater than or equal to the previous reading: "
          + prev.getValue() + " from " + prev.getReadingDate().getYear() + "-" + prev.getReadingDate().getMonthValue());
    }

    MeterReading next = findNextReading(meterId, date, excludeId);
    if (next != null && next.getValue().compareTo(value) < 0) {
      throw new BadRequestException("Reading value must be less than or equal to the subsequent reading: "
          + next.getValue() + " from " + next.getReadingDate().getYear() + "-" + next.getReadingDate().getMonthValue());
    }

    return prev;
  }

  private MeterReading findPreviousReading(UUID meterId, LocalDate date, UUID excludeId) {
    Page<MeterReading> page = (excludeId == null)
        ? meterReadingRepository.findPreviousReading(meterId, date, PageRequest.of(0, 1))
        : meterReadingRepository.findPreviousReadingExcludingId(meterId, date, excludeId, PageRequest.of(0, 1));
    return page.hasContent() ? page.getContent().getFirst() : null;
  }

  private MeterReading findNextReading(UUID meterId, LocalDate date, UUID excludeId) {
    Page<MeterReading> page = (excludeId == null)
        ? meterReadingRepository.findNextReading(meterId, date, PageRequest.of(0, 1))
        : meterReadingRepository.findNextReadingExcludingId(meterId, date, excludeId, PageRequest.of(0, 1));
    return page.hasContent() ? page.getContent().getFirst() : null;
  }

  private MeterReadingResponse buildResponse(MeterReading saved, MeterReading previousReading) {
    BigDecimal previousValue = previousReading != null ? previousReading.getValue() : BigDecimal.ZERO;
    BigDecimal monthlyConsumption = saved.getValue().subtract(previousValue);

    MeterReadingResponse response = meterReadingMapper.toResponseDto(saved);
    response.setMonthlyConsumption(monthlyConsumption);
    return response;
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
    MeterReading prevBeforePage = findPreviousReading(meterId, minDate, null);

    List<MeterReading> chronological = new ArrayList<>();
    if (prevBeforePage != null) {
      chronological.add(prevBeforePage);
    }
    chronological.addAll(rangeReadings);

    Map<UUID, BigDecimal> consumptions = new HashMap<>();

    for (MeterReading current : rangeReadings) {
      int indexInChronological = chronological.indexOf(current);

      BigDecimal prevVal = (indexInChronological > 0)
          ? chronological.get(indexInChronological - 1).getValue()
          : BigDecimal.ZERO;

      consumptions.put(current.getId(), current.getValue().subtract(prevVal));
    }

    return consumptions;
  }
}
