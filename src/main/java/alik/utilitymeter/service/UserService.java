package alik.utilitymeter.service;

import alik.utilitymeter.entity.User;
import alik.utilitymeter.exception.NotFoundException;
import alik.utilitymeter.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

  private final UserRepository userRepository;

  public User getUserById(UUID id) {
    return userRepository.findById(id).orElseThrow(() -> new NotFoundException("User with ID: " + id + " is not found"));
  }

}
