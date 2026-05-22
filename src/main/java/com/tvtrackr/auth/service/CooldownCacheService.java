package com.tvtrackr.auth.service;

public interface CooldownCacheService {

  boolean isOnCooldown(String key);

  void setCooldown(String key, Long ttl);
}
