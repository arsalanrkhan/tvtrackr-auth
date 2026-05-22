package com.tvtrackr.auth.repository;

import com.tvtrackr.auth.entity.RefreshToken;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  Optional<RefreshToken> findByToken(String token);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT r FROM RefreshToken r WHERE r.token = :token")
  Optional<RefreshToken> findByTokenForUpdate(@Param("token") String token);

  List<RefreshToken> findAllByUserIdAndRevokedFalse(Long userId);
}
