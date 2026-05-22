package com.tvtrackr.auth.validator;

import static com.tvtrackr.auth.exception.AuthErrors.EMAIL_ALREADY_EXISTS;
import static com.tvtrackr.auth.exception.AuthErrors.USERNAME_ALREADY_EXISTS;

import com.tvtrackr.auth.dto.req.RegisterRequest;
import com.tvtrackr.auth.repository.UserRepository;
import com.tvtrackr.common.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RegisterRequestValidator {
  private final UserRepository userRepository;

  public void validate(RegisterRequest request) {
    if (userRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
      throw new BusinessException(EMAIL_ALREADY_EXISTS);
    }
    if (userRepository.existsByUsernameCaseInsensitive(request.getUsername().trim())) {
      throw new BusinessException(USERNAME_ALREADY_EXISTS);
    }
  }
}
