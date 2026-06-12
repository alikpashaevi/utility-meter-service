package alik.utilitymeter.controller;

import alik.utilitymeter.config.CurrentUser;
import alik.utilitymeter.dto.internal.AuthenticatedUser;
import alik.utilitymeter.dto.request.MeterReadingRequest;
import alik.utilitymeter.dto.response.MeterReadingResponse;
import alik.utilitymeter.service.MeterReadingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@Tag(name = "Meter Readings", description = "Endpoints for creating, listing, and updating meter readings.")
public class MeterReadingController {

  private final MeterReadingService meterReadingService;

  @PostMapping
  @Operation(
      summary = "Submit a meter reading",
      description = "Creates a new reading for the specified meter after verifying access, reading date uniqueness, and chronological ordering."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Reading created successfully"),
      @ApiResponse(responseCode = "400", description = "The reading is invalid or violates business rules"),
      @ApiResponse(responseCode = "401", description = "Authentication is required"),
      @ApiResponse(responseCode = "403", description = "The user does not have access to the meter"),
      @ApiResponse(responseCode = "404", description = "Meter not found"),
      @ApiResponse(responseCode = "409", description = "A reading already exists for the same month and year"),
      @ApiResponse(responseCode = "500", description = "Unexpected server error")
  })
  public ResponseEntity<MeterReadingResponse> submitReading(
      @Parameter(description = "Identifier of the meter that will receive the new reading") @PathVariable("meterId") UUID meterId,
      @RequestBody @Valid MeterReadingRequest request,
      @Parameter(hidden = true) @CurrentUser AuthenticatedUser user
  ) {
    log.debug("API call to submit reading for meter: {} by user: {}", meterId, user.id());
    MeterReadingResponse response = meterReadingService.submitReading(meterId, request, user);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping
  @Operation(
      summary = "List meter readings",
      description = "Returns paginated meter readings for the specified meter, sorted by reading date in descending order by default."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Meter readings retrieved successfully"),
      @ApiResponse(responseCode = "401", description = "Authentication is required"),
      @ApiResponse(responseCode = "403", description = "The user does not have access to the meter"),
      @ApiResponse(responseCode = "404", description = "Meter not found"),
      @ApiResponse(responseCode = "500", description = "Unexpected server error")
  })
  public ResponseEntity<Page<MeterReadingResponse>> getReadings(
      @Parameter(description = "Identifier of the meter whose readings should be returned") @PathVariable("meterId") UUID meterId,
      @ParameterObject @PageableDefault(sort = {"readingDate"}, direction = Sort.Direction.DESC) Pageable pageable,
      @Parameter(hidden = true) @CurrentUser AuthenticatedUser user
  ) {
    log.debug("API call to fetch readings for meter: {} by user: {}", meterId, user.id());
    Page<MeterReadingResponse> responses = meterReadingService.getReadingsForMeter(meterId, pageable, user);
    return ResponseEntity.ok(responses);
  }

  @PutMapping("/{readingId}")
  @Operation(
      summary = "Update a meter reading",
      description = "Updates an existing reading for the specified meter after verifying ownership and chronological constraints."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Reading updated successfully"),
      @ApiResponse(responseCode = "400", description = "The reading is invalid or does not belong to the specified meter"),
      @ApiResponse(responseCode = "401", description = "Authentication is required"),
      @ApiResponse(responseCode = "403", description = "The user does not have access to the meter"),
      @ApiResponse(responseCode = "404", description = "Meter reading not found"),
      @ApiResponse(responseCode = "409", description = "A reading already exists for the same month and year"),
      @ApiResponse(responseCode = "500", description = "Unexpected server error")
  })
  public ResponseEntity<MeterReadingResponse> modifyReading(
      @Parameter(description = "Identifier of the meter that owns the reading") @PathVariable("meterId") UUID meterId,
      @Parameter(description = "Identifier of the reading to update") @PathVariable("readingId") UUID readingId,
      @RequestBody @Valid MeterReadingRequest request,
      @Parameter(hidden = true) @CurrentUser AuthenticatedUser user
  ) {
    log.debug("API call to modify reading: {} for meter: {} by user: {}", readingId, meterId, user.id());
    MeterReadingResponse response = meterReadingService.modifyReading(meterId, readingId, request, user);
    return ResponseEntity.ok(response);
  }
}
