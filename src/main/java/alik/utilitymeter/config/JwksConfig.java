package alik.utilitymeter.config;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.net.URL;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class JwksConfig {

  private final KeycloakProperties keycloakProperties;

  @Bean
  public JWKSource<SecurityContext> jwkSource() throws Exception {
    log.debug("Creating JWKSource bean from Keycloak JWKS URL: {}", keycloakProperties.getJwksUri());
    URL jwksUrl = URI.create(keycloakProperties.getJwksUri()).toURL();
    return new RemoteJWKSet<>(jwksUrl);
  }
}

