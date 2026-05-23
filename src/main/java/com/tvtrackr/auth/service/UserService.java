package com.tvtrackr.auth.service;

import com.tvtrackr.auth.entity.User;

import java.time.LocalDateTime;

public interface UserService {
  User getUserByUsernameOrEmail(String username, String email);

  User getUserByEmail(String email);

  User getUserById(Long id);

  User save(User user);

  boolean existsByUsername(String username);

  int deleteUnverifiedUsersWithCutoffInChunk(LocalDateTime cutoff, int chunkSize);
}
