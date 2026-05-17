package com.tvtrackr.auth.service;

import com.tvtrackr.auth.entity.User;

public interface UserService {
  User getUserByUsernameOrEmail(String username, String email);

  User save(User user);
}
