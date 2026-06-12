package alik.utilitymeter.controller;

import alik.utilitymeter.config.CurrentUser;
import alik.utilitymeter.dto.internal.AuthenticatedUser;
import alik.utilitymeter.dto.request.MeterRegistrationRequest;
import alik.utilitymeter.dto.response.MeterResponse;
import alik.utilitymeter.service.MeterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/meters")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Meters", description = "Endpoints for registering, listing, and deactivating utility meters.")
public class MeterController {

  private final MeterService meterService;

  @GetMapping
  @Operation(
      summary = "List active meters",
      description = "Returns all active meters that belong to the authenticated user."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Active meters retrieved successfully"),
      @ApiResponse(responseCode = "401", description = "Authentication is required"),
      @ApiResponse(responseCode = "500", description = "Unexpected server error")
  })
  public ResponseEntity<List<MeterResponse>> getMeters(@Parameter(hidden = true) @CurrentUser AuthenticatedUser user) {
    log.debug("Fetching meters for user: {}, role: {}", user.id(), user.role());
    return ResponseEntity.ok(meterService.getActiveMeters(user.id(), user.role()));
  }

  @PostMapping
  @Operation(
      summary = "Register a new meter",
      description = "Creates a new meter for the authenticated user if the serial number is unique and the user does not already have an active meter of the same type."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Meter created successfully"),
      @ApiResponse(responseCode = "400", description = "Request is invalid or business rules were violated"),
      @ApiResponse(responseCode = "401", description = "Authentication is required"),
      @ApiResponse(responseCode = "409", description = "A meter with the same serial number already exists"),
      @ApiResponse(responseCode = "500", description = "Unexpected server error")
  })
  public ResponseEntity<MeterResponse> createMeter(@RequestBody @Valid MeterRegistrationRequest request,
                                            @Parameter(hidden = true) @CurrentUser AuthenticatedUser user) {
    log.debug("Creating meter for user: {}", user.id());
    MeterResponse res = meterService.registerMeter(request, user.id());
    return ResponseEntity.status(HttpStatus.CREATED).body(res);
  }

  @DeleteMapping("/{id}")
  @Operation(
      summary = "Deactivate a meter",
      description = "Marks one of the authenticated user's active meters as inactive."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Meter deactivated successfully"),
      @ApiResponse(responseCode = "400", description = "The meter is already deactivated or does not belong to the user"),
      @ApiResponse(responseCode = "401", description = "Authentication is required"),
      @ApiResponse(responseCode = "404", description = "Meter not found"),
      @ApiResponse(responseCode = "500", description = "Unexpected server error")
  })
  public ResponseEntity<Void> deleteMeter(@PathVariable("id") UUID id,
                                          @Parameter(hidden = true) @CurrentUser AuthenticatedUser user) {
    log.debug("Deleting meter for user: {}, role: {}", user.id(), user.role());
    meterService.deactivateMeter(id, user.id(), user.role());
    return ResponseEntity.ok().build();
  }
}
