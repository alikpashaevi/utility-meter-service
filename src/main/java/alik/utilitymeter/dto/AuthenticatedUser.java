package alik.utilitymeter.dto;

import alik.utilitymeter.enums.Role;

public record AuthenticatedUser(
    String id,
    String email,
    Role role
) {
  public boolean isAdmin() {
    return role == Role.ADMIN;
  }
}
