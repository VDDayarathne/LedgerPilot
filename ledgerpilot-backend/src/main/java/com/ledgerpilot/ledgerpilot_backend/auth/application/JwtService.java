package com.ledgerpilot.ledgerpilot_backend.auth.application;

import com.ledgerpilot.ledgerpilot_backend.auth.config.AuthProperties;
import com.ledgerpilot.ledgerpilot_backend.identity.domain.UserAccount;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.List;

@Service
public class JwtService {
    private final JwtEncoder encoder;
    private final AuthProperties props;

    public JwtService(JwtEncoder encoder, AuthProperties props) {
        this.encoder = encoder;
        this.props = props;
    }

    public String accessToken(UserAccount user, String tenantId, String sessionId) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder().issuer(props.issuer()).audience(List.of(props.audience())).issuedAt(now)
                .expiresAt(now.plus(Duration.ofMinutes(props.accessTokenMinutes()))).subject(user.getId()).claim("sid", sessionId)
                .claim("email", user.getEmail());
        if (tenantId != null) claims.claim("tid", tenantId);
        JwsHeader header=JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header,claims.build())).getTokenValue();
    }
}
