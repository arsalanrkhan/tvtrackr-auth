package com.tvtrackr.auth.repository;

import com.tvtrackr.auth.entity.RefreshToken;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  Optional<RefreshToken> findByToken(String token);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT r FROM RefreshToken r WHERE r.token = :token")
  Optional<RefreshToken> findByTokenForUpdate(@Param("token") String token);

  @Modifying
  @Query(
      "UPDATE RefreshToken r SET r.revoked = true WHERE r.user.id = :userId AND r.revoked = false")
  void revokeAllByUserId(@Param("userId") Long userId);

  @Modifying
  @Query(
      value =
          "DELETE FROM RefreshToken WHERE id IN "
              + "(SELECT id FROM RefreshToken WHERE revoked = true OR expires_at < :cutoff LIMIT :chunkSize)",
      nativeQuery = true)
  int deleteExpiredOrRevokedChunk(
      @Param("cutoff") LocalDateTime cutoff, @Param("chunkSize") int chunkSize);
}
