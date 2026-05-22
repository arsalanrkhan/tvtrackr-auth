package com.tvtrackr.auth.service;

public interface EmailService {

  void send(String to, String subject, String html);
}
