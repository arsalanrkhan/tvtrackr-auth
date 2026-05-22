package com.tvtrackr.auth.config;

import jakarta.annotation.PostConstruct;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

  @Setter private String privateKey;
  @Setter private String publicKey;
  @Getter @Setter private long accessTokenExpiryMs;

  @Getter private RSAPrivateKey rsaPrivateKey;
  @Getter private RSAPublicKey rsaPublicKey;

  @PostConstruct
  public void init() {
    try {
      byte[] privateDecoded = Base64.getDecoder().decode(privateKey);
      KeyFactory kf = KeyFactory.getInstance("RSA");
      this.rsaPrivateKey =
          (RSAPrivateKey) kf.generatePrivate(new PKCS8EncodedKeySpec(privateDecoded));

      byte[] publicDecoded = Base64.getDecoder().decode(publicKey);
      this.rsaPublicKey = (RSAPublicKey) kf.generatePublic(new X509EncodedKeySpec(publicDecoded));
    } catch (Exception e) {
      throw new IllegalStateException("Failed to load RSA keys", e);
    }
  }
}
