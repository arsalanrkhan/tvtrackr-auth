package com.tvtrackr.auth.service.impl;

import com.tvtrackr.auth.entity.User;
import com.tvtrackr.auth.exception.AuthErrors;
import com.tvtrackr.auth.repository.UserRepository;
import com.tvtrackr.auth.service.UserService;
import com.tvtrackr.common.error.BusinessException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
  private final UserRepository userRepository;

  @Override
  public User getUserByUsernameOrEmail(String username, String email) {
    return userRepository
        .findByEmailOrUsernameCaseInsensitive(email.toLowerCase().trim(), username.trim())
        .orElseThrow(() -> new BusinessException(AuthErrors.INVALID_CREDENTIALS));
  }

  @Override
  public User getUserByEmail(String email) {
    return userRepository
        .findByEmail(email.toLowerCase().trim())
        .orElseThrow(() -> new BusinessException(AuthErrors.USER_NOT_FOUND));
  }

  @Override
  public User getUserById(Long id) {
    return userRepository
        .findById(id)
        .orElseThrow(() -> new BusinessException(AuthErrors.USER_NOT_FOUND));
  }

  @Override
  public User save(User user) {
    return userRepository.save(user);
  }

  @Override
  public boolean existsByUsername(String username) {
    return userRepository.existsByUsernameCaseInsensitive(username.trim());
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public int deleteUnverifiedUsersWithCutoffInChunk(LocalDateTime cutoff, int chunkSize) {
    return userRepository.deleteAllUnverifiedBeforeChunk(cutoff, chunkSize);
  }
}
