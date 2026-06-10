package alik.utilitymeter.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties {

  private String baseUrl;
  private String realm;

  public String getJwksUri() {
    return String.format("%s/realms/%s/protocol/openid-connect/certs", baseUrl, realm);
  }

  public String getIssuer() {
    return String.format("%s/realms/%s", baseUrl, realm);
  }
}