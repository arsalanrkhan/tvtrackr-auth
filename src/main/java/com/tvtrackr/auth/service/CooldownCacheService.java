package com.tvtrackr.auth.service;

public interface CooldownCacheService {

  boolean trySetCooldown(String key, long ttlMs);
}
