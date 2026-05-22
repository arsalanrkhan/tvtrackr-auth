package com.tvtrackr.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(ResendProperties.class)
public class ResendClientConfig {

  @Bean
  public RestClient resendRestClient(ResendProperties resendProperties) {
    return RestClient.builder()
        .baseUrl(resendProperties.getUrl())
        .defaultHeader("Authorization", "Bearer " + resendProperties.getApiKey())
        .build();
  }
}
