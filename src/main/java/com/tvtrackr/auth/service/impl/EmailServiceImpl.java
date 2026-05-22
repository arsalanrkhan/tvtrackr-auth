package com.tvtrackr.auth.service.impl;

import com.tvtrackr.auth.config.ResendProperties;
import com.tvtrackr.auth.exception.AuthErrors;
import com.tvtrackr.auth.service.EmailService;
import com.tvtrackr.common.error.BusinessException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

  private final RestClient resendRestClient;
  private final ResendProperties resendProperties;

  @Override
  public void send(String to, String subject, String html) {
    Map<String, Object> payload =
        Map.of(
            "from",
            resendProperties.getFromName() + " <" + resendProperties.getFromEmail() + ">",
            "to",
            List.of(to),
            "subject",
            subject,
            "html",
            html);

    try {
      resendRestClient
          .post()
          .uri("/emails")
          .contentType(MediaType.APPLICATION_JSON)
          .body(payload)
          .retrieve()
          .toBodilessEntity();
      log.info("[Email] Email sent to {}", to);
    } catch (Exception e) {
      log.error("[Email] Failed to send email to {}: {}", to, e.getMessage());
      throw new BusinessException(AuthErrors.EMAIL_SEND_FAILED);
    }
  }
}
