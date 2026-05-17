package com.tvtrackr.auth.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Entity
@Table(name = "refresh_tokens", schema = "auth")
@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
public class RefreshToken extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "token", nullable = false)
  private String token;

  @Column(name = "revoked", nullable = false)
  private boolean revoked = false;

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  public boolean isExpired() {
    return this.expiresAt == null || LocalDateTime.now().isAfter(this.expiresAt);
  }
}
