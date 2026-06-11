package alik.utilitymeter.mapper;

import alik.utilitymeter.dto.request.MeterRegistrationRequest;
import alik.utilitymeter.dto.response.MeterResponse;
import alik.utilitymeter.entity.Meter;
import alik.utilitymeter.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MeterMapper {

  @Mapping(target = "userId", source = "user.id")
  MeterResponse toResponseDto(Meter meter);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "active", constant = "true")
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "readings", ignore = true)
  @Mapping(target = "user", source = "user")
  Meter toEntity(MeterRegistrationRequest dto, User user);
}
