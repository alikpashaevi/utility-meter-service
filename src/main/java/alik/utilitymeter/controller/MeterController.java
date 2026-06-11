package alik.utilitymeter.controller;

import alik.utilitymeter.config.CurrentUser;
import alik.utilitymeter.dto.internal.AuthenticatedUser;
import alik.utilitymeter.dto.request.MeterRegistrationRequest;
import alik.utilitymeter.dto.response.MeterResponse;
import alik.utilitymeter.service.MeterService;
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
public class MeterController {

  private final MeterService meterService;

  @GetMapping
  public ResponseEntity<List<MeterResponse>> getMeters(@CurrentUser AuthenticatedUser user) {
    log.debug("Fetching meters for user: {}", user.id());
    return ResponseEntity.ok(meterService.getActiveMeters(user.id()));
  }

  @PostMapping
  public ResponseEntity<MeterResponse> createMeter(@RequestBody @Valid MeterRegistrationRequest request,
                                            @CurrentUser AuthenticatedUser user) {
    log.debug("Creating meter for user: {}", user.id());
    MeterResponse res = meterService.registerMeter(request, user.id());
    return ResponseEntity.status(HttpStatus.CREATED).body(res);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteMeter(@PathVariable("id") UUID id,
                                          @CurrentUser AuthenticatedUser user) {
    log.debug("Deleting meter for user: {}", user.id());
    meterService.deactivateMeter(id, user.id());
    return ResponseEntity.ok().build();
  }
}
