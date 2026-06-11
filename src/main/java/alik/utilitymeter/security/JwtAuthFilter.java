package alik.utilitymeter.security;

import alik.utilitymeter.dto.AuthenticatedUser;
import alik.utilitymeter.entity.User;
import alik.utilitymeter.service.UserSyncService;
import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

  private final JwksValidator jwksValidator;
  private final UserSyncService userSyncService;

  private static final List<String> PUBLIC_PATHS = List.of(
      "/actuator/health",
      "/actuator/info",
      "/actuator/prometheus",
      "/swagger-ui",
      "/api-docs"
  );

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain)
      throws ServletException, IOException {

    String authHeader = request.getHeader("Authorization");

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      sendUnauthorized(response, "Missing or invalid Authorization header");
      return;
    }

    String token = authHeader.substring(7);

    try {
      JWTClaimsSet claims = jwksValidator.validate(token);

      String subject = claims.getSubject();
      String email = (String) claims.getClaim("email");
      String name = (String) claims.getClaim("name");
      List<String> roles = extractRoles(claims);

      User user = userSyncService.syncUser(subject, email, name, roles);

      request.setAttribute("currentUser", new AuthenticatedUser(
          user.getId().toString(), user.getEmail(), user.getRole()
      ));

      filterChain.doFilter(request, response);

    } catch (Exception e) {
      log.warn("JWT validation failed: {}", e.getMessage());
      sendUnauthorized(response, "Invalid or expired token");
    }
  }

  @SuppressWarnings("unchecked")
  private List<String> extractRoles(JWTClaimsSet claims) {
    try {
      Map<String, Object> realmAccess =
          (Map<String, Object>) claims.getClaim("realm_access");
      if (realmAccess == null) return List.of();
      return (List<String>) realmAccess.get("roles");
    } catch (Exception e) {
      return List.of();
    }
  }

  private void sendUnauthorized(HttpServletResponse response, String message)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json");
    response.getWriter().write(
        """
        {"error": "Unauthorized", "message": "%s"}
        """.formatted(message));
  }
}