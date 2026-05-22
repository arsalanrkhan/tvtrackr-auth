package com.tvtrackr.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "resend")
public class ResendProperties {
  private String url;
  private String apiKey;
  private String fromEmail;
  private String fromName = "TVTrackr";
}
