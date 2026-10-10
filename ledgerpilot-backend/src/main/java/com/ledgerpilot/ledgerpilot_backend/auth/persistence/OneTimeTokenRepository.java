package com.ledgerpilot.ledgerpilot_backend.auth.persistence;

import com.ledgerpilot.ledgerpilot_backend.auth.domain.OneTimeToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;

import java.util.*;

public interface OneTimeTokenRepository extends JpaRepository<OneTimeToken, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OneTimeToken> findByTokenHashAndType(String hash, OneTimeToken.Type type);

    List<OneTimeToken> findAllByUserIdAndTypeAndUsedAtIsNull(String userId, OneTimeToken.Type type);
}
