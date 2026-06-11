package alik.utilitymeter.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "meter_readings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeterReading {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "meter_id", nullable = false)
  private Meter meter;

  /**
   * The absolute value shown on the meter display
   * Must always be >= the previous reading value (meters never go backwards)
   */
  @Column(name = "value", nullable = false, precision = 12, scale = 3)
  private BigDecimal value;

  /**
   * Year component of the reading period (e.g. 2024)
   */
  @Column(name = "reading_year", nullable = false)
  private int readingYear;

  /**
   * Month component of the reading period (1–12)
   */
  @Column(name = "reading_month", nullable = false)
  private int readingMonth;

  @Column(name = "note", length = 500)
  private String note;

  /**
   * Keycloak subject of whoever submitted this reading.
   */
  @Column(name = "submitted_by", nullable = false)
  private String submittedBy;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public YearMonth getYearMonth() {
    return YearMonth.of(readingYear, readingMonth);
  }

  public static MeterReading forMonth(YearMonth ym) {
    return MeterReading.builder()
        .readingYear(ym.getYear())
        .readingMonth(ym.getMonthValue())
        .build();
  }
}
