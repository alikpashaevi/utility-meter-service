package alik.utilitymeter.service;

import alik.utilitymeter.dto.request.MeterRegistrationRequest;
import alik.utilitymeter.dto.response.MeterResponse;
import alik.utilitymeter.entity.Meter;
import alik.utilitymeter.entity.User;
import alik.utilitymeter.enums.Role;
import alik.utilitymeter.exception.BadRequestException;
import alik.utilitymeter.exception.ConflictException;
import alik.utilitymeter.mapper.MeterMapper;
import alik.utilitymeter.repository.MeterRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeterService {

  private final MeterRepository meterRepository;
  private final UserService userService;
  private final MeterMapper meterMapper;

  @Transactional
  public MeterResponse registerMeter(MeterRegistrationRequest request, UUID userId) {
    log.info("Registering new meter for user: {}", userId);
    User user = userService.getUserById(userId);

    meterRepository.findBySerialNumber(request.getSerialNumber())
        .ifPresent(existingMeter -> {
          throw new ConflictException("Meter with this serial number already exists");
        });

    if (meterRepository.existsByUserIdAndTypeAndActiveTrue(user.getId(), request.getType())) {
      throw new BadRequestException("User already has an active meter of this type");
    }

    Meter meter = meterRepository.saveAndFlush(meterMapper.toEntity(request, user));
    return meterMapper.toResponseDto(meter);

  }

  @Transactional(readOnly = true)
  public List<MeterResponse> getActiveMeters(UUID userId, Role role) {
    log.info("Fetching meters for user: {}, role: {}", userId, role);
    List<Meter> meters = (role == Role.ADMIN)
        ? meterRepository.findByActiveTrue()
        : meterRepository.findByUserIdAndActiveTrue(userId);

    return meters.stream()
        .map(meterMapper::toResponseDto)
        .toList();
  }

  @Transactional
  public void deactivateMeter(UUID meterId, UUID userId, Role role) {
    log.info("Deactivating meter: {} for user: {}, role: {}", meterId, userId, role);
    Meter meter = getMeterAndVerifyAccess(meterId, userId, role);

    meter.setActive(false);
    meterRepository.saveAndFlush(meter);
  }

  private Meter getMeterAndVerifyAccess(UUID meterId, UUID userId, Role role) {
    Meter meter = meterRepository.findById(meterId)
        .filter(Meter::isActive)
        .orElseThrow(() -> new BadRequestException("Active meter not found for this user"));

    if (role != Role.ADMIN && !meter.getUser().getId().equals(userId)) {
      throw new BadRequestException("Active meter not found for this user");
    }
    return meter;
  }

}
