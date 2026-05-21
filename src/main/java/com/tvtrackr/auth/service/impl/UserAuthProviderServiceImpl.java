package com.tvtrackr.auth.service.impl;

import com.tvtrackr.auth.entity.UserAuthProvider;
import com.tvtrackr.auth.repository.UserAuthProviderRepository;
import com.tvtrackr.auth.service.UserAuthProviderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserAuthProviderServiceImpl implements UserAuthProviderService {

  private final UserAuthProviderRepository userAuthProviderRepository;

  @Override
  public UserAuthProvider save(UserAuthProvider userAuthProvider) {
    return userAuthProviderRepository.save(userAuthProvider);
  }
}
