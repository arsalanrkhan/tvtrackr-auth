package com.tvtrackr.auth.service.impl;

import com.tvtrackr.auth.entity.User;
import com.tvtrackr.auth.exception.AuthErrors;
import com.tvtrackr.auth.repository.UserRepository;
import com.tvtrackr.auth.service.UserService;
import com.tvtrackr.common.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
  private final UserRepository userRepository;

  @Override
  public User getUserByUsernameOrEmail(String username, String email) {
    return userRepository
        .findByEmailOrUsername(email, username)
        .orElseThrow(() -> new BusinessException(AuthErrors.INVALID_CREDENTIALS));
  }

  @Override
  public User getUserByEmail(String email) {
    return userRepository.findByEmail(email)
        .orElseThrow(() -> new BusinessException(AuthErrors.USER_NOT_FOUND));
  }

  @Override
  public User getUserById(Long id) {
    return userRepository.findById(id)
        .orElseThrow(() -> new BusinessException(AuthErrors.USER_NOT_FOUND));
  }

  @Override
  public User save(User user) {
    return userRepository.save(user);
  }

  @Override
  public boolean existsByUsername(String username) {
    return userRepository.existsByUsername(username);
  }
}
