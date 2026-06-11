package alik.utilitymeter.service;

import alik.utilitymeter.entity.User;
import alik.utilitymeter.enums.Role;
import alik.utilitymeter.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSyncService {

  private final UserRepository userRepository;

  @Transactional
  public User syncUser(String subject, String email, String fullName, List<String> roles) {
    return userRepository.findByKeycloakSubject(subject)
        .orElseGet(() -> {
          try {
            log.info("First login detected. Creating user profile: {}", email);
            return userRepository.saveAndFlush(User.builder()
                .keycloakSubject(subject)
                .email(email)
                .fullName(fullName)
                .role(roles.contains("ADMIN") ? Role.ADMIN : Role.USER)
                .build());
          } catch (DataIntegrityViolationException e) {
            log.debug("Concurrent identity insert detected. Fetching fallback reference tracking record.");
            // Fallback block for concurrency
            return userRepository.findByKeycloakSubject(subject)
                .orElseThrow(() -> new IllegalStateException("Failed to recover user synchronization record."));
          }
        });
  }
}