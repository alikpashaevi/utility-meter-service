package alik.utilitymeter.mapper;

import alik.utilitymeter.dto.request.MeterReadingRequest;
import alik.utilitymeter.dto.response.MeterReadingResponse;
import alik.utilitymeter.entity.MeterReading;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MeterReadingMapper {

  @Mapping(target = "meterId", source = "meter.id")
  @Mapping(target = "monthlyConsumption", ignore = true)
  @Mapping(target = "readingYear", expression = "java(reading.getReadingDate().getYear())")
  @Mapping(target = "readingMonth", expression = "java(reading.getReadingDate().getMonthValue())")
  MeterReadingResponse toResponseDto(MeterReading reading);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "meter", ignore = true)
  @Mapping(target = "submittedBy", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "readingDate", expression = "java(java.time.LocalDate.of(dto.getReadingYear(), dto.getReadingMonth(), 1))")
  MeterReading toEntity(MeterReadingRequest dto);
}
