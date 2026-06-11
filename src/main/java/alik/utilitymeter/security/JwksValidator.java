package alik.utilitymeter.security;

import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwksValidator {

  private final JWKSource<SecurityContext> jwkSource;

  public JWTClaimsSet validate(String token) throws Exception {
    SignedJWT signedJWT = SignedJWT.parse(token);

    JWKMatcher matcher = new JWKMatcher.Builder()
        .keyID(signedJWT.getHeader().getKeyID())
        .build();

    JWKSelector selector = new JWKSelector(matcher);
    List<JWK> keys = jwkSource.get(selector, null);

    if (keys.isEmpty()) {
      throw new Exception("No matching key found in JWKS");
    }

    RSAKey rsaKey = (RSAKey) keys.getFirst();
    JWSVerifier verifier = new RSASSAVerifier(rsaKey.toRSAPublicKey());

    if (!signedJWT.verify(verifier)) {
      throw new Exception("JWT signature verification failed");
    }

    JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

    if (claims.getExpirationTime().before(new Date())) {
      throw new Exception("Token has expired");
    }

    return claims;
  }
}