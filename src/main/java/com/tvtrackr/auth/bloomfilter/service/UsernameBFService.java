package com.tvtrackr.auth.bloomfilter.service;

public interface UsernameBFService {
  void add(String username);

  boolean mightExist(String username);

  boolean isSeeded();

  void markSeeded();

  void clear();
}
