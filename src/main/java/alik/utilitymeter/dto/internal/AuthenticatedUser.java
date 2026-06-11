package alik.utilitymeter.dto.internal;

import alik.utilitymeter.enums.Role;
import java.util.UUID;

public record AuthenticatedUser(
    UUID id,
    String keycloakSubject,
    String email,
    Role role
) {
  public boolean isAdmin() {
    return role == Role.ADMIN;
  }
}
