package alik.utilitymeter.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import alik.utilitymeter.dto.internal.AuthenticatedUser;
import alik.utilitymeter.dto.response.MeterReadingResponse;
import alik.utilitymeter.entity.User;
import alik.utilitymeter.enums.Role;
import alik.utilitymeter.security.JwksValidator;
import alik.utilitymeter.service.MeterReadingService;
import alik.utilitymeter.service.UserSyncService;
import com.nimbusds.jwt.JWTClaimsSet;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MeterReadingController.class)
class MeterReadingControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private MeterReadingService meterReadingService;

  @MockitoBean
  private JwksValidator jwksValidator;

  @MockitoBean
  private UserSyncService userSyncService;

  private static final UUID METER_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID READING_ID = UUID.randomUUID();
  private static final String KC_SUBJECT = "kc-subject-test";
  private static final String BEARER_TOKEN = "Bearer test-jwt-token";

  @BeforeEach
  void setUpAuth() throws Exception {
    JWTClaimsSet claims = new JWTClaimsSet.Builder()
        .subject(KC_SUBJECT)
        .claim("email", "test@example.com")
        .claim("name", "Test User")
        .build();

    when(jwksValidator.validate("test-jwt-token")).thenReturn(claims);

    User dbUser = User.builder()
        .id(USER_ID)
        .keycloakSubject(KC_SUBJECT)
        .email("test@example.com")
        .role(Role.USER)
        .build();

    when(userSyncService.syncUser(eq(KC_SUBJECT), eq("test@example.com"), eq("Test User"), any()))
        .thenReturn(dbUser);
  }

  @Test
  void submitReading_shouldReturn201WithConsumption() throws Exception {
    MeterReadingResponse serviceResponse = MeterReadingResponse.builder()
        .id(READING_ID)
        .meterId(METER_ID)
        .value(new BigDecimal("250.000"))
        .readingYear(2025)
        .readingMonth(5)
        .note("May reading")
        .submittedBy(KC_SUBJECT)
        .createdAt(LocalDateTime.of(2025, 5, 15, 10, 0))
        .monthlyConsumption(new BigDecimal("50.000"))
        .build();

    when(meterReadingService.submitReading(eq(METER_ID), any(), any(AuthenticatedUser.class)))
        .thenReturn(serviceResponse);

    String requestBody = """
        {
          "value": 250.000,
          "readingYear": 2025,
          "readingMonth": 5,
          "note": "May reading"
        }
        """;

    mockMvc.perform(post("/api/v1/meters/{meterId}/readings", METER_ID)
            .header("Authorization", BEARER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(READING_ID.toString()))
        .andExpect(jsonPath("$.meterId").value(METER_ID.toString()))
        .andExpect(jsonPath("$.value").value(250.000))
        .andExpect(jsonPath("$.readingYear").value(2025))
        .andExpect(jsonPath("$.readingMonth").value(5))
        .andExpect(jsonPath("$.note").value("May reading"))
        .andExpect(jsonPath("$.monthlyConsumption").value(50.000));
  }

  @Test
  void submitReading_shouldReturn400_whenValidationFails() throws Exception {
    String invalidRequestBody = """
        {
          "readingYear": 2025,
          "readingMonth": 13
        }
        """;

    mockMvc.perform(post("/api/v1/meters/{meterId}/readings", METER_ID)
            .header("Authorization", BEARER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidRequestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Validation failed")));
  }

  @Test
  void submitReading_shouldReturn401_whenNoAuthorizationHeader() throws Exception {
    String requestBody = """
        {
          "value": 100.000,
          "readingYear": 2025,
          "readingMonth": 1
        }
        """;

    mockMvc.perform(post("/api/v1/meters/{meterId}/readings", METER_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isUnauthorized());
  }
}
