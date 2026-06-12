package alik.utilitymeter.controller;

import alik.utilitymeter.config.CurrentUser;
import alik.utilitymeter.dto.internal.AuthenticatedUser;
import alik.utilitymeter.dto.request.MeterReadingRequest;
import alik.utilitymeter.dto.response.MeterReadingResponse;
import alik.utilitymeter.service.MeterReadingService;
import jakarta.validation.Valid;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/meters/{meterId}/readings")
@RequiredArgsConstructor
@Slf4j
public class MeterReadingController {

  private final MeterReadingService meterReadingService;

  @PostMapping
  public ResponseEntity<MeterReadingResponse> submitReading(
      @PathVariable("meterId") UUID meterId,
      @RequestBody @Valid MeterReadingRequest request,
      @CurrentUser AuthenticatedUser user
  ) {
    log.debug("API call to submit reading for meter: {} by user: {}", meterId, user.id());
    MeterReadingResponse response = meterReadingService.submitReading(meterId, request, user);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping
  public ResponseEntity<Page<MeterReadingResponse>> getReadings(
      @PathVariable("meterId") UUID meterId,
      @ParameterObject @PageableDefault(sort = {"readingDate"}, direction = Sort.Direction.DESC) Pageable pageable,
      @CurrentUser AuthenticatedUser user
  ) {
    log.debug("API call to fetch readings for meter: {} by user: {}", meterId, user.id());
    Page<MeterReadingResponse> responses = meterReadingService.getReadingsForMeter(meterId, pageable, user);
    return ResponseEntity.ok(responses);
  }

  @PutMapping("/{readingId}")
  public ResponseEntity<MeterReadingResponse> modifyReading(
      @PathVariable("meterId") UUID meterId,
      @PathVariable("readingId") UUID readingId,
      @RequestBody @Valid MeterReadingRequest request,
      @CurrentUser AuthenticatedUser user
  ) {
    log.debug("API call to modify reading: {} for meter: {} by user: {}", readingId, meterId, user.id());
    MeterReadingResponse response = meterReadingService.modifyReading(meterId, readingId, request, user);
    return ResponseEntity.ok(response);
  }
}
