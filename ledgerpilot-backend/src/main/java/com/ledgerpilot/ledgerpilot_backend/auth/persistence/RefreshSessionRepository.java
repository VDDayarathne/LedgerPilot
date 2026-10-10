package com.ledgerpilot.ledgerpilot_backend.auth.persistence;

import com.ledgerpilot.ledgerpilot_backend.auth.domain.RefreshSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.*;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, String> {
    Optional<RefreshSession> findByIdAndUserId(String id, String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from RefreshSession s where s.currentTokenHash=:hash or s.previousTokenHash=:hash")
    Optional<RefreshSession> lockByTokenHash(@Param("hash") String hash);

    @Modifying
    @Query("update RefreshSession s set s.revokedAt=:now where s.user.id=:userId and s.revokedAt is null")
    int revokeAllForUser(String userId, Instant now);

    @Modifying
    @Query("update RefreshSession s set s.revokedAt=:now where s.familyId=:family and s.revokedAt is null")
    int revokeFamily(String family, Instant now);
}
