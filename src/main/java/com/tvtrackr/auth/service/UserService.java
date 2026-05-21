package com.tvtrackr.auth.service;

import com.tvtrackr.auth.entity.User;

public interface UserService {
  User getUserByUsernameOrEmail(String username, String email);

  User getUserByEmail(String email);

  User getUserById(Long id);

  User save(User user);
}
